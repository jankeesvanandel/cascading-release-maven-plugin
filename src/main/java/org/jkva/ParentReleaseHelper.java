package org.jkva;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.*;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ParentReleaseHelper {

    private ProcessFactory processFactory;
    private Config config;
    private MavenSession session;
    private MavenProject project;
    private Log log;
    private ConfigUtil configUtil;
    private final ReleasedModuleTracker releasedModuleTracker;

    public ParentReleaseHelper(ProcessFactory processFactory, Config config, MavenSession session,
                               MavenProject project, Log log, ConfigUtil configUtil, ReleasedModuleTracker releasedModuleTracker) {
        this.processFactory = processFactory;
        this.config = config;
        this.session = session;
        this.project = project;
        this.log = log;
        this.configUtil = configUtil;
        this.releasedModuleTracker = releasedModuleTracker;
    }

    /**
     * Release the parent pom if there is a snapshot dependency to it. Future idea: validate if all modules are up to
     * date (point to the latest snapshot)???
     */
    public void releaseParentIfNeeded() throws MojoFailureException, MojoExecutionException, IOException {
        final Map<String, Set<String>> versionsPerParentPom = validateThatAllModulesShareTheSameParentVersion();
        final String parentPath = config.getParentPath();

        if (!versionsPerParentPom.isEmpty() && parentPath != null) {
            final MavenProject parentProject = configUtil.getMavenProjectFromPath(parentPath);
            final String latestParentVersion = parentProject.getVersion();
            final String usedParentVersion = getUsedVersionForProject(parentProject, versionsPerParentPom);

            if (!usedParentVersion.equals(latestParentVersion)) {
                String input = PromptUtil.promptWithDefault("Project parent [" + usedParentVersion + "] is not " +
                        "pointing to the latest version [" + latestParentVersion + "]. This doesn't have to be an " +
                        "issue, the release will be made with the current parent version. Continue release?", "y");
                if (!input.isEmpty() && !input.equalsIgnoreCase("y")) {
                    throw new MojoFailureException("Release aborted by user");
                }
            }

            final Artifact parentArtifact = project.getParentArtifact();
            if (parentArtifact.isSnapshot()) {
                final String groupId = parentArtifact.getGroupId();
                final String artifactId = parentArtifact.getArtifactId();
                if (!releasedModuleTracker.containsReleasedModule(groupId, artifactId)) {
                    releaseParent(parentArtifact, parentPath);
                }
                updateChildProjectsWithLatestParentVersion();
            }
        }
    }

    private String getUsedVersionForProject(MavenProject parent, Map<String, Set<String>> versionsPerParentPom)
            throws MojoFailureException {
        final String key = ConfigUtil.createProjectIdentifier(parent);
        final Set<String> versions = versionsPerParentPom.get(key);
        if (versions != null && !versions.isEmpty()) {
            return versions.iterator().next();
        } else {
            throw new MojoFailureException("Versions is empty");
        }
    }

    private Map<String, Set<String>> validateThatAllModulesShareTheSameParentVersion() throws MojoFailureException {
        final Map<String, Set<String>> versionsPerParentPom = new HashMap<String, Set<String>>();
        final List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            final MavenProject parent = project.getParent();
            if (parent != null) {
                final String key = ConfigUtil.createProjectIdentifier(parent);
                Set<String> versions = versionsPerParentPom.get(key);
                if (versions == null) {
                    versions = new HashSet<String>();
                    versionsPerParentPom.put(key, versions);
                }

                versions.add(parent.getVersion());
            }
        }
        log.info("All parent versions among modules: " + versionsPerParentPom);
        if (thereIsOnlyOneVersionPerParentArtifact(versionsPerParentPom)) {
            log.info("All modules use the same parent, continuing with release...");
        } else {
            String cont = PromptUtil.promptWithDefault("Not all modules in the project share the same parent. Continue? (Yn)", "Y");
            if (cont.equalsIgnoreCase("n")) {
                throw new MojoFailureException("Not all modules in the project share the same parent. Release aborted");
            } else {
                log.info("Continuing with release...");
            }
        }
        return versionsPerParentPom;
    }

    private boolean thereIsOnlyOneVersionPerParentArtifact(Map<String, Set<String>> versionsPerParentPom) {
        boolean oneVersionPerArtifact = true;
        final Set<Map.Entry<String, Set<String>>> entries = versionsPerParentPom.entrySet();
        for (Map.Entry<String, Set<String>> entry : entries) {
            final Set<String> versions = entry.getValue();
            if (versions.size() > 1) {
                final String parent = entry.getKey();
                log.info("Multiple used versions found for parent POM: " + parent + ". Versions: " + versions);
                oneVersionPerArtifact = false;
            }
        }
        return oneVersionPerArtifact;
    }

    /*
     * release the parent pom. first ask the user. then run clean install to make sure all tests pass etc scm:validate
     * will check the validity of the scm tags
     * TODO: Future idea: Somehow validate the DistributionManagement section and nexus authorisations, maybe with a
     * dummy deploy-file command? Goal is to check that the nexus credentials are there and the password is up to date.
     */
    private void releaseParent(Artifact parentArtifact, String parentPath) throws MojoFailureException, MojoExecutionException, IOException {
        releaseModule("Parent", parentPath, parentArtifact);
    }

    private void releaseModule(final String moduleName, final String path, Artifact parentArtifact) throws MojoFailureException {
        String input = PromptUtil.promptWithDefault(moduleName + " dependency is snapshot. Release?", "y");
        if (input.equalsIgnoreCase("n")) {
            throw new MojoFailureException("Aborted by user");
        }
        MavenInvoker mavenInvoker = processFactory.createMavenInvoker(path);

        int exitCode =
                mavenInvoker
                        .execute("clean install scm:update release:prepare release:perform --batch-mode -DautoVersionSubmodules=true");
        log.info(moduleName + " release exited with code " + exitCode);
        releasedModuleTracker.addReleasedModule(parentArtifact.getGroupId(), parentArtifact.getArtifactId(), parentArtifact.getVersion());
    }

    /**
     * Update all child poms. Commit the child poms.
     */
    private void updateChildProjectsWithLatestParentVersion() throws IOException, MojoFailureException {
        for (ProjectModule module : config.getModules()) {
            if (module.getParent() == null) {
                MavenInvoker mavenInvoker = processFactory.createMavenInvoker(configUtil.getFullPathFromBase(module, config.getBasedir()));

                // TODO: Add the version number of the parent to the commit message
                int exitCode =
                        mavenInvoker
                                .execute("versions:update-parent versions:commit scm:checkin -Dmessage=\"Update_parent_to_latest_release_version\"");
                log.info("Update parent for " + module.getGroupId() + ":" + module.getArtifactId()
                        + ". Exit code=" + exitCode);
            }
        }
    }
}

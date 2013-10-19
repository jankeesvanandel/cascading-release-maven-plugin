package nl.jkva;

import java.io.Console;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.ImmutableList;

import static nl.jkva.ConfigUtil.*;

public class CascadingDependencyReleaseHelper {
    private final ProcessFactory processFactory;
    private final Config config;
    private final MavenSession session;
    private final Log log;

    private final Set<ProjectModule> releasedModules = new HashSet<ProjectModule>();
    private final ConfigUtil configUtil;

    public CascadingDependencyReleaseHelper(ProcessFactory processFactory, Config config, MavenSession session, Log log, ConfigUtil configUtil) {
        this.processFactory = processFactory;
        this.config = config;
        this.session = session;
        this.log = log;
        this.configUtil = configUtil;
    }

    public void releaseDependencies(MavenProject project) throws MojoFailureException {
        log.info("About to release " + project.toString());
        Set<ProjectModule> releasableModules = determineReleasableModules(project);

        for (ProjectModule releasedModule : releasedModules) {
            updateVersionsInProjectForModule(project, releasedModule);
        }

        for (ProjectModule module : releasableModules) {
            log.info("Releasing SNAPSHOT module " + module.getArtifactId());
            releaseModuleAndUpdateDependencies(module);
        }
    }

    private Set<ProjectModule> determineReleasableModules(MavenProject mavenProject) throws MojoFailureException {
        List<Dependency> allModuleDependencies = configUtil.getDependenciesForAllModules(mavenProject);

        //LinkedHashSet to retain the correct order
        Set<ProjectModule> releasableDependencies = new LinkedHashSet<ProjectModule>();
        Set<Dependency> nonReleasableDependencies = new LinkedHashSet<Dependency>();
        for (Dependency dependency : allModuleDependencies) {
            if (isSnapshot(dependency)) {
                final ProjectModule module = getReleasableDependency(dependency);
                // If it's in the JSON file, it might be releasable, otherwise it's not.
                if (module != null) {
                    if (!isMultiModuleBuildInternalDependency(dependency, mavenProject)) {
                        ProjectModule moduleToRelease;
                        if (module.getReleasableParent() != null) {
                            moduleToRelease = module.getReleasableParent();
                        } else {
                            moduleToRelease = module;
                        }
                        if (!releasedModules.contains(moduleToRelease)) {
                            releasableDependencies.add(moduleToRelease);
                        }
                    }
                } else {
                    nonReleasableDependencies.add(dependency);
                }
            }
        }
        log.info("Dependencies that need to be released: ");
        for (ProjectModule releasableDependency : releasableDependencies) {
            log.info(" + " + createProjectIdentifier(releasableDependency));
        }
        if (!nonReleasableDependencies.isEmpty()) {
            throw new MojoFailureException("Cannot release because of external SNAPSHOT dependencies: [" + nonReleasableDependencies + "]");
        }

        return releasableDependencies;
    }

    private boolean isMultiModuleBuildInternalDependency(Dependency dependency, MavenProject mavenProject) throws MojoFailureException {
        final String dependencyKey = createProjectIdentifier(dependency);
        final List<MavenProject> allModules = configUtil.getAllModules(mavenProject);
        for (MavenProject module : allModules) {
            if (createProjectIdentifier(module).equals(dependencyKey)) {
                return true;
            }
        }
        return false;
    }

    private ProjectModule getReleasableDependency(Dependency dependency) {
        final ProjectModule module = configUtil.getModuleForDependency(dependency);

        return module;
    }

    private boolean isSnapshot(Dependency dependency) {
        final String version = dependency.getVersion();
        return isSnapshot(version);
    }

    private boolean isSnapshot(String version) {
        return version.contains("-SNAPSHOT");
    }

    public void releaseModuleAndUpdateDependencies(ProjectModule module) throws MojoFailureException {
        releaseModule(module);

        updateProjectsWithLatestDependencyVersions(module);
    }

    private void releaseModule(ProjectModule module) throws MojoFailureException {
        // release module
        final String path = configUtil.getFullPathFromBase(module);
        Console console = System.console();
        String input = console.readLine(createProjectIdentifier(module) + " dependency is snapshot. Release? [y]:");
        if (!input.isEmpty() && !input.equalsIgnoreCase("y")) {
            throw new MojoFailureException("Aborted by user");
        }

        MavenProject mavenProject = configUtil.getMavenProjectFromPath(path);

        final MavenProject parentProject = mavenProject.getParent();
        final ProjectModule parentModule = configUtil.getProjectModuleFromMavenProject(parentProject);
        releaseParentIfNeeded(parentModule, parentProject, module);

        MavenInvoker mavenInvoker = processFactory.createMavenInvoker(path);

        releaseDependencies(mavenProject);

        int exitCode = mavenInvoker.execute(
                "clean install scm:validate release:prepare release:perform --batch-mode -DautoVersionSubmodules=true");
        final String releasedVersion = getReleasedVersionNumberFromProcess(module, mavenInvoker.getOutput());
        module.setReleasedVersion(releasedVersion);

        log.info(createProjectIdentifier(module) + " release exited with code " + exitCode);
        releasedModules.add(module);
    }

    private void releaseParentIfNeeded(ProjectModule parentModule, MavenProject parentProject, ProjectModule module) throws MojoFailureException {
        final String parentVersion = parentProject.getVersion();
        if (isSnapshot(parentVersion)) {
            releaseModule(parentModule);
            updateChildren(parentModule, module);
        }
    }

    private void updateChildren(ProjectModule parentModule, ProjectModule module) throws MojoFailureException {
        final String path = configUtil.getFullPathFromBase(module);
        MavenInvoker mavenInvoker = processFactory.createMavenInvoker(path);

        int exitCode = mavenInvoker.execute(
                "versions:update-parent versions:commit scm:checkin -Dmessage=\"Update_" +
                        parentModule.getArtifactId() + "_to_" + parentModule.getReleasedVersion() + "\"");
        log.info("Update dependency for " + parentModule.getGroupId() + ":" + parentModule.getArtifactId() + ". Exit code=" + exitCode);
    }

    private String getReleasedVersionNumberFromProcess(ProjectModule module, ImmutableList<String> output) throws MojoFailureException {
        final String uploadingKeyword = "Uploading";
        final String regex = ".* " + uploadingKeyword + ": .*\\/(.*)\\-(.*)\\.pom";
        //        final String regex = "\\[INFO\\]\\ \\[INFO\\]\\ Building " + module.getArtifactId() + " (.*)";
        final Pattern pattern = Pattern.compile(regex);
        for (String line : output) {
            // Simple check to make the scanning faster
            if (!line.contains(uploadingKeyword) || !line.contains(module.getArtifactId())) {
                continue;
            }

            final Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            final String artifactIdInOutput = matcher.group(1);
            if (!artifactIdInOutput.equals(module.getArtifactId())) {
                continue;
            }
            final String version = matcher.group(2);
            log.info("Found version number in release output: [" + version + "]");
            return version;
        }

        throw new MojoFailureException("Error extracting release version from Release Plugin output. Release aborted");
    }

    /**
     * Update all child poms.
     * Commit the child poms.
     * @param releasedModule
     */
    private void updateProjectsWithLatestDependencyVersions(ProjectModule releasedModule) throws MojoFailureException {
        for (ProjectModule module : configUtil.getFlatListOfAllModules()) {
            final MavenProject dependentMavenProject = configUtil.getMavenProjectFromPath(module.getPath());
            if (doesProjectContainReleasedModule(dependentMavenProject, releasedModule)) {
                updateVersionsInProjectForModule(dependentMavenProject, releasedModule);
            }

            final List<MavenProject> subModules = configUtil.getAllModules(dependentMavenProject);
            for (MavenProject dependentSubModule : subModules) {
                if (doesProjectContainReleasedModule(dependentSubModule, releasedModule)) {
                    updateVersionsInProjectForModule(dependentSubModule, releasedModule);
                }
            }
        }
    }

    private void updateVersionsInProjectForModule(MavenProject mavenProject, ProjectModule releasedModule) throws MojoFailureException {
        final ProjectModule module = configUtil.getProjectModuleFromMavenProject(mavenProject);
        final String path = configUtil.getFullPathFromBase(module);
        final MavenInvoker mavenInvoker = processFactory.createMavenInvoker(path);

        final String includeArg = getIncludedArtifactsForVersionPlugin(releasedModule);
        final String commitMsg = "Update_" + createProjectIdentifier(mavenProject) + "_to_" + createProjectIdentifier(releasedModule);
        int exitCode = mavenInvoker.execute("versions:update-properties versions:use-latest-versions versions:commit " + //
                "scm:checkin -Dmessage=\"" + commitMsg + "\" " + includeArg);
        log.info("Update dependency for " + mavenProject.getGroupId() + ":" + mavenProject.getArtifactId() + ". Exit code=" + exitCode);
    }

    private String getIncludedArtifactsForVersionPlugin(ProjectModule releasedModule) {
        final StringBuilder sb = new StringBuilder("-Dincludes=");
        sb.append(releasedModule.getGroupId());
        sb.append(":");
        sb.append(releasedModule.getArtifactId());
        return sb.toString();
    }

    private boolean doesProjectContainReleasedModule(MavenProject mavenProject, ProjectModule releasedModule) {
        final String releasedModuleKey = createProjectIdentifier(releasedModule);
        final List<Dependency> dependencies = mavenProject.getDependencies();
        for (Dependency dependency : dependencies) {
            final String dependencyKey = createProjectIdentifier(dependency);
            if (releasedModuleKey.equals(dependencyKey)) {
                log.info("Found dependency to module [" + releasedModuleKey + "]: " + dependencyKey);
                return true;
            }
        }

        return false;
    }
}

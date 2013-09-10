package nl.jkva;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.google.gson.Gson;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Mojo(name = "cascading-release", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class CascadingReleaseMojo extends AbstractMojo {

    @Parameter(property = "version", required = true)
    private String version;

    @Parameter(property = "configFile", required = true, defaultValue = "release.json")
    private File configFile;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    private ProcessFactory processFactory;
    private ConfigFile config;

    public void execute() throws MojoExecutionException, MojoFailureException {
        config = readConfigFile();

        try {
            validateSystemSettings();
            validateConfigFile(config);
            validateCurrentWorkspace();
            releaseParentIfNeeded();
            releaseDependencies();
        } catch (IOException e) {
            throw new MojoFailureException("IO error", e);
        }
    }

    private void validateSystemSettings() throws MojoFailureException {
        validateHomeDir("M2_HOME", "mvn.bat", "Maven");
        validateHomeDir("SVN_HOME", "svn.exe", "SubVersion");
    }

    private void validateHomeDir(final String envVariable, final String executable, final String name) throws MojoFailureException {
        String variable = System.getenv(envVariable);
        if (StringUtils.isBlank(variable)) {
            throw new MojoFailureException("Environment variable "+envVariable+" not defined");
        }
        File path = new File(variable, "bin");
        if (!path.exists() || !path.isDirectory() || !path.canRead()) {
            throw new MojoFailureException("Environment variable "+envVariable+ " does not point to a valid " + name + " home (/bin dir does not exist or is not accessible)");
        }
        File exec = new File(path, executable);
        if (!exec.exists() || !exec.canExecute()) {
            throw new MojoFailureException("Environment variable "+envVariable+ " does not point to a valid " + name + " home (/bin/" + executable +" does not exist or is not executable)");
        }
    }

    /**
     * Validate the config file for mandatory parameters.
     * Validate the existence of directories.
     * @param config
     */
    private void validateConfigFile(ConfigFile config) {

    }

    /**
     * Validate & update current workspace.
     * svn check local modifications > error
     * svn up
     */
    private void validateCurrentWorkspace() throws MojoFailureException {
        int exitCode;
        exitCode = processFactory.createSvnInvoker("").execute("up");
        exitCode = processFactory.createSvnInvoker("").execute("st");
    }

    private ConfigFile readConfigFile() throws MojoFailureException {
        getLog().info("Reading file: " + configFile);
        if (!configFile.exists() || !configFile.canRead()) {
            throw new MojoFailureException("Invalid configFile. Does it exist?");
        }

        final ConfigFile config;
        try {
            String fileContents = IOUtil.toString(new FileInputStream(configFile));
            validateConfigFileWithJsonSchema(fileContents);
            config = new Gson().fromJson(fileContents, ConfigFile.class);
            File projectBase = new File(project.getBasedir(), config.getPathToBase());
            processFactory = new ProcessFactory(getLog(), projectBase);
        } catch (IOException e) {
            throw new MojoFailureException("Invalid configfile, valid JSON?", e);
        }
        return config;
    }

    private void validateConfigFileWithJsonSchema(String fileContents) throws MojoFailureException {
        try {
            final JsonNode schemaNode = JsonLoader.fromResource("/schema.json");
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final JsonSchema schema = factory.getJsonSchema(schemaNode);
            ProcessingReport report = schema.validate(JsonLoader.fromString(fileContents));
            if (!report.isSuccess()) {
                throw new MojoFailureException("Invalid release.json file");
            }
        } catch (IOException e) {
            throw new MojoFailureException("IOException", e);
        } catch (ProcessingException e) {
            throw new MojoFailureException("ProcecingException", e);
        }

    }

    /**
     * Release the parent pom if there is a snapshot dependency to it.
     * Future idea: validate if all modules are up to date (point to the latest snapshot)???
     */
    private void releaseParentIfNeeded() throws MojoFailureException, MojoExecutionException, IOException {
        final Artifact parentArtifact = project.getParentArtifact();
        if (parentArtifact.isSnapshot()) {
            releaseParentPom();
            updateChildProjectsWithLatestParentVersion();
        }
    }

    /**
     * release the parent pom.
     * first ask the user.
     * then run clean install to make sure all tests pass etc
     * scm:validate will check the validity of the scm tags TODO: verify what happens if not
     * Future idea: Somehow validate the distributionmanagement section and nexus authorisations, maybe with a dummy deploy-file command? Goal is to check that the nexus credentials are there and the password is up to date.
     */
    private void releaseParentPom() throws MojoFailureException, MojoExecutionException, IOException {
        releaseModule("Parent", config.getParentPathFromBase());
    }

    /**
     * Update all child poms.
     * Commit the child poms.
     */
    private void updateChildProjectsWithLatestParentVersion() throws IOException, MojoFailureException {
        for (Identifier moduleIdentifier : config.getDependenciesSorted()) {
            if (moduleIdentifier.getSubModuleOf() == null) {
                MavenInvoker mavenInvoker = processFactory.createMavenInvoker(moduleIdentifier.getPathFromBase());
                //TODO: fix this TODO
                int exitCode = mavenInvoker.execute(
                        "versions:update-parent versions:commit scm:checkin -Dmessage=\"Update parent to TODO LATEST release version\"");
                getLog().info("Update parent for " + moduleIdentifier.getGroupId() + ":" + moduleIdentifier.getArtifactId() + ". Exit code=" + exitCode);
            }
        }
    }

    private void releaseDependencies() throws MojoFailureException {
        Set<Artifact> dependencies = project.getDependencyArtifacts();
        Set<Identifier> releasedParentModules = new HashSet<Identifier>();
        for (Artifact dependency : dependencies) {
            if (dependency.isSnapshot()) {
                Identifier moduleIdentifier = getReleasableModule(dependency);
                if (moduleIdentifier != null) {
                    if (moduleIdentifier.getSubModuleOf() == null) {
                        String groupId = moduleIdentifier.getGroupId();
                        String artifactId = moduleIdentifier.getArtifactId();
                        releaseModule(groupId + ":" + artifactId, moduleIdentifier.getPathFromBase());
                    } else if (!isParentForSubModuleReleased(moduleIdentifier.getSubModuleOf(), releasedParentModules)) {
                        String pathFromBase = new File(moduleIdentifier.getPathFromBase(), "..").getPath();
                        String groupId = moduleIdentifier.getSubModuleOf().getGroupId();
                        String artifactId = moduleIdentifier.getSubModuleOf().getArtifactId();
                        releaseModule(groupId + ":" + artifactId, pathFromBase);
                    }
                    // release module
                    // update dependency version
                } else {
                    throw new MojoFailureException(String.format("Dependency to non-project SNAPSHOT: %s:%s:%s. Release this manually and try again", dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()));
                }
            }
        }
    }

    private boolean isParentForSubModuleReleased(Identifier subModuleOf, Set<Identifier> releasedParentModules) {
        for (Identifier releasedParentModule : releasedParentModules) {
            if (releasedParentModule.getGroupId().equals(subModuleOf.getGroupId())
             && releasedParentModule.getArtifactId().equals(subModuleOf.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private Identifier getReleasableModule(Artifact dependency) {
        Identifier[] dependenciesSorted = config.getDependenciesSorted();
        for (Identifier sortedDependency : dependenciesSorted) {
            if (sortedDependency.getGroupId().equals(dependency.getGroupId())
             && sortedDependency.getArtifactId().equals(dependency.getArtifactId())) {
                return sortedDependency;
            }
        }
        return null;
    }

    private void releaseModule(final String moduleName, final String path) throws MojoFailureException {
        Console console = System.console();
        String input = console.readLine(moduleName + " dependency is snapshot. Release? [y]:");
        if (input.equalsIgnoreCase("n")) {
            throw new MojoFailureException("Aborted by user");
        }
        MavenInvoker mavenInvoker = processFactory.createMavenInvoker(path);
        int exitCode = mavenInvoker.execute(
                "clean install scm:validate release:prepare release:perform --batch-mode -DautoVersionSubmodules=true");
        getLog().info(moduleName + " release exited with code " + exitCode);
    }


}

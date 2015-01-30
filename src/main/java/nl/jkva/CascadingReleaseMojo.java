package nl.jkva;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Mojo(name = "cascading-release", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, inheritByDefault = false, aggregator = true)
public class CascadingReleaseMojo extends AbstractMojo {

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    @Parameter(property = "basedir", required = true, defaultValue = "${basedir}")
    private File basedir;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/release-summary.txt", required = true)
    private File outputFile;

    @Parameter(property = "parentPath")
    private String parentPath;

    @Parameter(property = "distPath", defaultValue = "")
    private String distPath;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter( defaultValue = "${settings}", readonly = true, required = true )
    private Settings settings;

    private ProcessFactory processFactory;
    private Config config;
    private ConfigUtil configUtil;
    private ReleasedModuleTracker releasedModuleTracker;

    public void execute() throws MojoExecutionException, MojoFailureException {
        PromptUtil.settings = settings;
        ConfigFileReader configFileReader = new ConfigFileReader(getLog(), project);
        config = configFileReader.readConfigFile(parentPath, distPath, reactorProjects);
        config.setBasedir(basedir);
        String s = configFileReader.outputConfig(config);

        processFactory = new ProcessFactory(getLog(), config.getProjectBase());
        configUtil = new ConfigUtil(config, getLog(), session);
        releasedModuleTracker = new ReleasedModuleTracker(outputFile);

        try {
            validateSystemSettings();
            validateCurrentWorkspace();

            ParentReleaseHelper parentReleaseHelper = new ParentReleaseHelper(processFactory, config, session, project, getLog(), configUtil, releasedModuleTracker);
            parentReleaseHelper.releaseParentIfNeeded();

            CascadingDependencyReleaseHelper cascadingDependencyReleaseHelper = new CascadingDependencyReleaseHelper(processFactory, config, getLog(), configUtil, releasedModuleTracker);

            MavenProject releasableProject = configUtil.getMavenProjectFromPath(config.getDistPath());
            cascadingDependencyReleaseHelper.releaseDependencies(releasableProject);
            final ProjectModule distModule = configUtil.getProjectModuleFromMavenProject(releasableProject);
            cascadingDependencyReleaseHelper.releaseModuleAndUpdateDependencies(distModule);

            releasedModuleTracker.writeToFile();
        } catch (IOException e) {
            throw new MojoFailureException("IO error", e);
        }
    }

    private void validateSystemSettings() throws MojoFailureException {
        validateEnvVars("M2_BIN", "Maven");
    }

    private void validateEnvVars(final String envVariable, final String name) throws MojoFailureException {
        String variable = System.getenv(envVariable);
        if (StringUtils.isEmpty(variable)) {
            throw new MojoFailureException("Environment variable "+envVariable+" not defined");
        }
        File exec = new File(variable);
        getLog().info("Using " + name + ": " + variable);
        if (!exec.exists() || !exec.canExecute()) {
            throw new MojoFailureException("Environment variable "+envVariable+ " does not point to a valid " + name + " executable)" + exec);
        }
    }

    /**
     * Validate & update current workspace.
     * svn check local modifications > error
     * svn up
     */
    private void validateCurrentWorkspace() throws MojoFailureException {
        final MavenInvoker scmUpdateInvoker = processFactory.createMavenInvoker("");
        scmUpdateInvoker.execute("scm:update");

        final MavenInvoker scmStatusInvoker = processFactory.createMavenInvoker("");
        scmStatusInvoker.execute("scm:status");
        final ImmutableList<String> output = scmStatusInvoker.getOutput();
        final List<String> statusLines = extractScmStatusOutput(output);
        for (String statusLine : statusLines) {
            getLog().info("Local changes: " + statusLine);
        }
        if (!statusLines.isEmpty()) {
            throw new MojoFailureException("You have local changes. Release aborted");
        }
    }

    private List<String> extractScmStatusOutput(ImmutableList<String> output) {
        final List<String> statusLines = new ArrayList<String>();
        boolean started = false;
        for (String line : output) {
            getLog().debug("line: " + line);
            if (started) {
                if (line.startsWith("[INFO] --------------------------")) {
                    break;
                }
                if (isInterestingStatusLine(line)) {
                    statusLines.add(line);
                }
            }
            if (line.startsWith("[INFO] Working directory: ")) {
                started = true;
            }
        }
        return statusLines;
    }

    private boolean isInterestingStatusLine(final String line) {
        return !line.contains("[INFO] Executing: ")
            && !line.contains("[INFO] Working directory: ");
    }

}

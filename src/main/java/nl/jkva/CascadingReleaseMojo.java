package nl.jkva;

import java.io.File;
import java.io.IOException;

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
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.ImmutableList;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Mojo(name = "cascading-release", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, inheritByDefault = false, aggregator = true)
public class CascadingReleaseMojo extends AbstractMojo {

    @Parameter(property = "version", required = true)
    private String version;

    @Parameter(property = "configFile", required = true, defaultValue = "release.json")
    private File cfgFile;

    @Parameter(property = "basedir", required = true, defaultValue = "${basedir}")
    private File basedir;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    private ProcessFactory processFactory;
    private Config config;
    private ConfigUtil configUtil;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ///TODO: check if this can be removed
        if (!Util.isReactorRootProject(session, basedir)) {
            return;
        }

        ConfigFileReader configFileReader = new ConfigFileReader(getLog(), project);
        config = configFileReader.readConfigFile(cfgFile);
        configFileReader.outputConfig(config);

        processFactory = new ProcessFactory(getLog(), config.getProjectBase());
        configUtil = new ConfigUtil(config, getLog(), session);

        try {
            validateSystemSettings();
            validateCurrentWorkspace();
            ParentReleaseHelper parentReleaseHelper = new ParentReleaseHelper(processFactory, config, session, project, getLog(), configUtil);
            parentReleaseHelper.releaseParentIfNeeded();

            CascadingDependencyReleaseHelper cascadingDependencyReleaseHelper = new CascadingDependencyReleaseHelper(processFactory, config, session, getLog(), configUtil);

            //TODO: Create loop to release multiple EARs (and websphere plugins etc)
            if (true) {
                MavenProject releasableProject = configUtil.getMavenProjectFromPath(config.getDistPath());
                cascadingDependencyReleaseHelper.releaseDependencies(releasableProject);
                final ProjectModule distModule = configUtil.getProjectModuleFromMavenProject(releasableProject);
                cascadingDependencyReleaseHelper.releaseModuleAndUpdateDependencies(distModule);
            }

        } catch (IOException e) {
            throw new MojoFailureException("IO error", e);
        }
    }

    private void validateSystemSettings() throws MojoFailureException {
        validateEnvVars("M2_BIN", "Maven");
        validateEnvVars("SVN_BIN", "SubVersion");
    }

    private void validateEnvVars(final String envVariable, final String name) throws MojoFailureException {
        String variable = System.getenv(envVariable);
        if (StringUtils.isBlank(variable)) {
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
        final SvnInvoker svnUpInvoker = processFactory.createSvnInvoker("");
        svnUpInvoker.execute("up");

        final SvnInvoker svnStInvoker = processFactory.createSvnInvoker("");
        svnStInvoker.execute("st");
        final ImmutableList<String> output = svnStInvoker.getOutput();
        for (String s : output) {
            getLog().info(s);
        }
        if (!output.isEmpty()) {
            throw new MojoFailureException("You have local changes. Release aborted");
        }
    }

}

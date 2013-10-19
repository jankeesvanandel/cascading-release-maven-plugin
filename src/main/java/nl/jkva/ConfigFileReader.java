package nl.jkva;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.google.gson.Gson;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class ConfigFileReader {

    private final Log log;
    private final MavenProject project;

    public ConfigFileReader(Log log, MavenProject project) {
        this.log = log;
        this.project = project;
    }

    public Config readConfigFile(File configFile) throws MojoFailureException {
        log.info("Reading file: " + configFile);
        if (!configFile.exists() || !configFile.canRead()) {
            throw new MojoFailureException("Invalid configFile. Does it exist?");
        }

        final Config config;
        try {
            String fileContents = IOUtil.toString(new FileInputStream(configFile));
            validateConfigFileWithJsonSchema(fileContents);
            config = new Gson().fromJson(fileContents, Config.class);
            config.setProjectBase(project.getBasedir().getCanonicalFile());

            updateAutoDeterminedFields(config);

            validateConfigFile(config);
        } catch (IOException e) {
            throw new MojoFailureException("Invalid configfile, valid JSON?", e);
        }
        return config;
    }

    private void updateAutoDeterminedFields(Config config) {
        final List<ProjectModule> modules = config.getModules();
        for (ProjectModule module : modules) {
            updateAutoDeterminedFieldsForSubModules(module);
        }
    }

    private void updateAutoDeterminedFieldsForSubModules(ProjectModule module) {
        for (ProjectModule subModule : module.getModules()) {
            subModule.setParent(module);
            determineReleasableModuleParent(subModule);
            updateAutoDeterminedFieldsForSubModules(subModule);
        }
    }

    private void determineReleasableModuleParent(ProjectModule module) {
        if (module.getParent() != null) {
            ProjectModule parent = module;
            while ((parent = parent.getParent()) != null) {
                if (parent.isReleasableModuleParent()) {
                    module.setReleasableParent(parent);
                    return;
                }
            }
        }
    }

    private void validateConfigFileWithJsonSchema(String fileContents) throws MojoFailureException {
        if (true) return;
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
     * Validate the config file for mandatory parameters.
     * Validate the existence of directories.
     * @param config -
     */
    private void validateConfigFile(Config config) {

    }


    public void outputConfig(Config config) {
        log.info("Current project structure:");
        final List<ProjectModule> modules = config.getModules();
        outputModules(modules, "");
    }

    private void outputModules(List<ProjectModule> modules, String indent) {
        for (ProjectModule module : modules) {
            log.info(indent + " - " + module.toString());
            outputModules(module.getModules(), indent + "  ");
        }
    }

}

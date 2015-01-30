package org.jkva;

import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ConfigFileReader {

    private final Log log;
    private final MavenProject project;

    public ConfigFileReader(Log log, MavenProject project) {
        this.log = log;
        this.project = project;
    }

    public Config readConfigFile(String parentPath, String distPath, List<MavenProject> reactorProjects) throws MojoFailureException {
        final Config config;
        try {
            config = new Config();
            config.setParentPath(parentPath);
            config.setDistPath(distPath);
            config.setProjectBase(project.getBasedir().getCanonicalFile());

            MavenProject moduleProject = findMavenProjectForModule(project, reactorProjects, "");
            config.getModules().add(createProjectModule(project, moduleProject));
            setModules(project, reactorProjects, config.getModules());
            setModuleParents(config);
            setReleasableModuleParents(config);
            updateAutoDeterminedFields(config);
        } catch (IOException e) {
            throw new MojoFailureException("Invalid config file, valid JSON?", e);
        }
        return config;
    }

    private void setModules(MavenProject mavenProject, List<MavenProject> reactorProjects, List<ProjectModule> modules)
            throws MojoFailureException {
        List<String> moduleNames = mavenProject.getModules();
        for (String moduleName : moduleNames) {
            MavenProject moduleProject = findMavenProjectForModule(mavenProject, reactorProjects, moduleName);
            ProjectModule projectModule = createProjectModule(mavenProject, moduleProject);

            modules.add(projectModule);
            setModules(moduleProject, reactorProjects, projectModule.getModules());
        }
    }

    private ProjectModule createProjectModule(MavenProject mavenProject, MavenProject moduleProject) throws MojoFailureException {
        ProjectModule projectModule = new ProjectModule();
        projectModule.setGroupId(moduleProject.getGroupId());
        projectModule.setArtifactId(moduleProject.getArtifactId());
        projectModule.setRelatedMavenProject(moduleProject);

        String modulePath;
        String projectPath;
        try {
            modulePath = moduleProject.getFile().getParentFile().getCanonicalPath();
            projectPath = mavenProject.getFile().getParentFile().getCanonicalPath();
        } catch (IOException e) {
            throw new MojoFailureException("", e);
        }
//        log.info("asdasdas " + modulePath + " 123 " + projectPath);
        String relativeModulePath = modulePath.replace(projectPath, "").replace("\\", "/");
        if (!relativeModulePath.isEmpty()) {
            relativeModulePath = relativeModulePath.substring(1);
        }
        projectModule.setPath(relativeModulePath);
        return projectModule;
    }

    private void setReleasableModuleParents(Config config) throws MojoFailureException {
        List<ProjectModule> allModules = ConfigUtil.getFlatListOfAllModules(config.getModules());
        for (ProjectModule module : allModules) {
            boolean b = determineReleasableModuleParent(module);
            module.setReleasableModuleParent(b);
        }
    }

    // If this maven project is part of a larger multi-module build, it should never be released separately,
    // but always as part of the full multi-module build.
    private boolean determineReleasableModuleParent(final ProjectModule module) throws MojoFailureException {
        final ProjectModule parentModule = module.getParent();
        if (parentModule.getModules().contains(module)) {
            return false;
        } else {
            return true;
        }
    }

    private void setModuleParents(Config config) throws MojoFailureException {
        List<ProjectModule> allModules = ConfigUtil.getFlatListOfAllModules(config.getModules());
        for (ProjectModule module : allModules) {
            MavenProject relatedMavenProject = module.getRelatedMavenProject();
            if (relatedMavenProject == null) {
                throw new MojoFailureException("Null related maven project: " + module);
            }
            MavenProject parent = relatedMavenProject.getParent();
            if (parent != null) {
                String groupId = parent.getGroupId();
                String artifactId = parent.getArtifactId();
                ProjectModule parentModule = findProjectModule(groupId, artifactId, allModules);
                module.setParent(parentModule);
            }
        }
    }

    private ProjectModule findProjectModule(String groupId, String artifactId, List<ProjectModule> allModules)
            throws MojoFailureException {
        for (ProjectModule module : allModules) {
            if (module.getGroupId().equals(groupId) && module.getArtifactId().equals(artifactId)) {
                return module;
            }
        }
        return null;
    }

    private MavenProject findMavenProjectForModule(MavenProject mavenProject, List<MavenProject> reactorProjects,
                                                   String moduleName) throws MojoFailureException {
        String qModuleName = mavenProject.getFile().getParentFile().getPath() + "\\" + moduleName;
        qModuleName = qModuleName.replace("\\", "/");
        if (qModuleName.endsWith("/")) {
            qModuleName = qModuleName.substring(0, qModuleName.length() - 1);
        }
        for (MavenProject reactorProject : reactorProjects) {
            String reactorPath = reactorProject.getFile().getParentFile().getPath();
            reactorPath = reactorPath.replace("\\", "/");
//            log.info("reactorProject: " + reactorPath + " ||| " + qModuleName);
            if (reactorPath.equalsIgnoreCase(qModuleName)) {
                // log.info("qmodulename: " + qModuleName + " resolved to: " + reactorPath);
                return reactorProject;
            }
        }
        throw new MojoFailureException("Cannot find module MavenProject for module name: " + moduleName);
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
            determineReleasableParent(subModule);
            updateAutoDeterminedFieldsForSubModules(subModule);
        }
    }

    private void determineReleasableParent(ProjectModule module) {
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

    public String outputConfig(Config config) {
        final List<ProjectModule> modules = config.getModules();
        return outputModules(modules, "");
    }

    private String outputModules(List<ProjectModule> modules, String indent) {
        StringBuilder ret = new StringBuilder();
        for (ProjectModule module : modules) {
            ret.append(indent).append(" - ").append(module.toString()).append('\n');
            ret.append(outputModules(module.getModules(), indent + ".."));
        }
        return ret.toString();
    }

}

package nl.jkva;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class ConfigUtil {
    private Config config;
    private Log log;
    private MavenSession session;

    public ConfigUtil(Config config, Log log, MavenSession session) {
        this.config = config;
        this.log = log;
        this.session = session;
    }

    public static String createProjectIdentifier(MavenProject mavenProject) {
        return createProjectIdentifier(mavenProject.getGroupId(), mavenProject.getArtifactId());
    }

    public static String createProjectIdentifier(ProjectModule module) {
        return createProjectIdentifier(module.getGroupId(), module.getArtifactId());
    }

    public static String createProjectIdentifier(Dependency dependency) {
        return createProjectIdentifier(dependency.getGroupId(), dependency.getArtifactId());
    }

    private static String createProjectIdentifier(String groupId, String artifactId) {
        return groupId + ":" + artifactId;
    }

    public List<MavenProject> getAllModules(MavenProject project) throws MojoFailureException {
        List<MavenProject> ret = new ArrayList<MavenProject>();

        final List<String> modules = project.getModules();
        final List<String> moduleFullPaths = createFullPathsForModules(project, modules);
        for (String moduleFullPath : moduleFullPaths) {
            MavenProject moduleProject = getMavenProjectFromPath(moduleFullPath);
            ret.add(moduleProject);

            ret.addAll(getAllModules(moduleProject));
        }

        return ret;
    }

    public List<Dependency> getDependenciesForAllModules(MavenProject project) throws MojoFailureException {
        List<Dependency> ret = new ArrayList<Dependency>();

        ret.addAll(project.getDependencies());

        final List<String> modules = project.getModules();
        final List<String> moduleFullPaths = createFullPathsForModules(project, modules);
        for (String moduleFullPath : moduleFullPaths) {
            MavenProject moduleProject = getMavenProjectFromPath(moduleFullPath);
            ret.addAll(getDependenciesForAllModules(moduleProject));
        }

        return ret;
//
//        final List<MavenProject> projects = session.getProjects();
//        for (MavenProject mavenProject : session.getProjects()) {
//            final String parent = configUtil.normalizeFileSeparators(mavenProject.getFile().getParent());
//            if (parent.endsWith(configUtil.normalizeFileSeparators(path))) {
//                return mavenProject;
//            }
//        }
//        for (MavenProject project : projects) {
//            final String parent = project.getFile().getParent();
//            final MavenProject p = configUtil.getMavenProjectFromPath(parent);
//            allModuleDependencies.add(p.getDependencies());
//        }
//        for (Dependency dependency : allModuleDependencies) {
//            log.info(dependency.toString());
//        }
//        return allModuleDependencies;
    }

    private List<String> createFullPathsForModules(MavenProject project, List<String> modules) {
        List<String> ret = new ArrayList<String>();
        for (String module : modules) {
            ret.add(project.getFile().getParent() + File.separator + module);
        }
        return ret;
    }


    public List<ProjectModule> getDirectChildrenOfParent() {
        List<ProjectModule> ret = new ArrayList<ProjectModule>();
        final List<ProjectModule> modules = getFlatListOfAllModules(config.getModules());
        for (ProjectModule module : modules) {
            if (module.getParent() == null) {
                ret.add(module);
            }
        }
        return ret;
    }

    public ProjectModule getModuleForDependency(Dependency dependency) {
        final List<ProjectModule> allModules = getFlatListOfAllModules(config.getModules());
        for (ProjectModule module : allModules) {
            if (dependency.getGroupId().equals(module.getGroupId())
             && dependency.getArtifactId().equals(module.getArtifactId())) {
                return module;
            }
        }
        return null;
    }

    public List<ProjectModule> getFlatListOfAllModules() {
        return getFlatListOfAllModules(config.getModules());
    }

    private List<ProjectModule> getFlatListOfAllModules(List<ProjectModule> modules) {
        List<ProjectModule> ret = new ArrayList<ProjectModule>();
        ret.addAll(modules);
        for (ProjectModule module : modules) {
            ret.addAll(getFlatListOfAllModules(module.getModules()));
        }
        return ret;
    }

    public String getFullPathFromBase(ProjectModule module) {
        String ret = "", sep = "";

        while (module != null) {
            String modulePath = module.getPath();
            modulePath = normalizeFileSeparators(modulePath);
            ret = modulePath + sep + ret;
            sep = File.separator;
            module = module.getParent();
        }

        return ret;
    }

    public String normalizeFileSeparators(String pathName) {
        return pathName.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    }

    public ProjectModule getReleasableModule(ProjectModule dependency) {
        List<ProjectModule> dependenciesSorted = config.getModules();
        for (ProjectModule sortedDependency : dependenciesSorted) {
            if (sortedDependency.getGroupId().equals(dependency.getGroupId())
             && sortedDependency.getArtifactId().equals(dependency.getArtifactId())) {
                return sortedDependency;
            }
        }
        return null;
    }

    public MavenProject getMavenProjectFromPath(String path) throws MojoFailureException {
        for (MavenProject mavenProject : session.getProjects()) {
            final String parent = normalizeFileSeparators(mavenProject.getFile().getParent());
            if (parent.endsWith(normalizeFileSeparators(path))) {
                return mavenProject;
            }
        }
        throw new MojoFailureException("Can't find maven project in path: " + path);
    }

    public ProjectModule getProjectModuleFromMavenProject(MavenProject mavenProject) throws MojoFailureException {
        final String parentKey = createProjectIdentifier(mavenProject);
        for (ProjectModule module : getFlatListOfAllModules(config.getModules())) {
            final String moduleKey = createProjectIdentifier(module);
            if (parentKey.equals(moduleKey)) {
                return module;
            }
        }
        throw new MojoFailureException("Can't find module for MavenProject: " + mavenProject);
    }

//    public MavenProject getMavenProjectFromPath(String path, MavenSession session) throws MojoFailureException {
//        XPP3 approach doesn't work because it doesn't create a full Model object (parent is missing and properties are not resolved)
//        InputStream reader;
//        try {
//            final File pomFile = new File(new File(config.getProjectBase(), path), "pom.xml").getCanonicalFile();
//            reader = new FileInputStream(pomFile);
//            Model mavenModel = new MavenXpp3Reader().read(reader);
//            final MavenProject mavenProject = new MavenProject(mavenModel);
//            mavenProject.setFile(pomFile);
//
//            if (!path.equals(config.getParentPath())) {
//                mavenProject.setParent(getMavenProjectFromPath(config.getParentPath()));
//            }
//
//            return mavenProject;
//        } catch (FileNotFoundException e) {
//            throw new MojoFailureException(e.getMessage(), e);
//        } catch (XmlPullParserException e) {
//            throw new MojoFailureException(e.getMessage(), e);
//        } catch (IOException e) {
//            throw new MojoFailureException(e.getMessage(), e);
//        }

//        Can't get this to work (this approach is suggested by Brett Porter and is also used in Archiva). Problem creating the resolver and the builder.
//        try
//        {
//            final File pomFile = new File(new File(config.getProjectBase(), path), "pom.xml").getCanonicalFile();
//            ModelBuildingRequest req = new DefaultModelBuildingRequest();
//            req.setProcessPlugins( false );
//            req.setPomFile( pomFile );
//            DefaultModelBuilderFactory defaultModelBuilderFactory = new DefaultModelBuilderFactory();
//
//            req.setModelResolver( new RepositoryModelResolver( basedir, pathTranslator ) );
//            req.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
//
//
//            final DefaultModelBuilder builder = defaultModelBuilderFactory.newInstance();
//            Model model = builder.build(req ).getEffectiveModel();
//            return new MavenProject(model);
//        }
//        catch ( ModelBuildingException e )
//        {
//            throw new MojoFailureException("", e);
//        } catch (IOException e) {
//            throw new MojoFailureException("", e);
//        }
//    }
}

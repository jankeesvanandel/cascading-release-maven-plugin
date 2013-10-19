package nl.jkva;

import java.io.File;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class Config {
    private String name;
    private String parentPath;
    private List<ProjectModule> modules;
    private String distPath;
    private File projectBase;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProjectModule> getModules() {
        return modules;
    }

    public void setModules(List<ProjectModule> modules) {
        this.modules = modules;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getDistPath() {
        return distPath;
    }

    public void setDistPath(String distPath) {
        this.distPath = distPath;
    }

    public File getProjectBase() {
        return projectBase;
    }

    public void setProjectBase(File projectBase) {
        this.projectBase = projectBase;
    }
}

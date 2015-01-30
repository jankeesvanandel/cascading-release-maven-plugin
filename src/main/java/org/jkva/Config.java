package org.jkva;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class Config {
    private String parentPath;
    private List<ProjectModule> modules = new ArrayList<ProjectModule>();
    private String distPath;
    private File projectBase;
    private File basedir;

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
        if (parentPath == null || parentPath.isEmpty()) {
            this.parentPath = null;
        } else {
            this.parentPath = parentPath;
        }
    }

    public String getDistPath() {
        return distPath;
    }

    public void setDistPath(String distPath) {
        if (distPath == null) {
            this.distPath = "";
        } else {
            this.distPath = distPath;
        }
    }

    public File getProjectBase() {
        return projectBase;
    }

    public void setProjectBase(File projectBase) {
        this.projectBase = projectBase;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }
}

package nl.jkva;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ProjectModule {

    private String groupId;
    private String artifactId;
    private String path;
    private List<ProjectModule> modules;
    private ProjectModule parent;
    private ProjectModule releasableParent;
    private boolean releasableModuleParent = false;

    private String highestVersionInProject;
    private String releasedVersion;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String toString() {
        return "ProjectModule{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", path='" + path + '\'' +
                ", parent=" + (parent != null ? parent.getArtifactId() : null) +
                ", releasableParent=" + (releasableParent != null ? releasableParent.getArtifactId() : null) +
                ", releasableModuleParent=" + releasableModuleParent +
                '}';
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ProjectModule> getModules() {
        if (modules == null) {
            return new ArrayList<ProjectModule>();
        }
        return modules;
    }

    public void setModules(List<ProjectModule> modules) {
        this.modules = modules;
    }

    public ProjectModule getParent() {
        return parent;
    }

    public void setParent(ProjectModule parent) {
        this.parent = parent;
    }

    public ProjectModule getReleasableParent() {
        return releasableParent;
    }

    public void setReleasableParent(ProjectModule releasableParent) {
        this.releasableParent = releasableParent;
    }

    public boolean isReleasableModuleParent() {
        return releasableModuleParent;
    }

    public void setReleasableModuleParent(boolean releasableModuleParent) {
        this.releasableModuleParent = releasableModuleParent;
    }

    public String getHighestVersionInProject() {
        return highestVersionInProject;
    }

    public void setHighestVersionInProject(String highestVersionInProject) {
        this.highestVersionInProject = highestVersionInProject;
    }

    public String getReleasedVersion() {
        return releasedVersion;
    }

    public void setReleasedVersion(String releasedVersion) {
        this.releasedVersion = releasedVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectModule module = (ProjectModule) o;

        if (!artifactId.equals(module.artifactId)) return false;
        if (!groupId.equals(module.groupId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        return result;
    }
}

package org.jkva;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class Identifier {
    private String groupId;
    private String artifactId;
    private String pathFromBase;
    private Identifier subModuleOf;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identifier that = (Identifier) o;

        if (!artifactId.equals(that.artifactId)) return false;
        if (!groupId.equals(that.groupId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Identifier{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", pathFromBase='" + pathFromBase + '\'' +
                '}';
    }

    public String getPathFromBase() {
        return pathFromBase;
    }

    public void setPathFromBase(String pathFromBase) {
        this.pathFromBase = pathFromBase;
    }

    public Identifier getSubModuleOf() {
        return subModuleOf;
    }

    public void setSubModuleOf(Identifier subModuleOf) {
        this.subModuleOf = subModuleOf;
    }
}

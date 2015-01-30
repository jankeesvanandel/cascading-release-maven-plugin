package org.jkva;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ConfigFile {
    private String name;
    private String parentPathFromBase;
    private Identifier[] dependenciesSorted;
    private String pathToBase;
    private String earPathFromBase;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Identifier[] getDependenciesSorted() {
        return dependenciesSorted;
    }

    public void setDependenciesSorted(Identifier[] dependenciesSorted) {
        this.dependenciesSorted = dependenciesSorted;
    }

    public String getParentPathFromBase() {
        return parentPathFromBase;
    }

    public void setParentPathFromBase(String parentPathFromBase) {
        this.parentPathFromBase = parentPathFromBase;
    }

    public String getPathToBase() {
        return pathToBase;
    }

    public void setPathToBase(String pathToBase) {
        this.pathToBase = pathToBase;
    }

    public String getEarPathFromBase() {
        return earPathFromBase;
    }

    public void setEarPathFromBase(String earPathFromBase) {
        this.earPathFromBase = earPathFromBase;
    }
}

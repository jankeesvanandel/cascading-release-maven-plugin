package org.jkva;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ReleasedModuleTracker {

    /**
     * Template for a released module, like this: org.apache.maven:maven-core [1.0-SNAPSHOT] -> [1.0]
     */
    private static final String MODULE_TEMPLATE = "%s:%s [%s] -> [%s]";

    private final List<String> releasedModules = new ArrayList<String>();
    private final File outputFile;

    public ReleasedModuleTracker(final File outputFile) {
        this.outputFile = outputFile;
    }

    public void addReleasedModule(String groupId, String artifactId, String oldVersion) {
        addReleasedModule(groupId, artifactId, oldVersion, oldVersion.replace("-SNAPSHOT", ""));
    }

    public void addReleasedModule(String groupId, String artifactId, String oldVersion, String newVersion) {
        String moduleString = String.format(MODULE_TEMPLATE, groupId, artifactId, oldVersion, newVersion);
        releasedModules.add(moduleString);
    }

    public List<String> getReleasedModules() {
        return releasedModules;
    }

    public boolean containsReleasedModule(String groupId, String artifactId) {
        final String moduleIdentifier = String.format("%s:%s", groupId, artifactId);
        for (String releasedModule : releasedModules) {
            if (releasedModule.startsWith(moduleIdentifier)) {
                return true;
            }
        }
        return false;
    }

    public Identifier getReleasedModule(String groupId, String artifactId) {
        final String moduleIdentifier = String.format("%s:%s", groupId, artifactId);
        for (String releasedModule : releasedModules) {
            if (releasedModule.startsWith(moduleIdentifier)) {
                final Identifier identifier = new Identifier();
                final String[] splitted = releasedModule.split(":");
                identifier.setGroupId(splitted[0]);
                identifier.setArtifactId(splitted[1].split(" ")[0]);
                return identifier;
            }
        }
        return null;
    }

    public void writeToFile() throws MojoFailureException {
        String newLine = System.getProperty("line.separator");

        StringBuilder sb = new StringBuilder();
        sb.append(new SimpleDateFormat("dd-MM-yyyy").format(new Date())).append(newLine);
        for (String releasedModule : releasedModules) {
            sb.append(releasedModule).append(newLine);
        }
        try {
            if (!outputFile.exists()) {
                Files.createParentDirs(outputFile);
                outputFile.createNewFile();
            }
            Files.append(sb.toString(), outputFile, Charsets.UTF_8);
        }catch (IOException e) {
            throw new MojoFailureException("Error writing release summary to file", e);
        }
    }
}

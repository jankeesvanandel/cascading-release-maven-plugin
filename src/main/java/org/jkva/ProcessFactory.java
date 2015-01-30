package org.jkva;

import org.apache.maven.plugin.logging.Log;

import java.io.File;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ProcessFactory {

    private final Log log;
    private final File projectBase;

    public ProcessFactory(Log log, File projectBase) {
        this.log = log;
        this.projectBase = projectBase;
    }

    public MavenInvoker createMavenInvoker(String relativeWorkingDir) {
        File workDir = new File(projectBase, relativeWorkingDir);

        return new MavenInvoker(this.log, workDir, true);
    }

}

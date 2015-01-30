package org.jkva;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class Util {
    public static boolean isReactorRootProject(MavenSession session, File basedir) throws MojoExecutionException {
        try {
            String executionRootPath = new File(session.getExecutionRootDirectory()).getCanonicalFile().getAbsolutePath();
            String basedirPath = basedir.getCanonicalFile().getAbsolutePath();
            return executionRootPath.equals(basedirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("", e);
        }
    }

}

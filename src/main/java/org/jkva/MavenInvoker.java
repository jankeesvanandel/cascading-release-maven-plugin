package org.jkva;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class MavenInvoker extends Invoker {

    public MavenInvoker(Log log, final File workDir, final boolean redirectLogs) {
        super(log, workDir, redirectLogs);
    }

    @Override
    protected Process doExecute(String arguments, ProcessBuilder processBuilder) throws IOException {
        String mvnBin = processBuilder.environment().get("M2_BIN");
        String exec = new File(mvnBin).getCanonicalPath();
        List<String> goalsList = getProcessArguments(arguments, exec);
        processBuilder.command(goalsList);
        File canonicalWorkDir = workDir.getCanonicalFile();
        processBuilder.directory(canonicalWorkDir);
        return processBuilder.start();
    }

}

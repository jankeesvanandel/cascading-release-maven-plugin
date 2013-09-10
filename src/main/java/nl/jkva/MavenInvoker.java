package nl.jkva;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class MavenInvoker extends Invoker<MavenInvoker> {

    public MavenInvoker(Log log, final File workDir) {
        super(log, workDir);
    }

    @Override
    protected Process doExecute(String arguments, ProcessBuilder processBuilder) throws IOException {
        String mavenHome = processBuilder.environment().get("M2_HOME");
        String exec = new File(new File(mavenHome, "bin"), "mvn.bat").getCanonicalPath();
        List<String> goalsList = getProcessArguments(arguments, exec);
        processBuilder.command(goalsList);
        File canonicalWorkDir = workDir.getCanonicalFile();
        processBuilder.directory(canonicalWorkDir);
        return processBuilder.start();
    }

}

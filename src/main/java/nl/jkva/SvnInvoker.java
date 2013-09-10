package nl.jkva;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class SvnInvoker extends Invoker<SvnInvoker> {
    public SvnInvoker(Log log, File workDir) {
        super(log, workDir);
    }

    @Override
    protected Process doExecute(String arguments, ProcessBuilder processBuilder) throws IOException {
        String svnHome = processBuilder.environment().get("SVN_HOME");
        String exec = new File(new File(svnHome, "bin"), "svn.exe").getCanonicalPath();
        List<String> goalsList = getProcessArguments(arguments, exec);
        processBuilder.command(goalsList);
        File canonicalWorkDir = workDir.getCanonicalFile();
        processBuilder.directory(canonicalWorkDir);
        return processBuilder.start();
    }
}

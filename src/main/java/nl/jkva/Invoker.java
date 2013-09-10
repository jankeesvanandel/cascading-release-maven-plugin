package nl.jkva;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public abstract class Invoker<T extends Invoker> {
    private static final String ERR_MSG = "Error running maven command: ";
    final Log log;
    final File workDir;
    private List<OutputListener> outputListeners = new ArrayList<OutputListener>();

    public Invoker(Log log, File workDir) {
        this.log = log;
        this.workDir = workDir;
    }

    public T addOutputListener(OutputListener outputListener) {
        outputListeners.add(outputListener);
        return (T) this;
    }

    protected void setupListeners(Process releaseProcess) {
        StreamGobbler errorGobbler = new StreamGobbler(releaseProcess.getErrorStream(), "ERROR");
        StreamGobbler outputGobbler = new StreamGobbler(releaseProcess.getInputStream(), "OUTPUT");
        outputGobbler.start();
        errorGobbler.start();
    }

    protected Log getLog() {
        return log;
    }

    protected List<String> getProcessArguments(String goals, String exec) {
        String[] goalsSplitted = goals.split(" ");
        List<String> goalsList = new ArrayList<String>(goalsSplitted.length + 1);
        try {
            goalsList.add(new File(exec).getCanonicalPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        goalsList.addAll(Arrays.asList(goalsSplitted));
        getLog().info("GOALS:::: " + goalsList);
        return goalsList;
    }

    //TODO: output to separate file per invocation, and only log the summary in the main maven output.
    // Maybe the Log property can also be removed then.
    public int execute(String goals) throws MojoFailureException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();

            Process process = doExecute(goals, processBuilder);
            setupListeners(process);
            int exitCode;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                throw new MojoFailureException(ERR_MSG + "InterruptedException. Check the logs for details", e);
            }

            if (exitCode != 0) {
                throw new MojoFailureException(ERR_MSG + "Exit code=" + exitCode);
            } else {
                return exitCode;
            }
        } catch (IOException e) {
            throw new MojoFailureException(ERR_MSG + "IOException. Check the logs for details", e);
        }
    }

    protected abstract Process doExecute(String arguments, ProcessBuilder processBuilder) throws IOException;

    private class StreamGobbler extends Thread {
        InputStream is;
        String type;

        private StreamGobbler(InputStream is, String type) {
            this.is = is;
            this.type = type;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null)
                    if (type.equals("OUTPUT")) {
                        getLog().info(line);
                    } else {
                        getLog().error(line);
                    }
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}

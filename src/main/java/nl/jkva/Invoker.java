package nl.jkva;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.collect.ImmutableList;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public abstract class Invoker {
    private static final String ERR_MSG = "Error running maven command: ";
    final Log log;
    final File workDir;
    private final boolean redirectLogs;
    private StreamGobbler outputGobbler;
    private StreamGobbler errorGobbler;

    public Invoker(Log log, File workDir, boolean redirectLogs) {
        this.log = log;
        this.workDir = workDir;
        this.redirectLogs = redirectLogs;
    }

    protected void setupListeners(Process releaseProcess) {
        outputGobbler = new StreamGobbler(releaseProcess.getInputStream(), getLog(), redirectLogs);
        errorGobbler = new StreamGobbler(releaseProcess.getErrorStream(), getLog(), redirectLogs);
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
        getLog().info(" - Goals: " + goalsList);
        return goalsList;
    }

    public int execute(String goals) throws MojoFailureException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.directory(workDir);

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

    public ImmutableList<String> getOutput() {
        return ImmutableList.<String>builder() //
                .addAll(outputGobbler.getOutput()) //
                .addAll(errorGobbler.getOutput()).build();
    }

    private static class StreamGobbler extends Thread {
        private final InputStream is;
        private final Log log;
        private final boolean redirectLogs;
        private final List<String> output = new ArrayList<String>();
        private final AtomicBoolean done = new AtomicBoolean();

        private StreamGobbler(InputStream is, Log log, boolean redirectLogs) {
            this.is = is;
            this.log = log;
            this.redirectLogs = redirectLogs;
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    output.add(line);
                    if (redirectLogs) {
                        log.info(line);
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                done.set(true);
            }
        }

        public Iterable<String> getOutput() {
            try {
                while (!done.get()) {
                    log.debug("Not yet done...");
                    Thread.sleep(50);
                }
    
                return output;
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException(e);
            }
        }
    }
}

package nl.jkva;

import org.apache.maven.plugin.logging.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Named
public class ProcessFactory {

    private final Logger log;
    private File projectBase;

    @Inject
    public ProcessFactory(Logger log) {
        this.log = log;
    }

    public void setProjectBase(File projectBase) {
        this.projectBase = projectBase;
    }

    public MavenInvoker createMavenInvoker(String relativeWorkingDir) {
        File workDir = new File(projectBase, relativeWorkingDir);

        return new MavenInvoker(this.log, workDir, true);
    }

}

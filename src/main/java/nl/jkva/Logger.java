package nl.jkva;

import org.apache.maven.plugin.logging.Log;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class Logger {

    private Log log;

    public void error(String format, Object... args) {
        log.error(String.format(format, args));
    }

    public void warn(String format, Object... args) {
        log.warn(String.format(format, args));
    }

    public void info(String format, Object... args) {
        log.info(String.format(format, args));
    }

    public void debug(String format, Object... args) {
        log.debug(String.format(format, args));
    }

    public void setLog(Log log) {
        this.log = log;
    }
}

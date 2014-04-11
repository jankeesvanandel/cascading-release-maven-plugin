package nl.jkva;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.inject.Inject;

/**
 * Created by jankeesvanandel on 11-04-14.
 */
@Mojo(name = "bla", inheritByDefault = false, aggregator = true)
public class BlaMojo extends AbstractMojo {

    private Logger logger;

    @Inject
    public BlaMojo(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        logger.setLog(getLog());
    }
}

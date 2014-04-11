package nl.jkva;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Named
public class Prompter {

    private final Logger logger;
    private final InternalPrompter prompter;

    @Inject
    public Prompter(Logger logger, InternalPrompter prompter) {
        this.logger = logger;
        this.prompter = prompter;
    }

    String promptWithDefault(String promptMessage, String defaultValue) {
        String input = prompt(String.format(promptMessage + " [%s]:", defaultValue));
        if (input.isEmpty()) {
            input = defaultValue;
        }
        return input;
    }

    File promptForDirectory(String promptMessage, String defaultValue) {
        String input = promptWithDefault(promptMessage, defaultValue);
        input = input.replace("\\", "/");
        return new File(input);
    }

    String prompt(String message) {
        return prompter.prompt(message);
    }

}

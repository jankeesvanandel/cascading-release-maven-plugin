package nl.jkva;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Console;
import java.io.File;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
@Named
public class Prompter {

    private final Logger logger;

    @Inject
    public Prompter(Logger logger) {
        this.logger = logger;
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
        Console console = System.console();
        return console.readLine(message);
    }

}

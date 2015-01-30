package org.jkva;

import org.apache.maven.settings.Settings;

import java.io.Console;
import java.io.File;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class PromptUtil {

    public static Settings settings;

    static String promptWithDefault(String promptMessage, String defaultValue) {
        if (settings.isInteractiveMode()) {
            String input = prompt(String.format(promptMessage + " [%s]:", defaultValue));
            if (input.isEmpty()) {
                input = defaultValue;
            }
            return input;
        } else {
            return defaultValue;
        }
    }

    static File promptForDirectory(String promptMessage, String defaultValue) {
        String input = promptWithDefault(promptMessage, defaultValue);
        input = input.replace("\\", "/");
        return new File(input);
    }

    static String prompt(String message) {
        Console console = System.console();
        return console.readLine(message);
    }
}

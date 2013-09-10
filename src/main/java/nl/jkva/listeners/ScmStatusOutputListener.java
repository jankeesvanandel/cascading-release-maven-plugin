package nl.jkva.listeners;

import nl.jkva.OutputListener;

/**
 * @author Jan-Kees van Andel - @jankeesvanandel
 */
public class ScmStatusOutputListener implements OutputListener {

    boolean hasLocalChanges = false;

    public boolean handleLine(String line) {
        if (false) {// TODO: fix me
            hasLocalChanges = true;
        }
        return true;
    }
}

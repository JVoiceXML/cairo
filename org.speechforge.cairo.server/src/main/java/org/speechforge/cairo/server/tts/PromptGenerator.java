package org.speechforge.cairo.server.tts;

import java.io.File;
import java.io.IOException;

/**
 * A prompt generator.
 * @author Dirk Schnelle-Walka
 *
 */
public interface PromptGenerator {

    /**
     * Generates a prompt file containing the specified speech text.
     * @param text textual content of prompt file.
     * @param dir directory in which to save the generated prompt file.
     * @return the generated prompt file.
     * @throws IllegalArgumentException if the directory specified is not a directory.
     * @throws IOException  error creating the prompt file
     */
    File generatePrompt(String text, File dir)
            throws IllegalArgumentException, IOException;

}
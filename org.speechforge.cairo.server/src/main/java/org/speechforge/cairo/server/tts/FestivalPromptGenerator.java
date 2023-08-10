/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2005-2006 SpeechForge - http://www.speechforge.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: ngodfredsen@users.sourceforge.net
 * Modified by: Martin Mory, linuxfan91@users.sourceforge.net
 *
 */
package org.speechforge.cairo.server.tts;

import java.io.File;
import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.speechforge.cairo.util.pool.AbstractPoolableObject;

/**
 * Generates speech prompt files using the Festival text-to-speech engine.
 *
 * @author Martin Mory {@literal <}<a href="mailto:linuxfan91@users.sourceforge.net">linuxfan91@users.sourceforge.net</a>{@literal >}
 */
public class FestivalPromptGenerator extends AbstractPoolableObject {
    private static final Logger LOGGER = Logger.getLogger(FestivalPromptGenerator.class);

    // private Voice _voice;
    private String _voiceName;

    public FestivalPromptGenerator(String voiceName) {
	_voiceName = voiceName;
        LOGGER.info("Creating FESTIVAL prompt generator for voice '" + _voiceName + "'");
    }

    /**
     * Generates a prompt file containing the specified speech text.
     * @param text textual content of prompt file.
     * @param dir directory in which to save the generated prompt file.
     * @return the generated prompt file.
     * @throws IllegalArgumentException if the directory specified is not a directory.
     */
    public synchronized File generatePrompt(String text, File dir) throws IllegalArgumentException {
	if(dir == null || !dir.isDirectory()) {  
            throw new IllegalArgumentException("Directory file specified does not exist or is not a directory: " + dir);
        }

        if (text == null) {
            text = "";
        }

        String promptName = Long.toString(System.currentTimeMillis());

        // File promptFile = new File(dir, promptName);

        // AudioPlayer ap = new SingleFileAudioPlayer(promptFile.getAbsolutePath(), AudioFileFormat.Type.AU);
        // AudioFormat af = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 8, 8000, false);
        // ap.setAudioFormat(af);
        // _voice.setAudioPlayer(ap);
        // _voice.speak(text);
        // ap.close();
        // _voice.setAudioPlayer(null);
	try{
	    PrintWriter writer = new PrintWriter(dir + "/" + promptName);
	    writer.println(text);
	    writer.close();
	    Process p = Runtime.getRuntime().exec(
						  "/home/tjr/festival/bin/text2wave -o " +
	    dir + "/" + promptName + ".au -otype snd " + dir + "/" + promptName);
	    p.waitFor();
	}
	catch(Exception e){
	    throw new RuntimeException("Cannot invoke text2wave (Festival)!");
	}
        File promptFile = new File(dir, promptName + ".au");
	/*	        if (!promptFile.exists()) {
            throw new RuntimeException("Expected generated prompt file does not exist!");
		}*/
        return promptFile;
    }

}

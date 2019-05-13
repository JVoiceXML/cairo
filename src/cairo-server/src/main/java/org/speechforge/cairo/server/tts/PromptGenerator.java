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
 *
 */
package org.speechforge.cairo.server.tts;

import org.speechforge.cairo.util.pool.AbstractPoolableObject;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

/**
 * Generates speech prompt files using the FreeTTS text-to-speech engine.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class PromptGenerator extends AbstractPoolableObject {

    private Voice _voice;

    public PromptGenerator(String voiceName) {

        VoiceManager voiceManager = VoiceManager.getInstance();
        _voice = voiceManager.getVoice(voiceName);

        if (_voice == null) {
            throw new RuntimeException("TTS voice name <" + voiceName + "> not found!");
        }

        _voice.allocate();
    }

    /**
     * Generates a prompt file containing the specified speech text.
     * @param text textual content of prompt file.
     * @param dir directory in which to save the generated prompt file.
     * @return the generated prompt file.
     * @throws IllegalArgumentException if the directory specified is not a directory.
     * @throws IOException 
     */
    public synchronized File generatePrompt(String text, File dir) throws IllegalArgumentException, IOException {
        if (dir == null || !dir.isDirectory()) {
            throw new IllegalArgumentException("Directory file specified does not exist or is not a directory: " + dir);
        }

        if (text == null) {
            text = "";
        }

        String promptName = Long.toString(System.currentTimeMillis());
        File promptFile = new File(dir, promptName);

        AudioPlayer ap = new SingleFileAudioPlayer(promptFile.getAbsolutePath(), AudioFileFormat.Type.AU);
        AudioFormat af = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 8, 8000, false);
        ap.setAudioFormat(af);
        _voice.setAudioPlayer(ap);
        _voice.speak(text);
        ap.close();
        _voice.setAudioPlayer(null);

        promptFile = new File(dir, promptName + ".au");
        if (!promptFile.exists()) {
            throw new RuntimeException("Expected generated prompt file does not exist!");
        }
        return promptFile;
    }

}
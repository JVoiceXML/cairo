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
package org.speechforge.cairo.test.sphinx.wavfile;


import java.net.URL;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Unit test for basic recognition with Sphinx (no Cairo classes).
 */
public class TestSphinxWavFile {

    private static final Logger LOGGER =
            Logger.getLogger(TestSphinxWavFile.class);

    /**
     * Create the test case
     */
    public TestSphinxWavFile(){
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty("frontend", "mfcFrontEnd");
    }

    @Test
    public void test12345() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/12345-alt2.wav");
        String expected = "one two three four five";
//        recognize(audioFileURL, expected);
    }

    @Test
    public void test65536() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/65536.wav");
        String expected = "six five five three six";
//        recognize(audioFileURL, expected);
    }

    @Test
    public void test1984() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/1984.wav");
        String expected = "one nine eight four";
//        recognize(audioFileURL, expected);
    }

    private void recognize(URL audioFileURL, String expected) throws Exception {
        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        LOGGER.debug("configURL: " + sphinxConfigURL);

        LOGGER.debug("Loading Recognizer...");

        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);

        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        StreamDataSource source = (StreamDataSource) cm.lookup("streamDataSource");

        LOGGER.debug("Decoding " + audioFileURL.getFile());
        LOGGER.debug(AudioSystem.getAudioFileFormat(audioFileURL));

        /* set the stream data source to read from the audio file */
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);
        source.setInputStream(ais);

        /* decode the audio file */
        Result result = recognizer.recognize();

        Assert.assertNotNull(result);

        LOGGER.debug("Result: " + result.getBestFinalResultNoFiller() + '\n');
        Assert.assertEquals(expected, result.getBestFinalResultNoFiller());

    }

}

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
import org.speechforge.cairo.server.recog.sphinx.AbstractTestCase;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Unit test for basic recognition with Sphinx (no Cairo classes).
 */
public class TestSphinxWavFile extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TestSphinxWavFile.class);

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public TestSphinxWavFile(String testName){
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSphinxWavFile.class );
    }

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("frontend", "mfcFrontEnd");
    }

    public void test12345() throws Exception {
        debugTestName(_logger);

        URL audioFileURL = this.getClass().getResource("/prompts/12345-alt2.wav");
        String expected = "one two three four five";
//        recognize(audioFileURL, expected);
    }

    public void test65536() throws Exception {
        debugTestName(_logger);

        URL audioFileURL = this.getClass().getResource("/prompts/65536.wav");
        String expected = "six five five three six";
//        recognize(audioFileURL, expected);
    }

    public void test1984() throws Exception {
        debugTestName(_logger);

        URL audioFileURL = this.getClass().getResource("/prompts/1984.wav");
        String expected = "one nine eight four";
//        recognize(audioFileURL, expected);
    }

    private void recognize(URL audioFileURL, String expected) throws Exception {
        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        _logger.debug("configURL: " + sphinxConfigURL);

        _logger.debug("Loading Recognizer...");

        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);

        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        StreamDataSource source = (StreamDataSource) cm.lookup("streamDataSource");

        _logger.debug("Decoding " + audioFileURL.getFile());
        _logger.debug(AudioSystem.getAudioFileFormat(audioFileURL));

        /* set the stream data source to read from the audio file */
        AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);
        source.setInputStream(ais);

        /* decode the audio file */
        Result result = recognizer.recognize();

        assertNotNull(result);

        _logger.debug("Result: " + result.getBestFinalResultNoFiller() + '\n');
        assertEquals(expected, result.getBestFinalResultNoFiller());

    }

}

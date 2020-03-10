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
package org.speechforge.cairo.server.recog.sphinx;

import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.test.sphinx.util.RecogNotifier;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;

import java.net.URL;

import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.protocol.PushBufferDataSource;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.log4j.Logger;

/**
 * Unit test for SphinxRecEngine using raw (un-replicated) audio data from a prompt file for input.
 */
public class TestSphinxRecEngineRaw extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TestSphinxRecEngineRaw.class);
    
    private static final String PROP_FRONTEND =     "frontend";
    private static final String EP_FRONTEND =       "epFrontEnd";
    private static final String NO_EP_FRONTEND =    "frontEnd";

    private static final String PROP_GRAMMAR_NAME = "grammarName";

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TestSphinxRecEngineRaw(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSphinxRecEngineRaw.class);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtest12345() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, NO_EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/12345.wav");
        String expected = "one two three four five";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtest12345Alt() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, NO_EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/12345-alt.wav");
        String expected = "one two three four five";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtest12345Alt2() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, NO_EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/12345-alt2.wav");
        String expected = "one two three four five";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtestSilence12345() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/12345-silence.wav");
        String expected = "one two three four five";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtest123456() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/123456.wav");
        String expected = "one two three four five six";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    /**
     * The following test is disabled since TIDIGITS is not available for 8k sample rate
     */
    public void xtest123456NoSpeechClassifier() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, NO_EP_FRONTEND);

        URL audioFileURL = this.getClass().getResource("/prompts/123456.wav");
        String expected = "nine one two three four five six";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    public void testHelloRita() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "hello");

        URL audioFileURL = this.getClass().getResource("/prompts/hello_rita.wav");
        String expected = "hello rita";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    public void testGetMeAStockQuote() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "example");

        URL audioFileURL = this.getClass().getResource("/prompts/get_me_a_stock_quote.wav");
        String expected = "get me a stock quote<main:STOCKS>";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    public void testIWouldLikeSportsNews() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "example");

        URL audioFileURL = this.getClass().getResource("/prompts/i_would_like_sports_news.wav");
        String expected = "i would like sports news<main:SPORTS>";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected);
    }

    public void saltestMultiRecog() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "example");

        URL audioFileURL2 = this.getClass().getResource("/prompts/get_me_a_stock_quote.wav");
        String expected2 = "get me a stock quote<main:STOCKS>";

        URL audioFileURL1 = this.getClass().getResource("/prompts/i_would_like_sports_news.wav");
        String expected1 = "i would like sports news<main:SPORTS>";

//        recognizeAudioFile(sphinxConfigURL, audioFileURL1, expected1, audioFileURL2, expected2);
    }
    

    private static void recognizeAudioFile(URL sphinxConfigURL, URL audioFileURL, String expected) throws Exception {
        recognizeAudioFile(sphinxConfigURL, audioFileURL, expected, null, null);
    }

    private static void recognizeAudioFile(URL sphinxConfigURL, URL audioFileURL1, String expected1, URL audioFileURL2, String expected2)
      throws Exception {

        _logger.debug("sphinxConfigURL: " + sphinxConfigURL);
        _logger.debug("audioFileURL: " + audioFileURL1);
        _logger.debug("expected: \"" + expected1 + '"');

        assertNotNull(sphinxConfigURL);
        assertNotNull(audioFileURL1);
        assertNotNull(expected1);

        // configure sphinx
        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm,1);

        RecognitionResult result1 = doRecognize(engine, audioFileURL1);
        _logger.debug("result=" + result1);
        assertEquals(expected1, result1.toString());

        if (audioFileURL2 != null && expected2 != null) {
            _logger.debug("audioFileURL2: " + audioFileURL2);
            _logger.debug("expected2: \"" + expected2 + '"');

            RecognitionResult result2 = doRecognize(engine, audioFileURL2);
            _logger.debug("result2=" + result2);
            assertEquals(expected2, result2.toString());

        }

    }

    private static RecognitionResult doRecognize(SphinxRecEngine engine, URL audioFileURL) throws Exception {
        Processor processor = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();

        engine.activate();

        RecogNotifier listener = new RecogNotifier();
        engine.startRecognition(pbds, listener);
        processor.start();
        _logger.debug("Starting recog thread...");
        engine.startRecogThread();

        // wait for result
        RecognitionResult result = null;
        synchronized (listener) {
            while ((result = listener.getResult()) == null) {
                listener.wait(1000);
            }
        }

        engine.passivate();

        return result;

    }
}

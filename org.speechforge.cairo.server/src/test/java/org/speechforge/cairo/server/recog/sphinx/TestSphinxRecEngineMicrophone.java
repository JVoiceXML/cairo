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

import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;

import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.test.sphinx.util.RecogNotifier;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;

import java.net.URL;

import javax.media.Processor;
import javax.media.protocol.PushBufferDataSource;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.log4j.Logger;

/**
 * Unit test for SphinxRecEngine using local microphone for input.
 */
public class TestSphinxRecEngineMicrophone extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TestSphinxRecEngineMicrophone.class);

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
    public TestSphinxRecEngineMicrophone(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSphinxRecEngineMicrophone.class);
    }

    // TODO: rewrite config file "sphinx-config-TIDIGITS.xml" to support batch or live CMN.
    public void XtestTidigits12345() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);

        String expected = "one two three four five";

        recognizeMicrophone(sphinxConfigURL, expected);
    }

    public void testWsj12345() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "digits");

        String expected = "one two three four five";

        recognizeMicrophone(sphinxConfigURL, expected);
    }

    public void testHelloRita() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "hello");

        String expected = "hello rita";

        recognizeMicrophone(sphinxConfigURL, expected);
    }

    public void testGetMeAStockQuote() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "example");

        String expected = "get me a stock quote";

        recognizeMicrophone(sphinxConfigURL, expected);
    }

    public void testIWouldLikeSportsNews() throws Exception {
        debugTestName(_logger);

        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-WSJ.xml");
        System.setProperty(PROP_FRONTEND, EP_FRONTEND);
        System.setProperty(PROP_GRAMMAR_NAME, "example");

        String expected = "i would like sports news";

        recognizeMicrophone(sphinxConfigURL, expected);
    }

    private static void recognizeMicrophone(URL sphinxConfigURL, String expected) throws Exception {

        _logger.debug("sphinxConfigURL: " + sphinxConfigURL);
        _logger.debug("expected: \"" + expected + '"');

        assertNotNull(sphinxConfigURL);
        assertNotNull(expected);

        // configure sphinx
        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm,1);

        RecognitionResult result = doRecognize(engine, expected);
        _logger.debug("result=" + result);

        assertEquals(expected, result.toString());

    }

    private static RecognitionResult doRecognize(SphinxRecEngine engine, String expected) throws Exception {
        Processor processor = JMFUtil.createRealizedProcessor(MICROPHONE, SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();

        engine.activate();

        RecogNotifier listener = new RecogNotifier();
        processor.start();

        RecognitionResult result = null;


        engine.startRecognition(pbds, listener);
        _logger.debug("Starting recog thread...");
        engine.startRecogThread();
        _logger.info("\n\nWaiting for you to say \"" + expected + "\"...\n\n");

        // wait for result
        synchronized (listener) {
            while ((result = listener.getResult()) == null) {
                listener.wait(1000);
            }
        }

        engine.passivate();
        processor.stop();

        return result;

    }
}

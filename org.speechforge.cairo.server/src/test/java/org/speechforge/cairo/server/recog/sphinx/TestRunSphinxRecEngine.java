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

import java.net.URL;

import javax.media.MediaLocator;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.speechforge.cairo.server.recog.RecognitionResult;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Unit test for RunSphinxRecEngine.
 */
public class TestRunSphinxRecEngine {

    private static final Logger LOGGER =
            Logger.getLogger(TestSphinxRecEngineReplicated.class);

    private RunSphinxRecEngine _runner = null;

    /**
     * Create the test case
     */
    public TestRunSphinxRecEngine(){
    }

    @Before
    public void setUp() throws Exception {
        // configure sphinx
        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        Assert.assertNotNull(sphinxConfigURL);
        LOGGER.debug("sphinxConfigURL: " + sphinxConfigURL);

        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm,1);
        _runner = new RunSphinxRecEngine(engine);

    }

    @Test
    public void test12345() throws Exception {
        String promptFile = "/prompts/12345.wav";
        String expected = "one two three four five";

        doRecognize(promptFile, expected);
    }

    @Test
    public void test12345Alt2() throws Exception {
        String promptFile = "/prompts/12345-alt2.wav";
        String expected = "one two three four five";

        doRecognize(promptFile, expected);
    }

    private void doRecognize(String promptFile, String expected) throws Exception {

        URL audioFileURL = this.getClass().getResource(promptFile);
        Assert.assertNotNull(audioFileURL);

        Assert.assertTrue(_runner != null);

        RecognitionResult result = _runner.doRecognize(new MediaLocator(audioFileURL));
        LOGGER.debug(result);
        Assert.assertEquals(expected, result.toString());
    }

}

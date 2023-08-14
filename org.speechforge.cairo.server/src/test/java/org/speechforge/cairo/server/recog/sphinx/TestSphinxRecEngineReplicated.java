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

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.rtp.server.PBDSReplicator;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.test.sphinx.util.RecogNotifier;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Unit test for SphinxRecEngine using replicated audio data from a prompt file for input.
 */
public class TestSphinxRecEngineReplicated {

    private static final Logger LOGGER =
            LogManager.getLogger(TestSphinxRecEngineReplicated.class);

    /**
     * Create the test case
     */
    public TestSphinxRecEngineReplicated() {
    }

    @Test
    public void test12345() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/12345.wav");
        Assert.assertNotNull(audioFileURL);
        String expected = "one two three four five";
        recognizeAudioFile(audioFileURL, expected);
    }

    private void recognizeAudioFile(URL audioFileURL, String expected) throws Exception {

        // configure sphinx
        URL sphinxConfigURL = this.getClass().getResource("sphinx-config-TIDIGITS.xml");
        Assert.assertNotNull(sphinxConfigURL);
        LOGGER.debug("sphinxConfigURL: " + sphinxConfigURL);

        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        SphinxRecEngine engine = new SphinxRecEngine(cm,1);

        Processor processor1 = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor1.addControllerListener(new ProcessorStarter(true));

        PushBufferDataSource pbds1 = (PushBufferDataSource) processor1.getDataOutput();
        PBDSReplicator replicator = new PBDSReplicator(pbds1);

        DataSource dataSource = replicator.replicate();

        ProcessorModel pm = new ProcessorModel(
                dataSource,
                new AudioFormat[] { replicator.getAudioFormat() },
                JMFUtil.CONTENT_DESCRIPTOR_RAW
        );

        LOGGER.debug("Creating realized processor...");
        Processor processor2 = Manager.createRealizedProcessor(pm);
        LOGGER.debug("Processor realized.");

        processor2.addControllerListener(new ProcessorStarter(true));
        PushBufferDataSource pbds2 = (PushBufferDataSource) processor2.getDataOutput();

        engine.activate();

        RecogNotifier listener = new RecogNotifier();
        engine.startRecognition(pbds2, listener);

        processor2.start();
        Thread.sleep(1000);  // give processor2 a chance to start
        processor1.start();
        LOGGER.debug("Starting recog thread...");
        engine.startRecogThread();

        // wait for result
        RecognitionResult result = null;
        synchronized (listener) {
            while ((result = listener.getResult()) == null) {
                listener.wait(1000);
            }
        }

        engine.passivate();

        LOGGER.debug("result=" + result);
        Assert.assertEquals(expected, result.toString());

    }
}

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
package org.speechforge.cairo.rtp.server;

import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import org.speechforge.cairo.rtp.server.sphinx.AbstractTestCase;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

import org.apache.log4j.Logger;

/**
 * Unit test for PBDSReplicator.
 */
public class TtestPBDSReplicator extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TtestPBDSReplicator.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TtestPBDSReplicator(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TtestPBDSReplicator.class);
    }

    // TODO: fix timing issue that causes test to fail sometimes
    public void test12345() throws Exception {
        debugTestName(_logger);

        URL audioFileURL = this.getClass().getResource("/prompts/12345.wav");
        assertNotNull(audioFileURL);

        URL speechDataURL = this.getClass().getResource("/prompts/12345.speechdata.txt");
        assertNotNull(speechDataURL);

        Reader r = new BufferedReader(new InputStreamReader(speechDataURL.openStream()));
        StreamTokenizer tokenizer = new StreamTokenizer(r);
        tokenizer.parseNumbers();

        Processor processor1 = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor1.addControllerListener(new ProcessorStarter());
        PushBufferDataSource pbds1 = (PushBufferDataSource) processor1.getDataOutput();
        PBDSReplicator replicator = new PBDSReplicator(pbds1);

        DataSource dataSource = replicator.replicate();

        ProcessorModel pm = new ProcessorModel(
                dataSource,
                //new AudioFormat[] { replicator.getAudioFormat() },
                new AudioFormat[] { SourceAudioFormat.PREFERRED_MEDIA_FORMAT },
                JMFUtil.CONTENT_DESCRIPTOR_RAW
        );

        _logger.debug("Creating realized processor...");
        Processor processor2 = Manager.createRealizedProcessor(pm);
        _logger.debug("Processor realized.");

        processor2.addControllerListener(new ProcessorStarter());
        PushBufferDataSource pbds2 = (PushBufferDataSource) processor2.getDataOutput();
        processor2.start();
        Thread.sleep(1000);  // give processor2 a chance to start
        processor1.start();

        PushBufferStream[] streams = pbds2.getStreams();
        assert(streams.length == 1);

        RawAudioProcessor rawAudioProcessor = RawAudioProcessor.getInstanceForTesting();
        //SpeechDataLogger speechDataLogger = SpeechDataLogger.getInstanceForTesting(this.getClass().getSimpleName());
        //speechDataLogger.setPredecessor(rawAudioProcessor);

        RawAudioTransferHandler rawAudioTransferHandler = new RawAudioTransferHandler(rawAudioProcessor);
        rawAudioTransferHandler.startProcessing(streams[0]);

        int ttype = tokenizer.nextToken();
        assertEquals(StreamTokenizer.TT_WORD, ttype);

        _logger.debug("expected=edu.cmu.sphinx.frontend.DataStartSignal actual=" + tokenizer.sval);
        assertEquals("edu.cmu.sphinx.frontend.DataStartSignal", tokenizer.sval);

        Data data = rawAudioProcessor.getData();
        assertTrue(data instanceof DataStartSignal);

        ttype = tokenizer.nextToken();

        while (ttype == StreamTokenizer.TT_NUMBER) {

            data = rawAudioProcessor.getData();
            assertTrue(data instanceof DoubleData);

            double[] values = ((DoubleData) data).getValues();
            for (int i=0; i < values.length; i++) {
                _logger.debug("expected=" + tokenizer.nval + " actual=" + values[i]);
                if (_logger.isTraceEnabled()) {
                    _logger.trace("expected=" + tokenizer.nval + " actual=" + values[i]);
                }
                //assertEquals(tokenizer.nval, values[i]);
                ttype = tokenizer.nextToken();
            }

        }

        _logger.debug("expected=edu.cmu.sphinx.frontend.DataEndSignal actual=" + tokenizer.sval);
        assertEquals("edu.cmu.sphinx.frontend.DataEndSignal", tokenizer.sval);

        data = rawAudioProcessor.getData();
        assertTrue(data instanceof DataEndSignal);

    }

}

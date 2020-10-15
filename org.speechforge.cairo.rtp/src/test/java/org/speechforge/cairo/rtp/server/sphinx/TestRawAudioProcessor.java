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
package org.speechforge.cairo.rtp.server.sphinx;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;

import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;

/**
 * Unit test for RawAudioProcessor.
 */
public class TestRawAudioProcessor  {

    private static final Logger LOGGER = 
            Logger.getLogger(TestRawAudioProcessor.class);

    /**
     * Create the test case
     */
    public TestRawAudioProcessor() {
    }

    @Test
    public void test12345() throws Exception {
        URL audioFileURL = this.getClass().getResource("/prompts/12345.wav");
        Assert.assertNotNull(audioFileURL);

        URL speechDataURL = this.getClass().getResource("/prompts/12345.speechdata.txt");
        Assert.assertNotNull(speechDataURL);

        Reader r = new BufferedReader(new InputStreamReader(speechDataURL.openStream()));
        StreamTokenizer tokenizer = new StreamTokenizer(r);
        tokenizer.parseNumbers();

        Processor processor = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), SourceAudioFormat.PREFERRED_MEDIA_FORMAT);
        processor.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
        processor.start();

        PushBufferStream[] streams = pbds.getStreams();
        Assert.assertEquals("Should be single stream in data source.", 1, streams.length);
        LOGGER.debug("PushBufferStream format: " + streams[0].getFormat());

        RawAudioProcessor rawAudioProcessor = RawAudioProcessor.getInstanceForTesting();

        RawAudioTransferHandler rawAudioTransferHandler = new RawAudioTransferHandler(rawAudioProcessor);
        rawAudioTransferHandler.startProcessing(streams[0]);

        int ttype = tokenizer.nextToken();
        Assert.assertEquals(StreamTokenizer.TT_WORD, ttype);

        LOGGER.debug("expected=edu.cmu.sphinx.frontend.DataStartSignal actual=" + tokenizer.sval);
        Assert.assertEquals("edu.cmu.sphinx.frontend.DataStartSignal", tokenizer.sval);

        Data data = rawAudioProcessor.getData();
        Assert.assertTrue(data instanceof DataStartSignal);

        ttype = tokenizer.nextToken();

        while (ttype == StreamTokenizer.TT_NUMBER) {

            data = rawAudioProcessor.getData();
            Assert.assertTrue(data instanceof DoubleData);

            double[] values = ((DoubleData) data).getValues();
            for (int i=0; i < values.length; i++) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("expected=" + tokenizer.nval + " actual=" + values[i]);
                }
                Assert.assertEquals(tokenizer.nval, values[i]);
                ttype = tokenizer.nextToken();
            }

        }

        LOGGER.debug("expected=edu.cmu.sphinx.frontend.DataEndSignal actual=" + tokenizer.sval);
        Assert.assertEquals("edu.cmu.sphinx.frontend.DataEndSignal", tokenizer.sval);

        data = rawAudioProcessor.getData();
        Assert.assertTrue(data instanceof DataEndSignal);

    }

}

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

import static org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat.PREFERRED_MEDIA_FORMATS;
import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;

import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.rtp.server.PBDSReplicator;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;

import edu.cmu.sphinx.util.props.ConfigurationManager;

import org.apache.log4j.Logger;

/**
 * Provides main method for running SphinxRecEngine in standalone mode using the microphone or prompt file for input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RunSphinxRecEngine extends RecogListenerDecorator {

    private static Logger _logger = Logger.getLogger(RunSphinxRecEngine.class);

    private SphinxRecEngine _engine;
    private RecognitionResult _result;
    private PBDSReplicator _replicator;

    public RunSphinxRecEngine(SphinxRecEngine engine) {
        super(null);
        _engine = engine;
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
     */
    @Override
    public synchronized void recognitionComplete(RecognitionResult result) {
        _result = result;
        this.notify();
    }

    public RecognitionResult doRecognize(MediaLocator mediaLocator)
      throws IOException, NoProcessorException, CannotRealizeException, InterruptedException, NoDataSourceException {

        Processor processor1 = JMFUtil.createRealizedProcessor(mediaLocator, PREFERRED_MEDIA_FORMATS);
        processor1.addControllerListener(new ProcessorStarter());
        PushBufferDataSource pbds1 = (PushBufferDataSource) processor1.getDataOutput();
        _replicator = new PBDSReplicator(pbds1);

        _result = null;
        _engine.activate();

        Processor processor2 = JMFUtil.createRealizedProcessor(_replicator.replicate(),PREFERRED_MEDIA_FORMATS);
        processor2.addControllerListener(new ProcessorStarter());

        PushBufferDataSource pbds2 = (PushBufferDataSource) processor2.getDataOutput();
        _engine.startRecognition(pbds2, this);
        processor2.start();
        Thread.sleep(1000);  // give processor2 a chance to start
        // TODO: find better solution for timing processor starting
        processor1.start();

        _logger.debug("Starting recog thread...");
        _engine.startRecogThread();

        // wait for result
        RecognitionResult result = null;
        synchronized (this) {
            while (_result == null) {
                this.wait(1000);
            }
            result = _result;
            _result = null;
        }

        _engine.passivate();

        return result;
    }

    public static void main(String[] args) throws Exception {
        URL url;
        if (args.length > 0) {
            url = new File(args[0]).toURL();
        } else {
            url = SphinxRecEngine.class.getResource("/config/sphinx-config.xml");
        }
        
        if (url == null) {
            throw new RuntimeException("Sphinx config file not found!");
        }

        _logger.debug("Loading...");
        ConfigurationManager cm = new ConfigurationManager(url);
        SphinxRecEngine engine = new SphinxRecEngine(cm,1);

        // commented out since SphinxRecEngine._jsgfGrammar not visible from this class
//        if (_logger.isDebugEnabled()) {
//            for (int i=0; i < 12; i++) {
//                _logger.debug(engine._jsgfGrammar.getRandomSentence());
//            }
//        }

        RunSphinxRecEngine runner = new RunSphinxRecEngine(engine);
        

        RecognitionResult result;
        while (true) {
            result = runner.doRecognize(MICROPHONE);
        }

//        RuleParse ruleParse = engine.parse("", "main");

    }


}

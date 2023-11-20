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
import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;
import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;

import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;
import org.speechforge.cairo.rtp.server.sphinx.SpeechDataMonitor;
import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.RecogListener;
import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.rtp.server.SpeechEventListener;
import org.speechforge.cairo.rtp.server.PBDSReplicator;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.util.pool.AbstractPoolableObject;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.speech.recognition.GrammarException;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Provides a poolable recognition engine that takes raw audio data as input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class SphinxRecEngine extends AbstractPoolableObject implements SpeechEventListener {
    /** Logger instance. */
    private static final Logger LOGGER =
            LogManager.getLogger(SphinxRecEngine.class);
    private static Toolkit _toolkit = LOGGER.isTraceEnabled()? Toolkit.getDefaultToolkit() : null;

    private int _id;
    private Recognizer recognizer;
    private JSGFGrammar _jsgfGrammar;
    private RawAudioProcessor _rawAudioProcessor;

    private RawAudioTransferHandler _rawAudioTransferHandler;
    RecogListener _recogListener;
    
    private boolean hotword = false;

    public SphinxRecEngine(ConfigurationManager cm, int id)
      throws IOException, PropertyException, InstantiationException {

    	LOGGER.info("Creating Engine # " + id);
    	_id = id;
        recognizer = (Recognizer) cm.lookup("recognizer" + id);
        if (recognizer == null) {
            throw new InstantiationException("No configuration for recognizer" 
                    + id + " found in the sphinx configuration");
        }
        recognizer.allocate();

        _jsgfGrammar = (JSGFGrammar) cm.lookup("grammar");

        SpeechDataMonitor speechDataMonitor = (SpeechDataMonitor) cm.lookup("speechDataMonitor"+id);
        if (speechDataMonitor != null) {
            speechDataMonitor.setSpeechEventListener(this);
        }

        Object primaryInput = cm.lookup("primaryInput"+id);
        if (primaryInput instanceof RawAudioProcessor) {
            _rawAudioProcessor = (RawAudioProcessor) primaryInput;
        } else {
            String className = (primaryInput == null) ? null : primaryInput.getClass().getName();
            throw new InstantiationException("Unsupported primary input type: " + className);
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#activate()
     */
    @Override
    public synchronized void activate() {
        LOGGER.debug("SphinxRecEngine #"+_id +" activating...");
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.util.pool.PoolableObject#passivate()
     */
    @Override
    public synchronized void passivate() {
        LOGGER.debug("SphinxRecEngine #"+_id +"passivating...");
        stopProcessing();
        _recogListener = null;
    }

    /**
     * TODOC
     */
    public synchronized void stopProcessing() {
        LOGGER.debug("SphinxRecEngine  #"+_id +"stopping processing...");
        if (_rawAudioTransferHandler != null) {
            _rawAudioTransferHandler.stopProcessing();
            _rawAudioTransferHandler = null;
        }
        // TODO: should wait to set this until after run thread completes (i.e. recognizer is cleared)
    }

    /**
     * TODOC
     * @param grammarLocation the location of the grammar to load
     * @throws IOException error reading from the grammar location
     * @throws GrammarException error parsing the grammar
     * @throws JSGFGrammarException error parsing the grammar
     * @throws JSGFGrammarParseException error parsing the grammar
     */
    public synchronized void loadJSGF(GrammarLocation grammarLocation) throws IOException, GrammarException, JSGFGrammarParseException, JSGFGrammarException {
    	
        _jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
        _jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
       LOGGER.debug("loadJSGF(): completed successfully.");

    }

    /**
     * TODOC
     * @param text the recognized input text
     * @param ruleName the rule name to use for parsing
     * @return parsed rule
     * @throws GrammarException error in the grammar or while parsing
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        if (_rawAudioTransferHandler != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }
        
        RuleGrammar ruleGrammar = (RuleGrammar) _jsgfGrammar.getRuleGrammar();
        return ruleGrammar.parse(text, ruleName);
    }

    /**
     * TODOC
     * @param dataSource the data source
     * @param listener  the listener
     * @throws UnsupportedEncodingException
     * 			encoding not supported 
     */
    public synchronized void startRecognition(PushBufferDataSource dataSource, RecogListener listener)
      throws UnsupportedEncodingException {

        LOGGER.debug("SphinxRecEngine  #"+_id +"starting  recognition...");
        if (_rawAudioTransferHandler != null) {
            throw new IllegalStateException("Recognition already in progress!");
        }

        PushBufferStream[] streams = dataSource.getStreams();
        if (streams.length != 1) {
            throw new IllegalArgumentException(
                "Rec engine can handle only single stream datasources, # of streams: " + streams);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting recognition on stream format: " + streams[0].getFormat());
        }
        try {
            _rawAudioTransferHandler = new RawAudioTransferHandler(_rawAudioProcessor);
            _rawAudioTransferHandler.startProcessing(streams[0]);
        } catch (UnsupportedEncodingException e) {
            _rawAudioTransferHandler = null;
            throw e;
        }

        _recogListener = listener;
    }

    // TODO: rename method
    public void startRecogThread() {
        new RecogThread().start();
    }

    private RecognitionResult waitForResult(boolean hotword) {
        Result result = null;
        
        LOGGER.debug("The hotword flag is: "+hotword);
        //if hotword mode, run recognize until a match occurs
        if (hotword) {
            RecognitionResult rr = new RecognitionResult();
            boolean inGrammarResult = false;
            while (!inGrammarResult) {
                 result = recognizer.recognize();

                 if (result == null) {
                     LOGGER.debug("result is null");
                 } else {
                     LOGGER.debug("result is:"+result.toString());
                 }
                 rr.setNewResult(result, (RuleGrammar) _jsgfGrammar.getRuleGrammar());
                 LOGGER.debug("Rec result: "+rr.toString());
                 LOGGER.debug("text:"+rr.getText()+" matches:"+rr.getRuleMatches()+" oog flag:"+rr.isOutOfGrammar());
                 if( (!rr.getRuleMatches().isEmpty()) && (!rr.isOutOfGrammar())) {
                     inGrammarResult = true;
                 }
            }
         
        //if not hotword, just run recognize once
        } else {
             result = recognizer.recognize();
        }
        stopProcessing();
        if (result != null) {
            Result result2clear = recognizer.recognize();
            if (result2clear != null) {
                LOGGER.debug("waitForResult(): result2clear not null!");
            }
        } else {
            LOGGER.info("waitForResult(): got null result from recognizer!");
            return null;
        }
        return new RecognitionResult(result, (RuleGrammar) _jsgfGrammar.getRuleGrammar());

    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.SpeechEventListener#speechStarted()
     */
    public void speechStarted() {
        if (_toolkit != null) {
            _toolkit.beep();
        }

        RecogListener recogListener = null;
        synchronized (this) {
            recogListener = _recogListener; 
        }

        if (recogListener == null) {
            LOGGER.debug("speechStarted(): _recogListener is null!");
        } else {
            recogListener.speechStarted();
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.SpeechEventListener#speechEnded()
     */
    public void speechEnded() {
        if (_toolkit != null) {
            _toolkit.beep();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner classes
    ///////////////////////////////////////////////////////////////////////////

    private class RecogThread extends Thread {
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            LOGGER.debug("RecogThread waiting for result...");

            RecognitionResult result = SphinxRecEngine.this.waitForResult(hotword);

            if (LOGGER.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                sb.append("\n**************************************************************");
                sb.append("\nRecogThread got result: ").append(result);
                sb.append("\nUtterance"+result.getRawResult().getBestToken().getWordUnitPath());

                sb.append("\n**************************************************************");
                LOGGER.debug(sb);
            }
            
            RecogListener recogListener = null;
            synchronized (SphinxRecEngine.this) {
                recogListener = _recogListener;
            }

            if (recogListener == null) {
                LOGGER.debug("RecogThread.run(): _recogListener is null!");
            } else {
                recogListener.recognitionComplete(result);
            }
        }
    }

    /**
     * Provides a client for testing {@link org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine}
     * in standalone mode using the microphone for input.
     */
    public static class Test extends RecogListenerDecorator {

        private SphinxRecEngine _engine;
        private RecognitionResult _result;
        private PBDSReplicator _replicator;

        public Test(SphinxRecEngine engine)
          throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
            super(null);
            _engine = engine;
            _replicator = createMicrophoneReplicator();
        }

        /* (non-Javadoc)
         * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
         */
        @Override
        public synchronized void recognitionComplete(RecognitionResult result) {
            _result = result;
            this.notify();
        }

        public RecognitionResult doRecognize() throws IOException, NoProcessorException, CannotRealizeException,
                InterruptedException {

            _result = null;
            _engine.activate();

            Processor processor = createReplicatedProcessor();
            processor.addControllerListener(new ProcessorStarter());

            PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
            _engine.startRecognition(pbds, this);
            processor.start();
            LOGGER.debug("Performing recognition...");
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

        private Processor createReplicatedProcessor() throws IOException,
                IllegalStateException, NoProcessorException,
                CannotRealizeException {
            
            ProcessorModel pm = new ProcessorModel(
                    _replicator.replicate(),
                    PREFERRED_MEDIA_FORMATS,
                    CONTENT_DESCRIPTOR_RAW
            );
            
            LOGGER.debug("Creating realized processor...");
            Processor processor = Manager.createRealizedProcessor(pm);
            LOGGER.debug("Processor realized.");
            
            return processor;
        }

        private static Processor createMicrophoneProcessor()
          throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

            DataSource dataSource = Manager.createDataSource(MICROPHONE);
            ProcessorModel pm = new ProcessorModel(dataSource,
                    PREFERRED_MEDIA_FORMATS, CONTENT_DESCRIPTOR_RAW);
            Processor processor = Manager.createRealizedProcessor(pm);
            return processor;
        }

        private static PBDSReplicator createMicrophoneReplicator()
          throws NoProcessorException, NoDataSourceException, CannotRealizeException, IOException {
            Processor processor = createMicrophoneProcessor();
            processor.addControllerListener(new ProcessorStarter());
            PushBufferDataSource pbds = (PushBufferDataSource) processor.getDataOutput();
            PBDSReplicator replicator = new PBDSReplicator(pbds);
            processor.start();
            return replicator;
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

            LOGGER.info("Loading...");
            ConfigurationManager cm = new ConfigurationManager(url);
            SphinxRecEngine engine = new SphinxRecEngine(cm,1);

            if (LOGGER.isDebugEnabled()) {
                for (int i=0; i < 12; i++) {
                    LOGGER.debug(engine._jsgfGrammar.getRandomSentence());
                }
            }

            Test test = new Test(engine);
            

            RecognitionResult result;
            while (true) {
                result = test.doRecognize();
            }

//            RuleParse ruleParse = engine.parse("", "main");


            //System.exit(0);
        }

    }

    /**
     * @return the hotword
     */
    public boolean isHotword() {
        return hotword;
    }

    /**
     * @param hotword the hotword to set
     */
    public void setHotword(boolean hotword) {
        this.hotword = hotword;
    }

}

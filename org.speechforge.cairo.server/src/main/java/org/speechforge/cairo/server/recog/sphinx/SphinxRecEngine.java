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

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;
import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;
import static org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat.PREFERRED_MEDIA_FORMATS;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.rtp.server.PBDSReplicator;
import org.speechforge.cairo.rtp.server.SpeechEventListener;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;
import org.speechforge.cairo.rtp.server.sphinx.SpeechDataMonitor;
import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.RecogListener;
import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.util.pool.AbstractPoolableObject;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Provides a poolable recognition engine that takes raw audio data as input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class SphinxRecEngine extends AbstractPoolableObject
    implements SpeechEventListener, ResultListener {
    /** Logger instance. */
    private static final Logger LOGGER =
            LogManager.getLogger(SphinxRecEngine.class);
    private static Toolkit _toolkit = 
            LOGGER.isTraceEnabled() ? Toolkit.getDefaultToolkit() : null;
    /** The recognizer engine id. */
    private int id;
    /** The Sphinx recognizer. */
    private CairoSphinxRecognizer recognizer;
    private JSGFGrammar _jsgfGrammar;
    RecogListener _recogListener;
    
    private boolean hotword = false;

    /**
     * Constructs and initializes a new SphinxRecEngine using the supplied
     * Sphinx configuration XML and engine identifier.
     * <p>
     * This constructor performs the following actions:
     * <ul>
     *   <li>Creates a {@link edu.cmu.sphinx.util.props.ConfigurationManager}
     *       from the provided URL and looks up configuration entries using
     *       the following keys:
     *       <ul>
     *         <li>"primaryInput" + engineId — expected to be a
     *             {@link org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor}.</li>
     *         <li>"grammar" — expected to be a {@link JSGFGrammar} (may be null).</li>
     *         <li>"speechDataMonitor" + engineId — optional SpeechDataMonitor; if
     *             present, it will be registered with this engine as the
     *             {@link org.speechforge.cairo.rtp.server.SpeechEventListener}.</li>
     *       </ul>
     *   </li>
     *   <li>Builds a Sphinx {@link edu.cmu.sphinx.api.Configuration} and sets
     *       the following default resources:
     *       <ul>
     *         <li>Acoustic model: "resource:/edu/cmu/sphinx/models/en-us/en-us"</li>
     *         <li>Dictionary: "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict"</li>
     *         <li>Language model: "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"</li>
     *       </ul>
     *   </li>
     *   <li>Creates a {@link Context} and a {@link CairoSphinxRecognizer} using
     *       the constructed configuration and the looked-up RawAudioProcessor,
     *       then calls {@code recognizer.allocate()} to initialize the recognizer.
     *   </li>
     * </ul>
     * <p>
     * Note: the constructor does not validate every returned component; a
     * missing grammar will leave {@link #_jsgfGrammar} null which may cause
     * NPEs when grammar methods are invoked later.
     *
     * @param sphinxConfigURL URL of the Sphinx configuration XML (must not be null)
     * @param engineId        engine identifier used to select engine-specific
     *                        configuration entries (e.g. primaryInput{n})
     * @throws NullPointerException if {@code sphinxConfigURL} is null
     * @throws IOException            if the configuration resource cannot be read
     * @throws PropertyException      if required configuration properties are missing or invalid
     * @throws InstantiationException if the primary input is missing or is not a RawAudioProcessor
     */
    public SphinxRecEngine(URL sphinxConfigURL, int engineId)
      throws IOException, PropertyException, InstantiationException {
        LOGGER.info("Creating Engine # " + engineId);
        id = engineId;
        
        ConfigurationManager cm = new ConfigurationManager(sphinxConfigURL);
        Object primaryInput = cm.lookup("primaryInput" + engineId);
        RawAudioProcessor rawAudioProcessor = null;
        if (primaryInput instanceof RawAudioProcessor) {
            rawAudioProcessor = (RawAudioProcessor) primaryInput;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("RawAudioProcessor #" + engineId + ": "
                        + rawAudioProcessor);
            }
        } else {
            String className = (primaryInput == null) 
                    ? null : primaryInput.getClass().getName();
            throw new InstantiationException("Unsupported primary input type: "
                    + className);
        }

        // Create a configuration to supply the required and optional attributes
        // to the recognizer.
        final Configuration configuration = new Configuration();
        configuration.setAcousticModelPath(
                "resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath(
                "resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath(
                "resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        final Context context = new Context(sphinxConfigURL.toString(),
                configuration);
        recognizer = new CairoSphinxRecognizer(context, rawAudioProcessor);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Recognizer #" + engineId + ": " + recognizer);
        }
        recognizer.allocate();

        _jsgfGrammar = (JSGFGrammar) cm.lookup("grammar");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("JSGF Grammar #" + engineId + ": " + _jsgfGrammar);
        }

        SpeechDataMonitor speechDataMonitor = 
                (SpeechDataMonitor) cm.lookup("speechDataMonitor" + engineId);
        if (speechDataMonitor != null) {
            speechDataMonitor.setSpeechEventListener(this);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SpeechDataMonitor #" + engineId + ": "
                        + speechDataMonitor);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void activate() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SphinxRecEngine #" + id + " activating...");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void passivate() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SphinxRecEngine #" + id + " passivating...");
        }
        stopProcessing();
        _recogListener = null;
    }

    /**
     * Stops recognition processing on the underlying recognizer.
     * <p>
     * This requests the recognizer to stop any in-progress recognition. The
     * method is synchronized to ensure safe concurrent access. Note that the
     * recognizer's internal thread may still be completing work after this
     * method returns; callers should not assume immediate termination of all
     * recognizer activity.
     */
    public synchronized void stopProcessing() {
        LOGGER.debug("SphinxRecEngine  #" + id + " stopping processing...");
        recognizer.stopRecognition();
        // TODO: should wait to set this until after run thread completes (i.e. recognizer is cleared)
    }

    /**
     * Loads a JSGF grammar from the supplied grammar location into the
     * configured JSGFGrammar instance.
     *
     * @param grammarLocation the location of the grammar to load (contains base URL and grammar name)
     * @throws IOException if an I/O error occurs while accessing the grammar
     * @throws GrammarException if the grammar is invalid or cannot be parsed
     * @throws JSGFGrammarParseException if the grammar parser fails to parse the grammar
     * @throws JSGFGrammarException for other JSGF grammar related errors
     */
    public synchronized void loadJSGF(GrammarLocation grammarLocation) throws IOException, GrammarException, JSGFGrammarParseException, JSGFGrammarException {
        
        _jsgfGrammar.setBaseURL(grammarLocation.getBaseURL());
        _jsgfGrammar.loadJSGF(grammarLocation.getGrammarName());
       LOGGER.debug("loadJSGF(): completed successfully.");

    }

    /**
     * Parses the supplied text against a rule in the currently loaded
     * JSGF grammar and returns the resulting RuleParse.
     *
     * @param text the recognized input text to parse
     * @param ruleName the rule name to use within the grammar
     * @return a RuleParse representing the parse result
     * @throws GrammarException if the grammar is invalid or parsing fails
     * @throws IllegalStateException if recognition is currently in progress
     */
    public synchronized RuleParse parse(String text, String ruleName) throws GrammarException {
        if (recognizer.isRecognizing()) {
            throw new IllegalStateException("Recognition already in progress!");
        }
        
        RuleGrammar ruleGrammar = (RuleGrammar) _jsgfGrammar.getRuleGrammar();
        return ruleGrammar.parse(text, ruleName);
    }

    /**
     * Begins recognition using the provided PushBufferDataSource as the
     * audio input source and registers the listener to receive completion
     * notifications.
     *
     * @param dataSource the audio data source (PushBufferDataSource)
     * @param listener the listener to notify when recognition completes
     * @throws UnsupportedEncodingException if the data encoding is not supported
     * @throws IllegalStateException if recognition is already in progress
     */
    public synchronized void startRecognition(PushBufferDataSource dataSource, RecogListener listener)
      throws UnsupportedEncodingException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "SphinxRecEngine #" + id + " starting recognition...");
        }
        if (recognizer.isRecognizing()) {
            throw new IllegalStateException("Recognition already in progress!");
        }

        recognizer.startRecognition(dataSource);
        _recogListener = listener;
    }

    // TODO: rename method
//    public void startRecogThread() {
//        new RecogThread().start();
//    }

//    private RecognitionResult waitForResult(boolean hotword) {
//        Result result = null;
//        
//        //if hotword mode, run recognize until a match occurs
//        if (hotword) {
//            RecognitionResult rr = new RecognitionResult();
//            boolean inGrammarResult = false;
//            while (!inGrammarResult) {
//                 result = recognizer.recognize();
//                 if (LOGGER.isDebugEnabled()) {
//                     if (result == null) {
//                         LOGGER.debug("result is null");
//                     } else {
//                         LOGGER.debug("result is: " + result.toString());
//                     }
//                 }
//                 rr.setNewResult(result, (RuleGrammar) _jsgfGrammar.getRuleGrammar());
//                 LOGGER.debug("Rec result: "+rr.toString());
//                 LOGGER.debug("text:"+rr.getText()+" matches:"+rr.getRuleMatches()+" oog flag:"+rr.isOutOfGrammar());
//                 if( (!rr.getRuleMatches().isEmpty()) && (!rr.isOutOfGrammar())) {
//                     inGrammarResult = true;
//                 }
//            }
//         
//        //if not hotword, just run recognize once
//        } else {
//            try {
//                LOGGER.info("starting recognition on recognizer engine #" + id +
//                        " (" + recognizer + ")...");
//                result = recognizer.recognize();
//            } catch (IllegalStateException e) {
//                LOGGER.warn("error waiting for result " + e.getMessage(), e);
//            } 
//        }
//        stopProcessing();
//        if (result != null) {
//            Result result2clear = recognizer.recognize();
//            if (result2clear != null) {
//                LOGGER.debug("waitForResult(): result2clear not null!");
//            }
//        } else {
//            LOGGER.info("got no result from recognizer!");
//            return null;
//        }
//        return new RecognitionResult(result, (RuleGrammar) _jsgfGrammar.getRuleGrammar());
//
//    }

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

    /**
     * Called when the speech data monitor detects the end of speech.
     * <p>
     * Currently this method only emits a debug beep when trace logging is
     * enabled. It is provided for symmetry with {@link #speechStarted()} and
     * for future extension.
     */
    public void speechEnded() {
        if (_toolkit != null) {
            _toolkit.beep();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // inner classes
    ///////////////////////////////////////////////////////////////////////////

//    private class RecogThread extends Thread {
//        
//        /* (non-Javadoc)
//         * @see java.lang.Runnable#run()
//         */
//        @Override
//        public void run() {
//            LOGGER.debug("RecogThread waiting for result...");
//
//            RecognitionResult result = SphinxRecEngine.this.waitForResult(hotword);
//
//            if (LOGGER.isDebugEnabled() && (result != null)) {
//                StringBuilder sb = new StringBuilder();
//                sb.append("\n**************************************************************");
//                sb.append("\nRecogThread got result: ").append(result);
//                sb.append("\nUtterance"+result.getRawResult().getBestToken().getWordUnitPath());
//
//                sb.append("\n**************************************************************");
//                LOGGER.debug(sb);
//            }
//            
//            RecogListener recogListener = null;
//            synchronized (SphinxRecEngine.this) {
//                recogListener = _recogListener;
//            }
//
//            if (recogListener == null) {
//                LOGGER.debug("RecogThread.run(): _recogListener is null!");
//            } else {
//                recogListener.recognitionComplete(result);
//            }
//        }
//    }

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
//            _engine.startRecogThread();

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
            SphinxRecEngine engine = new SphinxRecEngine(url, 1);

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

    /**
     * Receives new property values from the Sphinx property infrastructure.
     * <p>
     * This implementation is intentionally empty; subclasses or users may
     * override it to react to property changes.
     *
     * @param ps the PropertySheet containing new property values
     * @throws PropertyException if required properties are missing or invalid
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        // No-op: provided for interface compliance and future extension.
    }

    /**
     * Called by the recognizer when a new recognition result is produced.
     * <p>
     * The engine stops the recognizer, wraps the result in a
     * {@link RecognitionResult} (including the configured RuleGrammar) and
     * notifies the registered RecogListener, if any.
     *
     * @param result the raw Sphinx recognition result
     */
    @Override
    public void newResult(Result result) {
        stopProcessing();
        LOGGER.info("got no result from recognizer: " + result);
        final RecognitionResult recogResult =
                new RecognitionResult(result, (RuleGrammar) _jsgfGrammar.getRuleGrammar());
        if (_recogListener == null) {
            LOGGER.warn("No listener to notify!");
        } else {
            _recogListener.recognitionComplete(recogResult);
        }

    }

}

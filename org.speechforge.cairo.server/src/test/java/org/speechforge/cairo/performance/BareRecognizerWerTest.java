package org.speechforge.cairo.performance;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.speechforge.cairo.server.recog.GrammarLocation;

/**
 * A simple Sphinx-4 application that decodes a .WAV file containing...
 * The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 */
public class BareRecognizerWerTest extends BaseRecognizerWerTest{
    
    private static Logger LOGGER = LogManager.getLogger(BareRecognizerWerTest.class);
  
    public void  shutdown() {
        
    }
    
    public  void setUp(URL config) {
        testCount = 0;
        AccumulatedWER = 0.0;
        //URL configURL = StandaloneSphinxTest.class.getResource("config.xml");

        try {
            //URL configURL = new URL(config);
            LOGGER.info("Loading Recognizer...\n");
            LOGGER.info(config.toString());
            cm = new ConfigurationManager(config);

            recognizer = (Recognizer) cm.lookup("recognizer");
            jsgfGrammarManager = (JSGFGrammar) cm.lookup("grammar");
            
            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();
        } catch (PropertyException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    public  String recognizeAudioFile(URL audioFileURL) {   
      Result result = null;
      try {
            LOGGER.info("Test file: "+audioFileURL.getFile());
            LOGGER.info(AudioSystem.getAudioFileFormat(audioFileURL));
            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
            AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);
            
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais);

            result = recognizer.recognize();

        } catch (IOException e) {
            LOGGER.warn("Problem when loading WavFile: " + e, e);
        } catch (PropertyException e) {
            LOGGER.warn("Problem configuring WavFile: " + e, e);
        } catch (UnsupportedAudioFileException e) {
            LOGGER.warn("Audio file format not supported: " + e, e);
        }
        return result.getBestFinalResultNoFiller();
        
    }
    
    public  void processGrammarLocation(GrammarLocation grammarLocation) throws IOException, JSGFGrammarParseException, JSGFGrammarException {
        jsgfGrammarManager.setBaseURL(grammarLocation.getBaseURL());
        jsgfGrammarManager.loadJSGF(grammarLocation.getGrammarName());
    }

    public static void main(String[] args) {
        LOGGER.info("Stating up with config file: "+args[0]);
        BaseRecognizerWerTest rp = new BareRecognizerWerTest();
        rp.runTests(args[0]);   
    }
    
}

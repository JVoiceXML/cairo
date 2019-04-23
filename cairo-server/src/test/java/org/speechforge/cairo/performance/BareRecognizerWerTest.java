package org.speechforge.cairo.performance;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.speechforge.cairo.server.recog.GrammarLocation;

/**
 * A simple Sphinx-4 application that decodes a .WAV file containing...
 * The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 */
public class BareRecognizerWerTest extends BaseRecognizerWerTest{
    
    private static Logger _logger = Logger.getLogger(BareRecognizerWerTest.class);
  
    public void  shutdown() {
        
    }
    
    public  void setUp(URL config) {
        testCount = 0;
        AccumulatedWER = 0.0;
        //URL configURL = StandaloneSphinxTest.class.getResource("config.xml");

        try {
            //URL configURL = new URL(config);
            System.out.println("Loading Recognizer...\n");
            System.out.println(config.toString());
            cm = new ConfigurationManager(config);

            recognizer = (Recognizer) cm.lookup("recognizer");
            jsgfGrammarManager = (JSGFGrammar) cm.lookup("grammar");
            
            /* allocate the resource necessary for the recognizer */
            recognizer.allocate();
        } catch (PropertyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public  String recognizeAudioFile(URL audioFileURL) {   
      Result result = null;
      try {
            _logger.info("Test file: "+audioFileURL.getFile());
            _logger.info(AudioSystem.getAudioFileFormat(audioFileURL));
            StreamDataSource reader = (StreamDataSource) cm.lookup("streamDataSource");
            AudioInputStream ais = AudioSystem.getAudioInputStream(audioFileURL);
            
            /* set the stream data source to read from the audio file */
            reader.setInputStream(ais, audioFileURL.getFile());

            result = recognizer.recognize();

        } catch (IOException e) {
            System.err.println("Problem when loading WavFile: " + e);
            e.printStackTrace();
        } catch (PropertyException e) {
            System.err.println("Problem configuring WavFile: " + e);
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Audio file format not supported: " + e);
            e.printStackTrace();
        }
        return result.getBestFinalResultNoFiller();
        
    }
    
    public  void processGrammarLocation(GrammarLocation grammarLocation) throws IOException {
        jsgfGrammarManager.setBaseURL(grammarLocation.getBaseURL());
        jsgfGrammarManager.loadJSGF(grammarLocation.getGrammarName());
    }

    public static void main(String[] args) {
        System.out.println("Stating up with config file: "+args[0]);
        BaseRecognizerWerTest rp = new BareRecognizerWerTest();
        rp.runTests(args[0]);   
    }
    
}

package org.speechforge.cairo.performance;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.jsapi.JSGFGrammar;

import edu.cmu.sphinx.recognizer.Recognizer;

import edu.cmu.sphinx.result.Result;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;
import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine;
import org.speechforge.cairo.rtp.server.PBDSReplicator;
import org.speechforge.cairo.test.sphinx.util.RecogNotifier;
import org.speechforge.cairo.jmf.JMFUtil;
import org.speechforge.cairo.jmf.ProcessorStarter;


/**
 * A simple Sphinx-4 application that decodes a .WAV file containing...
 * The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 */
public class ReplicatorRecognizerWerTest extends BaseRecognizerWerTest{
    
    private static Logger _logger = Logger.getLogger(ReplicatorRecognizerWerTest.class);
    ConfigurationManager cm;
    Recognizer recognizer;
    SphinxRecEngine engine;

    String grammarFileName;


    double AccumulatedWER;
    int testCount;


    public void  shutdown() {

    }
      
    public  void setUp(URL config) {
        testCount = 0;
        AccumulatedWER = 0.0;

        try {
            System.out.println("Loading Recognizer...\n");
            cm = new ConfigurationManager(config);
            jsgfGrammarManager = (JSGFGrammar) cm.lookup("grammar");
            engine = new SphinxRecEngine(cm,1);
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PropertyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

 
    public String recognizeAudioFile(URL audioFileURL) {

        
        AudioFileFormat fileFormat = null;
        try {
             fileFormat = AudioSystem.getAudioFileFormat(audioFileURL);
        } catch (UnsupportedAudioFileException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }      
        
        _logger.info("Test file: "+audioFileURL.getFile());
        _logger.info(fileFormat.toString());
        javax.media.format.AudioFormat format =  new javax.media.format.AudioFormat(
                javax.media.format.AudioFormat.LINEAR,                           //encoding
                fileFormat.getFormat().getSampleRate(),                          //sample rate
                fileFormat.getFormat().getSampleSizeInBits(),                   //sample size in bits
                1,                                                              //channels
                javax.media.format.AudioFormat.LITTLE_ENDIAN,
                javax.media.format.AudioFormat.SIGNED
            );

        Processor processor1 = null;
        try {
            processor1 = JMFUtil.createRealizedProcessor(new MediaLocator(audioFileURL), format);
        } catch (NoProcessorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoDataSourceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        processor1.addControllerListener(new ProcessorStarter(true));

        PushBufferDataSource pbds1 = (PushBufferDataSource) processor1.getDataOutput();
        PBDSReplicator replicator = new PBDSReplicator(pbds1);

        DataSource dataSource = replicator.replicate();

        ProcessorModel pm = new ProcessorModel(
                dataSource,
                new AudioFormat[] { replicator.getAudioFormat() },
                JMFUtil.CONTENT_DESCRIPTOR_RAW
        );

        Processor processor2 = null;
        try {
            processor2 = Manager.createRealizedProcessor(pm);
        } catch (NoProcessorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        processor2.addControllerListener(new ProcessorStarter(true));
        PushBufferDataSource pbds2 = (PushBufferDataSource) processor2.getDataOutput();

        engine.activate();

        RecogNotifier listener = new RecogNotifier();
        try {
            engine.startRecognition(pbds2, listener);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        processor2.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  // give processor2 a chance to start
        processor1.start();

        engine.startRecogThread();

        // wait for result
        RecognitionResult result = null;
        synchronized (listener) {
            while ((result = listener.getResult()) == null) {
                try {
                    listener.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        engine.passivate();
        return result.getText();
    }

    public void processGrammarLocation(GrammarLocation grammarLocation) throws IOException {
        jsgfGrammarManager.setBaseURL(grammarLocation.getBaseURL());
        jsgfGrammarManager.loadJSGF(grammarLocation.getGrammarName());
    }
    
    public static void main(String[] args) {
        System.out.println("Stating up with config file: "+args[0]);
        BaseRecognizerWerTest rp = new ReplicatorRecognizerWerTest();
        rp.runTests(args[0]);
        
        
    }
    
}

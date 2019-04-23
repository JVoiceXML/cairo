package org.speechforge.cairo.performance;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;
import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.ConfidenceResult;
import edu.cmu.sphinx.result.ConfidenceScorer;
import edu.cmu.sphinx.result.Path;
import edu.cmu.sphinx.result.WordResult;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.media.Processor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.sdp.SdpConstants;
import javax.speech.recognition.GrammarException;
import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import org.speechforge.cairo.exception.ResourceUnavailableException;
import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.RTPRecogChannel;
import org.speechforge.cairo.server.recog.RecogListener;
import org.speechforge.cairo.server.recog.RecognitionResult;
import org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine;
import org.speechforge.cairo.server.recog.sphinx.SphinxRecEngineFactory;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;
import org.speechforge.cairo.rtp.server.RTPStreamReplicatorFactory;
import org.speechforge.cairo.rtp.RTPPlayer;
import org.speechforge.cairo.jmf.ProcessorStarter;
import org.speechforge.cairo.rtp.AudioFormats;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator.ProcessorReplicatorPair;
import org.speechforge.cairo.rtp.server.sphinx.SourceAudioFormat;


/**
 * A simple Sphinx-4 application that decodes a .WAV file containing...
 * The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 */
public class RTPRecognizerWerTest extends BaseRecognizerWerTest{
    
    private static Logger _logger = Logger.getLogger(RTPRecognizerWerTest.class);

    private static final Long LONG_MINUS_ONE = new Long(-1);
    private static final Long FIFTEENSECS = new Long(15000);
    private static final Long TENSECS = new Long(10000);
    private static final Long FIVESECS = new Long(5000);
    private static final Long ONESECOND = new Long(1000);
    private static final Long HALFSECOND = new Long(500);
    
    private /*static*/ Timer _timer = new Timer();
    private NoResultTimeoutTask _noResultTimeoutTask;
    
    
      private ConfigurationManager cm;
      //private Recognizer recognizer;
      private SphinxRecEngine engine;
    
      String grammarFileName;
      double AccumulatedWER;
      int testCount;
      RTPPlayer audioFilePlayer;
      GrammarLocation grammarLocation;
      RTPStreamReplicator replicator;
      ConfidenceScorer scorer;
      PlayThread p;

	private ProcessorReplicatorPair pair;


      
      private static int RECEIVERPORT = 48050;
      private static int XMITTERPORT = 48000;
      private static String  RECEIVERADDRESS = "192.168.0.100"; 
      //private static String XMITTERADDRESS = "localhost";      
      
      public  void setUp(URL config) {
          testCount = 0;
          AccumulatedWER = 0.0;  

          //Get the recogntion engine
          try {
              cm = new ConfigurationManager(config);
              engine = new SphinxRecEngine(cm,1);
              scorer = (ConfidenceScorer) cm.lookup("confidenceScorer");
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

          
          // Setup the RTP player (to be used to send test waves down the rtp pipe)
          InetAddress address = null;
          try {
              address = InetAddress.getByName(RECEIVERADDRESS);
          } catch (UnknownHostException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          } 

          //create the audio file player
          AudioFormats af = new AudioFormats();
          Vector v = new Vector();
          v.add(SdpConstants.PCMU);
          af.setRequestedFormatsSDP(v);
          try {
              audioFilePlayer = new RTPPlayer(null,XMITTERPORT, address, RECEIVERPORT,af);
          } catch (InvalidSessionAddressException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

          // start up the player thread.  It is waiting for play commands (with test file as the parameter)
          (p = new PlayThread()).start();
          
          
          //Set up the RTP Stream replicator that will receive the rtp stream (sent by the play thread) and
          //stream the audio data  to the rec engine 
          try {
              replicator = new RTPStreamReplicator(RECEIVERPORT);
          } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }

      }

 
    public String recognizeAudioFile(URL audioFileURL) {

        Processor processor = null;
         Listener listener = new Listener();
        //engine.activate();
        try {
            File f = new File(audioFileURL.getFile()); 
            while ( p.playPrompt(f,listener) == -1 ) {
                _logger.info("Still playing the previous prompt.  Trying again...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    //TODO Auto-generated catch block
                    e1.printStackTrace();
                }               
            }
            pair  = replicator.createRealizedProcessor(CONTENT_DESCRIPTOR_RAW, 10000,SourceAudioFormat.PREFERRED_MEDIA_FORMATS); // TODO: specify audio format
            processor = pair.getProc();
            PushBufferDataSource dataSource = (PushBufferDataSource) processor.getDataOutput();

            if (dataSource == null) {
                throw new IOException("Processor.getDataOutput() returned null!");
            }     

            _logger.debug("Loading grammar...");
            engine.loadJSGF(grammarLocation);

            _logger.debug("Starting recognition...");
            engine.startRecognition(dataSource, listener);

            processor.addControllerListener(new ProcessorStarter());

            processor.start();
            engine.startRecogThread();

        } catch (GrammarException e) {
            //closeProcessor();
            e.printStackTrace();
        } catch (IOException e) {
            //closeProcessor();
            e.printStackTrace();
        }

        //Wait for a result or timeout (AND play completed)  See listener for details
        synchronized (listener) {
            while ((listener.isDone() == false)) {
                try {
                    listener.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        String result = null;
        RecognitionResult r = listener.getResult();
        
        //
        //Commented out becasue the Confidence Scoring only works with  LexTreeLinguist and the WordPruningBreadthFirstSearchManager
        //
        //ConfidenceResult confidenceResult = scorer.score(r.getRawResult());
        // confidence for best path 
        //Path bestPath = confidenceResult.getBestHypothesis(); 
        //double pathConfidence = bestPath.getConfidence();
        //System.out.println(bestPath.toString()+ " confidence: "+pathConfidence+"/"+bestPath.getScore());

        // confidence for each word in best path 
        //WordResult[] words = bestPath.getWords(); 
        //for (int i = 0; i < words.length; i++) { 
        //    WordResult wordResult = (WordResult) words[i]; 
        //    double wordConfidence = wordResult.getConfidence(); 
        //    System.out.println(wordResult.toString()+" confidence: "+wordConfidence);
        //} 


        if (r != null) {
            result = r.getText();       
        }
        closeProcessor(processor);
        //_logger.info(result);
        //replicator.shutdown();
        //engine.passivate();
        return result;
    }

    
    
    public  void processGrammarLocation(GrammarLocation grammarLocation) throws IOException {
       this.grammarLocation = grammarLocation;
    }

    private class Listener implements RecogListener {
        
        private boolean recogDone = false;
        private boolean playDone = false;
        private boolean noRecogResultTimeout = false;
        private RecognitionResult theResult = null;
        
        public Listener() {
            recogDone = false;
            playDone = false;
            noRecogResultTimeout = false;
        }
        
        public synchronized boolean isDone() {
            if ((recogDone && playDone) ||
                (noRecogResultTimeout && playDone)){
                return true ;
            }else {
                return false;
            }
        }
        
        public synchronized RecognitionResult getResult() {
            return theResult;
        }

        public void noInputTimeout() {
            _logger.debug("No input timeout ");
            synchronized (this) {
                if (_noResultTimeoutTask != null) {
                    _noResultTimeoutTask.cancel();
                    _noResultTimeoutTask = null;
                }
                this.notifyAll();
            }
        }

        public void recognitionComplete(RecognitionResult result) {
            if (result == null) {
               _logger.debug("recog complete with null result");
            }else {
                _logger.debug("recog complete with result: " + result.getText()); 
            }
            synchronized (this) {
                if (_noResultTimeoutTask != null) {
                    _noResultTimeoutTask.cancel();
                    _noResultTimeoutTask = null;
                }
                theResult = result;
                recogDone = true;
                this.notifyAll();
            }
        }

        public void speechStarted() {
            _logger.debug("speech started... ");   
        }  
        
        public void noResultTimeout() {
            _logger.debug("no result timeout ");
            synchronized (this) {
                //if (!recogDone) {
                   //recog.closeProcessor();
                   //engine.stopProcessing();
                //}             
                _noResultTimeoutTask = null;
                theResult = null;
                noRecogResultTimeout = true;
                this.notifyAll();
            }
        }
        
        public void playComplete() {
            //System.out.println("Play Completed");


            synchronized (this) {
                playDone = true;
                if (!recogDone) {
                    _logger.debug("play completed before recognition happened. starting timer.");
                    
                    //*** This is the magic statement, without which the progrtam fails.  ****
                    engine.stopProcessing();
                    
                    //give recognizer another N second to complete (setup a no result timer)
                   _noResultTimeoutTask = new NoResultTimeoutTask();
                   _noResultTimeoutTask.setListener(this);
                   _timer.schedule(_noResultTimeoutTask, FIVESECS);

                } else {
                    _logger.debug("play completed after recog already happened");
                }
            }

        }
       
    }
    
   
   private class PlayThread extends Thread {

       private boolean _run = true;
       private File file;
       private boolean playDone = true;
       private boolean newPrompt = false;
       private  Listener listener;
       
       public void run() {

           _run = true;
           playDone = true;         
           while(_run) {  
               synchronized (this) {
                   while (!newPrompt) {
                       try {
                           this.wait(500);
                       } catch (InterruptedException e) {
                           // TODO Auto-generated catch block
                           e.printStackTrace();
                       }
                   }
               }
               newPrompt = false;
               
               //try {
               //    Thread.sleep(500);
               //} catch (InterruptedException e) {
                   // TODO Auto-generated catch block
               //    e.printStackTrace();
               //}
               
               try {
                   _logger.info("Playing prompt: "+ file.getName());
                   audioFilePlayer.playPrompt(file);
               } catch (IllegalStateException e2) {
                   // TODO Auto-generated catch block
                   e2.printStackTrace();
               } catch (IllegalArgumentException e2) {
                   // TODO Auto-generated catch block
                   e2.printStackTrace();
               } catch (InterruptedException e2) {
                   // TODO Auto-generated catch block
                   e2.printStackTrace();
               }
               playDone = true; 
               listener.playComplete();
               
           }
       }
       
       
       public synchronized int playPrompt(File f, Listener l) {
           if (playDone == false) {
               return -1;
           } else {
              file = f;
              listener = l;
              newPrompt = true;
              playDone = false;
              this.notifyAll();
              return 1;
           }
       }

       public boolean getPlayDone() {
           return playDone;
       }
       
       public void shutdown() {
           _run = false;
       }
   }
   

   
   private class NoResultTimeoutTask extends TimerTask {

       private Listener listener;

       public void setListener(Listener l) {
           listener = l;
       }

       /* (non-Javadoc)
        * @see java.util.TimerTask#run()
        */
       @Override
       public void run() {
           synchronized (this) {
               listener.noResultTimeout();
           }
       }
   }
   
   private class NoInputTimeoutTask extends TimerTask {

       private Listener listener;

       public void setListener(Listener l) {
           listener = l;
       }

       /* (non-Javadoc)
        * @see java.util.TimerTask#run()
        */
       @Override
       public void run() {
           synchronized (this) {
               listener.noInputTimeout();
           }
       }
   }
       
   public void shutdown() {
       replicator.shutdown();
       audioFilePlayer.shutdown();
   }
   

   public void closeProcessor(Processor processor) {
       if (processor != null) {
         _logger.debug("Closing processor...");
           processor.close();
           processor = null;
           replicator.removeReplicant( pair.getPbds());
       } else {
           _logger.debug("Tried to close processor.. but it was null."); 
       }
   }
    
    
    public static void main(String[] args) {
        System.out.println("Stating up with config file: "+args[0]);
        BaseRecognizerWerTest rp = new RTPRecognizerWerTest();
        rp.runTests(args[0]);       
    }
    
  
}

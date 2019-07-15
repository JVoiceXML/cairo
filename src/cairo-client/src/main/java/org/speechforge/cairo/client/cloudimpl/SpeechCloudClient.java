package org.speechforge.cairo.client.cloudimpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.rtp.InvalidSessionAddressException;
import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientProvider;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.SpeechEventListener.SpeechEventType;
import org.speechforge.cairo.client.SpeechRequest;
import org.speechforge.cairo.client.SpeechRequest.RequestType;
import org.speechforge.cairo.client.recog.InvalidRecogResultException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;

import com.spokentech.speechdown.client.HttpRecognizer;
import com.spokentech.speechdown.client.HttpSynthesizer;
import com.spokentech.speechdown.client.PromptPlayListener;
import com.spokentech.speechdown.client.rtp.RtpTransmitter;


public class SpeechCloudClient implements SpeechClient, SpeechClientProvider, PromptPlayListener, com.spokentech.speechdown.common.SpeechEventListener {


	/** The _logger. */
    private static Logger _logger = Logger.getLogger(SpeechCloudClient.class);
 
    
    private static final String wav = "audio/x-wav";
    private static final String mpeg = "audio/mpeg";
    
    private static final String recServiceUrl = "http://spokentech.net/speechcloud/SpeechUploadServlet";    
    private static final String synthServiceUrl = "http://spokentech.net/speechcloud/SpeechDownloadServlet";    

    private static final  String s4audio = "audio/x-s4audio";
    
    /** The _barge in flag */
    private boolean _bargeIn = false;

    /**
     * The Enum DtmfState.
     */
    
    public enum DtmfState {
          notActive, 
          waitingForInput, 
          waitingForMatch, 
          complete}
    
    /** The _dtmf state. */
    private DtmfState _dtmfState = DtmfState.notActive; 
    
    /** The _timer. */
    private /*static*/ Timer _timer = new Timer();
    
    /** The _no input timeout task. */
    TimerTask _noInputTimeoutTask;
    
    /** The _no recog timeout task. */
    TimerTask _noRecogTimeoutTask;
    
    /** pattern to be matched for dtmf recognition (regex) */
    private Pattern _pattern;
    
    /** the listener set in the dtmf requests (for dtmf recognition events) */
    private SpeechEventListener _dtmfListener;
    
    /** dtmf no recognition timeout value */
    long _recogTimout;

    /** dtmf input buffer. the string to be matched against the pattern in the characterEventReceived 
     *  method when there is a match call the listener and this dtmf request is completed */
    String _inBuf;
    char[] _charArray;
    int _length = 0;
    String sal;

    
    boolean lmflg = false; 
    boolean batchFlag = true;
    int timeout = 0;
    
    private String devName="cairo-client";
    private String devSecret ="secret";
    private String userName =devName;
    
    private String voiceName = "default";
	//voice = "hmm-jmk";
	//voice = "jmk-arctic";
	//voice = "hmm-slt";
	//voice = "slt-arctic";
	 //voice = "hmm-bdl";
	 //voice = "bdl-arctic";
	 //voice = "misspelled";

    /**
     * @return the voiceName
     */
    public String getVoiceName() {
    	return voiceName;
    }


	/**
     * @param voiceName the voiceName to set
     */
    public void setVoiceName(String voiceName) {
    	this.voiceName = voiceName;
    }




	private  Collection<SpeechEventListener> listenerList = null;
	
 
    private String serviceUrl = null;
    
    private RTPStreamReplicator rtpReplicator;
    private HttpRecognizer recognizer;
    
    private RtpTransmitter rtpTransmitter;
    private HttpSynthesizer synthesizer;
    
    
    RequestType _activeRequestType;
    SpeechRequest _activeBlockingTts;
    
    /** The _active recognition. */
    SpeechRequest _activeRecognition;

    
    
    public SpeechCloudClient(RTPStreamReplicator rtpReplicator, RtpTransmitter rtpTransmitter, String url) {
        super();
     
        this.serviceUrl = url;
        this.rtpReplicator = rtpReplicator;
        this.rtpTransmitter = rtpTransmitter;  
        
//        synthesizer = new HttpSynthesizer(devName,devSecret);
        if (serviceUrl != null)
           synthesizer.setService(serviceUrl+"/SpeechDownloadServlet");
        else
            synthesizer.setService(synthServiceUrl);
        
//        recognizer = new HttpRecognizer(devName,devSecret);
        recognizer.enableAsynchMode(10);
        if (serviceUrl != null)
           recognizer.setService(serviceUrl+"/SpeechUploadServlet");
        else
           recognizer.setService(recServiceUrl);
        
        listenerList = new java.util.ArrayList<SpeechEventListener>();   
    }
    

    /**
     * @return the serviceUrl
     */
    public String getServiceUrl() {
    	return serviceUrl;
    }


	/**
     * @param serviceUrl the serviceUrl to set
     */
    public void setServiceUrl(String serviceUrl) {
    	this.serviceUrl = serviceUrl;
    	
        if ((serviceUrl != null) && (synthesizer !=null))
           synthesizer.setService(serviceUrl+"/SpeechDownloadServlet");
        
        if ((serviceUrl != null) && (recognizer != null))
           recognizer.setService(serviceUrl+"/SpeechUploadServlet");

    }


	/* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClientProvider#characterEventReceived(char)
     */
    public void characterEventReceived(char c) {
        _logger.debug("speechclient.chareventreceived: "+c);
        
        if (_dtmfState == DtmfState.waitingForInput) {
            _logger.debug("   waitingfor input...");
            //if the first char, 
            //  1.  cancel the no input timer  and
            //  2.  start the no recognition timer and 
            //  3.  cancel the speech recognition
            
            //cancel the no input timer
            if (_noInputTimeoutTask != null) {
                _noInputTimeoutTask.cancel();
                _noInputTimeoutTask = null;
            }
            
            // start the recognition Timer
            if (_recogTimout != 0) {
                startRecognitionTimer(_recogTimout);
            }
            
            //TODO: cancel speech recognition
            
                
            // if barge in enabled, send bareg in request (to transmitter)
			if ((_bargeIn) ) { //&&  (_activeRequestType == RequestType.playAndRecognize)){
				try {
					sendBargeinRequest();
				} catch (MrcpInvocationException e) {
					_logger.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
					_logger.warn(e, e);
				} catch (IOException e) {
					_logger.warn(e, e);
				} catch (InterruptedException e) {
					_logger.warn(e, e);
				}
			}
            
            
            //now we have the first char, we are waiting for a match
            _dtmfState = DtmfState.waitingForMatch;


            _charArray = new char[20];
            _charArray[0] = c;
            _length=1;    
            _inBuf =  new String(_charArray);
            _logger.debug("The first inBuf is : "+ _inBuf);


            // do the DTMF pattern matching
            checkForDtmfMatch(_inBuf);
            
        } else if (_dtmfState == DtmfState.waitingForMatch) {
            _logger.debug("   waiting for match...");

                //concatenate the new char to end of the dtmf string receievd up till now
          
                _charArray[_length++] = c;
                _inBuf =  new String(_charArray);
                _logger.debug("The new inBuf is: "+_inBuf);
                
                // do the DTMF pattern matching
                checkForDtmfMatch(_inBuf);

        } else {
            _logger.warn("Got dtmf signal while dtmf was not enabled by the client: "+c+  "  Discarding it.");
        }
    }


    private void checkForDtmfMatch(String c) {

   
        //do a regex match.  if it matches we are done
        Matcher m = _pattern.matcher(_inBuf);

        //if it matches
        if (m.find()) {
            _logger.debug("Got a dtmf match : "+_inBuf);
            _dtmfState = DtmfState.complete;
            
            //cancel the recog timer
            if (_noRecogTimeoutTask != null) {
                _noRecogTimeoutTask.cancel();
                _noRecogTimeoutTask = null;
            }
           
            //return the recognition results
           _dtmfListener.characterEventReceived(_inBuf,SpeechEventListener.DtmfEventType.recognitionMatch);
           

        }  else {
            _logger.debug("No match : "+_inBuf); 
        }
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#disableDtmf()
     */
    public void disableDtmf() {
        _dtmfState = DtmfState.notActive;
        _pattern = null;
        _dtmfListener = null;
        if (_noInputTimeoutTask != null) {
            _noInputTimeoutTask.cancel();
            _noInputTimeoutTask = null;
        }
        
        //cancel the recog timer
        if (_noRecogTimeoutTask != null) {
            _noRecogTimeoutTask.cancel();
            _noRecogTimeoutTask = null;
        }
        
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#enableDtmf(java.lang.String, org.speechforge.cairo.client.SpeechEventListener, long, long)
     */
    public void enableDtmf(String pattern, SpeechEventListener listener, long inputTimeout, long recogTimeout) {
        
        //check if there is already dtmf enabled (TODO if so throw exception)
        //if not go ahead and enable dtmf with this pattern
        if ((_dtmfState == DtmfState.notActive) || (_dtmfState == DtmfState.complete)) {
            _dtmfState = DtmfState.waitingForInput;
            
            //save the pattern
            _pattern =  Pattern.compile(pattern);
            
            //save the listener
            _dtmfListener = listener;
            
            //save the no recognition timeout value
            _recogTimout = recogTimeout;
            
            //TODO start the noInput Timer
            if (inputTimeout != 0) {
                startInputTimer(inputTimeout);
            }
            
            // Initialize the input buffer.  That is the string to be matched
            // against the pattern in the characterEventReceived method
            // when there is a match call the listener and this dtmf request is completed
            _inBuf = new String();

        
        } else {   //already an active dtmf recognition request
            _logger.warn("DTMF Recognition already active.");   
        }
        
    }
	
    

	public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl,
            boolean hotword) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException, InvalidSessionAddressException {
		queuePrompt(urlPrompt, prompt);
		boolean attachGrammar = false;;
		int noInputTimeout =0;
		
		return recognizeBlocking(grammarUrl,hotword,attachGrammar,noInputTimeout);
    }

	public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader,
            boolean hotword) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {
		_logger.warn("Not implemented.");
		return null;
    }

	public void playBlocking(boolean urlPrompt, String prompt) throws IOException, MrcpInvocationException,
            InterruptedException, NoMediaControlChannelException, InvalidSessionAddressException {
		
		AudioFormat synthFormat = rtpTransmitter.getFormat();
		String fileFormat = rtpTransmitter.getFileType();
		
//		InputStream stream = synthesizer.synthesize(userName,prompt, synthFormat, fileFormat, voiceName);
		
		//TODO: remove this step (converting stream to file) should just queue the stream
        String fname = Long.toString(System.currentTimeMillis())+".wav";
		if (fileFormat.equals("audio/x-au")) {
             fname = Long.toString(System.currentTimeMillis())+".au";
		} else if (fileFormat.equals("audio/x-wav")) {
             fname = Long.toString(System.currentTimeMillis())+".wav";
		} else {
			_logger.warn("Unrecognzied file format:"+ fileFormat+" Trying wav");
		}
//        rtpTransmitter.queueAudio(stream, this,fname);
		//File f = streamToFile(stream,fname);
		//rtpTransmitter.queueAudio(f, this);
		
    }



	

	

	public SpeechRequest queuePrompt(boolean urlPrompt, String prompt) throws IOException,
            MrcpInvocationException, InterruptedException, NoMediaControlChannelException {

    	if (rtpTransmitter == null)
    		throw new  NoMediaControlChannelException();
    	
 
        // speak request

		AudioFormat synthFormat = rtpTransmitter.getFormat();
		String fileFormat = rtpTransmitter.getFileType();
		
//		InputStream stream = synthesizer.synthesize(userName,prompt, synthFormat, fileFormat, voiceName);
		

		//filenames are needed just in case the audio needs to be queued
        String fname = Long.toString(System.currentTimeMillis())+".wav";
		if (fileFormat.equals("audio/x-au")) {
             fname = Long.toString(System.currentTimeMillis())+".au";
		} else if (fileFormat.equals("audio/x-wav")) {
             fname = Long.toString(System.currentTimeMillis())+".wav";
		} else {
			_logger.warn("Unrecognzied file format:"+ fileFormat+" Trying wav");
		}
		//File f = streamToFile(stream,fname);
		
//		try {
//			//rtpTransmitter.queueAudio(f,this);
//	        rtpTransmitter.queueAudio(stream, this,fname);
//        } catch (InvalidSessionAddressException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        }
 
		//TODO: Need a unique id
		int id = 1;
        
        //_activeRequestType = RequestType.play;
        SpeechRequest queuedTts = new SpeechRequest(id,RequestType.play,false);   
        queuedTts.setBlockingCall(false);

        return queuedTts;

    }

	public SpeechRequest recognize(String grammar, boolean hotword, boolean attachGrammar,
            long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {
		
		

		//todo: implement hotword flag
		//todo: implement timeout
		
//    	S4EndPointer ep = new S4EndPointer();
//		RtpS4EndPointingInputStream eStream = new RtpS4EndPointingInputStream(ep);
//		eStream.setMimeType(s4audio);
//		eStream.setupStream(rtpReplicator);
 
	    URL grammarUrl = null;
		if (!attachGrammar) {
	    	try {
	    		grammarUrl = new URL(grammar);
			} catch (MalformedURLException e) {  
		         e.printStackTrace();  
			}
		}

    	boolean lmflg = false;
    	boolean batchFlag = false;
//        try {	
//        	if (attachGrammar) {
//               recognizer.recognizeAsynch(devName, devSecret, userName, grammar,  eStream,  lmflg,  batchFlag, OutputFormat.text, timeout,this);
//        	} else {
//                recognizer.recognizeAsynch(devName, devSecret, userName, grammarUrl,  eStream,  lmflg,  batchFlag, OutputFormat.text, timeout,this);
//        	}
//        } catch (InstantiationException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (StreamInUseException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } catch (AsynchNotEnabledException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        }
        
        //todo: unique request Id's
        int id =1;
        
        SpeechRequest request = new SpeechRequest(id,RequestType.recognize,false);   
        request.setBlockingCall(false);
        return request;
        
    }

	public SpeechRequest recognize(Reader reader, boolean hotword, boolean attachGrammar, long noInputTimeout)
            throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException,
            NoMediaControlChannelException {
		
		//todo: implement attachGrammar flag
		//todo: implement hotword flag
		//todo: implement timeout
		
        BufferedReader in  = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        String line = null;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        
       _logger.debug("The grammar text: " +sb.toString());
//   	    S4EndPointer ep = new S4EndPointer();
//		RtpS4EndPointingInputStream eStream = new RtpS4EndPointingInputStream(ep);
//		eStream.setMimeType(s4audio);
//		eStream.setupStream(rtpReplicator);


  
    	boolean lmflg = false;
    	boolean batchFlag = false;
//        try {	            
//            recognizer.recognizeAsynch(devName, devSecret, userName, sb.toString(),  eStream,  lmflg,  batchFlag,OutputFormat.text, timeout,this) ;
//        } catch (InstantiationException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (StreamInUseException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        } catch (AsynchNotEnabledException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        }
        //todo: unique request Id's
        int id =1;
        
        SpeechRequest request = new SpeechRequest(id,RequestType.recognize,false);   
        request.setBlockingCall(false);
        return request;

    }

	public RecognitionResult recognizeBlocking(String grammar, boolean hotword, boolean attachGrammar,
            long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException,
            IllegalValueException, NoMediaControlChannelException {
		

		//todo: implement hotword flag
		//todo: implement timeeout
		
        URL grammarUrl = null;
		if (!attachGrammar) {
	    	try {
	    		grammarUrl = new URL(grammar);
			} catch (MalformedURLException e) {  
		         e.printStackTrace();  
			}
		}

//    	S4EndPointer ep = new S4EndPointer();
//		RtpS4EndPointingInputStream eStream = new RtpS4EndPointingInputStream(ep);
//		eStream.setMimeType(s4audio);
//		eStream.setupStream(rtpReplicator);
		

    	String rr = null;
//        try {
//
//        	if (attachGrammar) {
//    			rr = recognizer.recognize(userName,grammar, eStream, lmflg,  batchFlag, OutputFormat.text, timeout,this);
//         	} else {
//    			rr = recognizer.recognize(userName,grammarUrl, eStream, lmflg,  batchFlag, OutputFormat.text, timeout,this);
//         	}
//        	
//
//        } catch (InstantiationException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//        }        
        
        
        //TODO:  Remove this hack.  Have a single RecognitionResult object used in both Cairo client and the cloud client
        //       Perhaps combine the two client libs.
    	RecognitionResult r = null;
        try {
	        r = RecognitionResult.constructResultFromString(rr.toString());
        } catch (InvalidRecogResultException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }

	    return r;
    }

	public RecognitionResult recognizeBlocking(Reader reader, boolean hotword, long noInputTimeout) throws IOException, 
		MrcpInvocationException, InterruptedException, IllegalValueException,NoMediaControlChannelException {

		BufferedReader in  = new BufferedReader(reader);
		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = in.readLine()) != null) {
			sb.append(line);
			sb.append("\n");
		} 
		_logger.debug("The grammar text: " +sb.toString());
		
//    	S4EndPointer ep = new S4EndPointer();
//		RtpS4EndPointingInputStream eStream = new RtpS4EndPointingInputStream(ep);
//		eStream.setMimeType(s4audio);
//		eStream.setupStream(rtpReplicator);


		String rr = null;
//		try {
//			rr = recognizer.recognize(userName, sb.toString(), eStream, lmflg,  batchFlag, OutputFormat.text ,timeout, this);
//
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}        


		//TODO:  Remove this hack.  Have a single RecognitionResult object used in both Cairo client and the cloud client
		//       Perhaps combine the two client libs.
		RecognitionResult r = null;
		try {
			r = RecognitionResult.constructResultFromString(rr.toString());
		} catch (InvalidRecogResultException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return r;


	}




	public MrcpRequestState sendStartInputTimersRequest() throws MrcpInvocationException, IOException,
            InterruptedException {
	    // TODO Auto-generated method stub
	    return null;
    }

	public void setDefaultListener(SpeechEventListener listener) {
	     this.setListener(listener);
    }

	public void setListener(SpeechEventListener listener) {
        addListener(listener);
	    
    }

	public void shutdown() throws MrcpInvocationException, IOException, InterruptedException {
        // TODO Determine if there are active requests before stopping them
        
        // Stop any active requests
        try {
	        this.stopActiveRecognitionRequests();
        } catch (NoMediaControlChannelException e) {
	       _logger.debug("As part of shutting down the speech client, stopping active recognition requests.  No recog control channel so nothing to stop.");
        }
        
        //shutdown the timers
        //cancel the no input timer
        if (_noInputTimeoutTask != null) {
            _noInputTimeoutTask.cancel();
            _noInputTimeoutTask = null;
        }
        
        //cancel the recog timer
        if (_noRecogTimeoutTask != null) {
            _noRecogTimeoutTask.cancel();
            _noRecogTimeoutTask = null;
        }
	    
    }

	public void stopActiveRecognitionRequests() throws MrcpInvocationException, IOException,
            InterruptedException, NoMediaControlChannelException {
	    // TODO Auto-generated method stub
	    
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOnBargeIn()
     */
    public void turnOnBargeIn() {
        _bargeIn = true;    
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOffBargeIn()
     */
    public void turnOffBargeIn() {
       _bargeIn = false;
    }



	public void addListener(SpeechEventListener listener) {
        synchronized (listenerList) {
        	listenerList.add(listener);
        }
	    
    }


	public void removeListener(SpeechEventListener listener) {
        synchronized (listenerList) {
        	listenerList.remove(listener);
        }
    }


    private void fireSynthEvent(final SpeechEventType event) {
        synchronized (listenerList) {
            Collection<SpeechEventListener> copy =  new java.util.ArrayList<SpeechEventListener>();        
            copy.addAll(listenerList);
            for (SpeechEventListener current : copy) {
                current.speechSynthEventReceived(event);
            }
        }
    }
    

    private void fireRecogEvent(final SpeechEventType event,RecognitionResult result) {
        synchronized (listenerList) {
            Collection<SpeechEventListener> copy =  new java.util.ArrayList<SpeechEventListener>();        
            copy.addAll(listenerList);
            for (SpeechEventListener current : copy) {
                current.recognitionEventReceived(event, result);
            }
        }
    }

    
    /**
     * The Class NoInputTimeoutTask.
     */
    private class NoInputTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (this) {
                _noInputTimeoutTask = null;
                if (_dtmfState == DtmfState.waitingForInput) {
                    _dtmfState = DtmfState.complete;
                    if (_dtmfListener != null) {
                        _dtmfListener.characterEventReceived(null,SpeechEventListener.DtmfEventType.noInputTimeout);
                    }
                }
            }
        }
        
    }
    
   //TODO: Combine the two timer tasks into a single task (as well as combining the accompanying startTimer methods)
    
    /**
     * Starts the input timers which trigger no-input-timeout if first dtmf key has not been depressed after the specified time.
     * 
     * @param noInputTimeout the amount of time to wait, in milliseconds, before triggering a no-input-timeout.
     * 
     * @return {@code true} if input timers were started or {@code false} if speech has already started.
     * 
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    private synchronized boolean startInputTimer(long noInputTimeout) throws IllegalStateException {
        if (noInputTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noInputTimeout);
        }
        if (_pattern == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noInputTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startInputTimers = (_dtmfState == DtmfState.waitingForInput); 
        if (startInputTimers) {
            _noInputTimeoutTask = new NoInputTimeoutTask();
            _timer.schedule(_noInputTimeoutTask, noInputTimeout);
        }

        return startInputTimers;
    } 
    
    
    /**
     * The Class NoRecogTimeoutTask.
     */
    private class NoRecogTimeoutTask extends TimerTask {

        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            synchronized (this) {
                _noRecogTimeoutTask = null;
                if (_dtmfState == DtmfState.waitingForInput) {
                    _dtmfState = DtmfState.complete;
                    if (_dtmfListener != null) {
                        _dtmfListener.characterEventReceived(null,SpeechEventListener.DtmfEventType.noMatchTimeout);
                    }
                }
            }
        }
        
    }
    
    /**
     * Starts the input timers which trigger no-recognition-timeout if no recognition match has occurred after the specified time.
     * 
     * @param noMatchTimeout the amount of time to wait, in milliseconds, before triggering a no-recognition-timeout.
     * 
     * @return {@code true} if recog timers were started.
     * 
     * @throws IllegalStateException if recognition is not in progress or if the input timers have already been started.
     */
    private synchronized boolean startRecognitionTimer(long noMatchTimeout) throws IllegalStateException {
        if (noMatchTimeout <= 0) {
            throw new IllegalArgumentException("Illegal value for no-input-timeout: " + noMatchTimeout);
        }
        if (_pattern == null) {
            throw new IllegalStateException("Recognition not in progress!");
        }
        if (_noRecogTimeoutTask != null) {
            throw new IllegalStateException("InputTimer already started!");
        }

        boolean startRecognitionTimers = (_dtmfState == DtmfState.waitingForMatch); 
        if (startRecognitionTimers) {
            _noRecogTimeoutTask = new NoRecogTimeoutTask();
            _timer.schedule(_noRecogTimeoutTask, noMatchTimeout);
        }

        return startRecognitionTimers;
    }

    

	public MrcpRequestState sendBargeinRequest() throws IOException, MrcpInvocationException,
            InterruptedException {
	    rtpTransmitter.stopPlayback();
	    return null;
    }
    
    
    //methods for prompt play listener

	public void playCompleted() {
	    _logger.debug("Play complete event");
		//first determine if this is event for a blocking request
		if ((_activeBlockingTts != null) ) { //&& (event.getRequestID() == _activeBlockingTts.getRequestId() )) {
			// if there is an active recognition request and bargein is enabled, start the timer
			if ((_bargeIn)&&(_activeRecognition != null)&&(!_activeRecognition.isCompleted())){				
				//sendStartInputTimersRequest();
			}
		//else an event from an asynch request, just send the event on
		} else {
			//fireSynthEvent(event);
		}
	}


	public void playFailed(Exception e) {
		e.printStackTrace();
	    _logger.warn(e.getMessage());
	    
    }


	public void playInterrupted() {
	    _logger.debug("play interupted");
	       
    }



	public void noInputTimeout() {
	    _logger.debug("no input timeout");
	         
    }



	public void speechEnded() {
	    _logger.debug("Speech Ended event");
	    	    
    }


	public void speechStarted() {
	    _logger.debug("Speech Started event");
		if ((rtpTransmitter!= null) && (_bargeIn)){
			rtpTransmitter.stopPlayback();
		}
    }


	@Override
	public void recognitionComplete(com.spokentech.speechdown.common.RecognitionResult arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setContentType(String contentType) {
		// TODO Auto-generated method stub
		
	}


//	public void recognitionComplete(Utterance result) {
//	    _logger.debug("Recognition complete event");
//		//TODO:  THIS IS NOT TESTED.  ONLY CHANGED TO COMPILE!!  CONVERTING FROM UTTERANCE.tostring() TO RecResult will probably not work!!!
//	    // This will be a conversion process issue (REAL SOLUTION IS TO USE UTTERANCE IN CAIRO TOO)
//	 	RecognitionResult r = null;
//	     try {
//		        r = RecognitionResult.constructResultFromString(result.toString());
//	     } catch (InvalidRecogResultException e) {
//		        // TODO Auto-generated catch block
//		        e.printStackTrace();
//	     }
//		 fireRecogEvent(SpeechEventType.RECOGNITION_COMPLETE,r);
//    }
		
		
}

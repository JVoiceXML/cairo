/*
 * cairo-client - Open source client for control of speech media resources.
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
package org.speechforge.cairo.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpMethodName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpEventListener;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.CompletionCause;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequest;
import org.speechforge.cairo.client.SpeechEventListener.SpeechEventType;
import org.speechforge.cairo.client.SpeechRequest.RequestType;
import org.speechforge.cairo.client.recog.InvalidRecogResultException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.sip.SdpMessage;

/**
 * SpeechClient Implementation.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechClientImpl implements MrcpEventListener, SpeechClient, SpeechClientProvider {

    /** The logger instance. */
    private static final Logger LOGGER =
        LogManager.getLogger(SpeechClientImpl.class);
 
    //InetAddress _cairoSipInetAddress = null;
    //private  String _cairoSipHostName;
    //private  int _peerHostPort;
    
    /** The _tts channel. */
    private MrcpChannel ttsChannel;
    
    /** The _recog channel. */
    private MrcpChannel recogChannel;
    
    /** The _barge in flag */
    private boolean bargeIn;

    //used to construct mrcp channels (in the static metods below)
    private static String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
    private static MrcpFactory factory = MrcpFactory.newInstance();
    private static MrcpProvider provider = factory.createProvider();

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

    //TODO: Deal with dead calls -- clean up map periocially or leases or ...
    /** private Map<String,SpeechRequest> activeRequests;
     *  map of active reuests for non-blocking calls.  This is turning out to be more difficult than expected
     *  so only supporting blocking calls for now and using the following state variables rather than the map */
    RequestType _activeRequestType;
    SpeechRequest _activeBlockingTts;
    
    /** The _active recognition. */
    SpeechRequest _activeRecognition;

    private final Collection<SpeechEventListener> listenerList;
    
    private String _contentType;
    
    /**
     * Instantiates a new speech client impl.
     * 
     * @param tts the TTS channel
     * @param recog the recognition channel
     */
    public SpeechClientImpl(MrcpChannel tts, MrcpChannel recog) {
        super();

        ttsChannel = tts;
        if (tts != null)
           ttsChannel.addEventListener(this);
        recogChannel = recog;
        if (recog != null)
           recogChannel.addEventListener(this); 
        listenerList = new java.util.ArrayList<SpeechEventListener>();
        
    }

    public void setContentType(String contentType) {
    	_contentType = contentType;
    }


    /* (non-Javadoc)
     * @see org.mrcp4j.client.MrcpEventListener#eventReceived(org.mrcp4j.message.MrcpEvent)
     */
    public void eventReceived(MrcpEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP event received:\n" + event.toString());
        }

        try {
            switch (event.getChannelIdentifier().getResourceType()) {
            case SPEECHSYNTH:
                ttsEventReceived(event);
                break;

            case SPEECHRECOG:
                recogEventReceived(event);
                break;

            default:
                LOGGER.warn("Unexpected value for event resource type!");
                break;
            }
        } catch (IllegalValueException e) {
            LOGGER.warn("Illegal value for event resource type!", e);
        }
   }

    /**
     * Tts event received.
     * 
     * @param event the event
     */
    private void ttsEventReceived(MrcpEvent event) {
        
        //first determine if this is event for a blocking request
        if ((_activeBlockingTts != null) && (event.getRequestID() == _activeBlockingTts.getRequestId() )) {
        
            if (MrcpEventName.SPEAK_COMPLETE.equals(event.getEventName())) {

                // if there is an active recognition request and bargein is enabled, start the timer
                if ((bargeIn)&&(_activeRecognition != null)&&(!_activeRecognition.isCompleted())){
                    try {
                        sendStartInputTimersRequest();
                    } catch (MrcpInvocationException e) {
                        LOGGER.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
                        LOGGER.warn(e, e);
                    } catch (IOException e) {
                        LOGGER.warn(e, e);
                    } catch (InterruptedException e) {
                        LOGGER.warn(e, e);
                    }
                }
                
                //signal for the blocking call to check for unblocking
                synchronized (this) {
                    _activeBlockingTts.setCompleted(true);
                    //activeRequests.remove(key);
                    this.notifyAll();
                }
            }
            
            //else an event from an asynch request, just send the event on
        } else {
            
        	fireSynthEvent(convertEventType(event));

        }
        
   

    }

    private SpeechEventListener.SpeechEventType convertEventType(MrcpEvent event) {
        if (MrcpEventName.SPEAK_COMPLETE.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.SPEAK_COMPLETE;
        } else if (MrcpEventName.INTERPRETATION_COMPLETE.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.INTERPRETATION_COMPLETE;
            
        } else if (MrcpEventName.RECOGNITION_COMPLETE.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.RECOGNITION_COMPLETE;
        	  
        } else if (MrcpEventName.SPEECH_MARKER.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.SPEECH_MARKER;
        	  
        } else if (MrcpEventName.START_OF_INPUT.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.START_OF_INPUT;
        } else if (MrcpEventName.VERIFICATION_COMPLETE.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.VERIFICATION_COMPLETE;
        } else if (MrcpEventName.RECORD_COMPLETE.equals(event.getEventName())) {
        	return SpeechEventListener.SpeechEventType.RECORD_COMPLETE;
        } else {
        	return SpeechEventListener.SpeechEventType.UNKNOWN;

        }
    }


	/**
     * Recog event received.
     * 
     * @param event the event
     */
    private void recogEventReceived(MrcpEvent event) {
    	MrcpEventName eventName = event.getEventName();

    	//first determine if this is event for a blocking request
    	if ((_activeRecognition != null) && (event.getRequestID() == _activeRecognition.getRequestId() )) {


    		if (MrcpEventName.START_OF_INPUT.equals(eventName)) {
    			//TODO: DO you need to check if there is something to barge in on (what if the play already completed?  Or if there is no play in teh first place?
    			//used to check if part of a playAndRecognize.  But now that one can queue a play non-blocking and then call recognize with bargein enabled
    			//that check is no longer valid. 
    			if ((bargeIn) ) { //&&  (_activeRequestType == RequestType.playAndRecognize)){
    				try {
    					sendBargeinRequest();
    				} catch (MrcpInvocationException e) {
    					LOGGER.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
    					LOGGER.warn(e, e);
    				} catch (IOException e) {
    					LOGGER.warn(e, e);
    				} catch (InterruptedException e) {
    					LOGGER.warn(e, e);
    				}
    			}

    		} else if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {

    			//get the result and place in the active recog request object where it is retrieved by the blocking method
    			MrcpHeader completionCauseHeader = event.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
    			CompletionCause completionCause = null;
    			try {
    				completionCause = (CompletionCause) completionCauseHeader.getValueObject();
    			} catch (IllegalValueException e) {
    				// TODO Auto-generated catch block
    				LOGGER.warn("Illegal Value getting the completion cause", e);

    			}

    			RecognitionResult r = null;
    			if (completionCause.getCauseCode() != 0) { 
    				r = null; 
    			} else {
    				try {
    					LOGGER.debug("Recognition event content: "+event.getContent());
    					r = RecognitionResult.constructResultFromString(event.getContent());
    					LOGGER.debug("recognition result text: "+r.getText());
    				} catch (InvalidRecogResultException e) {
    					LOGGER.warn("Illegal recognition result", e);
    					r = null;
    				}
    			}
    			_activeRecognition.setResult(r);


    			//signal for the blocking call to check for unblocking
    			synchronized (this) {
    				_activeRecognition.setCompleted(true);
    				this.notifyAll();
    			}
    		}
    	}
    	
		// else it is a non blocking requests, just forward on the event (with the recognition results)
		//    Always try to always send event (blocking or non) -- could be useful for status on client
    	RecognitionResult r = null;
    	if (MrcpEventName.RECOGNITION_COMPLETE.equals(eventName)) {
    		MrcpHeader completionCauseHeader = event.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
    		CompletionCause completionCause = null;
    		try {
    			completionCause = (CompletionCause) completionCauseHeader.getValueObject();
    		} catch (IllegalValueException e) {
    			// TODO Auto-generated catch block
    			LOGGER.warn("Illegal Value getting the completion cause", e);
    		}
    		if (completionCause.getCauseCode() == 0) { 
    			try {
    				LOGGER.debug("Recognition event content: "+event.getContent());
    				r = RecognitionResult.constructResultFromString(event.getContent());
    				LOGGER.debug("recognition result text: "+r.getText());
    			} catch (InvalidRecogResultException e) {
    				LOGGER.warn("Illegal Recognition Result", e);
    				r = null;
    			}
    		}
    	}

    	fireRecogEvent(convertEventType(event),r);

    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#sendStartInputTimersRequest()
     */
    public MrcpRequestState sendStartInputTimersRequest()
      throws MrcpInvocationException, IOException, InterruptedException {

        // construct request
        MrcpRequest request = recogChannel.createRequest(MrcpMethodName.START_INPUT_TIMERS);

        // send request
        MrcpResponse response = recogChannel.sendRequest(request);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP response received:\n" + response.toString());
        }
        return response.getRequestState();
    }

    /**
     * Send bargein request.
     * 
     * @return the mrcp request state
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    public MrcpRequestState sendBargeinRequest()
      throws IOException, MrcpInvocationException, InterruptedException {
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending a barge in occurred message to tts resource");
        }

        // construct request
        MrcpRequest request = ttsChannel.createRequest(MrcpMethodName.BARGE_IN_OCCURRED);

        // send request
        MrcpResponse response = ttsChannel.sendRequest(request);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP response received:\n" + response.toString());
        }

        return response.getRequestState();
    }


    /**
     * {@inheritDoc} 
     */
    @Override
    public SpeechRequest queuePrompt(String prompt) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) {
            throw new  NoMediaControlChannelException();
        }
 
        // speak request
        MrcpRequest request = ttsChannel.createRequest(MrcpMethodName.SPEAK);
       request.setContent("text/plain", null, prompt);
        return sendPlayRequest(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SpeechRequest queuePrompt(URL prompt) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) {
            throw new  NoMediaControlChannelException();
        }
 
        // speak request
        MrcpRequest request = ttsChannel.createRequest(MrcpMethodName.SPEAK);
        request.setContent(_contentType, null, prompt);
        return sendPlayRequest(request);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public SpeechRequest queuePrompt(List<URL> prompts) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) {
            throw new  NoMediaControlChannelException();
        }
 
        // speak request
        MrcpRequest request = ttsChannel.createRequest(MrcpMethodName.SPEAK);
        final StringBuilder str = new StringBuilder();
        for (URL url : prompts) {
            if (str.length() > 0) {
                str.append(System.lineSeparator());
                str.append(url.toString());
            }
        }
        request.setContent("text/uri-list", null, str.toString());
        return sendPlayRequest(request);
    }

    /**
     * Sends a play request.
     * @param request the request to send
     * @return queued request
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    private SpeechRequest sendPlayRequest(MrcpRequest request)
            throws IOException, MrcpInvocationException, InterruptedException {
        MrcpResponse response = ttsChannel.sendRequest(request);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP response received:\n" + response.toString());
        }
        
        //_activeRequestType = RequestType.play;
        SpeechRequest queuedTts = new SpeechRequest(response.getRequestID(), RequestType.play, false);
        queuedTts.setBlockingCall(false);

        return queuedTts;
    }
    
    /**
     * Recognize.
     * 
     * @param grammarUrl the grammar url
     * @param hotword hotword detection enabled
     * @param attachGrammar grammar should be attached
     * @param noInputTimeout no-speech timeout in msec 
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException error accessing the media control
     * 		channel 
     */
    public SpeechRequest recognize(String grammarUrl, boolean hotword, boolean attachGrammar, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
  	
    	if (recogChannel == null)
    		throw new  NoMediaControlChannelException();
  
        MrcpRequest request = constructRecogRequest(grammarUrl,hotword, attachGrammar,  noInputTimeout);
      
        LOGGER.debug("REQUEST: "+request.toString());
        MrcpResponse response = recogChannel.sendRequest(request);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP response received:\n" + response.toString());
        }

        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");           
        }
        //_activeRequestType = RequestType.recognize;
        SpeechRequest queuedRecognition = new SpeechRequest(response.getRequestID(),RequestType.recognize,false);   
        queuedRecognition.setBlockingCall(false);
               
        return queuedRecognition;
    }

    
    /**
     * Play and recognize.
     * 
     * @param urlPrompt the URL to a prompt
     * @param prompt the prompt
     * @param grammarUrl the grammar url
     * @param hotword hotword enabled
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException error accessing the media control channel
     */
    public  SpeechRequest playAndRecognize(boolean urlPrompt, String prompt, String grammarUrl, boolean hotword)
    throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
    	if ((ttsChannel == null) ||  (recogChannel == null))
    		throw new  NoMediaControlChannelException();
    
        long noInputTimeout=0;
        MrcpRequest request = constructRecogRequest(grammarUrl, hotword, true,noInputTimeout);
        SpeechRequest speechRequest = internalPlayAndRecogize(urlPrompt, prompt, request);

        return speechRequest;  

    }


    /**
     * Internal play and recogize.
     * 
     * @param urlPrompt the url prompt
     * @param prompt the prompt
     * @param listener the listener
     * @param request the request
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    private SpeechRequest internalPlayAndRecogize(boolean urlPrompt, String prompt, MrcpRequest request) throws IOException, MrcpInvocationException, InterruptedException {
        // TODO delay start of recognition if barge-in is disabled!
    	LOGGER.debug("Sending the mrcp request");
    	MrcpResponse response = recogChannel.sendRequest(request);

          if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("MRCP response received:\n" + response.toString());
          }

          if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
              throw new RuntimeException("Recognition failed to start!");
          }
          
          //_activeRequestType = RequestType.playAndRecognize;

          _activeRecognition = new SpeechRequest(response.getRequestID(),RequestType.playAndRecognize,false);   
          _activeRecognition.setBlockingCall(false);
          
          
          //activeRequests.put(String.valueOf(response.getRequestID()),recognizeCall);   

          // speak request
          request = ttsChannel.createRequest(MrcpMethodName.SPEAK);
          if (!urlPrompt) {
              request.setContent("text/plain", null, prompt);
           } else {
               request.setContent("text/uri-list", null, prompt); 
           }
          response = ttsChannel.sendRequest(request);

          if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("MRCP response received:\n" + response.toString());
          }
          
          _activeBlockingTts = new SpeechRequest(response.getRequestID(),RequestType.playAndRecognize,false);   
          _activeBlockingTts.setBlockingCall(false);
          //activeRequests.put(String.valueOf(response.getRequestID()),playCall);  
          
          //link the play and recogize requests.
          _activeBlockingTts.setLinkedRequest(_activeRecognition);
          _activeRecognition.setLinkedRequest(_activeBlockingTts);
          
          return _activeRecognition;

//            MrcpHeader completionCauseHeader = _mrcpEvent.getHeader(MrcpHeaderName.COMPLETION_CAUSE);
//            CompletionCause completionCause = (CompletionCause) completionCauseHeader.getValueObject();          
//            return (completionCause.getCauseCode() == 0) ? _mrcpEvent.getContent() : null ;
    }


    /**
     * Construct recog request.
     * 
     * @param grammarUrl the grammar url
     * 
     * @return the mrcp request
     * 
     * @throws MalformedURLException the malformed URL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private MrcpRequest constructRecogRequest(String grammarUrl, boolean hotword, boolean attachGrammar, long noInputTimeout) throws MalformedURLException, IOException {

          // recog request
          MrcpRequest request = recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
          
          if (hotword) {
              request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
           }
           if (noInputTimeout != 0) {
              request.addHeader(MrcpHeaderName.NO_INPUT_TIMEOUT.constructHeader(new Long(noInputTimeout)));
              request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.TRUE)); 
           } else {
              request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.FALSE));     
           }
          
           if (hotword) {
              request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
           }

           if (attachGrammar) {
              URL gUrl = new URL(grammarUrl);
              request.setContent(_contentType, null, gUrl);
          } else {
              request.setContent("text/uri-list", null, grammarUrl);
          }
        return request;
    } 

    
    /**
     * Construct recog request.
     * 
     * @param reader the reader
     * 
     * @return the mrcp request
     * 
     * @throws MalformedURLException the malformed URL exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private MrcpRequest constructRecogRequest(Reader reader, boolean hotword, long noInputTimeout) throws MalformedURLException, IOException {

        BufferedReader in  = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        String line = null;
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        
       LOGGER.debug("The grammar text: " +sb.toString());
        
        // recog request
        MrcpRequest request = recogChannel.createRequest(MrcpMethodName.RECOGNIZE);
        if (noInputTimeout != 0) {
            request.addHeader(MrcpHeaderName.NO_INPUT_TIMEOUT.constructHeader(new Long(noInputTimeout)));
            request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.TRUE)); 
         } else {
            request.addHeader(MrcpHeaderName.START_INPUT_TIMERS.constructHeader(Boolean.FALSE));     
         }
        if (hotword) {
            request.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
         }
        request.setContent("application/jsgf", null, sb.toString());
        return request;
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void playBlocking(String prompt) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) { 
            throw new  NoMediaControlChannelException();
        }
        _activeBlockingTts = queuePrompt(prompt);
        waitPlayCompleted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playBlocking(URL prompt) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) { 
            throw new  NoMediaControlChannelException();
        }
        _activeBlockingTts = queuePrompt(prompt);
        waitPlayCompleted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playBlocking(List<URL> prompts) throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException {
        if (ttsChannel == null) { 
            throw new  NoMediaControlChannelException();
        }
        _activeBlockingTts = queuePrompt(prompts);
        waitPlayCompleted();
    }

    /**
     * Delays until a playback has been completed.
     */
    private void waitPlayCompleted() {
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        _activeBlockingTts.setBlockingCall(true);
        while (!_activeBlockingTts.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#recognizeBlocking(java.lang.String, boolean, boolean)
     */
    public synchronized RecognitionResult recognizeBlocking(String grammarUrl, boolean hotword, boolean attachGrammar, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
        _activeRecognition = this.recognize(grammarUrl, hotword, attachGrammar, noInputTimeout);
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        _activeRecognition.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                	LOGGER.debug("Interrupt Excepton while waiting for recognition to complete");
                }
            }
  
        }
        return _activeRecognition.getResult();
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#playAndRecognizeBlocking(boolean, java.lang.String, java.lang.String, boolean)
     */
    public synchronized RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
    	if ((ttsChannel == null) ||  (recogChannel == null))
    		throw new  NoMediaControlChannelException();
    
        long noInputTimeout=0;
        MrcpRequest mrcpRequest = constructRecogRequest(grammarUrl, hotword, true,noInputTimeout);
        LOGGER.debug("The requestis: "+mrcpRequest.toString());
        _activeRecognition = internalPlayAndRecogize(urlPrompt, prompt, mrcpRequest);
        
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        _activeRecognition.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                	LOGGER.debug("Interrupt Excepton while waiting for recognition to complete");
                }
            }
  
        }
        return _activeRecognition.getResult();
    }
    
    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOnBargeIn()
     */
    public void turnOnBargeIn() {
        bargeIn = true;    
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#turnOffBargeIn()
     */
    public void turnOffBargeIn() {
       bargeIn = false;
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#setListener(org.speechforge.cairo.client.SpeechEventListener)
     */
    public void setListener(SpeechEventListener listener) {
        addListener(listener);
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#setDefaultListener(org.speechforge.cairo.client.SpeechEventListener)
     */
    public void setDefaultListener(SpeechEventListener listener) {
        this.setListener(listener);
        
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#playAndRecognizeBlocking(boolean, java.lang.String, java.io.Reader, boolean)
     */
    public synchronized RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
    	if ((ttsChannel == null) ||  (recogChannel == null))
    		throw new  NoMediaControlChannelException();
    
    	
        long noInputTimeout=0;
        MrcpRequest mrcpRequest = constructRecogRequest(reader,hotword,noInputTimeout);
        SpeechRequest request = internalPlayAndRecogize(urlPrompt,prompt, mrcpRequest);
        
        //Block...
        //ActiveRequest request = activeRequests.get(String.valueOf(response.getRequestID()));
        
        request.setBlockingCall(true);
        while (!_activeRecognition.isCompleted()) {
            synchronized (this) {        
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                   	LOGGER.debug("Interrupt Excepton while waiting for recognition to complete");
                }
            }
  
        }
        return request.getResult();
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#recognizeBlocking(java.io.Reader, boolean)
     */
    public synchronized RecognitionResult recognizeBlocking(Reader reader, boolean hotword, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
    	
    	if (recogChannel == null)
    		throw new  NoMediaControlChannelException();
  
    	LOGGER.warn("The recognize blocking(reader,hotwordFlag) method is not implemented");
        return null;
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClientProvider#characterEventReceived(char)
     */
    public void characterEventReceived(char c) {
        LOGGER.debug("speechclient.chareventreceived: "+c);
        
        if (_dtmfState == DtmfState.waitingForInput) {
            LOGGER.debug("   waitingfor input...");
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
			if ((bargeIn) ) { //&&  (_activeRequestType == RequestType.playAndRecognize)){
				try {
					sendBargeinRequest();
				} catch (MrcpInvocationException e) {
					LOGGER.warn("MRCPv2 Status Code "+ e.getResponse().getStatusCode());
					LOGGER.warn(e, e);
				} catch (IOException e) {
					LOGGER.warn(e, e);
				} catch (InterruptedException e) {
					LOGGER.warn(e, e);
				}
			}
            
            
            //now we have the first char, we are waiting for a match
            _dtmfState = DtmfState.waitingForMatch;


            _charArray = new char[20];
            _charArray[0] = c;
            _length=1;    
            _inBuf =  new String(_charArray);
            LOGGER.debug("The first inBuf is : "+ _inBuf);


            // do the DTMF pattern matching
            checkForDtmfMatch(_inBuf);
            
        } else if (_dtmfState == DtmfState.waitingForMatch) {
            LOGGER.debug("   waiting for match...");

                //concatenate the new char to end of the dtmf string receievd up till now
          
                _charArray[_length++] = c;
                _inBuf =  new String(_charArray);
                LOGGER.debug("The new inBuf is: "+_inBuf);
                
                // do the DTMF pattern matching
                checkForDtmfMatch(_inBuf);

        } else {
            LOGGER.warn("Got dtmf signal while dtmf was not enabled by the client: "+c+  "  Discarding it.");
        }
    }


    private void checkForDtmfMatch(String c) {

   
        //do a regex match.  if it matches we are done
        Matcher m = _pattern.matcher(_inBuf);

        //if it matches
        if (m.find()) {
            LOGGER.debug("Got a dtmf match : "+_inBuf);
            _dtmfState = DtmfState.complete;
            
            //cancel the recog timer
            if (_noRecogTimeoutTask != null) {
                _noRecogTimeoutTask.cancel();
                _noRecogTimeoutTask = null;
            }
           
            //return the recognition results
           _dtmfListener.characterEventReceived(_inBuf,SpeechEventListener.DtmfEventType.recognitionMatch);
           

        }  else {
            LOGGER.debug("No match : "+_inBuf); 
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
            LOGGER.warn("DTMF Recognition already active.");   
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


    //Utility methods
    
    public static MrcpChannel createTtsChannel(String xmitterChannelId, InetAddress remoteHostAdress, int xmitterPort) throws IllegalArgumentException, IllegalValueException, IOException {
        //Construct the MRCP Channels    
        MrcpChannel ttsChannel = provider.createChannel(xmitterChannelId, remoteHostAdress, xmitterPort, protocol);
        return ttsChannel;
    }
    
    public static MrcpChannel createRecogChannel(String receiverChannelId, InetAddress remoteHostAdress, int receiverPort) throws IllegalArgumentException, IllegalValueException, IOException {
        //Construct the MRCP Channels
        MrcpChannel recogChannel = provider.createChannel(receiverChannelId, remoteHostAdress, receiverPort, protocol);
        return recogChannel;
    }
    
    public static NativeMediaClient createMediaClient(int localRtpPort, InetAddress rserverHost, int remoteRtpPort) throws IllegalArgumentException, IllegalValueException, IOException {
       NativeMediaClient mediaClient = new NativeMediaClient(localRtpPort, rserverHost, remoteRtpPort);
       mediaClient.startTransmit();
       return mediaClient;
    }
    
    
    public static SdpMessage constructResourceMessage(int localRtpPort, Vector format, String mySipAddress, String localHost, String seesionName) throws UnknownHostException, SdpException {
        SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, localHost, seesionName);
        MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(localRtpPort, format);
        MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
        MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
        Vector<MediaDescription> v = new Vector<MediaDescription>();
        v.add(synthControlChannel);
        v.add(recogControlChannel);
        v.add(rtpChannel);
        sdpMessage.getSessionDescription().setMediaDescriptions(v);
        return sdpMessage;
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#cancelRequest(org.speechforge.cairo.client.SpeechRequest)
     */
    public void stopActiveRecognitionRequests() throws MrcpInvocationException, IOException, InterruptedException, NoMediaControlChannelException {

    	if (recogChannel == null) 
    		throw new  NoMediaControlChannelException();
    	
        MrcpRequest cancelRequest = recogChannel.createRequest(MrcpMethodName.STOP);
        MrcpResponse response = recogChannel.sendRequest(cancelRequest);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Stopped Recognition, MRCP response received:\n" + response.toString());
        }
        
        //Blocking Methods need to be notified and unblocked
        //signal for the blocking call to check for unblocking
        if (_activeRecognition != null) {
            synchronized (this) {
                _activeRecognition.setCompleted(true);
                this.notifyAll();
            } 
        }
    }


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#shutdown()
     */
    public void shutdown() throws MrcpInvocationException, IOException, InterruptedException {
        
        // TODO Determine if there are active requests before stopping them
        
        // Stop any active requests
        try {
	        this.stopActiveRecognitionRequests();
        } catch (NoMediaControlChannelException e) {
	       LOGGER.debug("As part of shutting down the speech client, stopping active recognition requests.  No recog control channel so nothing to stop.");
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


    /* (non-Javadoc)
     * @see org.speechforge.cairo.client.SpeechClient#recognize(java.io.Reader, boolean, boolean)
     */
    public SpeechRequest recognize(Reader reader, boolean hotword, boolean attachGrammar, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException {
    	
    	if (recogChannel == null)
    		throw new  NoMediaControlChannelException();
    
    	
        MrcpRequest mrcpRequest = constructRecogRequest(reader,hotword,noInputTimeout);
        
        if (hotword) {
            mrcpRequest.addHeader(MrcpHeaderName.RECOGNITION_MODE.constructHeader("hotword"));
        }
        if (noInputTimeout != 0) {
            mrcpRequest.addHeader(MrcpHeaderName.NO_INPUT_TIMEOUT.constructHeader(new Long(noInputTimeout)));
        }
        
        MrcpResponse response = recogChannel.sendRequest(mrcpRequest);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MRCP response received:\n" + response.toString());
        }

        if (response.getRequestState().equals(MrcpRequestState.COMPLETE)) {
            throw new RuntimeException("Recognition failed to start!");
        }
   
        //_activeRequestType = RequestType.recognize;
        SpeechRequest queuedRecognition = new SpeechRequest(response.getRequestID(),RequestType.recognize,false);   
        queuedRecognition.setBlockingCall(false);
        
           
        return queuedRecognition;
        
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


}

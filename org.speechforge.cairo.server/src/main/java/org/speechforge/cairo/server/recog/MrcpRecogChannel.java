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
package org.speechforge.cairo.server.recog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import javax.speech.recognition.GrammarException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mrcp4j.MrcpEventName;
import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.header.CompletionCause;
import org.mrcp4j.message.header.IllegalValueException;
import org.mrcp4j.message.header.MrcpHeader;
import org.mrcp4j.message.header.MrcpHeaderName;
import org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest;
import org.mrcp4j.message.request.StartInputTimersRequest;
import org.mrcp4j.message.request.StopRequest;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.RecogOnlyRequestHandler;
import org.speechforge.cairo.exception.ResourceUnavailableException;
import org.speechforge.cairo.exception.UnsupportedHeaderException;
import org.speechforge.cairo.server.MrcpGenericChannel;

import edu.cmu.sphinx.jsgf.JSGFGrammarException;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;

/**
 * Handles MRCPv2 recognition requests by delegating to a dedicated {@link org.speechforge.cairo.server.recog.RTPRecogChannel}.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class MrcpRecogChannel extends MrcpGenericChannel implements RecogOnlyRequestHandler {

    /** Logger instance. */
    private static final Logger LOGGER =
            LogManager.getLogger(MrcpRecogChannel.class);

    private static final Long LONG_MINUS_ONE = new Long(-1);

    public static Long DEFAULT_NO_INPUT_TIMEOUT = new Long(10000);
    public static Boolean DEFAULT_START_INPUT_TIMERS = Boolean.TRUE;

    static short IDLE = 0;
    static short RECOGNIZING = 1;
    static short RECOGNIZED = 2;

    /** The RTP channel. */
    private RTPRecogChannel rtpChannel;
    /*volatile*/ short _state = IDLE;
    //private String _channelID;
    private GrammarManager _grammarManager;

    /**
     * Constructs a new object.
     * @param channelID the ID of the channel 
     * @param channel the RTP channel
     * @param baseGrammarDir base directory for grammars
     */
    public MrcpRecogChannel(String channelID, RTPRecogChannel channel,
            File baseGrammarDir) {
        //_channelID = channelID;
        rtpChannel = channel;
        _grammarManager = new GrammarManager(channelID, baseGrammarDir);
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#defineGrammar(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse defineGrammar(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#recognize(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse recognize(UnimplementedRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        MrcpHeader completionCauseHeader = null;
        MrcpHeader completionReasonHeader = null;
        short statusCode = -1;

        if (_state == RECOGNIZING) {
            // TODO: cancel or queue request instead (depending upon value of 'cancel-if-queue' header)
            statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
        } else {
            GrammarLocation grammarLocation = null;
            if (request.hasContent()) {
                String contentType = request.getContentType();
                if (isJSGFGrammar(contentType)) {
                    try {
                        final String grammarUrl = request.getContent();
                        MrcpHeader contentIdHeader =
                                request.getHeader(MrcpHeaderName.CONTENT_ID);
                        grammarLocation = processJSGFGrammar(grammarUrl,
                                contentIdHeader);
                    } catch (IOException e) {
                        LOGGER.warn(e, e);
                        statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                    }
                } else if (contentType.equalsIgnoreCase("text/uri-list")) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("processing uri list");
                    }
                    String text = request.getContent();
                    final List<String> uris = parseUriList(text);
                    MrcpHeader contentIdHeader =
                            request.getHeader(MrcpHeaderName.CONTENT_ID);
                    for (String str : uris) {
                        try {
                            processJSGFGrammar(str, contentIdHeader);
                        } catch (IOException e) {
                            LOGGER.warn(e.getMessage(), e);
                            statusCode = MrcpResponse.STATUS_OPERATION_FAILED;
                        }
                    }
                } else {
                    LOGGER.warn("unsupported grammar type '" + contentType +
                            "'");
                    statusCode = MrcpResponse.STATUS_UNSUPPORTED_HEADER_VALUE;
                }
            }
            if (statusCode < 0) { 
                // if no error so far, start recognition
                try {
                    requestState = startRecognition(request, session,
                            grammarLocation);
                    statusCode = MrcpResponse.STATUS_SUCCESS;
                } catch (IllegalStateException e) {
                    LOGGER.warn(e, e);
                    statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
                    // TODO: cancel or queue request instead (depending upon value of 'cancel-if-queue' header)
                } catch (IOException | ResourceUnavailableException e) {
                    LOGGER.warn("recognize error : " + e, e);
                    statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
                    CompletionCause completionCause = 
                            new CompletionCause((short) 6, "recognizer-error");
                    completionCauseHeader = MrcpHeaderName.COMPLETION_CAUSE.constructHeader(completionCause);
                    completionReasonHeader = MrcpHeaderName.COMPLETION_REASON.constructHeader(e.getMessage());
                } catch (GrammarException | JSGFGrammarException 
                        | JSGFGrammarParseException e) {
                    LOGGER.warn("grammar load failure: " + e, e);
                    statusCode = MrcpResponse.STATUS_OPERATION_FAILED;
                    CompletionCause completionCause = 
                            new CompletionCause((short) 4, "grammar-load-failure");
                    completionCauseHeader = MrcpHeaderName.COMPLETION_CAUSE.constructHeader(completionCause);
                    completionReasonHeader = MrcpHeaderName.COMPLETION_REASON.constructHeader(e.getMessage());
                } catch (IllegalValueException e) {
                    LOGGER.warn(e, e);
                    statusCode = MrcpResponse.STATUS_ILLEGAL_VALUE_FOR_HEADER;
                    // TODO: add completion cause header
                    // TODO: add bad value headers
		}
            }
        }

        MrcpResponse response = session.createResponse(statusCode, requestState);
        response.addHeader(completionCauseHeader);
        response.addHeader(completionReasonHeader);
        return response;
    }

    /**
     * Processes a JSGF grammar.
     * @param grammarURL the URL of the grammar to load
     * @return the grammar location
     * @throws IOException
     *           if an I/O error occurs
     */
    private GrammarLocation processJSGFGrammar(final String grammarURL,
            final MrcpHeader contentIdHeader)
                throws IOException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processing jsgf '" + grammarURL + "'");
        }
        // save grammar to file
        final URL url = new URL(grammarURL);
        final URLConnection uc = url.openConnection();
        
        //TODO:  Should replace this check content type and not a lways assume it is JSGF 
        //       (But using the URI-LIST as a work around for large grammars not supported in mrcp4j 
        //        and in some cases the uri does not hav a content type (file uri's)).
        //if ((uc.getContentType().equals("text/plain")) ||                //TODO: Remove this should not assume text/plain is jsgf
        //    (uc.getContentType().equals("application/jsgf"))){
        BufferedReader in = new BufferedReader(new InputStreamReader(
                        uc.getInputStream()));
           
        //TODO: Make this more efficient
        String inputLine;
        String grammarText = new String();
        while ((inputLine = in.readLine()) != null) {
            grammarText = grammarText + inputLine+"\n";
        }
        in.close();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(grammarText);
        }
        String grammarID = (contentIdHeader == null) ? null : contentIdHeader.getValueString();
        return _grammarManager.saveGrammar(grammarID, grammarText);
    }

    /**
     * Checks if the content type is a JSGF grammar.
     * 
     * @param contentType
     *            the content type
     * @return true if the content type is a JSGF grammar
     */
    private boolean isJSGFGrammar(String contentType) {
        return contentType.equalsIgnoreCase("application/jsgf") ||
                contentType.equalsIgnoreCase("application/x-jsgf");
    }

    /**
     * Starts the recognition process.
     * @param request the MRCP request
     * @param session t
     * @param grammarLocation the location of the grammar
     * @return the request state
     * @throws IllegalValueException
     * @throws IOException
     * @throws ResourceUnavailableException
     * @throws GrammarException
     * @throws JSGFGrammarParseException
     * @throws JSGFGrammarException
     */
    private MrcpRequestState startRecognition(UnimplementedRequest request,
            MrcpSession session, GrammarLocation grammarLocation)
            throws IllegalValueException, IOException,
            ResourceUnavailableException, GrammarException,
            JSGFGrammarParseException, JSGFGrammarException {
        MrcpRequestState requestState;
        Boolean startInputTimers = (Boolean) getParam(
                MrcpHeaderName.START_INPUT_TIMERS, request,
                DEFAULT_START_INPUT_TIMERS);
        Long noInputTimeout = (startInputTimers.booleanValue()) ?
                (Long) getParam(MrcpHeaderName.NO_INPUT_TIMEOUT, request, 
                        DEFAULT_NO_INPUT_TIMEOUT) : LONG_MINUS_ONE;
        //TODO: get the hotword mode from mrcp message        
        boolean hotword = false;
        String hw = (String) getParam(MrcpHeaderName.RECOGNITION_MODE, request,
                "normal");
        if (hw.equals("hotword")) {
           hotword = true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting recognition with grammar '" + 
                grammarLocation.getFilename() + "' and hotword mode " +
                hotword + " and no input timeout " + 
                noInputTimeout.longValue() + " ms.");
        }
        final RecogListener listener = new Listener(session);
        rtpChannel.recognize(listener, grammarLocation, 
                noInputTimeout.longValue(), hotword);
        requestState = MrcpRequestState.IN_PROGRESS;
        _state = RECOGNIZING;
        return requestState;
    }

    /**
     * Parses a list of URIs from the request content into a list. 
     * @param content the request content
     * @return determined list
     */
    private List<String> parseUriList(final String content) {
        final List<String> uris = new java.util.ArrayList<String>();
        final Scanner scanner = new Scanner(content);
        while (scanner.hasNextLine()) {
            final String uri = scanner.nextLine();
            uris.add(uri);
        }
        scanner.close();
        return uris;
    }
    
    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#interpret(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse interpret(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#getResult(org.mrcp4j.message.request.MrcpRequestFactory.UnimplementedRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse getResult(UnimplementedRequest request, MrcpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#startInputTimers(org.mrcp4j.message.request.StartInputTimersRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse startInputTimers(StartInputTimersRequest request, MrcpSession session) {
        MrcpResponse response = null;

        try {
            Long noInputTimeout = (Long) getParam(MrcpHeaderName.NO_INPUT_TIMEOUT, request, DEFAULT_NO_INPUT_TIMEOUT);
            rtpChannel.startInputTimers(noInputTimeout.longValue());
            response = session.createResponse(MrcpResponse.STATUS_SUCCESS, MrcpRequestState.COMPLETE);
        } catch (IllegalStateException e) {
            LOGGER.debug(e, e);
            response = session.createResponse(MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE, MrcpRequestState.COMPLETE);
        } catch (IllegalValueException e) {
            LOGGER.debug(e, e);
            response = session.createResponse(MrcpResponse.STATUS_ILLEGAL_VALUE_FOR_HEADER, MrcpRequestState.COMPLETE);
            response.addHeader(request.getHeader(MrcpHeaderName.NO_INPUT_TIMEOUT));  // TODO: get header name from exception?
        }

        return response;
    }

    /* (non-Javadoc)
     * @see org.mrcp4j.server.provider.RecogOnlyRequestHandler#stop(org.mrcp4j.message.request.StopRequest, org.mrcp4j.server.MrcpSession)
     */
    public synchronized MrcpResponse stop(StopRequest request, MrcpSession session) {
        
        LOGGER.debug("Stop recognition called, mrcp channel state: "+_state+" rtp channel state: "+rtpChannel._state);



        
        if (_state == IDLE) {
            //Nothing to cancel
            LOGGER.warn("Stopping recognition, but nothing to cancel.  Mrcp channel state is IDLE");
        } else if (_state == RECOGNIZED) {
            //Nothing to cancel
            LOGGER.warn("Stopping recognition, but nothing to cancel.  Mrcp channel state is RECOGNIZED");            
        } else if (_state == RECOGNIZING) {
            LOGGER.info("Stopping recognition.  Mrcp channel state is RECOGNIZING and rtp channel state is is "+rtpChannel._state);
            if (rtpChannel._state == RTPRecogChannel.WAITING_FOR_SPEECH) {
                if (rtpChannel._noInputTimeoutTask != null) {
                   rtpChannel._noInputTimeoutTask.cancel();
                   LOGGER.info("Stopping recognition, canceled no input timer");
                }
                rtpChannel.closeProcessor();
                //TODO: Add  active-request-id-list header containing the request-id of the RECOGNIZE request that was terminated.
            } else if (rtpChannel._state == RTPRecogChannel.SPEECH_IN_PROGRESS) {
                rtpChannel.closeProcessor();
                //TODO: Add  active-request-id-list header containing the request-id of the RECOGNIZE request that was terminated.
            } else if (rtpChannel._state == RTPRecogChannel.COMPLETE) {
                LOGGER.warn("Stopping recognition, but nothing to cancel.  Mrcp channel state is recognizing, but rtp chan state is complete");
            } else {
                LOGGER.warn("Stopping recognition, but invalid rtp channel state: "+rtpChannel._state);
            }
            
        } else {
            LOGGER.warn("Stopping recognition, but invalid mrcp channel state: "+_state);
        }
        
        //change the state to IDLE
        _state = IDLE;
        
        MrcpResponse response = null;
        response = session.createResponse(MrcpResponse.STATUS_SUCCESS, MrcpRequestState.COMPLETE);

        return response;
    }
    
    /**
     * Listener for the recognition process.
     */
    private class Listener implements RecogListener {

        private MrcpSession session;

        /**
         * Creates a new instance.
         * @param mrcpSession the MRCP session
         */
        public Listener(MrcpSession mrcpSession) {
            session = mrcpSession;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void recognitionComplete(RecognitionResult result) {
            if (result != null) {
                LOGGER.info("recognition complete: " + result.toString());
            } else {
                LOGGER.info("recognition complete: null");
            }
            synchronized (MrcpRecogChannel.this) {
                _state = RECOGNIZED;
            }
            try {
                MrcpEvent event = session.createEvent(
                        MrcpEventName.RECOGNITION_COMPLETE,
                        MrcpRequestState.COMPLETE);
                String content = result.toString();
                if (content == null || content.trim().length() < 1) {
                    CompletionCause completionCause = new CompletionCause(
                            (short) 1, "no-match");
                    event.addHeader(MrcpHeaderName.COMPLETION_CAUSE
                            .constructHeader(completionCause));
                } else {
                    CompletionCause completionCause = new CompletionCause(
                            (short) 0, "success");
                    event.addHeader(MrcpHeaderName.COMPLETION_CAUSE
                            .constructHeader(completionCause));
                    event.setContent("text/plain", null, content);
                }
                session.postEvent(event);
            } catch (IllegalStateException | TimeoutException e) {
                LOGGER.warn("error processing the recognition result: " +e,
                        e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void speechStarted() {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("speech started");
            }
            short state;
            synchronized (MrcpRecogChannel.this) {
                state = _state;
            }
            if (state == RECOGNIZING) try {
                MrcpEvent event = session.createEvent(
                        MrcpEventName.START_OF_INPUT,
                        MrcpRequestState.IN_PROGRESS
                );
                session.postEvent(event);
            } catch (IllegalStateException | TimeoutException e) {
                LOGGER.warn("error processing speech started : " + e, e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void noInputTimeout() {
            LOGGER.info("no input timeout");
            short state;
            synchronized (MrcpRecogChannel.this) {
                state = _state;
                _state = IDLE;
            }
            if (state == RECOGNIZING)
                try {
                    MrcpEvent event = session.createEvent(
                            MrcpEventName.RECOGNITION_COMPLETE,
                            MrcpRequestState.COMPLETE);
                    CompletionCause completionCause = new CompletionCause(
                            (short) 2, "no-input-timeout");
                    MrcpHeader completionCauseHeader = MrcpHeaderName.COMPLETION_CAUSE
                            .constructHeader(completionCause);
                    event.addHeader(completionCauseHeader);
                    session.postEvent(event);
                } catch (IllegalStateException | TimeoutException e) {
                    LOGGER.warn("error processing timeout: " + e, e);
                }
        }
        
    }

    // TODO: define which headers are supported fully
    private static EnumSet FULLY_SUPPORTED_HEADERS  = EnumSet.of(MrcpHeaderName.START_INPUT_TIMERS);

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.MrcpGenericChannel#validateParam(org.mrcp4j.message.header.MrcpHeader)
     */
    @Override
    protected boolean validateParam(MrcpHeader header) throws UnsupportedHeaderException, IllegalValueException {
        header.getValueObject();
        MrcpHeaderName headerName = header.getHeaderName();
        if (headerName == null) {
            throw new UnsupportedHeaderException();
        }
        if (!headerName.isApplicableTo(MrcpResourceType.SPEECHRECOG)) {
            throw new UnsupportedHeaderException();
        }
        if (MrcpHeaderName.ENROLLMENT_HEADER_NAMES.contains(headerName)) {
            throw new UnsupportedHeaderException();
        }
        return FULLY_SUPPORTED_HEADERS.contains(header);
    }

}

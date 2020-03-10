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
package org.speechforge.cairo.sip;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.Iterator;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionAlreadyExistsException;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;


/**
 * Implements the JAINSIP SipListener interface. Receives the low level sip
 * events via JAIN SIP.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipListenerImpl implements SipListener {

    static Logger _logger = Logger.getLogger(SipListenerImpl.class);

    private SipAgent sipClient;

    public SipListenerImpl(SipAgent sipClient) {
        this.sipClient = sipClient;
    }

    public void processDialogTerminated(DialogTerminatedEvent arg0) {
        _logger.info("Got a dialog terminated event");
    }

    public void processIOException(IOExceptionEvent arg0) {
        _logger.info("Got an IO Exception");
    }

    public void processRequest(RequestEvent requestEvent) {

        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
    /*
         * 
         * TODO: Check if the request is really addressed to me. check To header
         * ot route or contact header? ToHeader to = (ToHeader)
         * request.getHeader(ToHeader.NAME); if
         * (sipClient.getMyAddress().getURI().toString().equals(
         * to.getAddress().getURI().getScheme())) {
         */
        
        if (_logger.isDebugEnabled()) {
        	  StringBuilder sb = new StringBuilder(); 
        	  sb.append("\n------------- RECEIVED A SIP REQUEST ---------------");

        	  sb.append("\nReceived a "+ request.getMethod() +" SIP request");
            if (requestEvent.getDialog() != null) {
            	sb.append("\nPre-existing Dialog: "+requestEvent.getDialog().toString());
            }
            Iterator headers = request.getHeaderNames();
            while (headers.hasNext()) {
           	    sb.append("\n");
            	sb.append(request.getHeader((String) headers.next()).toString());
            }
            byte[] contentBytes = request.getRawContent();
            if (contentBytes == null) {
            	sb.append("\nNo content in the request.");
            } else {
               String contentString = new String(contentBytes);
               sb.append("\n");
               sb.append(contentString);
            }
            _logger.debug(sb);
         }

        if (request.getMethod().equals(Request.INVITE)) {
            processInvite(requestEvent);
        } else if (request.getMethod().equals(Request.ACK)) {
            processAck(requestEvent);
        } else if (request.getMethod().equals(Request.BYE)) {
            processBye(requestEvent);
        } else if (request.getMethod().equals(Request.CANCEL)) {
            processCancel(requestEvent);
        } else if (request.getMethod().equals(Request.REGISTER)) {
            processRegister(requestEvent);
        } else if (request.getMethod().equals(Request.INFO)) {
            processInfo(requestEvent);
        } else {
            // TODO: this snippet is taken from the shootist example. Shootme
            // only has teh first line
            // I dont really undersatnd why it is sending an accepted response
            // and thaen a REFER request
            // and why the shootist example just sends the response. I would
            // think it should be symetrical.
            try {
                _logger.info("Got an unhandled SIP request Method = " + request.getMethod());
                SipAgent.sendResponse(stx, sipClient.getMessageFactory().createResponse(202, request));
                // send one back
                SipProvider prov = (SipProvider) requestEvent.getSource();
                Request refer = requestEvent.getDialog().createRequest("REFER");
                SipAgent.sendRequest(requestEvent.getDialog(), prov.getNewClientTransaction(refer));
            } catch (SipException e) {
                _logger.error(e, e);
            } catch (InvalidArgumentException e) {
                _logger.error(e, e);
            } catch (ParseException e) {
                _logger.error(e, e);
            }
        }
    }

    public void processResponse(ResponseEvent responseEvent) {

        Dialog dialog = null;
        SipSession session = null;

        Response response = (Response) responseEvent.getResponse();
        ClientTransaction ctx = responseEvent.getClientTransaction();
        CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

        if (_logger.isDebugEnabled()) {
        	 StringBuilder sb = new StringBuilder();
        	 sb.append("\n------------- RECEIVED A SIP RESPONSE ---------------");
        	 sb.append("\nSip Response received : Status:" + response.getStatusCode()+", "+response.getReasonPhrase());
           
           Iterator headers = response.getHeaderNames();
           while (headers.hasNext()) {
        	   sb.append("\n");
        	   sb.append(response.getHeader((String) headers.next()).toString());
           }
           
           byte[] contentBytes = response.getRawContent();

           if (contentBytes == null) {
        	   sb.append("\nNo content in the response.");
           } else {
               String contentString = new String(contentBytes);
               sb.append("\n");
               sb.append(contentString);
           } 
           _logger.debug(sb);
        }
        
        
        if (ctx != null) {
            dialog = ctx.getDialog();
        } else {
            _logger.info("Stray SIP response -- dropping ");
            //_logger.info("Status:" + response.getStatusCode()+", "+response.getReasonPhrase());
            
            return;
        }

        try {
            if (response.getStatusCode() == Response.OK) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    // Got an INVITE OK
                    //Request ackRequest = dialog.createRequest(Request.ACK);
                    Request ackRequest = null;
                    try {
                        ackRequest = dialog.createAck( ((CSeqHeader) response.getHeader(CSeqHeader.NAME)).getSeqNumber() );
                    } catch (InvalidArgumentException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    
                    if (_logger.isDebugEnabled()) {
                        StringBuilder sb = new StringBuilder(); 
                        sb.append("\n------------- SENDING A SIP ACK REQUEST ---------------");
                     
                        Iterator headers = ackRequest.getHeaderNames();
                        while (headers.hasNext()) {
                        	   sb.append("\n");
                        	 sb.append(ackRequest.getHeader((String) headers.next()).toString());
                        }
                        
                        byte[] contentBytes = ackRequest.getRawContent();

                        if (contentBytes == null) {
                        	sb.append("\nNo content in the response.");
                        } else {
                            String contentString = new String(contentBytes);
                            sb.append("\n");
                            sb.append(contentString);
                        } 
                        _logger.debug(sb);
                     }
                    
                    
                    dialog.sendAck(ackRequest);

                    // put the dialog into the session and remove from pending
                    // map and place into active session map
                    session = SipSession.getSessionFromPending(ctx.toString());

                    if (session != null) {
                        session.setSipDialog(dialog);
                        SipSession.moveFromPending(session);

                        byte[] contentBytes = response.getRawContent();
                        SdpFactory sdpFactory = SdpFactory.getInstance();

                        if (contentBytes == null) {
                            _logger.info("No content in the response.");
                        }
                        String contentString = new String(contentBytes);
                        SessionDescription sd = null;
                        try {
                            sd = sdpFactory.createSessionDescription(contentString);
                        } catch (SdpParseException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);
                        _logger.debug(sdpMessage.toString());                        
                        SdpMessage sdpResponse = sipClient.getSessionListener().processInviteResponse(true, sdpMessage, session);
                        session.setState(SipSession.SessionState.inviteResponseReceived);
                        synchronized(session){
                        	session.notifyAll();
                        }
                    } else {
                        // TODO: handle error condition where the session was
                        // not in the pending map
                        _logger.info("SIP Invite Response received but the session was not in pending map. "
                                + response.toString());
                    }

                } else if (cseq.getMethod().equals(Request.CANCEL)) {
                    // TODO: handle cancel processing properly
                    if (dialog.getState() == DialogState.CONFIRMED) {
                        // oops cancel went in too late. Need to hang up the
                        // dialog.
                        // System.out.println("Sending BYE -- cancel went in too
                        // late !!");
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        ClientTransaction ct = sipClient.getSipProvider().getNewClientTransaction(byeRequest);
                        SipAgent.sendRequest(dialog, ct);
                    }
                } else if (cseq.getMethod().equals(Request.BYE)) {
                }
     
            } else if (responseEvent.getResponse().getStatusCode() == Response.NOT_ACCEPTABLE_HERE) {
                if (cseq.getMethod().equals(Request.INVITE)) {
                    session = SipSession.getSessionFromPending(ctx.toString());
                    if (session != null) {
                       SipSession.removeSessionFromPending(session) ;
                       session.setState(SipSession.SessionState.inviteResponseReceived);
                       synchronized(session){
                       	session.notifyAll();
                       }
                       SdpMessage sdpResponse = sipClient.getSessionListener().processInviteResponse(false, null, session);
                    }
                } else { //methods not handled for this repsonse code
                    _logger.warn("Received an SIP response status code for an unhandled method (ignoring it): "+ responseEvent.getResponse().getStatusCode()+" : "+responseEvent.getResponse().getReasonPhrase()+" response to a "+cseq.getMethod());
                }
            } else {  //response code not handled
               _logger.warn("Received an unhandled SIP response status code (ignoring it): " + responseEvent.getResponse().getStatusCode() +" : "+responseEvent.getResponse().getReasonPhrase());
            }
        } catch (SipException e) {
            // TODO: Handle case where there is an exception handling the invite
            // response. I think We still need to respond with an ACK (or NACK)
            _logger.error(e, e);
        }
    }

    public void processTimeout(TimeoutEvent event) {
        _logger.debug("Transaction Time out");
        
        // if this is a client transaction timeout and if an invite
        //cleanup pending sessions
        ClientTransaction ctx = event.getClientTransaction();
        if (ctx != null) {
            SipSession session = SipSession.getSessionFromPending(ctx.toString());
            if (session != null) {
                SipSession.removeSessionFromPending(session);
                session.setState(SipSession.SessionState.inviteTimedOut);
            }
        }
        //TODO: should send a cancel request too
        
        //notify the application layer of a timeout
        sipClient.getSessionListener().processTimeout(event);
    }

    public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
        _logger.debug("Got a transaction terminated event");
    }

    /**
     * Process the ACK request.
     */
    public void processAck(RequestEvent requestEvent) {
        // _logger.info("Got a ACK event");
        // ServerTransaction stx = requestEvent.getServerTransaction();
        // Dialog dialog = stx.getDialog();
        // SipSession session = SipSession.getSession(dialog.getDialogId());
    }

    /**
     * Process the invite request.
     */
    public void processInvite(RequestEvent requestEvent) {
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        Response okResponse = null;
        ServerTransaction stx = null;
        SipSession session = null;
        boolean reinvite = false;
        
        String guid = sipClient.getGUID();
        
        String channelName = null;
        Header channelHeader = requestEvent.getRequest().getHeader("x-channel");
        if (channelHeader != null) {
           String channel[] = channelHeader.toString().split(":",2);
           channelName = channel[1];
        }
        
        String applicationName = null;
        Header applicationHeader = requestEvent.getRequest().getHeader("x-application");
        if (applicationHeader != null) {
           String application[] = applicationHeader.toString().split(":",2);
           applicationName = application[1];
        }
        
        try {

            stx = requestEvent.getServerTransaction();
            if (stx == null) {
                stx = sipProvider.getNewServerTransaction(request);
            }
           

            byte[] contentBytes = request.getRawContent();
            SdpFactory sdpFactory = SdpFactory.getInstance();

            boolean noOffer = false;
            if (contentBytes == null) {
                // TODO: How to deal with the absense of an offer in the invite
                // the sepc says that the UAS should send an offer in the 2xx
                // response and
                // expect a response to the offer in the ACK (Does it make sense
                // here what should the server offer
                // two mrcp channels and a rtp channel?
                noOffer = true;
                _logger.info("No offer in the invite request.  Should provide offer in response but not supported yet.");
            } else {
                Dialog dialog = requestEvent.getDialog();
                if ( dialog == null) {
                   // Send a provisional Response: Session Progress -- establishes the dialog id
                   Response response = sipClient.getMessageFactory().createResponse(Response.SESSION_PROGRESS, request);
                   ToHeader provToHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                   provToHeader.setTag(guid);
                   try {
                       SipAgent.sendResponse(stx, response);
                   } catch (InvalidArgumentException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }
                   dialog = stx.getDialog();
                   if (dialog != null) {
                       String did = dialog.getDialogId();
                       if (did != null) {
                           session = SipSession.getSession(did);
                           if (session != null) {
                               // TODO: handle a re-invite. This must be a re-invite if
                               // there already is a dialog with a valid ID and a session
                               reinvite = true;
                               session.setStx(stx);
                               session.setRequest(requestEvent);
                              _logger.warn("Recieved a re-invite request. Type A");
                           } else {
                               _logger.debug("adding the session with dialog ID: "+dialog.getDialogId());
                               session = SipSession.createSipSession(sipClient, null, dialog, requestEvent, stx, channelName, applicationName);
                               SipSession.addSession(session);
                           }
                       } else {
                           _logger.warn("Failed to create a SIP session for teh invite request.  No Dialog id established for the dialog.");
                       }
                   } else {
                       _logger.warn("Failed to create a SIP session for the invite request. No Dialog attached to server transaction.");
                   }
                   
                } else {
                    String did = dialog.getDialogId();
                    session = SipSession.getSession(did);
                    session.setStx(stx);
                    session.setRequest(requestEvent);
                    reinvite = true;
                    _logger.warn("Recieved a re-invite request. Type B.");
                }
                
                //establish the session, now that the dialog id is established

            
                
                String contentString = new String(contentBytes);
                SessionDescription sd = sdpFactory.createSessionDescription(contentString);
                SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

                //valudate the sdp message (throw sdpException if the message is invalid)
                SdpMessageValidator.validate(sdpMessage);
                
                //process the invitaion (the resource manager processInviteRequest method)
                SdpMessage sdpResponse = sipClient.getSessionListener().processInviteRequest(sdpMessage, session);
                
                //----- REmoved the response sending
                //------Must send yourself. 
                //------To do so use the SipAgent.sendResponse()


            }
        } catch (SipException e) {
            e.printStackTrace();
            OfferRejected(requestEvent, session, stx);
            _logger.info("Could not process invite." + e, e);

        } catch (ParseException e) {
            OfferRejected(requestEvent,session, stx);
            _logger.info("Could not process invite." + e, e);
        } catch (ResourceUnavailableException e) {
            OfferRejected(requestEvent,session, stx);
            _logger.info("Could not process invite." + e, e);
        } catch (RemoteException e) {
            OfferRejected(requestEvent,session, stx);
            _logger.info("Could not process invite." + e, e);
        } catch (SdpException e) {
            OfferRejected(requestEvent,session, stx);
            _logger.info("Could not process invite." + e, e);
        }



    }
    

    /**
     * Process the invite request.
     */
    public void processInfo(RequestEvent requestEvent) {

        Request request = requestEvent.getRequest();
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            ServerTransaction stx = null;
            SipSession session = null;
            stx = requestEvent.getServerTransaction();
            
            //get the content, content type and content subtype out of the request, to be passed to the listener
 
            byte[] contentBytes = request.getRawContent();
            String contentString = new String(contentBytes);
            ContentTypeHeader cTypeHeader = (ContentTypeHeader) request.getHeader(ContentTypeHeader.NAME);
            if (cTypeHeader == null) {
                _logger.warn("no content type header in the info request."); 
            }

            if (stx == null) {
                _logger.warn("null stx so getting a new one...");
                try {
                    stx = sipProvider.getNewServerTransaction(request);
                } catch (TransactionAlreadyExistsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (TransactionUnavailableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            //_logger.info("the stx: "+stx.toString());

            Dialog dialog = requestEvent.getDialog();
            if ( dialog == null) {
                _logger.warn("info request: null dialog.  Therefore no session to find out where to forward the info");  
            } else {
                String did = dialog.getDialogId();
                session = SipSession.getSession(did);
                session.setStx(stx);
                session.setRequest(requestEvent);
            }

            //process the invitaion (the resource manager processInviteRequest method)
            sipClient.getSessionListener().processInfoRequest(session, cTypeHeader.getContentType(),cTypeHeader.getContentSubType(), contentString);

            //send the OK response
            try {
                Response response = sipClient.getMessageFactory().createResponse(200, request);
                SipAgent.sendResponse(stx, response);
            } catch (SipException e) {
                _logger.error(e, e);
            } catch (ParseException e) {
                _logger.error(e, e);
            } catch (InvalidArgumentException e) {
                _logger.error(e, e);
            }
           
    }


    

    private void OfferRejected(RequestEvent requestEvent, SipSession session, ServerTransaction stx) {
        // TODO: Distinguish between a rejected offer(488) and a busy here (486)
        // At present the code below just sends a 488 if there was any exception
        // Should handle a rsource unavalable exception differently for a resource that
        // is not supported vs a resource that is truely unavailable/busy.
        // The spec says the following about processing of a rejected offer or rejected call
        // 1) If the offer is rejected the spec says INVITE SHOULD return a 488
        // (Not Acceptable Here) response. Such a response
        // SHOULD include a Warning header field value explaining why the offer
        // was rejected.
        // 2) If the callee is currently not willing or able to take additional
        // calls at this end system. A 486 (Busy Here)
        // SHOULD be returned in such a scenario.
        _logger.info("Could not process invite request.");
        SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        try {
            if (stx == null) {
                stx = sipProvider.getNewServerTransaction(request);
            }
            Response response = sipClient.getMessageFactory().createResponse(Response.NOT_ACCEPTABLE_HERE, request);
            SipAgent.sendResponse(stx, response);

            // release resources
            for (SipResource r: session.getResources() ){
                r.bye(session.getId());
            }

            // cleanup the session
            SipSession.removeSession(session);
            
        } catch (SipException e) {
            _logger.error(e, e);
        } catch (InvalidArgumentException e) {
            _logger.error(e, e);
        } catch (ParseException e) {
            _logger.error(e, e);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public void processCancel(RequestEvent requestEvent) {
        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        // Dialog dialog = stx.getDialog();
        try {
            Response response = sipClient.getMessageFactory().createResponse(200, request);
            SipAgent.sendResponse(stx, response);

            // Not sure if this is really required. Do I need to save the invite
            // requests
            // that was sent earlier nd is now being cacnelled? Cancellation is
            // not needed
            // for now in any case...
            // if (dialog.getState() != DialogState.CONFIRMED) {
            // response = sipClient.getMessageFactory().createResponse(
            // Response.REQUEST_TERMINATED, inviteRequest);
            // stx.sendResponse(response);
            // }

        } catch (Exception e) {
            _logger.error(e, e);
        }
    }

    
    
    
    /**
     * Process the bye request.
     */
    public void processBye(RequestEvent requestEvent) {
        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        Dialog dialog = requestEvent.getDialog();
        SipSession session = SipSession.getSession(dialog.getDialogId());

        // TODO: check for any pending requests. The spec says that the
        // "UAS MUST still respond to any pending requests received for that
        // dialog. It is RECOMMENDED that a 487 (Request Terminated) response
        // be generated to those pending requests."
        if (session == null) {
            _logger.info("Receieved a BYE for which there is no corresponding session.  SessionID: "+dialog.getDialogId());
        } else {
            try {
                //process the invitaion (the resource manager processInviteRequest method)
                sipClient.getSessionListener().processByeRequest(session);
                SipSession.removeSession(session);
                Response response = sipClient.getMessageFactory().createResponse(200, request);
                SipAgent.sendResponse(stx, response);
            } catch (SipException e) {
                _logger.error(e, e);
            } catch (ParseException e) {
                _logger.error(e, e);
            } catch (InvalidArgumentException e) {
                _logger.error(e, e);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void processRegister(RequestEvent requestEvent) {
        // SipProvider sipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
        ServerTransaction stx = requestEvent.getServerTransaction();
        Dialog dialog = requestEvent.getDialog();
  
        
        try {
            //TODO:  to be able to forward calls to this device need to maintain a registry
            // for now this is just for demo support (i.e. calling from the device to cairo)
            _logger.info("registering device "+request.toString());
            Response response = sipClient.getMessageFactory().createResponse(200, request);
            SipAgent.sendResponse(stx, response);
        } catch (SipException e) {
            _logger.error(e, e);
        } catch (ParseException e) {
            _logger.error(e, e);
        } catch (InvalidArgumentException e) {
            _logger.error(e, e);

        }

    }
    
}

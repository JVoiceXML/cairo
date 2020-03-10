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


import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import java.util.Random;

import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionDoesNotExistException;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;

import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;

import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;



/**
 * The SipAgent used by Cairo elements for SIP signaling.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:slord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipAgent {

    private static Logger _logger = Logger.getLogger(SipAgent.class);

    // SIP protocol objects
    static AddressFactory addressFactory;

    static MessageFactory messageFactory;

    static HeaderFactory headerFactory;

    static SipStack sipStack;

    private SipProvider sipProvider;

    private ListeningPoint listeningPoint;

    private SipListener listener;

    private String transport = "udp";

    private int port;

    private String mySipAddress;

    private Address myAddress;

    private Address contactAddress;

    private String guidPrefix;

    static int sipStackLogLevel = 0;

    static String logFileDirectory = "logs/";

    private String sipAddress = "sip:cairo@speechforge.com";

    private String stackName = "SipStack";

    private String host;
    
    private String publicHost;

    private SessionListener sessionListener;

    private Random random = new Random((new Date()).getTime());

    public SipAgent(SessionListener sessionListener, String mySipAddress) throws SipException {
        this(sessionListener, mySipAddress, "CairoSipStack", 5060, "udp");
    }
 
    public SipAgent(SessionListener sessionListener, String mySipAddress, String stackName, int port,
            String transport) throws SipException {
        this(sessionListener, mySipAddress, stackName, null, null, port, transport);
    }

    public SipAgent(SessionListener sessionListener, String mySipAddress, String stackName, String host, String publicHost, int port,
            String transport) throws SipException {
        this.sessionListener = sessionListener;
        this.stackName = stackName;
        this.port = port;
        this.host = host;
        this.publicHost = publicHost;
        this.transport = transport;
        this.mySipAddress = mySipAddress;
        init();
    }
    
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getTransport() {
        return transport;
    }

    public String getStackName() {
        return stackName;
    }

    public AddressFactory getAddressFactory() {
        return addressFactory;
    }

    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    public HeaderFactory getHeaderFactory() {
        return headerFactory;
    }

    public SipStack getSipStack() {
        return sipStack;
    }

    public SipProvider getSipProvider() {
        return sipProvider;
    }

    public ListeningPoint getListeningPoint() {
        return listeningPoint;
    }

    private void init() throws SipException {

        if (host == null) {
            try {
                //InetAddress addr = InetAddress.getLocalHost();
                InetAddress addr = SipAgent.getLocalHost();
                host = addr.getHostAddress();
                //host = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                host = "127.0.0.1";
                //host = "localhost";
                _logger.debug(e, e);
                e.printStackTrace();
            } catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        guidPrefix = host + port + System.currentTimeMillis();
        SipFactory sipFactory = null;

        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", stackName);

        // The following properties are specific to nist-sip
        // and are not necessarily part of any other jain-sip
        // implementation.
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", logFileDirectory + stackName + "debug.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", logFileDirectory + stackName + "log.txt");
        properties.setProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS","false"); 


        // Set to 0 in your production code for max speed.
        // You need 16 for logging traces. 32 for debug + traces.
        // Your code will limp at 32 but it is best for debugging.
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", new Integer(sipStackLogLevel).toString());
        try {
            sipStack = sipFactory.createSipStack(properties);
        } catch (PeerUnavailableException e) {
            _logger.debug(e, e);
            throw new SipException("Stack failed to initialize", e);
        }

        try {
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();
            messageFactory = sipFactory.createMessageFactory();
        } catch (SipException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create SIP factories", e);
        }

        try {
            listeningPoint = sipStack.createListeningPoint(host, port, transport);
            sipProvider = sipStack.createSipProvider(listeningPoint);
        } catch (TransportNotSupportedException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Transport not supported.", e);
        } catch (InvalidArgumentException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Invalid argument.", e);
        } catch (ObjectInUseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create listening point. Object in use.", e);
        }

        try {
            listener = (SipListener) new SipListenerImpl(this);
            sipProvider.addSipListener(listener);
        } catch (TooManyListenersException e) {
            _logger.debug(e, e);
            throw new SipException("Could not add listener. Too many listeners.", e);
        }

        // create my address (for from headers) and the contact address (for
        // contact header)
        try {
            URI uri = addressFactory.createURI(mySipAddress);
            if (uri.isSipURI() == false) {
                _logger.error("Invalid sip uri: " + mySipAddress);
                throw new SipException("Invalid sip uri: " + mySipAddress);
            }
            myAddress = addressFactory.createAddress(uri);

            // create a contact address (for contact header)
            SipURI contactUri = null;
            
            //poor mans STUN
            if (publicHost != null) {
               contactUri = addressFactory.createSipURI(((SipURI) uri).getUser(), this.publicHost);
               URI tmpUri =  addressFactory.createSipURI(((SipURI) uri).getUser(), this.publicHost);
               myAddress = addressFactory.createAddress(tmpUri);
            } else {
               myAddress = addressFactory.createAddress(uri);
               contactUri = addressFactory.createSipURI(((SipURI) uri).getUser(), this.host);
               // SipURI contactUrl = addressFactory.createSipURI(from.getName(),host);
            }
            contactUri.setPort(listeningPoint.getPort());
            contactUri.setTransportParam(transport);
            contactAddress = addressFactory.createAddress(contactUri);
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not create contact URI.", e);
        }
    }

    public void dispose() throws ObjectInUseException {
        sipStack.deleteListeningPoint(sipProvider.getListeningPoints()[0]);
        sipProvider.removeSipListener(listener);
        sipStack.deleteSipProvider(sipProvider);
    }


    
    public void sendInfoRequest(SipSession session, String contentType, String contentSubType, String content)  throws SipException {
        
        //get the dialog object from the session
        Dialog d = session.getSipDialog();
        
        //create an info request
        Request infoRequest;
        infoRequest = d.createRequest(Request.INFO);
        

        try { 
            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader;
            contentTypeHeader = headerFactory.createContentTypeHeader(contentType,contentSubType);

           // add the message body (sdp)
           infoRequest.setContent(content, contentTypeHeader);
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Parse Exception when trying to add content to info message.", e);
        }
        
        ClientTransaction ct = sipProvider.getNewClientTransaction(infoRequest);
        SipAgent.sendRequest(d, ct);
    }

	public SipSession sendInviteWithoutProxy(String to, SdpMessage message, String peerHost, int peerPort)
            throws SipException {
        SipSession session = null;
        try {

            // create >From Header
            FromHeader fromHeader = headerFactory.createFromHeader(myAddress, getGUID());

            URI toUri = null;
            Address toAddress = null;


            URI tmpUri = addressFactory.createURI(to);
            //poor mans STUN
            if (publicHost != null) {
                toUri = addressFactory.createSipURI(((SipURI) tmpUri).getUser(), this.publicHost);
            } else {
                toUri = tmpUri;
            }
            
            if (toUri.isSipURI() == false) {
                _logger.error("Invalid sip uri: " + mySipAddress);
                throw new SipException("Invalid sip uri: " + mySipAddress);
            }
            toAddress = addressFactory.createAddress(toUri);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

            // create Request URI
            //poor mans STUN
            String peerHostPort = null;
            if (publicHost != null) {
                peerHostPort = publicHost + ":" + sipProvider.getListeningPoint(transport).getPort();
            } else {
                peerHostPort = host + ":" + sipProvider.getListeningPoint(transport).getPort();
            }

            SipURI requestURI = addressFactory.createSipURI(((SipURI) toUri).getUser(), peerHostPort);

            // Create ViaHeaders

            ArrayList viaHeaders = new ArrayList();
            //poor mans STUN
            String useHost = host;
            if (publicHost != null) {
                useHost = publicHost;
            }
            ViaHeader viaHeader = headerFactory.createViaHeader(useHost, sipProvider.getListeningPoint(
                    transport).getPort(), transport, null);

            // add via headers
            viaHeaders.add(viaHeader);

            // Create a new CallId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create a new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

            // Create a new MaxForwardsHeader
            MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

            // Create the request.
            Request request = messageFactory.createRequest(requestURI, Request.INVITE, callIdHeader,
                    cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwards);

            // Add the contact address.
            ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // create and add the Route Header
            // Dont use the Outbound Proxy. Use Lr instead.
            SipURI sipuri = addressFactory.createSipURI(null, peerHost);
            sipuri.setPort(peerPort);
            sipuri.setLrParam();
            sipuri.setTransportParam(transport);
            RouteHeader routeHeader = headerFactory.createRouteHeader(addressFactory.createAddress(sipuri));
            request.setHeader(routeHeader);

            // Create ContentTypeHeader
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");

            // add the message body (sdp)
            request.setContent(message.getSessionDescription().toString(), contentTypeHeader);

            // Header callInfoHeader = headerFactory.createHeader(
            // "Call-Info", "<http://www.antd.nist.gov>");
            // request.addHeader(callInfoHeader);

            // Create the client transaction.
            ClientTransaction ctx = sipProvider.getNewClientTransaction(request);

            _logger.debug("Just before Send request: "+peerHost+":"+peerPort);
            

            if (_logger.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder(); 
                sb.append("------------- SENDING A SIP REQUEST ---------------");
                sb.append("\nSending a "+ ctx.getRequest().getMethod()+" SIP Request");
                Iterator headers = ctx.getRequest().getHeaderNames();
                while (headers.hasNext()) {
                	sb.append("\n");
                	sb.append(ctx.getRequest().getHeader((String) headers.next()).toString());
                }
                byte[] contentBytes = ctx.getRequest().getRawContent();
                if (contentBytes == null) {
                	sb.append("\nNo content in the request.");
                } else {
                   String contentString = new String(contentBytes);
                   sb.append("\n");
                   sb.append(contentString);
                } 
                _logger.debug(sb);
             } 
            
            // send the request out.
            ctx.sendRequest();

            Dialog dialog = ctx.getDialog();
            
            session = SipSession.createSipSession(this, ctx, dialog, null,null,null,null);
            session.setCtx(ctx);
            session.setState(SipSession.SessionState.waitingForInviteResponse);
            SipSession.addPendingSession(session);
            session.setSdpMessage(message);            

        } catch (TransactionUnavailableException e) {
            _logger.debug(e, e);
            throw e;
        } catch (SipException e) {
            _logger.debug(e, e);
            throw e;
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send invite due to a parse error in SIP stack.", e);
        } catch (InvalidArgumentException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send invite due to invalid argument in SIP stack.", e);
        }

        return session;
    }
    
    public void sendBye(SipSession session)  throws SipException {
        Dialog d = session.getSipDialog();
        Request byeRequest;
        byeRequest = d.createRequest(Request.BYE);
        ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
        SipAgent.sendRequest(d, ct);

    }
    
    public void sendreInvite(SipSession session, String rtpHost, int rtpPort)  throws SipException {
        Dialog d = session.getSipDialog();      
        
        Request inviteRequest = session.getCtx().getRequest(); 
        
        Request reinvite = d.createRequest(Request.INVITE); 
        SdpMessage sdpMessage = session.getSdpMessage();
        try {
         
        	MaxForwardsHeader mf = headerFactory.createMaxForwardsHeader(10);
        	reinvite.setHeader(mf);  
        	reinvite.setHeader(inviteRequest.getHeader("Contact"));   
        	            
	        sdpMessage.setSessionAddress(rtpHost);	       
	        
	        List <MediaDescription> rtpChans = sdpMessage.getRtpChannels();
	        if (!rtpChans.isEmpty()) {
	        	//get the mrcp receiver (recognition) channel in from the sdp message
	            MediaDescription controlChan = rtpChans.get(0);
	            controlChan.getMedia().setMediaPort(rtpPort);	           
	            controlChan.getConnection().setAddress(rtpHost);
	            //controlChan.setAttribute(SdpMessage.SDP_CONNECTION_ATTR_NAME, SdpMessage.SDP_EXISTING_CONNECTION);
	        }
	        
	        for (MediaDescription md : sdpMessage.getMrcpChannels()) {
	            md.setAttribute(SdpMessage.SDP_CONNECTION_ATTR_NAME, SdpMessage.SDP_EXISTING_CONNECTION);
	        }        
	        
	        // Create ContentTypeHeader
	        ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("application", "sdp");
	        // add the message body (sdp)
	        reinvite.setContent(sdpMessage.getSessionDescription().toString(), contentTypeHeader);
	        
	        ClientTransaction ct = sipProvider.getNewClientTransaction(reinvite);
	               
	        SipAgent.sendRequest(d, ct);
	        
	        session.setCtx(ct);	       	        
	        session.setState(SipSession.SessionState.waitingForInviteResponse);
            SipSession.addPendingSession(session);
            session.setSdpMessage(sdpMessage);            
            
        } catch (SipException e) {
            _logger.debug(e, e);
            throw e;
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send reinvite due to SdpException in SIP stack.", e);
        } catch (ParseException e) {
            _logger.debug(e, e);
            throw new SipException("Could not send reinvite due to a parse error in SIP stack.", e);        
	    } catch (InvalidArgumentException e) {
	        _logger.error(e, e);
	    }
	    
    }

    public String getGUID() {
        // counter++;
        // return guidPrefix+counter;
        int r = random.nextInt();
        r = (r < 0) ? 0 - r : r; // generate a positive number
        return Integer.toString(r);
    }

    /**
     * @return the sipAddress
     */
    public String getSipAddress() {
        return sipAddress;
    }

    /**
     * @param sipAddress
     *            the sipAddress to set
     */
    public void setSipAddress(String sipAddress) {
        this.sipAddress = sipAddress;
    }

    /**
     * @return the sessionListener
     */
    public SessionListener getSessionListener() {
        return sessionListener;
    }

    /**
     * @param sessionListener
     *            the sessionListener to set
     */
    public void setSessionListener(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    /**
     * @return the myAddress
     */
    public Address getMyAddress() {
        return myAddress;
    }

    /**
     * @param myAddress
     *            the myAddress to set
     */
    public void setMyAddress(Address myAddress) {
        this.myAddress = myAddress;
    }
    
    public void sendResponse(SipSession session, SdpMessage sdpResponse) {

        // send the ok (assuming that the offer is accepted with the response in the sdpMessaage)
        //TODO what if the offer is not accepted?  Do all non-ok response come thru the exception path?
        Response okResponse = null;
        try {
            okResponse = getMessageFactory().createResponse(Response.OK, session.getRequest().getRequest());
        } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Create a application/sdp ContentTypeHeader
        ContentTypeHeader contentTypeHeader = null;
        try {
            contentTypeHeader = getHeaderFactory().createContentTypeHeader("application", "sdp");
        } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // add the sdp response to the message
        try {
            okResponse.setContent(sdpResponse.getSessionDescription().toString(), contentTypeHeader);
        } catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
        //toHeader.setTag(guid);
        
        ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
        okResponse.addHeader(contactHeader);
        
        // Now if there were no exceptions, we were able to process the invite
        // request and we have a valid reponse to send back
        // if there is an exception here, not much that can be done.
        try {
            SipAgent.sendResponse(session.getStx(), okResponse);
        } catch (SipException e) {
            _logger.error(e, e);
        } catch (InvalidArgumentException e) {
            _logger.error(e, e);
        }
        
    }
    
	public static void sendResponse(ServerTransaction serverTransaction, Response response) throws SipException, InvalidArgumentException {
        if (_logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder(); 
            sb.append("------------- SENDING A SIP RESPONSE ---------------");
            sb.append("\nSending a SIP Response.  Status: "+response.getStatusCode()+", "+response.getReasonPhrase());
            Iterator headers = response.getHeaderNames();
            while (headers.hasNext()) {
            	sb.append("\n");
            	sb.append(response.getHeader((String) headers.next()).toString());
            }
            byte[] contentBytes = response.getRawContent();
            if (contentBytes == null) {
            	sb.append("\nNo content in the response.");
            } else {
            	sb.append("\n");
                String contentString = new String(contentBytes);
                sb.append(contentString);
            } 
            _logger.debug(sb);
         }
        serverTransaction.sendResponse(response);
    }

	public static void sendRequest(Dialog dialog, ClientTransaction ctx) throws TransactionDoesNotExistException, SipException {
        if (_logger.isDebugEnabled()) {
        	StringBuilder sb = new StringBuilder(); 
        	sb.append("------------- SENDING A SIP REQUEST ---------------");
        	sb.append("\nSending a "+ ctx.getRequest().getMethod()+" SIP Request");
            Iterator headers = ctx.getRequest().getHeaderNames();
            while (headers.hasNext()) {
            	sb.append("\n");
            	sb.append(ctx.getRequest().getHeader((String) headers.next()).toString());
            }
            byte[] contentBytes = ctx.getRequest().getRawContent();
            if (contentBytes == null) {
            	sb.append("\nNo content in the request.");
            } else {
               String contentString = new String(contentBytes);
           	   sb.append("\n");
               sb.append(contentString);
            } 
            _logger.debug(sb);
         } 
        dialog.sendRequest(ctx);
    }

	public static InetAddress getLocalHost() throws SocketException, UnknownHostException {
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			for (Enumeration en2 = networkInterface.getInetAddresses(); en2.hasMoreElements();) {
	            InetAddress addr = (InetAddress) en2.nextElement();
	            if (!addr.isLoopbackAddress()) {
	                if (addr instanceof Inet4Address) {
	                    return addr;
	                }
	            }
			}

		}
		return InetAddress.getLocalHost();
	}
	
}

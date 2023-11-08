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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.sip.ResourceUnavailableException;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SessionListener;
import org.speechforge.cairo.sip.SipAgent;
import org.speechforge.cairo.sip.SipSession;

/**
 * SessionManager encapsulates the sip agent for the speech client.  It implements SessionListner, so it
 * is notified with Significant SIP events.  It also provides methods to initiate and terminate sessions
 * (i.e. invite and bye).
 * 
 * Before using this agent, the following properties need to be set either
 * by calling setters and then startup()(perhaps with Spring)
 * OR use the 4 parameter constructor and the setCairoServer method
 * mySipAddress     - the sip address of the agent
 * stackName        - a name for the agents sip stack
 * port             - the port used by this sip agent
 * transport        - transport too use (udp or tcp)
 * cairoSipHostName - the host name of the speech servers sip agent (RM in cairo)
 * cairoSipPort     - the port used by the speech servers sip agent
 * cairoSipAddress  - the sip address of the speech server
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SessionManager  {
        /** Logger instance. */
        private static final Logger LOGGER = LogManager.getLogger(SessionManager.class);
        
        // properties need to be set either
        //   - call setters and then the no arg constructor then startup()(perhaps with SPring) OR
        //   - use the 4 parameter constructor and the setDaultCairoServer method
        private String mySipAddress;
        private String stackName;
        private int port;
        private String transport;
        private String cairoSipHostName;
        private int cairoSipPort;
        private String cairoSipAddress;
        
        private SipAgent _sipAgent;
        private SessionListener _listener;

        // Need this to construct the MRCP Channels
        String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
        MrcpFactory factory = MrcpFactory.newInstance();
        MrcpProvider ttsProvider = factory.createProvider();
        MrcpProvider recogProvider = factory.createProvider();
        
        /**
         * Instantiates a new session manager.
         */
        public SessionManager() {
            super();
        }

        /**
         * Instantiates a new session manager. You must also call setCairoServer before trying to set up a session.
         * 
         * @param mySipAddress the my sip address
         * @param stackName the stack name
         * @param port the port
         * @param transport the transport
         * 
         * @throws SipException the sip exception
         */
        public SessionManager(String mySipAddress, String stackName, int port, String transport) throws SipException {

            this.mySipAddress = mySipAddress;
            this.stackName = stackName;
            this.port = port;
            this.transport = transport;
            
            //create the listener for sip events
            _listener = new SipSessionListener();
            
            // Construct a SIP agent to be used to send a SIP Invitation to the cairo server
            _sipAgent = new SipAgent(_listener, mySipAddress, stackName, port, transport);
        }
        
        
        /**
         * Startup.  Useful for IoC frameworks.  Use all the setter methods needed to configure the object then call this method to start.
         * 
         * @throws SipException the sip exception
         */
        public void startup() throws SipException {
        	
            //create the listener for sip events
            _listener = new SipSessionListener();          
            _sipAgent = new SipAgent(_listener, mySipAddress, stackName, port, transport);
        }
        
        
        /**
         * Shutdown.
         * 
         * @throws ObjectInUseException the object in use exception
         */
        public void shutdown() throws ObjectInUseException {
            _sipAgent.dispose();
        }
        
        
        /**
         * Sets the mrcp server.
         * 
         * @param cairoSipAddress the cairo sip address
         * @param cairoSipHostName the cairo sip host name
         * @param cairoSipPort the cairo sip port
         */
        public void setCairoServer(String cairoSipAddress, String cairoSipHostName, int cairoSipPort) {
            this.cairoSipAddress = cairoSipAddress;
            this.cairoSipHostName = cairoSipHostName;
            this.cairoSipPort = cairoSipPort;   
        }

        /**
         * New recog channel.  Use this method to create a session with just a recognition mrcpv2 channel.
         * 
         * @param clientRtpPort the client RTP port
         * @param clientHost the client host
         * @param sessionName the session name
         * 
         * @return the sip session
         * 
         * @throws SdpException the sdp exception
         * @throws SipException the sip exception
         */
        public SipSession newRecogChannel(int clientRtpPort, String clientHost, String sessionName) throws SdpException, SipException {

        	//TODO: Really dont need the client side RTP port in this case. Should be able to have no rtp media channel in the sip invite and have 
        	// the server come back with the rtp media line with the port it is listeneing on.  Essesently this is a workaround due to a deficciency in
        	// the cairo server.
        	
            Vector<Integer> formats = new Vector<Integer>();
            formats.add(SdpConstants.PCMU);
        	SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, clientHost, sessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(clientRtpPort,formats,clientHost);
            //MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
            Vector<MediaDescription> v = new Vector<MediaDescription>();
            //v.add(synthControlChannel);
            v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);    	
        	SipSession s = sendInviteWithoutProxy(cairoSipAddress, sdpMessage, cairoSipHostName, cairoSipPort);
        	return s;
        }
    
        /**
         * New synth channel.  Use this method to create a session with hust a sythesizer mrcpv2 channel.
         * 
         * @param clientRtpPort the client rtp port
         * @param clientHost the client host
         * @param sessionName the session name
         * 
         * @return the sip session
         * 
         * @throws SdpException the sdp exception
         * @throws SipException the sip exception
         */
        public SipSession newSynthChannel(int clientRtpPort, String clientHost, String sessionName) throws SdpException, SipException {
            Vector<Integer> format = new Vector<Integer>();
            format.add(SdpConstants.PCMU);
            SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, clientHost, sessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(clientRtpPort,format,clientHost);
            MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            //MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
            Vector<MediaDescription> v = new Vector<MediaDescription>();
            v.add(synthControlChannel);
            //v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);    	
        	SipSession s = sendInviteWithoutProxy(cairoSipAddress, sdpMessage, cairoSipHostName, cairoSipPort);
        	return s;
        }
        
        /**
         * New receiver transmitter channel.  Use this method to create a session with both a receiver and transmitter mrcpv2 channel.
         * 
         * @param clientRtpPort the client rtp port
         * @param clientHost the client host
         * @param sessionName the session name
         * 
         * @return the sip session
         * 
         * @throws SdpException the sdp exception
         * @throws SipException the sip exception
         */
        public SipSession newRecogAndSynthChannels(int clientRtpPort, String clientHost, String sessionName) throws SdpException, SipException {
            final long startTime = System.nanoTime();
            Vector<Integer> format = new Vector<Integer>();
            format.add(SdpConstants.PCMU);
        	SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, clientHost, sessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(clientRtpPort,format,clientHost);
            //rtpChannel.getMedia().setMediaFormats(formats);
            MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
            Vector<MediaDescription> v = new Vector<MediaDescription>();
            v.add(synthControlChannel);
            v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);  
            LOGGER.info("before snd invite  " +System.currentTimeMillis());
            SipSession s = sendInviteWithoutProxy(cairoSipAddress, sdpMessage, cairoSipHostName, cairoSipPort);
        
            long endTime = System.nanoTime();
            long duration = (endTime - startTime)/1000000;
            LOGGER.info("sipInvite time: "+duration+" ms  (" +System.currentTimeMillis()+")");

            return s;
        }
        
        
        public SipSession newRecorderChannel(int clientRtpPort, String clientHost, String sessionName) throws SdpException, SipException {

        	//TODO: Really dont need the client side RTP port in this case. Should be able to have no rtp media channel in the sip invite and have 
        	// the server come back with the rtp media line with the port it is listeneing on.  Essesently this is a workaround due to a deficciency in
        	// the cairo server.
        	
            Vector<Integer> formats = new Vector<Integer>();
            formats.add(SdpConstants.PCMU);
        	SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, clientHost, sessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(clientRtpPort,formats,clientHost);
            //MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.RECORDER);
            Vector<MediaDescription> v = new Vector<MediaDescription>();
            //v.add(synthControlChannel);
            v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);    	
            SipSession s = sendInviteWithoutProxy(cairoSipAddress, sdpMessage, cairoSipHostName, cairoSipPort);
            return s;
        }
    
        
        public SipSession newRecogAndSynthAndRecorderChannels(int clientRtpPort, String clientHost, String sessionName) throws SdpException, SipException {
            Vector<Integer> format = new Vector<Integer>();
            format.add(SdpConstants.PCMU);
        	SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, clientHost, sessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(clientRtpPort,format,clientHost);
            //rtpChannel.getMedia().setMediaFormats(formats);
            MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
            MediaDescription recorderControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.RECORDER);
            Vector<MediaDescription> v = new Vector<MediaDescription>();
            v.add(synthControlChannel);
            v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);    	
        	SipSession s = sendInviteWithoutProxy(cairoSipAddress, sdpMessage, cairoSipHostName, cairoSipPort);
        	return s;
        }
        
        
        /**
         * Send invite without proxy.  This is an internal method to send an invite sip request with a sdp payload.
         * 
         * @param to the to
         * @param message the message
         * @param peerAddress the peer address
         * @param peerPort the peer port
         * 
         * @return the sip session
         * 
         * @throws SipException the sip exception
         */
        private synchronized SipSession sendInviteWithoutProxy(String to, SdpMessage message, String peerAddress, int peerPort) throws SipException {

            // Send the sip invitation
            SipSession session = _sipAgent.sendInviteWithoutProxy(to, message, peerAddress, peerPort);         

            while (session.getState() == SipSession.SessionState.waitingForInviteResponse) {
 	       LOGGER.info("in loop not done...");
               synchronized (session) {        
    	       try {
    	           session.wait(); 
               } catch (InterruptedException e) {
                   LOGGER.debug("Interupt Exception while blocked in sip invite method.");
               }
               }
	    }
            return session;
        }

        /**
         * @return the cairoSipAddress
         */
        public String getCairoSipAddress() {
            return cairoSipAddress;
        }

        /**
         * @param cairoSipAddress the cairoSipAddress to set
         */
        public void setCairoSipAddress(String cairoSipAddress) {
            this.cairoSipAddress = cairoSipAddress;
        }

        /**
         * @return the cairoSipHostName
         */
        public String getCairoSipHostName() {
            return cairoSipHostName;
        }

        /**
         * @param cairoSipHostName the cairoSipHostName to set
         */
        public void setCairoSipHostName(String cairoSipHostName) {
            this.cairoSipHostName = cairoSipHostName;
        }

        /**
         * @return the cairoSipPort
         */
        public int getCairoSipPort() {
            return cairoSipPort;
        }

        /**
         * @param cairoSipPort the cairoSipPort to set
         */
        public void setCairoSipPort(int cairoSipPort) {
            this.cairoSipPort = cairoSipPort;
        }

        /**
         * @return the mySipAddress
         */
        public String getMySipAddress() {
            return mySipAddress;
        }

        /**
         * @param mySipAddress the mySipAddress to set
         */
        public void setMySipAddress(String mySipAddress) {
            this.mySipAddress = mySipAddress;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * @param port the port to set
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * @return the stackName
         */
        public String getStackName() {
            return stackName;
        }

        /**
         * @param stackName the stackName to set
         */
        public void setStackName(String stackName) {
            this.stackName = stackName;
        }

        /**
         * @return the transport
         */
        public String getTransport() {
            return transport;
        }

        /**
         * @param transport the transport to set
         */
        public void setTransport(String transport) {
            this.transport = transport;
        }
        
        /**
         * The listener interface for receiving sipSession events.
         * The class that is interested in processing a sipSession
         * event implements this interface, and the object created
         * with that class is registered with a component using the
         * component's <code>addSipSessionListener<code> method. When
         * the sipSession event occurs, that object's appropriate
         * method is invoked.
         * 
         * @see SipSessionEvent
         */
        private class SipSessionListener implements SessionListener {
        	

            /* (non-Javadoc)
             * @see org.speechforge.cairo.sip.SessionListener#processInfoRequest(org.speechforge.cairo.sip.SipSession, java.lang.String, java.lang.String, java.lang.String)
             */
            public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content) {

            	
                LOGGER.debug("SIP INFO request: "+contentType+"/"+contentSubType+"\n"+content);
               
                String code = null;
                int duration = 0 ;
                
                //if dtmf signal     
                if ((contentType.trim().equals("application")) &&(contentSubType.trim().equals("dtmf-relay"))) {

                    //Handle the client side dtmf signaling
                    if (content == null) {
                        LOGGER.warn("sip info request with a dtmf-relay content type with no content.");
                    } else {

                        String lines[] = content.toString().split("\n");
                        for (int i=0; i<lines.length;i++) {
                            String parse[] = lines[i].toString().split("=");
                            if (parse[0].equals("Signal")) {
                                code = parse[1];
                            }
                            if (parse[0].equals("Duration")) {
                                duration = Integer.parseInt(parse[1].trim());
                            }
                        }
                        LOGGER.debug("The DTMF code : "+code);
                        LOGGER.debug("The duration: "+ duration);

                        //TODO:  Pass it along to whoever is interested
                    	//It is unlikely that a dtmf info message will arrive at the client, and if it did, it is not clear what to do with it.
                        
                    }

                } else {
                    LOGGER.warn("Unhandled SIP INFO request content type: "+contentType+"/"+contentSubType+"\n"+content);
                }
            }
       

            /* (non-Javadoc)
             * @see org.speechforge.cairo.sip.SessionListener#processInviteResponse(boolean, org.speechforge.cairo.sip.SdpMessage, org.speechforge.cairo.sip.SipSession)
             */
            @SuppressWarnings("unchecked")
            public synchronized SdpMessage processInviteResponse(boolean ok, SdpMessage response, SipSession session) {
                LOGGER.debug("Got an invite response, ok is: "+ok + " "+System.currentTimeMillis());
                
                SdpMessage pbxResponse = null;
                if (ok) {


                        // Get the MRCP media channels (need the port number and the channelID that are sent
                        // back from the server in the response in order to setup the MRCP channel)
                        String remoteHostName = null;
                        InetAddress remoteHostAdress = null;

                        try {
                        	

                        	
                        	// Get the host info from the sdp response (common to all channels)
                            remoteHostName = response.getSessionDescription().getConnection().getAddress();
                            remoteHostAdress = InetAddress.getByName(remoteHostName);

                            // Get the transmitter channel (if it is in the repsonse.  It is posssible that none was asked for) 
                            List <MediaDescription> xmitterChans = response.getMrcpTransmitterChannels();
                            if (!xmitterChans.isEmpty()) {
                            	//get the mrcp transmitter (tts) channel in from the sdp message
                            	int xmitterPort = xmitterChans.get(0).getMedia().getMediaPort();
                            	String xmitterChannelId = xmitterChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

                            	//create the transmitter channel and add it to the session
                                MrcpChannel ttsChannel = ttsProvider.createChannel(xmitterChannelId, remoteHostAdress, xmitterPort, protocol);
                                session.setTtsChannel(ttsChannel);
                            }
                            
                            // Get the receiver channel (if it is in the repsonse.  It is posssible that none was asked for) 
                            List <MediaDescription> receiverChans = response.getMrcpReceiverChannels();
                            if (!receiverChans.isEmpty()) {
                            	//get the mrcp receiver (recognition) channel in from the sdp message
	                            MediaDescription controlChan = receiverChans.get(0);
	                            int receiverPort = controlChan.getMedia().getMediaPort();
	                            String receiverChannelId = receiverChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

	                            //create the recognition channel and add it to the session
	                            MrcpChannel recogChannel = recogProvider.createChannel(receiverChannelId, remoteHostAdress, receiverPort, protocol);
	                            session.setRecogChannel(recogChannel);
	                            
	                            // get the rtp channel info, that is needed for the receiver channel.  The clienet will need it for setting up its stream
	                            // that will go to the recognizer.
	                            List <MediaDescription> rtpChans = response.getAudioChansForThisControlChan(controlChan);
	                            int remoteRtpPort = -1;	                            
	                            Vector<String> supportedFormats = null;
	                            if (rtpChans.size() > 0) {
	                                //TODO: What if there is more than 1 media channels?
	                                //TODO: check if there is an override for the host attribute in the m block
	                                //InetAddress remoteHost = InetAddress.getByName(rtpmd.get(1).getAttribute();
	                                remoteRtpPort =  rtpChans.get(0).getMedia().getMediaPort();	                                
	                                
	                                LOGGER.debug("processInviteResponse-remoteRtpHost: " + remoteHostName + "remoteRtpPort:" + remoteRtpPort);
	                                //rtpmd.get(1).getMedia().setMediaPort(localPort);
	                                supportedFormats = rtpChans.get(0).getMedia().getMediaFormats(true);    
	                            } else {
	                                LOGGER.warn("No Media channel specified in the invite request");
	                                //TODO:  handle no media channel in the response corresponding tp the mrcp channel (sip/sdp error)
	                            } 
	                            session.setRemoteRtpPort(remoteRtpPort);
	                            session.setRemoteRtpHost(remoteHostName);
	                            
                            }
                            
  

                        } catch (UnknownHostException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Unknown host Excepton while sending sip invite", e);
                        } catch (SdpParseException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Sdp Parse Excepton while sending sip invite", e);
                        } catch (IllegalArgumentException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Illegal Argument Excepton while sending sip invite", e);
                        } catch (IllegalValueException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Illeagal Value Excepton while sending sip invite", e);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Io Excepton while sending sip invite", e);
                        } catch (SdpException e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Sdp Excepton while sending sip invite", e);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        	LOGGER.warn("Excepton while sending sip invite", e);
                        }
                 
                } else {
                    LOGGER.info("Invite Response not ok");
                }

                return pbxResponse;
            }
                            

            /* (non-Javadoc)
             * @see org.speechforge.cairo.sip.SessionListener#processTimeout(javax.sip.TimeoutEvent)
             */
            public synchronized void processTimeout(TimeoutEvent event) {
               LOGGER.debug("Timeout occurred");   
               this.notify();
            }
       

            /* (non-Javadoc)
             * @see org.speechforge.cairo.sip.SessionListener#processByeRequest(org.speechforge.cairo.sip.SipSession)
             */
            public void processByeRequest(SipSession session) throws RemoteException, InterruptedException {
                LOGGER.info("Got a bye request.  Not implemented.  Discarding.");
            }

            /* (non-Javadoc)
             * @see org.speechforge.cairo.sip.SessionListener#processInviteRequest(org.speechforge.cairo.sip.SdpMessage, org.speechforge.cairo.sip.SipSession)
             */
            public SdpMessage processInviteRequest(SdpMessage request, SipSession session) throws SdpException, ResourceUnavailableException, RemoteException {   
                LOGGER.info("Got an invite request.  Not implememted.  Discarding.");          	
                return null;
            }
        }
        
}
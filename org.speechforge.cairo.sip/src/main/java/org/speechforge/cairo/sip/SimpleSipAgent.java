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

import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;
import org.apache.log4j.Logger;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SessionListener;
import org.speechforge.cairo.sip.SipAgent;
import org.speechforge.cairo.sip.SipSession;

public class SimpleSipAgent implements SessionListener {
        private static Logger _logger = Logger.getLogger(SimpleSipAgent.class);
        private String _mySipAddress;
        private String _stackName;
        private int _port;
        private String _localHost;
        private String _publicLocalHost;
        private String _transport;
        private SipAgent _sipAgent;
        
        private SdpMessage sipResponse;
        private SipSession sipSession;
        
        public SimpleSipAgent(String mySipAddress, String stackName, String host, String publicLocalHost, int port, String transport) throws SipException {
            this(mySipAddress, stackName, port, transport);
            _localHost = host;
            _publicLocalHost = publicLocalHost;
        }

        public SimpleSipAgent(String mySipAddress, String stackName, int port, String transport) throws SipException {

            _mySipAddress = mySipAddress;
            _stackName = stackName;
            _port = port;
            _transport = transport;
            _localHost = null;
            _publicLocalHost = null;
        }

        
        public SdpMessage sendInviteWithoutProxy(String to, SdpMessage message, String peerAddress, int peerPort) throws SipException {

            // Construct a SIP agent to be used to send a SIP Invitation to the cairo server
            if (_sipAgent == null) {
               _sipAgent = new SipAgent(this, _mySipAddress, _stackName, _localHost, _publicLocalHost, _port, _transport);
            }
            
            // Send the sip invitation
            SipSession session = _sipAgent.sendInviteWithoutProxy(to, message, peerAddress, peerPort);

            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return sipResponse;
        }
        
        public void dispose() throws ObjectInUseException {
            _sipAgent.dispose();
        }
        

	public void processByeRequest(SipSession session) {
		// TODO Auto-generated method stub
	}


	public SdpMessage processInviteRequest(SdpMessage request,
			SipSession session) {
		// TODO Auto-generated method stub
		return null;
	}

	public synchronized SdpMessage processInviteResponse(boolean ok, SdpMessage response,SipSession session) {
            if (ok) {
		sipResponse = response;
                sipSession = session;
            } else {
                sipResponse = null;  
            }
            this.notify();
            return null;
	}

        public synchronized void processTimeout(TimeoutEvent event) {
            sipResponse = null;
            this.notify();
        }
        
        public void sendBye() throws SipException {
            if(sipSession != null) {
               _sipAgent.sendBye(sipSession);
               _logger.info("Sent a SIP BYE.");
            } else {
                _logger.info("Could not send SIP Bye.  There is no session yet.");
            }
        }

        public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content) {
            // TODO Auto-generated method stub
            
        }

        /**
         * @return the sipSession
         */
        public SipSession getSipSession() {
            return sipSession;
        }

        /**
         * @param sipSession the sipSession to set
         */
        public void setSipSession(SipSession sipSession) {
            this.sipSession = sipSession;
        }

}

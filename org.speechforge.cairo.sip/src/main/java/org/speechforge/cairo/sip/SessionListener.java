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

import javax.sdp.SdpException;
import javax.sip.TimeoutEvent;

/**
 * The listener interface that must be implememted to receive application level
 * session signaling events.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SessionListener {

    public SdpMessage processInviteRequest(SdpMessage request, SipSession session) throws SdpException,
            ResourceUnavailableException, RemoteException;

    public SdpMessage processInviteResponse(boolean ok, SdpMessage response, SipSession session);

    public void processByeRequest(SipSession session) throws RemoteException, InterruptedException;
    
    public void processTimeout(TimeoutEvent event);
    
    public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content);

}

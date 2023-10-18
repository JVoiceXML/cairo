package org.speechforge.cairo.sip;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * SIP resource.
 * @author Dirk Schnelle-Walka
 *
 */
public interface SipResource extends Remote {
    public void bye(String sessionId) throws  RemoteException, InterruptedException;
    public SdpMessage invite(SdpMessage request, String sessionId) throws ResourceUnavailableException, RemoteException;
}

package org.speechforge.cairo.sip;

import java.rmi.RemoteException;

public interface SipResource {
    public void bye(String sessionId) throws  RemoteException, InterruptedException;
    public SdpMessage invite(SdpMessage request, String sessionId) throws ResourceUnavailableException, RemoteException;
}

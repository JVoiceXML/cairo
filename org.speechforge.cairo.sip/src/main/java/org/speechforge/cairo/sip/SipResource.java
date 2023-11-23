package org.speechforge.cairo.sip;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * SIP resource.
 * @author Dirk Schnelle-Walka
 */
public interface SipResource extends Remote {
    /**
     * Process a SIP {@code INVITE} request.
     * @param request the request
     * @param sessionId the session id
     * @return response to the INVITE
     * @throws ResourceUnavailableException
     *          if needed resources could not be created
     * @throws RemoteException
     *          error interacting with the remote object
     */
    public SdpMessage invite(SdpMessage request, String sessionId) 
            throws ResourceUnavailableException, RemoteException;

    /**
     * Process a SIP {@code BYE} request.
     * @param sessionId the session id
     * @throws RemoteException
     *          error interacting with the remote object
     * @throws InterruptedException
     *          execution was interrupted
     */
    public void bye(String sessionId) 
            throws  RemoteException, InterruptedException;
}

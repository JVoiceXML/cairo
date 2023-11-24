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
package org.speechforge.cairo.server.resource;


import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.mrcp4j.MrcpResourceType;
import org.speechforge.cairo.sip.ResourceUnavailableException;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SessionListener;
import org.speechforge.cairo.sip.SipAgent;
import org.speechforge.cairo.sip.SipResource;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Implements a ResourceServer that can be utilized by MRCPv2
 * clients for establishing and managing connections to MRCPv2 resource implementations.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class ResourceServerImpl implements SessionListener {

    public static final String NAME = "ResourceServer";

    private static Logger LOGGER = LogManager.getLogger(ResourceServerImpl.class);


    private static final String SIPPORT_OPTION = "sipPort";
    private static final String SIPTRANSPORT_OPTION = "sipTransport";
    private static final String SIPPUBLICADDRESS_OPTION = "publicAddress";
    private static final String LOCALADDRESS_OPTION = "publicAddress";

  
    private long _channelID = System.currentTimeMillis();

    private ResourceRegistry registryImpl;

    private SipAgent _ua;

    private String cairoSipAddress = "sip:cairo@speechforge.org";

    private String hostIpAddress;

    /**
     * Constructs a new object
     * 
     * @param registry the registry to use 
     * @param sipPort the port to use
     * @param sipTransport the transport to use, e.g. UDP
     * @param hostIpAddress the address of the host
     * @param publicAddress the public address
     * @throws RemoteException error exporting the object to the registry
     * @throws SipException error creating the SIP agent
     */
    public ResourceServerImpl(ResourceRegistry registry, int sipPort, 
    		String sipTransport, String hostIpAddress, String publicAddress) throws RemoteException, SipException {
        super();
        this.hostIpAddress = hostIpAddress;
        if( hostIpAddress == null ) {
            try {
                InetAddress addr = CairoUtil.getLocalHost();
                this.hostIpAddress = addr.getHostAddress();
                //host = addr.getCanonicalHostName();
            } catch (UnknownHostException | SocketException e) {
                this.hostIpAddress = "127.0.0.1";
                LOGGER.warn("unable to determine IP address. using fedault " + this.hostIpAddress, e);
            }
        }
        if (sipPort == 0) {
            sipPort = 5050;
        }
        if (sipTransport == null) {
            sipTransport = "tcp";
        }
        cairoSipAddress = "sip:cairo@" + this.hostIpAddress;
        _ua = new SipAgent(this, cairoSipAddress, "Cairo SIP Stack", this.hostIpAddress, publicAddress, sipPort, sipTransport);

        registryImpl = registry;
    }

    /**
     * Constructs a new object
     * 
     * @param port the port to use
     * @param registry the registry to use
     * @throws RemoteException error exporting the object to the registry
     */
    public ResourceServerImpl(int port, ResourceRegistryImpl registry) throws RemoteException {
        registryImpl = registry;
    }

    private synchronized String getNextChannelID() { // TODO: convert from synchronized to atomic
        return Long.toHexString(_channelID++);
    }

    /**
     * Invite.
     * 
     * @param request
     *            the invite request
     * 
     * @return the invite response
     * 
     * @throws ResourceUnavailableException
     *             the resource unavailable exception
     * @throws RemoteException
     *             the remote exception
     * @throws SdpException
     *             the sdp exception
     */
    private SdpMessage invite(SdpMessage request, SipSession session) throws ResourceUnavailableException, RemoteException,
            SdpException {

        // determine if there receivers and/or transmitter channel requests in the invite
        // and preprocess the message so that it can be sent back as a response to the inviter
        // (i.e. set the channel and setup attributes).
        boolean receiver;
        boolean transmitter;
        try {
            receiver = handleRecognizerChannelRequests(request) 
                    || handleRecordererChannelRequests(request);
            transmitter = hanldeTransmitterChannelRequests(request);
        } catch (SdpException e) {
            LOGGER.warn(e.getMessage(), e);
            throw e;
        }

        // process the invitation (transmitter and/or receiver)
        if (transmitter) {
            request = processTransmitterInvite(request, session);
        }
        if (receiver) {
            request = processReceiverInvite(request, session);
        } // TODO: catch exception and release transmitter resources

        // post process the message
        // - remove the resource attribute
        // TODO: change the host adresss on a per channel basis (in case the resources are distributed across a network)
        for (MediaDescription md : request.getMrcpChannels()) {
            md.removeAttribute("resource");
        }
        // message.getSessionDescription().getConnection().setAddress(host);

        request.getSessionDescription().getConnection().setAddress(hostIpAddress);
        return request;
    }

    /**
     * Checks if a recognizer is requested because there are channel requests.
     * @param request the SDP request.
     * @return {@code true} if a receiver should be created
     * @throws SdpException
     *          error evaluating the SDP message
     */
    private boolean handleRecognizerChannelRequests(SdpMessage request)
            throws SdpException {
        boolean receiver = false;
        List<MediaDescription> descriptions = request.getMrcpReceiverChannels();
        for (MediaDescription md : descriptions) {
            String channelID = getNextChannelID();
            String chanid = channelID + '@' 
                    + MrcpResourceType.SPEECHRECOG.toString();
            md.setAttribute("channel", chanid);
            md.setAttribute("setup", "passive");
            receiver = true;
        }
        return receiver;
    }

    /**
     * Checks if a recorder is requested because there are channel requests.
     * @param request the SDP request.
     * @return {@code true} if a receiver should be created
     * @throws SdpException
     *          error evaluating the SDP message
     */
    private boolean handleRecordererChannelRequests(SdpMessage request)
            throws SdpException {
        boolean receiver = false;
        List<MediaDescription> descriptions = request.getMrcpRecorderChannels();
        for (MediaDescription md : descriptions) {
            String channelID = getNextChannelID();
            String chanid = channelID + '@' 
                    + MrcpResourceType.RECORDER.toString();
            md.setAttribute("channel", chanid);
            md.setAttribute("setup", "passive");
            receiver = true;
        }
        return receiver;
    }

    /**
     * Checks if a recorder is requested because there are channel requests.
     * @param request the SDP request.
     * @return {@code true} if a transmitter should be created
     * @throws SdpException
     *          error evaluating the SDP message
     */
    private boolean hanldeTransmitterChannelRequests(SdpMessage request)
            throws SdpException {
        boolean transmitter = false;
        List<MediaDescription> descriptions =
                request.getMrcpTransmitterChannels();
        for (MediaDescription md : descriptions) {
            String channelID = getNextChannelID();
            String chanid = channelID + '@' 
                    + MrcpResourceType.SPEECHSYNTH.toString();
            md.setAttribute("channel", chanid);
            md.setAttribute("setup", "passive");
            transmitter = true;
        }
        return transmitter;
    }


    /**
     * Process the INVITE at a receiver.
     * @param request the received request.
     * @param session the SIP session
     * @return modified request after processing from the receiver.
     * @throws RemoteException error accessing the receiver
     * @throws ResourceUnavailableException resource not available
     */
    private SdpMessage processReceiverInvite(SdpMessage request,
            SipSession session)
            throws RemoteException, ResourceUnavailableException {
        final Resource resource;
        try {
            resource = registryImpl.getResource(Resource.Type.RECEIVER);
        } catch (org.speechforge.cairo.exception.ResourceUnavailableException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new org.speechforge.cairo.sip.ResourceUnavailableException("Could not get a receiver resource");
        }
        final String id = session.getId();
        LOGGER.info("inviting receiver for " + id);
        request = resource.invite(request, id);
        session.addResource(resource);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("receiver proxy: " + resource);
        }
        return request;
    }

    /**
     * Process the INVITE at a transmitter.
     * @param request the received request.
     * @param session the SIP session
     * @return modified request after processing from the transmitter.
     * @throws RemoteException error accessing the transmitter
     * @throws ResourceUnavailableException resource not available
     */
    private SdpMessage processTransmitterInvite(SdpMessage request,
            SipSession session)
            throws RemoteException, ResourceUnavailableException {
        Resource resource;
        try {
            resource = registryImpl.getResource(Resource.Type.TRANSMITTER);
        } catch (org.speechforge.cairo.exception.ResourceUnavailableException e) {
            LOGGER.warn(e.getMessage(), e);
            throw new org.speechforge.cairo.sip.ResourceUnavailableException("Could not get a transmitter resource");
        }
        final String id = session.getId();
        LOGGER.info("inviting transmitter for " + id);
        request = resource.invite(request, id);
        session.addResource(resource);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("transmitter proxy: " + resource);
        }
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processByeRequest(SipSession session) 
            throws RemoteException, InterruptedException {
        RemoteException re = null;
        List<SipResource> resources = session.getResources();
        String sessionId = session.getId();
        for (SipResource resource : resources) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("removing resource from session '" + sessionId 
                        + "' with proxy: " + resource);
            }
            try {
                resource.bye(sessionId);
            } catch (RemoteException e) {
                re = e;
                LOGGER.warn("error removing resource after bye " 
                        + e.getMessage(), e);
            }
        }
        if (re != null) {
            throw re;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SdpMessage processInviteRequest(SdpMessage request, 
            SipSession session) throws SdpException,
                ResourceUnavailableException, RemoteException {
        final SdpMessage m = invite(request, session);
        try {
            _ua.sendResponse(session, m);
        } catch (SipException e) {
            LOGGER.warn("error processing invite: " + e.getMessage(), e);
            throw new SdpException(e.getMessage(), e);
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SdpMessage processInviteResponse(boolean ok, SdpMessage response, SipSession session) {
        return null;
    }
    
    private static Options getOptions() {
        Options options = ResourceImpl.getOptions();

        Option option = Option.builder("p")
                .argName(SIPPORT_OPTION)
                .longOpt(SIPPORT_OPTION)
                .hasArg(true)
                .desc("The port the sip agent uses to listen for requests.")
                .build();
        options.addOption(option);

        option = Option.builder("t")
                .argName(SIPTRANSPORT_OPTION)
                .longOpt(SIPTRANSPORT_OPTION)
                .hasArg(true)
                .desc("The transport used by the sip agent udp or tcp.")
                .build();
        options.addOption(option);

        option = Option.builder("P")
                .argName(SIPPUBLICADDRESS_OPTION)
                .longOpt(SIPPUBLICADDRESS_OPTION)
                .hasArg(true)
                .desc("The public address of the server (use this if the server is using NAT).")
                .build();
        options.addOption(option);
        
        option = Option.builder("L")
                .argName(LOCALADDRESS_OPTION)
                .longOpt(LOCALADDRESS_OPTION)
                .hasArg(true)
                .desc("The local address of the server (sometimes there is a problem getting the local address -- ie.e VMWare virtual ip).")
                .build();
        options.addOption(option);

        return options;
    }


    /**
     * Provides logging information about the used environment like host system.
     * and used Java version
     */
    private static void reportEnvironmentInformation() {
        // Get JRE info
        final Package pkg = Runtime.class.getPackage();
        final String version = pkg.getImplementationVersion();
        final String vendor = pkg.getImplementationVendor();
        final String title = pkg.getImplementationTitle();
        LOGGER.info("Java:\t\t\t" + title + " " + version);
        LOGGER.info("Java vendor:\t\t" + vendor);
        
        // Get OS info
        final String os = System.getProperty("os.name", "generic");
        LOGGER.info("Operating system:\t" + os);
        
        // Check the security policy
        final String policy = System.getProperty("java.security.policy");
        if (policy == null) {
            LOGGER.info("java.security.policy:\t(undefined)");
        } else {
            final File policyFile = new File(policy);
            if (policyFile.exists()) {
                LOGGER.info("java.security.policy:\t" + policy);
            } else {
                LOGGER.info("java.security.policy:\t" + policy
                        + " (not found)");
            }
        } 
    }

    /**
     * Main method of the resource server
     * 
     * @param args program arguments
     * @throws Exception error running the program
     */
    public static void main(String[] args) throws Exception {
        PrintStream logger = IoBuilder.forLogger("System.out").setLevel(Level.INFO).buildPrintStream();
        PrintStream errorLogger = IoBuilder.forLogger("System.err").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(logger);
        System.setErr(errorLogger);

        CommandLineParser parser = new DefaultParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();

        /*if (args.length < 3 || args.length > 5 || line.hasOption(ResourceImpl.HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("ResourceServerImpl [options] ", options);
            return;
        }*/

        int sipPort = 0;
        String sipTransport = null;
        String publicAddress = null;
        if (line.hasOption(SIPPORT_OPTION)) {
            String tmp = line.getOptionValue(SIPPORT_OPTION);
            sipPort = Integer.valueOf(tmp);
        }

        if (line.hasOption(SIPTRANSPORT_OPTION)) {
           sipTransport = line.getOptionValue(SIPTRANSPORT_OPTION);
        }
        
        if (line.hasOption(SIPPUBLICADDRESS_OPTION)) {
            publicAddress = line.getOptionValue(SIPPUBLICADDRESS_OPTION);
        }
        
        String hostName = null;
        if (line.hasOption(LOCALADDRESS_OPTION)) {
            hostName = line.getOptionValue(LOCALADDRESS_OPTION);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("command line options: ");
            for (String option : line.getArgs()) {
                LOGGER.debug("- '" + option + "'");
            }
        }
        LOGGER.info("----------------------------------------------------");
        LOGGER.info("starting cairo...");
        reportEnvironmentInformation();
        ResourceRegistryImpl rr = new ResourceRegistryImpl();
        ResourceServerImpl rs = new ResourceServerImpl(rr, sipPort,
                sipTransport, hostName, publicAddress);

        Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        registry.rebind(ResourceRegistry.NAME, rr);
//        registry.rebind(ResourceServerImpl.NAME, rs);

        LOGGER.info("Server and registry bound and waiting...");
    }

    public void processTimeout(TimeoutEvent event) {
        // TODO Auto-generated method stub
        
    }
    public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content) {
        // TODO Auto-generated method stub
    }

}

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
package  org.speechforge.cairo.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Vector;

import javax.media.Format;
import javax.media.PlugInManager;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.Participant;
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.InactiveReceiveStreamEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;
import javax.media.rtp.rtcp.SourceDescription;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.speechforge.cairo.jmf.codec.audio.dtmf.JavaDecoder;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Manages connection with and consumption from an incoming RTP audio stream.
 * <p>
 * This abstract class provides the base for RTP stream consumers, handling session
 * initialization, event processing, and DTMF support registration. Subclasses must
 * implement stream handling methods for received, mapped, and inactive streams.
 * </p>
 *
 * @author Niels Godfredsen {@literal <}ngodfredsen@users.sourceforge.net{@literal >}
 * @author Dirk Schnelle-Walka
 */
public abstract class RTPConsumer implements SessionListener, ReceiveStreamListener {
    /** Logger instance for RTPConsumer events and errors. */
    private static final Logger LOGGER = 
            LogManager.getLogger(RTPConsumer.class);
    /** Highest possible TCP port value (exclusive upper bound). */
    public static final int TCP_PORT_MAX = 65536;
    /** The RTP manager controlled by this consumer. */
    protected RTPManager rtpManager;
    /** Local session address for RTP. */
    private SessionAddress _localAddress;
    /** Target (remote) session address for RTP. */
    private SessionAddress _targetAddress;
    /** Preferred media formats for incoming streams. */
    private Format[] preferredMediaFormats;
    /** The current audio format of the active RTP stream. */
    private AudioFormat currentFormat;
    
    /**
     * Instantiates a new RTP consumer using the specified port on localhost.
     *
     * @param port the port to use for both local and remote addresses
     * @throws IOException if an I/O error occurs or port is invalid
     * @throws IllegalArgumentException if port is out of range
     */
    public RTPConsumer(int port) throws IOException {
        if (port < 0 || port >= TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
        _localAddress = new SessionAddress(CairoUtil.getLocalHost(), port);
        _targetAddress = _localAddress;
        init();
    }

    /**
     * Instantiates a new RTP consumer with a specific local address and port.
     *
     * @param localAddress the local host address
     * @param port the local port
     * @throws IOException if an I/O error occurs or port is invalid
     * @throws IllegalArgumentException if port is out of range
     */
    public RTPConsumer(InetAddress localAddress, int port) throws IOException {
        if (port < 0 || port >= TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
        _localAddress = new SessionAddress(localAddress, port);
        _targetAddress = _localAddress;
        init();
    }
    
    /**
     * Instantiates a new RTP consumer with full local and remote address/port specification.
     *
     * @param localHost the local host name
     * @param localPort the local port
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * @param preferredMediaFormats preferred media formats for the stream
     * @throws IOException if an I/O error occurs or addresses/ports are invalid
     * @throws IllegalArgumentException if any address/port is invalid
     */
    public RTPConsumer(String localHost, int localPort, 
            InetAddress remoteAddress, int remotePort, 
            Format[] preferredMediaFormats) throws IOException {
        if (localPort < 0 || localPort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid local port value: " + localPort);
        }
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address supplied must not be null!");
        }
        if (remotePort < 0 || remotePort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid remote port value: " + remotePort);
        }
        _localAddress = new SessionAddress(InetAddress.getByName(localHost), localPort);
        _targetAddress = new SessionAddress(remoteAddress, remotePort);
        this.preferredMediaFormats = preferredMediaFormats;
        init();
    }
    
    /**
     * Instantiates a new RTP consumer with local port and remote address/port, using localhost.
     *
     * @param localPort the local port
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * @throws IOException if an I/O error occurs or addresses/ports are invalid
     * @throws IllegalArgumentException if any address/port is invalid
     */
    public RTPConsumer(int localPort, InetAddress remoteAddress, int remotePort) throws IOException {
        if (localPort < 0 || localPort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid local port value: " + localPort);
        }
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address supplied must not be null!");
        }
        if (remotePort < 0 || remotePort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid remote port value: " + remotePort);
        }
        _localAddress = new SessionAddress(CairoUtil.getLocalHost(), localPort);
        _targetAddress = new SessionAddress(remoteAddress, remotePort);
     
        init();
    }
    
    /**
     * Instantiates a new RTP consumer with explicit local and remote addresses and ports.
     *
     * @param localAddress the local address
     * @param localPort the local port
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * @throws IOException if an I/O error occurs or addresses/ports are invalid
     * @throws IllegalArgumentException if any address/port is invalid
     */
    public RTPConsumer(InetAddress localAddress, int localPort, InetAddress remoteAddress, int remotePort) throws IOException {
        if (localPort < 0 || localPort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid local port value: " + localPort);
        }
        if (remoteAddress == null) {
            throw new IllegalArgumentException("Remote address supplied must not be null!");
        }
        if (remotePort < 0 || remotePort > TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid remote port value: " + remotePort);
        }
        _localAddress = new SessionAddress(localAddress, localPort);
        _targetAddress = new SessionAddress(remoteAddress, remotePort);
     
        init();
    }
    
    /**
     * Initializes this RTP Consumer, registering DTMF support and setting up RTPManager.
     *
     * @throws IOException error initializing RTPManager or DTMF support
     */
    private void init() throws IOException {
        registerDTMFSupport();

        /** Create a new RTP manager. */
        rtpManager = RTPManager.newInstance();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created new RTPManager '" + 
                    rtpManager.getClass().getName() + "'");
            LOGGER.debug("Initializing RTPManager with local address '" 
                    + _localAddress + "'");
        }
        
        // Register DTMD handling
        rtpManager.addFormat(JavaDecoder.DTMF_FORMAT,
                JavaDecoder.DTMF_PAYLOAD);
        rtpManager.addSessionListener(this);
        rtpManager.addReceiveStreamListener(this);

        try {
            rtpManager.initialize(_localAddress);
            rtpManager.addTarget(_targetAddress);
        } catch (InvalidSessionAddressException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Registers DTMF support by installing the JavaDecoder codec.
     *
     * @throws IOException error registering DTMF support
     */
    private void registerDTMFSupport() throws IOException {
        JavaDecoder decoder = new JavaDecoder();
        PlugInManager.addPlugIn(JavaDecoder.class.getCanonicalName(), 
                decoder.getSupportedInputFormats(), 
                decoder.getSupportedInputFormats(), PlugInManager.CODEC );
        PlugInManager.commit();
        if (isDTMFCodecInstalled()) {
            LOGGER.info("registered DTMF Decoder");
        } else {
            LOGGER.warn("DTMF Decoder not registered");
        }
    }

    /**
     * Checks if the DTMF codec is installed in the PlugInManager.
     *
     * @return true if the DTMF codec is installed, false otherwise
     */
    private boolean isDTMFCodecInstalled() {
        @SuppressWarnings("unchecked")
        Vector<String> codecs = PlugInManager.getPlugInList(null, null, 
                PlugInManager.CODEC);
        for (String codec : codecs) {
            if (codec.equals(JavaDecoder.class.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shutdown this RTP Consumer and free resources.
     * Closes RTP streams and disposes the RTPManager.
     */
    public synchronized void shutdown() {
        // close RTP streams
        if (rtpManager != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("shutting down RTPManager with local address '"
                        +  _localAddress + "'");
            }
            rtpManager.removeTargets("RTP receiver shutting down.");
            rtpManager.dispose();
            rtpManager = null;
        }

    }

    /**
     * Handles session-level events from the RTPManager.
     *
     * @param event the session event
     */
    @Override
    public synchronized void update(SessionEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SessionEvent received: " + event);
            if (event instanceof NewParticipantEvent) {
                Participant p = ((NewParticipantEvent) event).getParticipant();
                LOGGER.debug("A new participant has just joined: " + p.getCNAME());
            }
        }
    }

    /**
     * Handles receive stream events from the RTPManager.
     *
     * @param event the receive stream event
     */
    @Override
    public synchronized void update(ReceiveStreamEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ReceiveStreamEvent received: " + event);
        }

        final ReceiveStream stream = event.getReceiveStream();
        if (event instanceof RemotePayloadChangeEvent) {
            handlePayloadChangeEvent((RemotePayloadChangeEvent) event, stream);
        } else if (event instanceof NewReceiveStreamEvent) {
            handleNewReceiveStreamEvent(stream);
        } else if (event instanceof StreamMappedEvent) {
            handleStreamMappedEvent(event, stream);
        } else if (event instanceof InactiveReceiveStreamEvent || event instanceof ByeEvent) {
            handleInactiveStreamEvent(event, stream);
        } else {
            LOGGER.warn("Received unknown RTP event: " + event);
        }
    }

    /**
     * Handle inactive stream event, such as BYE or inactivity.
     *
     * @param event the event
     * @param stream the stream
     */
    private void handleInactiveStreamEvent(ReceiveStreamEvent event,
            final ReceiveStream stream) {
        if (stream != null) {
            this.streamInactive(stream, (event instanceof ByeEvent));
        }
    }

    /**
     * Handle stream mapped event, associating a stream with a participant.
     *
     * @param event the event
     * @param stream the stream
     */
    private void handleStreamMappedEvent(ReceiveStreamEvent event,
            final ReceiveStream stream) {
        Participant participant = event.getParticipant();
        if (participant != null && LOGGER.isDebugEnabled()) {
            for (Object o : participant.getSourceDescription()) {
                final SourceDescription sd = (SourceDescription) o;
                LOGGER.debug("Source description: " + toString(sd));
            }
        }
        if (stream == null) {
            LOGGER.warn("StreamMappedEvent: receive stream is null!");
            return;
        }
        if (participant == null) {
            LOGGER.warn("StreamMappedEvent: participant is null!");
            return;
        } 
        streamMapped(stream, participant);
    }

    /**
     * Handle new receive stream event, initializing the stream and format.
     *
     * @param stream the receive stream
     */
    private void handleNewReceiveStreamEvent(ReceiveStream stream) {
        if (stream == null) {
            LOGGER.warn("NewReceiveStreamEvent: receive stream is null!");
            return;
        } 
        final DataSource dataSource = stream.getDataSource();
        if (dataSource == null) {
            LOGGER.warn("NewReceiveStreamEvent: data source is null!");
            return;
        }
        if (!(dataSource instanceof PushBufferDataSource)) {
            LOGGER.warn("NewReceiveStreamEvent: data source is not PushBufferDataSource!");
            return;
        }
        // Find out the formats.
        final RTPControl control = (RTPControl) dataSource.getControl(
                "javax.media.rtp.RTPControl");
        if (control != null) {
            currentFormat = (AudioFormat) control.getFormat();
            LOGGER.info("Received new RTP stream: " + currentFormat);
        } else {
            LOGGER.warn("Received new RTP stream: RTPControl is null, "
                    + "unable to determine format!");
        }
        streamReceived(stream, (PushBufferDataSource) dataSource, 
                preferredMediaFormats);
    }

    /**
     * Handle payload change event, including DTMF payloads.
     *
     * @param event the payload change event
     * @param stream the receive stream
     */
    private void handlePayloadChangeEvent(RemotePayloadChangeEvent event,
            ReceiveStream stream) {
        int payload = event.getNewPayload();

        // Check if the new payload type is for DTMF events
        if (payload == JavaDecoder.DTMF_PAYLOAD) {
            LOGGER.warn("Handling of DTMF payload types not implemented yet.");
            handleDTMFPayload(event, stream);
        } else {
            // This will also be called after the DTMF payload type has been
            // handled, with a payplod type of 0
            LOGGER.warn("Received an RTP PayloadChangeEvent to " + payload 
                    + ". Sorry, cannot handle payload change.");
            AudioFormat requestedAudioFormat =
                    AudioFormats.getAudioFormat(payload);
            if (currentFormat.matches(requestedAudioFormat)) {
                // TODO This may change once DTMD handling is implemented
                LOGGER.info("reverting to original format: " + currentFormat);
            }
        }
    }

    /**
     * Handle DTMF payload change event for the stream.
     *
     * @param event the event
     * @param stream the receive stream
     */
    private void handleDTMFPayload(RemotePayloadChangeEvent event,
            ReceiveStream stream) {
        final PushBufferDataSource dataSource = 
                (PushBufferDataSource) stream.getDataSource();
        try {
            RTPControl control = (RTPControl) dataSource.getControl(
                    RTPControl.class.getCanonicalName());
            control.addFormat(JavaDecoder.DTMF_FORMAT, 
                    JavaDecoder.DTMF_PAYLOAD);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DTMF payload with format: " + 
                        control.getFormat());
            }
            
            dataSource.connect();
        } catch (IOException e) {
            LOGGER.warn("Error connecting data source: " + e.getMessage(), e);
        }
        
    }
    
    /**
     * Called when a new RTP stream is received.
     *
     * @param stream the receive stream
     * @param dataSource the data source for the stream
     * @param preferredMediaFormats preferred media formats
     */
    public abstract void streamReceived(ReceiveStream stream, 
            PushBufferDataSource dataSource,Format[] preferredMediaFormats);

    /**
     * Called when a stream is mapped to a participant.
     *
     * @param stream the receive stream
     * @param participant the participant
     */
    public abstract void streamMapped(ReceiveStream stream, 
            Participant participant);

    /**
     * Called when a stream becomes inactive or receives a BYE event.
     *
     * @param stream the receive stream
     * @param byeEvent true if the event is a BYE event
     */
    public abstract void streamInactive(ReceiveStream stream, boolean byeEvent);

    /**
     * Generates a string representation of the given source description.
     *
     * @param sd the source description
     * @return string representation of the source description
     */
    private static String toString(SourceDescription sd) {
        final StringBuilder sb = new StringBuilder();
        switch (sd.getType()) {
        case SourceDescription.SOURCE_DESC_CNAME:
            sb.append("SOURCE_DESC_CNAME");
            break;
        
        case SourceDescription.SOURCE_DESC_NAME:
            sb.append("SOURCE_DESC_NAME");
            break;
        
        case SourceDescription.SOURCE_DESC_EMAIL:
            sb.append("SOURCE_DESC_EMAIL");
            break;
        
        case SourceDescription.SOURCE_DESC_PHONE:
            sb.append("SOURCE_DESC_PHONE");
            break;
        
        case SourceDescription.SOURCE_DESC_LOC:
            sb.append("SOURCE_DESC_LOC");
            break;
        
        case SourceDescription.SOURCE_DESC_TOOL:
            sb.append("SOURCE_DESC_TOOL");
            break;
        
        case SourceDescription.SOURCE_DESC_NOTE:
            sb.append("SOURCE_DESC_NOTE");
            break;
        
        case SourceDescription.SOURCE_DESC_PRIV:
            sb.append("SOURCE_DESC_PRIV");
            break;

        default:
            sb.append("SOURCE_DESC_???");
            break;

        }
        sb.append('=');
        sb.append(sd.getDescription());
        return sb.toString();
    }

}
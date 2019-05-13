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
import java.util.Iterator;

import javax.media.Format;
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

import org.apache.log4j.Logger;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Manages connection with and consumption from an incoming RTP audio stream.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public abstract class RTPConsumer implements SessionListener, ReceiveStreamListener {

    private static Logger _logger = Logger.getLogger(RTPConsumer.class);
    
    public static final int TCP_PORT_MAX = 65536;

    protected RTPManager _rtpManager;
    private SessionAddress _localAddress;
    private SessionAddress _targetAddress;
    
    
    private Format[] preferredMediaFormats;

    /**
     * Instantiates a new RTP consumer.  Just needs a remote port.  
     * Assumes the local and remote host is the localhost.
     * 
     * @param port the port
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public RTPConsumer(int port) throws IOException {
        if (port < 0 || port >= TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
        _localAddress = new SessionAddress(CairoUtil.getLocalHost(), port);
        _targetAddress = _localAddress;
        init();
    }

    
    public RTPConsumer(InetAddress localAddress, int port) throws IOException {
        if (port < 0 || port >= TCP_PORT_MAX) {
            throw new IllegalArgumentException("Invalid port value: " + port);
        }
        _localAddress = new SessionAddress(localAddress, port);
        _targetAddress = _localAddress;
        init();
    }
    
    /**
     * Instantiates a new RTP consumer.  It needs both the local and remote host names 
     * as well ast the local and remote port.
     * 
     * @param localHost the local host
     * @param localPort the local port
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public RTPConsumer(String localHost, int localPort, InetAddress remoteAddress, int remotePort, Format[] preferredMediaFormats) throws IOException {
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
     * Instantiates a new RTP consumer.  Requires the remote host name and the local and remote port.  
     * It uses localhost for the local host name.
     * 
     * @param localPort the local port
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * 
     * @throws IOException Signals that an I/O exception has occurred.
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
    
    private void init() throws IOException {
           
        _rtpManager = RTPManager.newInstance();
        if (_logger.isDebugEnabled()) {
            _logger.debug("RTPManager class: " + _rtpManager.getClass().getName());
        }
        _rtpManager.addSessionListener(this);
        _rtpManager.addReceiveStreamListener(this);

        try {
            _rtpManager.initialize(_localAddress);
            _rtpManager.addTarget(_targetAddress);
        } catch (InvalidSessionAddressException e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    /**
     * TODOC
     */
    public synchronized void shutdown() {
        // close RTP streams
        if (_rtpManager != null) {
            _rtpManager.removeTargets("RTP receiver shutting down.");
            _rtpManager.dispose();
            _rtpManager = null;
        }

    }

    /* (non-Javadoc)
     * @see javax.media.rtp.SessionListener#update(javax.media.rtp.event.SessionEvent)
     */
    public synchronized void update(SessionEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("SessionEvent received: " + event);
            if (event instanceof NewParticipantEvent) {
                Participant p = ((NewParticipantEvent) event).getParticipant();
                _logger.debug("  - A new participant has just joined: " + p.getCNAME());
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.media.rtp.ReceiveStreamListener#update(javax.media.rtp.event.ReceiveStreamEvent)
     */
    public synchronized void update(ReceiveStreamEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("ReceiveStreamEvent received: " + event);
        }

        if (event instanceof RemotePayloadChangeEvent) {
            _logger.warn("  - Received an RTP PayloadChangeEvent.\nSorry, cannot handle payload change.");
            //System.exit(0);
            return;
        }

        ReceiveStream stream = event.getReceiveStream();

        if (event instanceof NewReceiveStreamEvent) {
            if (stream == null) {
                _logger.debug("NewReceiveStreamEvent: receive stream is null!");
            } else {
                DataSource dataSource = stream.getDataSource();
                if (dataSource == null) {
                    _logger.debug("NewReceiveStreamEvent: data source is null!");
                } else if (!(dataSource instanceof PushBufferDataSource)) {
                    _logger.debug("NewReceiveStreamEvent: data source is not PushBufferDataSource!");
                } else {
                    if (_logger.isDebugEnabled()) {
                        // Find out the formats.
                        RTPControl control = (RTPControl) dataSource.getControl("javax.media.rtp.RTPControl");
                        if (control != null) {
                            _logger.debug("  - Recevied new RTP stream: " + control.getFormat());
                        } else {
                            _logger.debug("  - Recevied new RTP stream: RTPControl is null!");
                        }
                    }
                    this.streamReceived(stream, (PushBufferDataSource) dataSource, preferredMediaFormats);
                }
            }
        } else if (event instanceof StreamMappedEvent) {
            Participant participant = event.getParticipant();
            if (participant != null && _logger.isDebugEnabled()) {
                for (Iterator it = participant.getSourceDescription().iterator(); it.hasNext(); ) {
                    SourceDescription sd = (SourceDescription) it.next();
                    _logger.debug("Source description: " + toString(sd));
                }
            }
            if (stream == null) {
                _logger.debug("StreamMappedEvent: receive stream is null!");
            } else if (participant == null) {
                _logger.debug("StreamMappedEvent: participant is null!");
            } else {
                this.streamMapped(stream, participant);
            }
        } else if (event instanceof InactiveReceiveStreamEvent || event instanceof ByeEvent) {
            if (stream != null) {
                this.streamInactive(stream, (event instanceof ByeEvent));
            }
        }
    }

    public abstract void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource,Format[] preferredMediaFormats);

    public abstract void streamMapped(ReceiveStream stream, Participant participant);

    public abstract void streamInactive(ReceiveStream stream, boolean byeEvent);

    private static String toString(SourceDescription sd) {
        StringBuffer sb = new StringBuffer();
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
        sb.append('=').append(sd.getDescription());
        return sb.toString();
    }

}

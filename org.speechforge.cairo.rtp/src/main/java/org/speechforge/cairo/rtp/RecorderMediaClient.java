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
package org.speechforge.cairo.rtp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Iterator;
import javax.media.CannotRealizeException;
import javax.media.DataSink;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSinkException;
import javax.media.NoPlayerException;
import javax.media.NotRealizedError;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.DataSource;
import javax.media.protocol.FileTypeDescriptor;
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
 * Reusable media client that plays audio received from an RTP media stream through the system speakers
 * while simultaneously streaming audio from the system microphone to an RTP destination.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecorderMediaClient implements SessionListener, ReceiveStreamListener { 

    private static Logger _logger = Logger.getLogger(RecorderMediaClient.class);

    private PushBufferDataSource _pbds;
    
    protected RTPManager _rtpManager;
    private SessionAddress _localAddress;
    private SessionAddress _targetAddress;
    DataSink _sink = null;    
    Processor _processor;
    
    public RecorderMediaClient(PushBufferDataSource pbds) throws IOException, NoPlayerException, CannotRealizeException {
        _localAddress=null;
        _targetAddress=null;
        _localAddress = new SessionAddress(CairoUtil.getLocalHost(),javax.media.rtp.SessionAddress.ANY_PORT);
        _targetAddress = _localAddress;
        
        _rtpManager = RTPManager.newInstance();
        if (_logger.isDebugEnabled()) {
            _logger.debug("RTPManager class: " + _rtpManager.getClass().getName());
        }
        //_rtpManager.addSessionListener(this);
        //_rtpManager.addReceiveStreamListener(this);

        try {
            _rtpManager.initialize(_localAddress);
        } catch (InvalidSessionAddressException e) {
            e.printStackTrace();
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
        _pbds = pbds;
        System.out.println("Creating player..");
        ProcessorModel pm = new ProcessorModel(pbds, 
                                               new AudioFormat[] {new AudioFormat(javax.media.format.AudioFormat.LINEAR,44100,16,2)}, 
                                               new FileTypeDescriptor(FileTypeDescriptor.BASIC_AUDIO));
        _logger.debug("Creating realized processor...");
        _processor = Manager.createRealizedProcessor(pm);        
        
        URL url = new URL("file://temp/cairo/tmp/"+System.nanoTime()+".au");
        MediaLocator dest = new MediaLocator(url);

        try {
            _sink = Manager.createDataSink(_processor.getDataOutput(), dest);
        } catch (NoDataSinkException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NotRealizedError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        _sink.open();
        _sink.start();
        
        System.out.println("Starting processor..");
        _processor.start();
        System.out.println("processor started");
    }


    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource) {
        if (_processor == null) {
            
        }
    }


    public void streamMapped(ReceiveStream stream, Participant participant) {
        // TODO Auto-generated method stub
        
    }


    public void streamInactive(ReceiveStream stream, boolean byeEvent) {
        if (byeEvent) {
            synchronized (this) {
                // TODO: close player
            }
        }
        try {
            _sink.stop();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        _sink.close();
    }
    

    public synchronized void update(SessionEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("SessionEvent received: " + event);
            if (event instanceof NewParticipantEvent) {
                Participant p = ((NewParticipantEvent) event).getParticipant();
                _logger.debug("  - A new participant has just joined: " + p.getCNAME());
            }
        }
    }


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
                    this.streamReceived(stream, (PushBufferDataSource) dataSource);
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
       // GlobalTransmissionStats gts = _rtpManager.getGlobalTransmissionStats();
       // System.out.println("TRANS: "+gts.getTransmitFailed());
       // GlobalReceptionStats grs = _rtpManager.getGlobalReceptionStats();
       // System.out.println("RECV: "+grs.toString());
    }

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

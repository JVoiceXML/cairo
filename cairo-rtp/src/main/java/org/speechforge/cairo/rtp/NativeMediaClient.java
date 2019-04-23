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

import static org.speechforge.cairo.jmf.JMFUtil.MICROPHONE;

import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.rtp.RTPPlayer;
import org.speechforge.cairo.util.CairoUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import javax.media.Format;
import javax.media.Manager;
import javax.media.Player;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;

import org.apache.log4j.Logger;

/**
 * Reusable media client that plays audio received from an RTP media stream through the system speakers
 * while simultaneously streaming audio from the system microphone to an RTP destination.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class NativeMediaClient extends RTPConsumer {

    private static Logger _logger = Logger.getLogger(NativeMediaClient.class);

    private Player _player;
    private RTPPlayer _rtpPlayer;
    
    public NativeMediaClient(String localHost, int localPort, InetAddress remoteAddress, int remotePort, Format[] formats)
    throws IOException {
      super(localHost, localPort, remoteAddress, remotePort, formats);
      _rtpPlayer = new RTPPlayer(_rtpManager);
  }

    /**
     * Constructs media client in send/receive mode.
     * @param localPort 
     * @param remoteAddress 
     * @param remotePort 
     * @throws IOException 
     */
    public NativeMediaClient(int localPort, InetAddress remoteAddress, int remotePort)
      throws IOException {
        super(localPort, remoteAddress, remotePort);
        _rtpPlayer = new RTPPlayer(_rtpManager);
    }

    /**
     * Constructs media client in receive only mode.
     * @param localPort 
     * @param remoteAddress 
     * @param remotePort 
     * @throws IOException 
     */
    public NativeMediaClient(int localPort)
      throws IOException {
        super(localPort);
        _rtpPlayer = null;
    }
    
    public NativeMediaClient(InetAddress localAddress,int localPort)
    throws IOException {
      super(localAddress, localPort);
      _rtpPlayer = null;
  }

    public void startTransmit() {
        if (_rtpPlayer != null) {
            new TransmitThread().start();
        }
    }
    
    public void stop() {
        _rtpPlayer.shutdown();
        super.shutdown();
        if (_player != null) {
           _player.stop();
           _player.deallocate();
           _player.close();
        }
    }
    
    public void stopTransmit() {
        _rtpPlayer.shutdown();
    }

    private class TransmitThread extends Thread {
        public void run() {
            try {
                _rtpPlayer.playSource(MICROPHONE);
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                _logger.warn(e, e);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                _logger.warn(e, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamReceived(javax.media.rtp.ReceiveStream, javax.media.protocol.PushBufferDataSource)
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource, Format[] preferredMediaFormats) {
        if (_player == null) {
            try {
                _logger.debug("Creating player for new stream...");
                _player = Manager.createRealizedPlayer(dataSource);
            } catch (Exception e) {
                _logger.warn("Could not create player for new stream!", e);
                return;
            }
            _logger.debug("Starting player...");
            _player.start();
        } else {
            _logger.warn("Stream already received, ignoring new stream!");
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamMapped(javax.media.rtp.ReceiveStream, javax.media.rtp.Participant)
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamInactive(javax.media.rtp.ReceiveStream, boolean)
     */
    @Override
    public void streamInactive(ReceiveStream stream, boolean byeEvent) {
        if (byeEvent) {
            synchronized (this) {
                // TODO: close player
            }
        }
    }

    /**
     * TODOC
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        int localPort = 42048;
        InetAddress remoteAddress = CairoUtil.getLocalHost();
        int remotePort = 42050;

        NativeMediaClient client = new NativeMediaClient(localPort, remoteAddress, remotePort);
        client.startTransmit();

        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        _logger.info("Hit <enter> to shutdown...");
        String cmd = consoleReader.readLine();
        _logger.info("Shutting down...");
        client.shutdown();

    }

}

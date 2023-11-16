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
package org.speechforge.cairo.server.tts;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.media.rtp.InvalidSessionAddressException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.speechforge.cairo.rtp.AudioFormats;
import org.speechforge.cairo.rtp.RTPPlayer;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Handle requests for speech synthesis (TTS) to be streamed through an outbound RTP channel.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class RTPSpeechSynthChannel {
    /** Logger instance. */
    private static final Logger LOGGER = 
            LogManager.getLogger(RTPSpeechSynthChannel.class);

    static final short IDLE = 0;
    static final short SPEAKING = 1;
    static final short PAUSED = 2;

    volatile short _state = IDLE;

    /** List of prompts to be played back. */
    BlockingQueue<PromptPlay> promptQueue = new LinkedBlockingQueue<PromptPlay>();
    private SendThread _sendThread;
    RTPPlayer _promptPlayer;
    private int _localPort;
    private InetAddress _remoteAddress;
    private int _remotePort;
    private AudioFormats _af;

    private InetAddress _localAddress;
    
    /**
     * TODOC
     * @param localPort the port to use
     * @param localAddress the local address
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     * @param af the aaudio format to use 
     */
    public RTPSpeechSynthChannel(int localPort, InetAddress localAddress,
    		InetAddress remoteAddress, int remotePort, AudioFormats af) {
        _localPort = localPort;
        _remoteAddress = remoteAddress;
        _remotePort = remotePort;
        _af = af;
        _localAddress = localAddress;
    }

    private boolean init() throws InvalidSessionAddressException, IOException {
        if (_promptPlayer == null) {
            _promptPlayer = new RTPPlayer(_localAddress, _localPort, _remoteAddress, _remotePort, _af);
            (_sendThread = new SendThread()).start();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("created a player and started it");
            }
            return true;
        }
        return false;
    }
    
    public synchronized void shutdown() throws InterruptedException {
        _sendThread.shutdown();
        _promptPlayer.shutdown();
    }

    public synchronized int queuePrompt(File promptFile, PromptPlayListener listener)
      throws InvalidSessionAddressException, IOException {

        int state = _state;
        try {
            init();
            PromptPlay play = new PromptPlay(promptFile, listener); 
            promptQueue.put(play);
            _state = SPEAKING;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return state;
    }
    
    public synchronized void stopPlayback() {
        _sendThread.shutdown();
        //TODO: wait for send thread to complete?  (prevent double interrupt while draining queue)
    }

    private class SendThread extends Thread {
        
        volatile boolean _run = true;

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            while (_run) {
                PromptPlay promptPlay = null;
                boolean drainQueue = false;
                Exception cause = null;

                try {
                    // get next prompt to play
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("taking next prompt from prompt queue...");
                    }
                    promptPlay = promptQueue.take();
                    if (promptPlay != null) {
                        _promptPlayer.playPrompt(promptPlay._promptFile);
                    }

                    // drain all prompts in queue if current prompt playback is interrupted (e.g. by STOP request)
                    drainQueue = !_run;
                } catch (Exception e) {
                    LOGGER.warn(e, e);
                    cause = e;
                    // TODO: cancel current prompt playback
                    drainQueue = true;
                }

                if (drainQueue) {
                    LOGGER.info("draining prompt queue...");
                    while (!promptQueue.isEmpty()) {
                        try {
                            promptQueue.take();
                            //TODO: may need to remove only specific prompts
                            // (e.g. save and put back in queue if not in cancel list)
                        } catch (InterruptedException e) {
                            // should not happen since this is the only thread consuming from queue
                            LOGGER.warn(e, e);
                        }
                    }
                } else if (promptPlay != null) {
                    if (promptPlay._listener != null) {
                        if (cause == null) {
                            try {
                                // give rtp stream a chance to catch up...
                                Thread.sleep(250);
                            } catch (InterruptedException e) {
                                LOGGER.warn("InterruptedException encountered!", e);
                            }
                            promptPlay._listener.playCompleted();
                        } else {
                            promptPlay._listener.playFailed(cause);
                        }
                    }
                } else {
                    LOGGER.warn("promptPlay is null!", cause);
                }

                _state = promptQueue.isEmpty() ? IDLE : SPEAKING;
            }
        }
        
        public void shutdown() {
            _run = false;
            // Add an empty play to trigger the playback
            promptQueue.offer(null);
        }
    }

    private static class PromptPlay {

        private File _promptFile;
        private PromptPlayListener _listener;

        PromptPlay(File promptFile, PromptPlayListener listener) {
            _promptFile = promptFile;
            _listener = listener;
        }
    }

    /**
     * TODOC
     * @param args program argumetns
     * @throws Exception error executing the program
     */
    public static void main(String[] args) throws Exception {
        
        File promptDir = new File("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\test");

        int localPort = 42050;
        InetAddress remoteAddress = CairoUtil.getLocalHost();
        int remotePort = 42048;
        InetAddress localAddress =  CairoUtil.getLocalHost();
        RTPSpeechSynthChannel player = new RTPSpeechSynthChannel(localPort, localAddress, remoteAddress, remotePort, new AudioFormats());
        
        File prompt = new File(promptDir, "good_morning_rita.wav");
        player.queuePrompt(prompt, null);
        player.queuePrompt(prompt, null);
        player.queuePrompt(prompt, null);
        player.queuePrompt(prompt, null);
        player.queuePrompt(prompt, null);
    }

}

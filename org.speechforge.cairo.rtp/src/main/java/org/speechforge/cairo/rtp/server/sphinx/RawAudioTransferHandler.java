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
package org.speechforge.cairo.rtp.server.sphinx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.PushBufferStream;

import org.apache.log4j.Logger;

/**
 * Transfers raw audio data from a {@code javax.media.protocol.PushBufferStream} to a
 * {@link org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor}.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RawAudioTransferHandler implements BufferTransferHandler {

    private static Logger LOGGER = Logger.getLogger(RawAudioTransferHandler.class);

    private RawAudioProcessor _rawAudioProcessor;

    public RawAudioTransferHandler(RawAudioProcessor rawAudioProcessor) {
        _rawAudioProcessor = rawAudioProcessor;
    }

    public synchronized void startProcessing(PushBufferStream pbStream)
      throws UnsupportedEncodingException, IllegalStateException {

    	LOGGER.debug("STARTING PROCESSING IN RAWAUDIO PROCESSOR");
        if (_rawAudioProcessor == null) {
            throw new IllegalStateException("RawAudioProcessor is null!");
        }

        Format format = pbStream.getFormat();
        if (!(format instanceof AudioFormat)) {
        	LOGGER.info("Bad format "+format);
            throw new UnsupportedEncodingException("RawAudioTransferHandler can only process audio formats!");
        }

        pbStream.setTransferHandler(this);
        try {
            _rawAudioProcessor.startProcessing((AudioFormat) format);
        } catch (UnsupportedEncodingException e) {
            pbStream.setTransferHandler(null);
            LOGGER.error(e.getMessage(), e);
            throw e;
        }

    }

    public synchronized void stopProcessing() {
        LOGGER.debug("Stopping RawAudioProcessor...");
        if (_rawAudioProcessor != null) {
            _rawAudioProcessor.stopProcessing();
            _rawAudioProcessor = null;
        }
    }

    /* (non-Javadoc)
     * @see javax.media.protocol.BufferTransferHandler#transferData(javax.media.protocol.PushBufferStream)
     */
    public synchronized void transferData(PushBufferStream stream) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("transferData callback entered with stream format = " + stream.getFormat());
        }

        if (stream.endOfStream()) {
            LOGGER.debug("transferData(): end of stream reached.");
            //stopProcessing();
        } else {
            try {
                Buffer buffer = new Buffer();
                LOGGER.trace("transferData(): reading stream into buffer...");
                stream.read(buffer);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("transferData(): stream read into buffer : offset=" + buffer.getOffset() + " length=" + buffer.getLength());
                }
                if (buffer.isEOM()) {
                    LOGGER.debug("transferData(): buffer is EOM.");
                    stopProcessing();
                } else if (buffer.isDiscard()) {
                    LOGGER.debug("transferData(): buffer is discard!");
                } else {
                    byte[] data = (byte[]) buffer.getData();
                    if (_rawAudioProcessor != null) {
                        if (buffer.getLength() > 0) {
                            _rawAudioProcessor.addRawData(data, buffer.getOffset(), buffer.getLength());
                        } else {
                            LOGGER.debug("transferData(): buffer length is zero!");
                        }
                    } else {
                        LOGGER.trace("transferData(): _rawAudioProcessor is null, discarding data.");
                    }
                }
            } catch (IOException e){
                LOGGER.warn("transferData() encountered IOException!", e);
            } catch (RuntimeException e){
                LOGGER.warn("transferData() encountered RuntimeException!", e);
            }
        }
        
    }

}

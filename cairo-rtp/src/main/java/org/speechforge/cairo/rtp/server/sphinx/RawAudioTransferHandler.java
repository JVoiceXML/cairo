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

    private static Logger _logger = Logger.getLogger(RawAudioTransferHandler.class);

    private RawAudioProcessor _rawAudioProcessor;

    public RawAudioTransferHandler(RawAudioProcessor rawAudioProcessor) {
        _rawAudioProcessor = rawAudioProcessor;
    }

    public synchronized void startProcessing(PushBufferStream pbStream)
      throws UnsupportedEncodingException, IllegalStateException {

    	_logger.debug("STARTING PROCESSING IN RAWAUDIO PROCESSOR");
        if (_rawAudioProcessor == null) {
            throw new IllegalStateException("RawAudioProcessor is null!");
        }

        Format format = pbStream.getFormat();
        if (!(format instanceof AudioFormat)) {
        	_logger.info("Bad format "+format);
            throw new UnsupportedEncodingException("RawAudioTransferHandler can only process audio formats!");
        }

        pbStream.setTransferHandler(this);
        try {
            _rawAudioProcessor.startProcessing((AudioFormat) format);
        } catch (UnsupportedEncodingException e) {
            pbStream.setTransferHandler(null);
            e.printStackTrace();
            throw e;
        }

    }

    public synchronized void stopProcessing() {
        _logger.debug("Stopping RawAudioProcessor...");
        if (_rawAudioProcessor != null) {
            _rawAudioProcessor.stopProcessing();
            _rawAudioProcessor = null;
        }
    }

    /* (non-Javadoc)
     * @see javax.media.protocol.BufferTransferHandler#transferData(javax.media.protocol.PushBufferStream)
     */
    public synchronized void transferData(PushBufferStream stream) {
        if (_logger.isTraceEnabled()) {
            _logger.trace("transferData callback entered with stream format = " + stream.getFormat());
        }

        if (stream.endOfStream()) {
            _logger.debug("transferData(): end of stream reached.");
            //stopProcessing();
        } else {
            try {
                Buffer buffer = new Buffer();
                _logger.trace("transferData(): reading stream into buffer...");
                stream.read(buffer);
                if (_logger.isTraceEnabled()) {
                    _logger.trace("transferData(): stream read into buffer : offset=" + buffer.getOffset() + " length=" + buffer.getLength());
                }
                if (buffer.isEOM()) {
                    _logger.debug("transferData(): buffer is EOM.");
                    stopProcessing();
                } else if (buffer.isDiscard()) {
                    _logger.debug("transferData(): buffer is discard!");
                } else {
                    byte[] data = (byte[]) buffer.getData();
                    if (_rawAudioProcessor != null) {
                        if (buffer.getLength() > 0) {
                            _rawAudioProcessor.addRawData(data, buffer.getOffset(), buffer.getLength());
                        } else {
                            _logger.debug("transferData(): buffer length is zero!");
                        }
                    } else {
                        _logger.trace("transferData(): _rawAudioProcessor is null, discarding data.");
                    }
                }
            } catch (IOException e){
                _logger.warn("transferData() encountered IOException!", e);
            } catch (RuntimeException e){
                _logger.warn("transferData() encountered RuntimeException!", e);
            }
        }
        
    }

}

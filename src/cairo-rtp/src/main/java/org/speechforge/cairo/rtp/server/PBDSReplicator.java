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
package org.speechforge.cairo.rtp.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.media.Buffer;
import javax.media.Duration;
import javax.media.Format;
import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.log4j.Logger;

/**
 * Serves to replicate a {@code javax.media.protocol.PushBufferDataSource} so that it may be
 * consumed by multiple destinations at varying time intervals without starting or stopping
 * the underlying data source.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class PBDSReplicator implements BufferTransferHandler {

    static Logger _logger = Logger.getLogger(PBDSReplicator.class);

    static final Object[] EMPTY_CONTROLS_ARRAY = new Object[0];

    List<PBDS> _destinations = new ArrayList<PBDS>();
    PushBufferDataSource _pbds;
    AudioFormat _format = null;

    /**
     * TODOC
     * @param pbds a data source to be replicated.
     * @throws IllegalArgumentException if the data source provided has no audio streams.
     */
    public PBDSReplicator(PushBufferDataSource pbds) throws IllegalArgumentException {

        _pbds = pbds;

        PushBufferStream[] pbStreams = _pbds.getStreams();

        if (pbStreams.length < 1) {
            throw new IllegalArgumentException("No streams found in provided data source!");
        }

        if (_logger.isDebugEnabled()) {
            if (pbStreams.length == 1) {
                ContentDescriptor cd = pbStreams[0].getContentDescriptor();
                _logger.debug("stream details: contentType=" + cd.getContentType() + ", format=" + pbStreams[0].getFormat());
            } else {
                _logger.debug("Only first audio stream handled, total streams received: " + pbStreams.length);
                for (int i = 0; i < pbStreams.length; i++) {
                    ContentDescriptor cd = pbStreams[i].getContentDescriptor();
                    _logger.debug("stream " + i + " details: contentType=" + cd.getContentType() + ", format=" + pbStreams[i].getFormat());
                }
            }
        }

        for (int i = 0; i < pbStreams.length; i++) {
            Format format = pbStreams[i].getFormat();
            if (format instanceof AudioFormat) {
                _format = (AudioFormat) format;
                pbStreams[i].setTransferHandler(this);
                break;
            }
        }

        if (_format == null) {
            throw new IllegalArgumentException("No audio streams found in provided data source!");
        }

//        _pbds.connect();
//        _pbds.start();
    }

    /**
     * @return the format of the underlying PushBufferDataSource
     */
    public AudioFormat getAudioFormat() {
        return _format;
    }
    
    public void removeReplicator(PushBufferDataSource pbds) {
    	_destinations.remove(pbds);
    }
    

    /**
     * Replicates the underlying data source.
     * @return replication of the underlying data source.
     */
    public PushBufferDataSource replicate() {
        PBDS pbds = new PBDS();
        synchronized (_destinations) {
            _destinations.add(pbds);
        }
        _logger.debug("destination count now: "+_destinations.size());
        return pbds;
    }

    /* (non-Javadoc)
     * @see javax.media.protocol.BufferTransferHandler#transferData(javax.media.protocol.PushBufferStream)
     */
    public void transferData(PushBufferStream pbs) {
        _logger.trace("transferData()");
        
        Buffer buffer = new Buffer();
        IOException ioe = null;

        try {
            pbs.read(buffer);
        } catch (IOException e) {
            ioe = e;
            _logger.debug(e, e);
        }
        
        synchronized (_destinations) {
        	_logger.trace("num destinations: "+_destinations.size());
            if (_destinations.size() > 0) {
                for (PBDS pbds : _destinations) {
                    pbds.newData(buffer, ioe);
                }
            } else if (_logger.isDebugEnabled()) {
                _logger.debug("transferData called with no destinations registered!");
            }
        }
    }

    /**
     * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
     *
     */
    private class PBDS extends PushBufferDataSource implements PushBufferStream {

        private BufferTransferHandler _bufferTransferHandler;
        private Buffer _buffer;
        private IOException _ioe;
        private volatile boolean _started = false;

        /**
         * @param buffer
         * @param ioe
         */
        public void newData(Buffer buffer, IOException ioe) {
            if (_started) {
                _logger.trace("newData()");
                synchronized (this) {
                    _buffer = buffer;
                    _ioe = ioe;
                    if (_bufferTransferHandler != null) {
                        _bufferTransferHandler.transferData(this);
                    }
                }
            }
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferDataSource#getStreams()
         */
        @Override
        public PushBufferStream[] getStreams() {
            _logger.debug("getStreams()");
            return new PushBufferStream[] {this};
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getContentType()
         */
        @Override
        public String getContentType() {
            _logger.debug("getContentType()");
            return _pbds.getContentType();
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#connect()
         */
        @Override
        public void connect() {
            _logger.debug("connect()");
            // do nothing, base source is already connected
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#disconnect()
         */
        @Override
        public void disconnect() {
            _logger.debug("disconnect()");
            _started = false;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#start()
         */
        @Override
        public void start() {
            _logger.debug("start()");
            _started = true;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#stop()
         */
        @Override
        public void stop() {
            _logger.debug("stop()");
            _started = false;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getControl(java.lang.String)
         */
        @Override
        public Object getControl(String controlType) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("getControl() request received: controlType=" + controlType);
            }
            return _pbds.getControl(controlType);
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getControls()
         */
        @Override
        public Object[] getControls() {
            _logger.debug("getControls()");
            //return EMPTY_CONTROLS_ARRAY;
            return _pbds.getControls();
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.DataSource#getDuration()
         */
        @Override
        public Time getDuration() {
            _logger.debug("getDuration()");
            return Duration.DURATION_UNKNOWN;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#getFormat()
         */
        public Format getFormat() {
            _logger.debug("getFormat()");
            return _format;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#read(javax.media.Buffer)
         */
        public void read(Buffer buffer) throws IOException {
            _logger.trace("read()");
            synchronized (this) {
                if (_ioe != null) {
                    throw _ioe;
                }
                _logger.trace("readig buffer of length: "+_buffer.getLength());
                buffer.copy(_buffer);
            }
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.PushBufferStream#setTransferHandler(javax.media.protocol.BufferTransferHandler)
         */
        public void setTransferHandler(BufferTransferHandler bufferTransferHandler) {
            if (_logger.isDebugEnabled()) {
                if (bufferTransferHandler == null) {
                    _logger.debug("setTransferHandler(): bufferTransferHandler is null!");
                } else {
                    _logger.debug("setTransferHandler(): bufferTransferHandler.class=" + bufferTransferHandler.getClass());
                }
            }

            if (bufferTransferHandler != null) { // TODO: this is a hack to avoid deadlock when processor is closing
                //synchronized (this) {
                    _bufferTransferHandler = bufferTransferHandler;
                //}
            }
            _logger.debug("DONE set settransferhandler()...");
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#getContentDescriptor()
         */
        public ContentDescriptor getContentDescriptor() {
            _logger.debug("getContentDescriptor()");
            return new ContentDescriptor(_pbds.getContentType());
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#getContentLength()
         */
        public long getContentLength() {
            _logger.debug("getContentLength()");
            return LENGTH_UNKNOWN;
        }

        /* (non-Javadoc)
         * @see javax.media.protocol.SourceStream#endOfStream()
         */
        public boolean endOfStream() {
            _logger.debug("endOfStream()");
            synchronized (this) {
                return (_buffer != null && _buffer.isEOM());
            }
        }

    }

}

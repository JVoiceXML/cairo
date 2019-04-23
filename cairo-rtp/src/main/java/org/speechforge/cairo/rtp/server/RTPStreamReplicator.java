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

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;

import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.rtp.RecorderMediaClient;

import org.speechforge.cairo.jmf.ProcessorStarter;

import java.io.IOException;
import java.net.InetAddress;

import javax.media.Format;
import javax.media.Manager;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;

import org.apache.log4j.Logger;

/**
 * Serves to replicate an incoming RTP audio stream so that it may be consumed by multiple
 * destinations at varying time intervals without starting or stopping the underlying data
 * source.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPStreamReplicator extends RTPConsumer {

    private static Logger _logger = Logger.getLogger(RTPStreamReplicator.class);

    private PBDSReplicator _replicator;
    private Processor _processor;
    private RecorderMediaClient recorder;
    private int _port;
    

    public RTPStreamReplicator(int port) throws IOException {
        super(port);
        _port = port;
    }
    
    public RTPStreamReplicator(InetAddress localAddress, int port) throws IOException {
        super(localAddress,port);
        _port = port;
    }
    
    /**
     * TODOC
     * @return Returns the port.
     */
    public int getPort() {
        return _port;
    }
    
    public void removeReplicant(PushBufferDataSource pbds) {
    	_replicator.removeReplicator(pbds);
    	
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#shutdown()
     */
    @Override
    public void shutdown() {
    	if (_processor != null) {
    		_processor.close();
    		_processor = null;
    	}
    	if (_replicator != null) 
    	   _replicator = null;
    	//_replicator.cleanup();
        //super.shutdown();
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamReceived(javax.media.rtp.ReceiveStream, javax.media.protocol.PushBufferDataSource)
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource, Format[] preferredFormats) {
        if (_replicator == null) {
            try {
                ProcessorModel pm = new ProcessorModel(
                        dataSource, preferredFormats, CONTENT_DESCRIPTOR_RAW);
                try {
                    _logger.debug("Creating realized processor...");
                    _processor = Manager.createRealizedProcessor(pm);
                    _processor.addControllerListener(new ProcessorStarter());
                } catch (IOException e){
                    throw e;
                } catch (javax.media.CannotRealizeException e){
                    throw (IOException) new IOException(e.getMessage()).initCause(e);
                } catch (javax.media.NoProcessorException e){
                    throw (IOException) new IOException(e.getMessage()).initCause(e);
                }

                _logger.debug("Internal Processor realized.");

                PushBufferDataSource pbds = (PushBufferDataSource) _processor.getDataOutput();
                _replicator = new PBDSReplicator(pbds);
                _processor.start();
                this.notifyAll();
            } catch (IOException e) {
                _processor = null;
                _replicator = null;  // TODO: close properly
                _logger.warn(e, e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamMapped(javax.media.rtp.ReceiveStream, javax.media.rtp.Participant)
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // ignore
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamInactive(javax.media.rtp.ReceiveStream, boolean)
     */
    @Override
    public synchronized void streamInactive(ReceiveStream stream, boolean byeEvent) {
        //if (byeEvent) {

            //_replicator.shutdown();
            _replicator = null; // TODO: close data source properly, make sure this triggers EndOfStreamEvent in replicated PBDS
            if (_processor != null) {
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Closing RTP processor for SSRC=" + stream.getSSRC());
                }
                _processor.close();
                _processor = null;
                if (_logger.isDebugEnabled()) 
                   recorder.streamInactive(null,false);
            }
        //}
    }

    /**
     * TODOC
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @param maxWait the maximum time to wait in milliseconds if the stream has not yet been received.
     * @param preferredMediaFormats 
     * @return A new <code>Processor</code> that is in the <code>Realized</code> state.
     * @throws IOException if there are I/O problems creating the processor from the stream.
     * @throws IllegalStateException if the stream has not been received yet, and is not received within the maximum time to wait.
     */
    public synchronized ProcessorReplicatorPair createRealizedProcessor(ContentDescriptor outputContentDescriptor, long maxWait, Format[] preferredMediaFormats)
      throws IOException, IllegalStateException {

        if (_replicator == null) {
            if (maxWait >= 0) {
                try {
                    this.wait(maxWait); //TODO: make sure timeout period has passed
                } catch (InterruptedException e) {
                    // TODO: throw this exception?
                    _logger.warn(e, e);
                }
            }
            if (_replicator == null) {
                throw new IllegalStateException("No RTP stream yet received!");
            }
        }


        PushBufferDataSource pbds = _replicator.replicate();
        ProcessorModel pm = new ProcessorModel(
        		pbds, preferredMediaFormats, outputContentDescriptor);
        Processor processor =null;
        try {
            _logger.debug("Creating realized processor...");
            processor = Manager.createRealizedProcessor(pm);
            _logger.debug("Done Creating realized processor...");
        } catch (IOException e){
        	e.printStackTrace();
           // throw e;
        } catch (javax.media.CannotRealizeException e){
           	e.printStackTrace();
            //throw (IOException) new IOException(e.getMessage()).initCause(e);
        } catch (javax.media.NoProcessorException e){
           	e.printStackTrace();
           //throw (IOException) new IOException(e.getMessage()).initCause(e);
        }

        /*if (_logger.isDebugEnabled()) {
            try {
                recorder = new RecorderMediaClient(_replicator.replicate());
            } catch (NoPlayerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (CannotRealizeException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }*/

        _logger.debug("Processor realized.");

        return new ProcessorReplicatorPair(processor,pbds);

    }

    public class ProcessorReplicatorPair {
    	public ProcessorReplicatorPair(Processor proc, PushBufferDataSource pbds) {
	        super();
	        this.proc = proc;
	        this.pbds = pbds;
        }
		/**
         * @return the proc
         */
        public Processor getProc() {
        	return proc;
        }
		/**
         * @param proc the proc to set
         */
        public void setProc(Processor proc) {
        	this.proc = proc;
        }
		/**
         * @return the pbds
         */
        public PushBufferDataSource getPbds() {
        	return pbds;
        }
		/**
         * @param pbds the pbds to set
         */
        public void setPbds(PushBufferDataSource pbds) {
        	this.pbds = pbds;
        }
		private Processor proc;
    	private PushBufferDataSource pbds;
    }

}

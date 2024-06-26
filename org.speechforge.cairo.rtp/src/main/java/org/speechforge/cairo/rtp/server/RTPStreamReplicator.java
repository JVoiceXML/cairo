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

import javax.media.CannotRealizeException;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoProcessorException;
import javax.media.NotRealizedError;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Serves to replicate an incoming RTP audio stream so that it may be consumed
 * by multiple destinations at varying time intervals without starting or
 * stopping the underlying data source.
 *
 * @author Niels Godfredsen {@literal <}<a href=
 *         "mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class RTPStreamReplicator extends RTPConsumer {
    /** Logger instance. */
    private static final Logger LOGGER =
            LogManager.getLogger(RTPStreamReplicator.class);

    /** The replicator that replicates the incoming stream. */
    private PBDSReplicator replicator;
    /** The processor that processes the replicated stream. */
    private Processor processor;
    /** The recorder that records the replicated stream. */
    private RecorderMediaClient recorder;
    /** The port that this replicator is listening on. */
    private int port;
    
    /**
     * Creates a new RTP stream replicator that listens on the given port.
     * 
     * @param replicatorPort
     *            the port to listen on
     * @throws IOException
     *             if the replicator could not be created
     */
    public RTPStreamReplicator(int replicatorPort) throws IOException {
        super(replicatorPort);
        port = replicatorPort;
    }
    
    /**
     * Creates a new RTP stream replicator that listens on the given port.
     * 
     * @param localAddress
     *            the local address to bind to
     * @param replicatorPort
     *            the port to listen on
     * @throws IOException
     *             if the replicator could not be created
     */
    public RTPStreamReplicator(InetAddress localAddress, int replicatorPort) 
            throws IOException {
        super(localAddress, replicatorPort);
        port = replicatorPort;
    }
    
    /**
     * Retrieves the port that this replicator is listening on.
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Removes a replicant from the list of replicants.
     * @param pbds the data source to remove
     */
    public void removeReplicant(PushBufferDataSource pbds) {
        if (replicator != null) {
            replicator.removeReplicator(pbds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
        if (replicator != null) {
            replicator = null;
        }
        //_replicator.cleanup();
        super.shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, 
            PushBufferDataSource dataSource, Format[] preferredFormats) {
        LOGGER.info("Stream received for SSRC=" + stream.getSSRC());
        if (replicator == null) {
            createNewReplicator(dataSource, preferredFormats);
        }
    }

    /**
     * Creates a new replicator for the given data source.
     * 
     * @param dataSource
     *            the data source to replicate
     * @param preferredFormats
     *            the preferred formats
     * @throws NotRealizedError
     *             if the processor could not be realized
     */
    private void createNewReplicator(PushBufferDataSource dataSource,
            Format[] preferredFormats) throws NotRealizedError {
        try {
            ProcessorModel pm = new ProcessorModel(
                    dataSource, preferredFormats, CONTENT_DESCRIPTOR_RAW);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating realized processor...");
            }
            processor = Manager.createRealizedProcessor(pm);
            final ControllerListener listener = new ProcessorStarter();
            processor.addControllerListener(listener);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Internal Processor realized.");
            }

            final PushBufferDataSource pbds = 
                    (PushBufferDataSource) processor.getDataOutput();
            replicator = new PBDSReplicator(pbds);
            processor.start();
            this.notifyAll();
        } catch (IOException | NoProcessorException | CannotRealizeException e) {
            processor = null;
            replicator = null;  // TODO: close properly
            LOGGER.warn(e, e);
            final NotRealizedError nre = new NotRealizedError(
                    "Could not create processor: " + e.getMessage());
            nre.initCause(e);
            throw nre;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void streamInactive(ReceiveStream stream, boolean byeEvent) {
        //if (byeEvent) {

        //_replicator.shutdown();
        replicator = null; 
        // TODO: close data source properly, make sure this triggers
        // EndOfStreamEvent in replicated PBDS
        if (processor != null) {
            LOGGER.info("Stream deactivated for SSRC=" + stream.getSSRC());
            processor.close();
            processor = null;
            if (LOGGER.isDebugEnabled()) {
                if (recorder != null) {
                    recorder.streamInactive(null, false);
                }
            }
        }
        //}
    }

    /**
     * TODOC
     * @param outputContentDescriptor A <code>ContentDescriptor</code> that describes the desired output content-type.
     * @param maxWait the maximum time to wait in milliseconds if the stream has not yet been received.
     * @param preferredMediaFormats the preferred media formats
     * @return A new <code>Processor</code> that is in the <code>Realized</code> state.
     * @throws IOException if there are I/O problems creating the processor from the stream.
     * @throws IllegalStateException if the stream has not been received yet, and is not received within the maximum time to wait.
     */
    public synchronized ProcessorReplicatorPair createRealizedProcessor(
            ContentDescriptor outputContentDescriptor, long maxWait, 
            Format[] preferredMediaFormats)
                    throws IOException, IllegalStateException {
        if (replicator == null) {
            if (maxWait >= 0) {
                try {
                    this.wait(maxWait); //TODO: make sure timeout period has passed
                } catch (InterruptedException e) {
                    // TODO: throw this exception?
                    LOGGER.warn(e, e);
                }
            }
            if (replicator == null) {
                throw new IllegalStateException("No RTP stream yet received!");
            }
        }


        PushBufferDataSource pbds = replicator.replicate();
        ProcessorModel pm = new ProcessorModel(
        		pbds, preferredMediaFormats, outputContentDescriptor);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creatin realized processor...");
        }
        Processor processor = null;
        try {
            processor = Manager.createRealizedProcessor(pm);
        } catch (IOException | javax.media.CannotRealizeException | javax.media.NoProcessorException e){
            LOGGER.warn(e.getMessage(), e);
        }

        /*if (_logger.isDebugEnabled()) {
            try {
                recorder = new RecorderMediaClient(_replicator.replicate());
            } catch (NoPlayerException e) {
                // TODO Auto-generated catch block
                LOGGER.warn(e.getMessage(), e);
            } catch (CannotRealizeException e) {
                // TODO Auto-generated catch block
                LOGGER.warn(e.getMessage(), e);
            }
        }*/
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Processor realized.");
        }
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

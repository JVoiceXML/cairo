package org.speechforge.cairo.rtp.server;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW;

import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.rtp.RecorderMediaClient;

import org.speechforge.cairo.jmf.ProcessorStarter;

import java.io.IOException;

import javax.media.Format;
import javax.media.Manager;

import javax.media.Processor;
import javax.media.ProcessorModel;

import javax.media.protocol.PushBufferDataSource;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Serves to replicate an incoming RTP audio stream so that it may be consumed by multiple
 * destinations at varying time intervals without starting or stopping the underlying data
 * source.
 * <p>
 * This class manages a JMF processor to handle incoming RTP streams and allows for recording
 * and consumption by multiple clients. It provides access to the listening port and the underlying
 * processor, and handles stream lifecycle events such as receiving, mapping, and inactivation.
 * </p>
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class RTPStreamReader extends RTPConsumer {
    /** Logger instance. */
    private static final Logger LOGGER = 
            LogManager.getLogger(RTPStreamReader.class);

    /**
     * The processor that is used to replicate the incoming RTP stream.
     */
    private Processor processor;
    /**
     * The recorder that is used to record the incoming RTP stream.
     */
    private RecorderMediaClient recorder;
    /**
     * The port that this RTPStreamReader is listening on.
     */
    private int port;

    /**
     * Creates a new RTPStreamReader instance.
     *
     * @param portNumber the port that this RTPStreamReader is listening on
     * @throws IOException if an I/O error occurs during initialization
     */
    public RTPStreamReader(int portNumber) throws IOException {
        super(portNumber);
        port = portNumber;
    }
    
    /**
     * Retrieves the port that this RTPStreamReader is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the JMF processor used for stream replication.
     *
     * @return the JMF {@link Processor} instance, or {@code null} if not initialized
     */
    public Processor getProcessor() {
    	return processor;
    }
    
    /**
     * Shuts down the RTPStreamReader and releases resources.
     * Closes the processor if it is active.
     */
    @Override
    public void shutdown() {
        if (processor != null) {
            processor.close();
            processor = null;
        }
    }

    /**
     * Handles an incoming RTP stream and initializes the JMF processor.
     *
     * @param stream the received RTP stream
     * @param dataSource the data source for the stream
     * @param preferredFormats the preferred media formats
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, 
            PushBufferDataSource dataSource, Format[] preferredFormats) {
            try {
                ProcessorModel pm = new ProcessorModel(
                        dataSource, preferredFormats, CONTENT_DESCRIPTOR_RAW);
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Creating recorder...");
                    }
                    processor = Manager.createRealizedProcessor(pm);
                    processor.addControllerListener(new ProcessorStarter());
                } catch (IOException e){
                    throw e;
                } catch (javax.media.CannotRealizeException 
                        | javax.media.NoProcessorException e){
                    throw new IOException(e.getMessage(), e);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Realized processor created.");
                }

                PushBufferDataSource pbds = 
                        (PushBufferDataSource) processor.getDataOutput();

                processor.start();
                this.notifyAll();
            } catch (IOException e) {
                LOGGER.warn(e, e);
            }
    }

    /**
     * Called when a stream is mapped to a participant. This implementation ignores the event.
     *
     * @param stream the received RTP stream
     * @param participant the RTP participant
     */
    @Override
    public void streamMapped(ReceiveStream stream, Participant participant) {
        // ignore
    }

    /**
     * Handles the event when a stream becomes inactive.
     * Closes the processor and notifies the recorder if present.
     *
     * @param stream the received RTP stream
     * @param byeEvent {@code true} if the stream ended with a BYE event, {@code false} otherwise
     */
    @Override
    public synchronized void streamInactive(ReceiveStream stream, boolean byeEvent) {
        if (processor != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Closing RTP processor for SSRC=" + stream.getSSRC());
            }
            processor.close();
            processor = null;
            if (LOGGER.isDebugEnabled()) 
               recorder.streamInactive(null,false);
        }
    }
}
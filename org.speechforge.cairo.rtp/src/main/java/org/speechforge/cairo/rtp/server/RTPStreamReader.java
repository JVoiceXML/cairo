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
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class RTPStreamReader extends RTPConsumer {
    /** Logger instance. */
    private static final Logger LOGGER = 
            LogManager.getLogger(RTPStreamReader.class);

    /** The processor that is used to replicate the incoming RTP stream. */
    private Processor processor;
    /** The recorder that is used to record the incoming RTP stream. */
    private RecorderMediaClient recorder;
    /** The port that this RTPStreamReader is listening on. */
    private int port;

    /**
     * Creates a new RTPStreamReader instance.
     * 
     * @param portNumber
     *            The port that this RTPStreamReader is listening on.
     * @throws IOException
     *             If an I/O error occurs.
     */
    public RTPStreamReader(int portNumber) throws IOException {
        super(portNumber);
        port = portNumber;
    }
    
    /**
     * Retrieves the port that this RTPStreamReader is listening on.
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Retrieves the JMF processor.
     * @return
     */
    public Processor getProcessor() {
    	return processor;
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
    }

    /**
     * {@inheritDoc}
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

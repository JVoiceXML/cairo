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

import org.apache.log4j.Logger;

/**
 * Serves to replicate an incoming RTP audio stream so that it may be consumed by multiple
 * destinations at varying time intervals without starting or stopping the underlying data
 * source.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPStreamReader extends RTPConsumer {

    private static Logger _logger = Logger.getLogger(RTPStreamReader.class);

  
    private Processor _processor;
    private RecorderMediaClient recorder;
    private int _port;

    public RTPStreamReader(int port) throws IOException {
        super(port);
        _port = port;
    }
    
    /**
     * TODOC
     * @return Returns the port.
     */
    public int getPort() {
        return _port;
    }

    public Processor getProcessor() {
    	return _processor;
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

    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.rtp.RTPConsumer#streamReceived(javax.media.rtp.ReceiveStream, javax.media.protocol.PushBufferDataSource)
     */
    @Override
    public synchronized void streamReceived(ReceiveStream stream, PushBufferDataSource dataSource, Format[] preferredFormats) {

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

                _processor.start();
                this.notifyAll();
            } catch (IOException e) {

                _logger.warn(e, e);
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

            if (_processor != null) {
                if (_logger.isDebugEnabled()) {
                    _logger.debug("Closing RTP processor for SSRC=" + stream.getSSRC());
                }
                _processor.close();
                _processor = null;
                if (_logger.isDebugEnabled()) 
                   recorder.streamInactive(null,false);
            }
    }

}

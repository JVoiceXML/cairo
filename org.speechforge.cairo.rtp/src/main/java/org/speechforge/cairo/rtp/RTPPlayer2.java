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
package  org.speechforge.cairo.rtp;

import static org.speechforge.cairo.jmf.JMFUtil.CONTENT_DESCRIPTOR_RAW_RTP;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.media.Buffer;
import javax.media.CannotRealizeException;
import javax.media.Codec;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Processor;
import javax.media.Time;
import javax.media.UnsupportedPlugInException;

import javax.media.control.PacketSizeControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;

import org.apache.log4j.Logger;
import org.speechforge.cairo.util.CairoUtil;


/**
 * Handles playing of audio prompt files over an RTP output stream.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPPlayer2 implements ControllerListener {

 
    private static Logger _logger = Logger.getLogger(RTPPlayer2.class);

    private Object _lock = new Object();
    private Processor _processor;

    private RTPManager _rtpManager;
    private SendStream _sendStream;
    private DataSource _dataSource;
    private SessionAddress _targetAddress;
    private AudioFormats _af;
    private PBDS _pbds;

    public RTPPlayer2(int localPort, InetAddress remoteAddress, int remotePort, AudioFormats af)
      throws InvalidSessionAddressException, IOException {

      SessionAddress localAddress = new SessionAddress(CairoUtil.getLocalHost(), localPort);
       _targetAddress = new SessionAddress(remoteAddress, remotePort);
      _rtpManager = RTPManager.newInstance();
      _rtpManager.initialize(localAddress);
      _rtpManager.addTarget(_targetAddress);
      _af = af;
      _pbds = new PBDS();
      
    }

    public RTPPlayer2(RTPManager rtpManager) {
        _rtpManager = rtpManager;
        _af = new AudioFormats();
    }

    public void playPrompt(File promptFile) throws InterruptedException, IllegalStateException, IllegalArgumentException {
        if (promptFile != null && promptFile.exists()) {
            try {
                MediaLocator source = new MediaLocator(promptFile.toURL());
                playSource(source);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Specified prompt file does not exist: " + promptFile);
        }
    }

    public void playSource(MediaLocator source) throws InterruptedException, IllegalStateException {
        try {
            synchronized(this) {
                if (_processor != null) {
                    throw new IllegalStateException("Attempt to call playPrompt() when prompt already playing!");
                }
                _dataSource = Manager.createDataSource(source);
                _processor = Manager.createProcessor(_dataSource);
                _processor.addControllerListener(this);

            }

            configure();
                
            program();
     
            realize();

            play(); 

        } catch (InterruptedException e) {
            _logger.debug("playSource() interrupted, closing processor...");
            try {
                close();
            } catch (InterruptedException ie) {
                // TODO Auto-generated catch block
                _logger.debug(ie, ie);
            }
            throw e;
        } catch (Exception e) {
            _logger.warn("playSource(): encountered unexpected exception: ", e);
            try {
                close();
            } catch (InterruptedException ie) {
                // TODO Auto-generated catch block
                _logger.debug(ie, ie);
            }
            throw new RuntimeException("playSource() encountered unexpected exception", e);
        }

    }
    
    private void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }

    private void configure() throws IOException, InterruptedException {
        synchronized (_lock) {
            _processor.configure();
            while(_processor != null && _processor.getState() < Processor.Configured) {
                checkInterrupted();
                _lock.wait();
            }
            if (_processor == null) {
                throw new IOException("Processor closed unexpectedly!");
            }
        }
    }

    private void program() throws UnsupportedFormatException {

        TrackControl[] trackControls = _processor.getTrackControls();
        int tracks = (trackControls == null) ? -1 : trackControls.length;
        if (tracks != 1 || !trackControls[0].isEnabled()) {
            throw new UnsupportedFormatException("Cannot handle track count: " + tracks, null);
        }

        _processor.setContentDescriptor(CONTENT_DESCRIPTOR_RAW_RTP);

        Format[] supported = trackControls[0].getSupportedFormats();
       
        int formats = (supported == null) ? -1 : supported.length;
        if (formats < 1) {
            throw new UnsupportedFormatException(
                    "No supported formats found: " + formats, trackControls[0].getFormat());
        }
        
        ContentDescriptor c = _processor.getContentDescriptor();
        //System.out.println("Content Descriptor: "+c.toString());
        boolean foundOne = false;
        for (int i=0; i< supported.length; i++) {

            //System.out.println("FORMAT# "+i+" "+supported[i].toString());
            //System.out.println("FORMAT# "+i+" "+supported[i].getEncoding());
            if (_af.isSupported(supported[i])) {
                trackControls[0].setFormat(supported[i]);
                foundOne = true;
                break;
           }
        }
        
        
        Codec codec[] = new Codec[3];

        codec[0] = new com.ibm.media.codec.audio.rc.RCModule();
        codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
        codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
        ((com.sun.media.codec.audio.ulaw.Packetizer) codec
                [2]).setPacketSize(160);

        try {
            trackControls[0].setCodecChain(codec);
        }
        catch (UnsupportedPlugInException e) {
            e.printStackTrace();
        } 
          
        
        if (!foundOne) {
            throw new UnsupportedFormatException(
                    "No supported formats found: " + formats, trackControls[0].getFormat());
        }
    }
    
    /**
     * Get the best packet size for a given codec and a codec rate
     *
     * @param codecFormat
     * @param milliseconds
     * @return
     * @throws IllegalArgumentException
     */
    private int getPacketSize(Format codecFormat, int milliseconds) throws IllegalArgumentException {
        String encoding = codecFormat.getEncoding();
        if (encoding.equalsIgnoreCase(AudioFormat.GSM) ||
                encoding.equalsIgnoreCase(AudioFormat.GSM_RTP)) {
            return milliseconds * 4; // 1 byte per millisec
        }
        else if (encoding.equalsIgnoreCase(AudioFormat.ULAW) ||
                encoding.equalsIgnoreCase(AudioFormat.ULAW_RTP)) {
            return milliseconds * 8;
        }
        else {
            throw new IllegalArgumentException("Unknown codec type");
        }
    }
    

    private void realize() throws CannotRealizeException, InterruptedException {
        synchronized (_lock) {
           _processor.realize();
           while(_processor != null && _processor.getState() < Controller.Realized) {
               checkInterrupted();
               _lock.wait();
           }
           if (_processor == null) {
               throw new CannotRealizeException("Processor closed unexpectedly!");
           }           
        }
    }
    
    public void setPacketSize(PacketSizeControl pktCtrl) {
        TrackControl[] trackControls = _processor.getTrackControls();
        int packetRate = 20;
        //PacketSizeControl pktCtrl = (PacketSizeControl) c.getControl(PacketSizeControl.class.getName());
       
        if (pktCtrl != null) {
            System.out.println("The track control packet size was: "+pktCtrl.getPacketSize());
            try {
                pktCtrl.setPacketSize(getPacketSize(trackControls[0].getFormat(), packetRate));
                System.out.println("   ... but now it is: "+pktCtrl.getPacketSize());
            }
            catch (IllegalArgumentException e) {
                pktCtrl.setPacketSize(80);
                e.printStackTrace();
            }

        } else {
            System.out.println("Null packet controller!");
        } 
        
        if (trackControls[0].getFormat().getEncoding().equals(AudioFormat.ULAW_RTP)) {
            Codec codec[] = new Codec[3];

            codec[0] = new com.ibm.media.codec.audio.rc.RCModule();
            codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
            codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
            ((com.sun.media.codec.audio.ulaw.Packetizer) codec
                    [2]).setPacketSize(160);

            try {
                trackControls[0].setCodecChain(codec);
            }
            catch (UnsupportedPlugInException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Not ULAW_RTP.  Format is: "+trackControls[0].getFormat().getEncoding());
        }
    }

    private void play() throws UnsupportedFormatException, IOException, InterruptedException {
        synchronized (_lock) {
    
            DataSource dataOutput = _processor.getDataOutput();
            _pbds.setDataSource(_processor.getDataOutput());
            if (_sendStream == null) {
                try {
                    _sendStream = _rtpManager.createSendStream(_pbds, 0);
                 } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                 }
                _sendStream.start();
            }
            //_logger.debug("init(): Waiting 5 seconds for send stream to start...");
            //Thread.sleep(5000);
            _pbds.start();
            _processor.start();
    
            do {
                checkInterrupted();
                _lock.wait();

            } while(_processor != null);

            if (_dataSource != null) {
                _dataSource.disconnect();
                _dataSource=null;
                _pbds.stop();
            }
            //if (_sendStream != null) {
            //    _sendStream.close();
            //    _sendStream = null;
            //}     
            _logger.debug("play(): completed successfully.");
        }
    }
    
    private void close() throws InterruptedException {
        synchronized (_lock) {
            if (_processor != null && _processor.getState() > Processor.Configured) {
                _processor.close();

                do {
                    try {
                        _lock.wait();
                    } catch (InterruptedException e) {
                        _processor.removeControllerListener(this);
                        _processor = null;
                        throw e;
                    }
                } while(_processor != null);
                try {
                    _pbds.stop();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
     */
    public void controllerUpdate(ControllerEvent event) {
        synchronized (_lock) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("ControllerEvent received: " + event);
            }
    
            if (event instanceof EndOfMediaEvent) {
                event.getSourceController().close();
            } else if (event instanceof ControllerClosedEvent) {
                event.getSourceController().removeControllerListener(this);                
                _processor.close();
                _processor = null;
                try {
                    _pbds.stop();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            _lock.notifyAll();
        }
    }

    public void shutdown() {
        _rtpManager.removeTargets("Disconnecting...");
        _rtpManager.dispose();
        _rtpManager = null;
        try {
            _pbds.stop();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
   private class PBDS extends PushBufferDataSource implements PushBufferStream {
        
        DataSource _ds;
        PushBufferStream[] streams;
        BlockingQueue<Buffer> _bufferQueue = new LinkedBlockingQueue<Buffer>();
        private ReadThread _readThread;
        BufferTransferHandler _bufferTransferHandler;
        Boolean endOfStream = false;
        
        public void setDataSource(DataSource source) {
            _ds =  source;
            try {
                _ds.connect();
                streams = ((PushBufferDataSource)_ds).getStreams();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    
        @Override
        public PushBufferStream[] getStreams() {
            // TODO Auto-generated method stub
            PushBufferStream[] pbs= new PushBufferStream[1];
            pbs[0] = this;
            return pbs;
        }
    
        @Override
        public void connect() throws IOException {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public void disconnect() {
            // TODO Auto-generated method stub
            
        }
    
        @Override
        public String getContentType() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Object getControl(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Object[] getControls() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public Time getDuration() {
            // TODO Auto-generated method stub
            return null;
        }
    
        @Override
        public void start() throws IOException {
            _ds.start();
            (_readThread = new ReadThread()).start();
            _readThread.setPBDS(this);
            endOfStream = false;
        }
    
        @Override
        public void stop() throws IOException {
            if (_readThread == null) {
                _logger.warn("Trying to stop push buffer data source but the read but thread is null");
            } else {
              _readThread.shutdown();
            }
            if (_ds == null) {
                _logger.warn("Trying to stop push buffer data source but the input data source is already null");  
            } else {
              _ds.stop();
              _ds = null;
              streams = null;
            }
        }
    
        public Format getFormat() {
            // TODO Auto-generated method stub
            return streams[0].getFormat();
        }
    
        public void read(Buffer outBuffer) throws IOException {
            try {
                Buffer b = _bufferQueue.take();
                if (b.isEOM()) {
                    endOfStream = true;
                }
                //System.out.println("read a buffer: "+b.getLength()+" "+b.getTimeStamp());
                outBuffer.copy(b);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    
        public void setTransferHandler(BufferTransferHandler bufferTransferHandler) {
            if (_logger.isDebugEnabled()) {
                if (bufferTransferHandler == null) {
                    _logger.debug("setTransferHandler(): bufferTransferHandler is null!");
                } else {
                    _logger.debug("setTransferHandler(): bufferTransferHandler.class=" + bufferTransferHandler.getClass());
                }
            }

            if (bufferTransferHandler != null) { // TODO: this is a hack to avoid deadlock when processor is closing
                synchronized (this) {
                    _bufferTransferHandler = bufferTransferHandler;
                }
            }
            
        }
    
        public boolean endOfStream() {
            // TODO Auto-generated method stub
            return endOfStream;
        }
    
        public ContentDescriptor getContentDescriptor() {
            // TODO Auto-generated method stub
            return null;
        }
    
        public long getContentLength() {
            // TODO Auto-generated method stub
            return 0;
        }
    
   private class ReadThread extends Thread {
       
       volatile boolean _run = true;
       
       PBDS _pbds;
       
       public void setPBDS(PBDS p) {
           _pbds = p;
       }
       

       /* (non-Javadoc)
        * @see java.lang.Runnable#run()
        */
       @Override
       public void run() {
           /*if (Thread.currentThread() != this) {
               throw new RuntimeException();
           }*/
           while (_run) {
               boolean drainQueue = false;
               Exception cause = null;

               try {

                   // first clear interrupted status of current thread
                   Thread.interrupted();

                   
                   //get the next chick of data
                   Buffer b = new Buffer();
                   streams[0].read(b);
                   _bufferQueue.add(b);
                   if (b.isEOM()) {
                       _run = false;
                   }
                   
                   if (_bufferTransferHandler != null) {
                       _bufferTransferHandler.transferData(_pbds);
                   }
                   
                   // drain all prompts in queue if current prompt playback is interrupted (e.g. by STOP request)
                   drainQueue = Thread.interrupted();

             //  } catch (InterruptedException e) {
             //      _logger.debug(e, e);
                   // TODO: cancel current prompt playback
             //      drainQueue = true;

               } catch (Exception e) {
                   _logger.debug(e, e);
                   cause = e;
               }

               if (drainQueue) {
                   _logger.debug("draining prompt queue...");
                   while (!_bufferQueue.isEmpty()) {
                       try {
                           _bufferQueue.take();
                           //TODO: may need to remove only specific prompts
                           // (e.g. save and put back in queue if not in cancel list)
                       } catch (InterruptedException e1) {
                           // should not happen since this is the only thread consuming from queue
                           _logger.warn(e1, e1);
                       }
                   }
               }

               //_state = _promptQueue.isEmpty() ? IDLE : SPEAKING;
           }
       }
       
       public void shutdown() {
           _run = false;
       }
   }

   }
    
}

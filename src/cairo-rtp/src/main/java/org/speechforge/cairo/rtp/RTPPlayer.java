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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.Vector;

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
import javax.media.PackageManager;
import javax.media.Processor;
import javax.media.UnsupportedPlugInException;
import javax.media.control.PacketSizeControl;
import javax.media.control.TrackControl;
//import javax.media.format.AudioFormat;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;
import org.speechforge.cairo.util.CustomDataSource;


/**
 * Handles playing of audio prompt files over an RTP output stream.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPPlayer implements ControllerListener {

    private static Logger _logger = Logger.getLogger(RTPPlayer.class);

    private Object _lock = new Object();
    private Processor _processor;

    private RTPManager _rtpManager;
    private SendStream _sendStream;
    //private CustomDataSource _dataSource;
    private DataSource _dataSource;
    private SessionAddress _targetAddress;
    private AudioFormats _af;

    public RTPPlayer(InetAddress localIpAddress, int localPort, InetAddress remoteAddress, int remotePort, AudioFormats af)
      throws InvalidSessionAddressException, IOException {

    	try {
 	       //SessionAddress localAddress = new SessionAddress(CairoUtil.getLocalHost(), localPort);
	       SessionAddress localAddress = new SessionAddress(localIpAddress, localPort);
	       _targetAddress = new SessionAddress(remoteAddress, remotePort);
	  	   _logger.debug("Constructing the RtpPlayer, localAddress: "+localAddress.toString() +" and remote address: "+ _targetAddress.toString());	  	   
	      _rtpManager = RTPManager.newInstance();
	      _rtpManager.initialize(localAddress);
	      _rtpManager.addTarget(_targetAddress);
	      _af = af;
    	}catch (InvalidSessionAddressException e) {
    		
    		e.printStackTrace();
    		throw e;
    	}catch (IOException e) {
    		
    		e.printStackTrace();
    		throw e;
    	}
      //registerDatasource();
    }

    public RTPPlayer(RTPManager rtpManager) {
        _rtpManager = rtpManager;
        _af = new AudioFormats();
        //registerDatasource();
    }
    
    private void registerDatasource() {
    	 Vector  packagePrefix = PackageManager.getProtocolPrefixList();
    	 String myPackagePrefix = new String("org.speechforge");
    	 // Add new package prefix to end of the package prefix list. 
    	 packagePrefix.addElement(myPackagePrefix);
    	 PackageManager.setProtocolPrefixList(packagePrefix);
    	 // Save the changes to the package prefix list.
    	 PackageManager.commitProtocolPrefixList();

    	
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
    	_logger.info("Source: " +source);
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
          
            TrackControl[] trackControls = _processor.getTrackControls();
            Codec codec[] = new Codec[3];
            codec[0] = new com.ibm.media.codec.audio.rc.RCModule();
            codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
            codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
            ((com.sun.media.codec.audio.ulaw.Packetizer) codec[2]).setPacketSize(160);
            
            try {
                trackControls[0].setCodecChain(codec);
            } catch (UnsupportedPlugInException e) {
                e.printStackTrace();
            }
                        
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
        	e.printStackTrace();
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
    
    public void playStream(InputStream stream, AudioFormat af) throws InterruptedException, IllegalStateException {
    	
    	javax.media.format.AudioFormat format= AudioFormats.convertToJmfFormat(af);
    	
        try {
            synchronized(this) {
                if (_processor != null) {
                    throw new IllegalStateException("Attempt to call playPrompt() when prompt already playing!");
                }
                _dataSource = new CustomDataSource(stream,format);
                _dataSource.connect();
                _processor = Manager.createProcessor(_dataSource);
                _processor.addControllerListener(this);
            }

            configure();
            
            program();
          
            TrackControl[] trackControls = _processor.getTrackControls();
            Codec codec[] = new Codec[3];
            codec[0] = new com.ibm.media.codec.audio.rc.RCModule();
            codec[1] = new com.ibm.media.codec.audio.ulaw.JavaEncoder();
            codec[2] = new com.sun.media.codec.audio.ulaw.Packetizer();
            ((com.sun.media.codec.audio.ulaw.Packetizer) codec[2]).setPacketSize(160);
            
            try {
                trackControls[0].setCodecChain(codec);
            } catch (UnsupportedPlugInException e) {
                e.printStackTrace();
            }
                        
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
        
        //ContentDescriptor c = _processor.getContentDescriptor();
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

        if (!foundOne) {
            throw new UnsupportedFormatException(
                    "No supported formats found: " + formats, trackControls[0].getFormat());
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

    private void play() throws UnsupportedFormatException, IOException, InterruptedException {
        synchronized (_lock) {
    
        	DataSource dataOutput = _processor.getDataOutput();
            SendStream _sendStream = _rtpManager.createSendStream(dataOutput, 0);
            _sendStream.start();
            //_logger.debug("init(): Waiting 5 seconds for send stream to start...");
            //Thread.sleep(5000);
            _processor.start();
    
            do {
                checkInterrupted();
                _lock.wait();

            } while(_processor != null);

            if (_dataSource != null) {
                _dataSource.disconnect();
                _dataSource=null;
            }
            if (_sendStream != null) {
                _sendStream.close();
                _sendStream = null;
            }     
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
                
            }

            _lock.notifyAll();
        }
    }

    public void shutdown() {
     
        try {
            this.close();
        } catch (InterruptedException e) {
            _logger.warn("Interrupted while closing rtp processor, exception message: "+e.getLocalizedMessage());
        }
        
        /* Some of the possible methods that may be needed for shutting down the rp player
         * All that seems to be needed, is to close the processor.  The RTPManager gets shutdown by the RTPConsumer
         * (RTPManager is shared with this class and the NativeMediaClient which is a subclass of the RTPCOnsumer).
         * 
         */
        if (_rtpManager != null) {
	          _rtpManager.removeTargets("Disconnecting...");
	          _rtpManager.dispose();
	          _rtpManager = null;
        }
          if (_processor != null) {
	          _processor.stop();
	          _processor.close();
	          _processor.deallocate();
	          _processor = null;
          }
           if (_dataSource != null) {
               _dataSource.disconnect();
               _dataSource=null;
          }
          if (_sendStream != null) {
              _sendStream.close();
              _sendStream = null;
          }
           

    }
}

package org.speechforge.cairo.util;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;
import javax.sound.sampled.AudioFileFormat;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.log4j.Logger;

import edu.cmu.sphinx.frontend.util.DataUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
 
public class LiveAudioStream implements PushBufferStream, Runnable {
	
    private static Logger _logger = Logger.getLogger(LiveAudioStream.class);
    
	
    protected ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);

	    protected byte [] data;
	    protected AudioFormat audioFormat;
	    protected boolean started;
	    protected Thread thread;
	    protected BufferTransferHandler transferHandler;
	    protected Control [] controls = new Control[0];
	    
		private InputStream is;
		
		int MAXSIZE=1024;
		boolean recording = false;
		
		boolean endOfStream = false;

        byte[] buff = new byte[MAXSIZE];
		
	    private ByteArrayOutputStream baos;
	    private DataOutputStream dos;

	    
	    public LiveAudioStream(InputStream is, AudioFormat format) {
		
	    	this.is = is;
	    	audioFormat = format;
	

		thread = new Thread(this);
	    }

	    /***************************************************************************
	     * SourceStream
	     ***************************************************************************/
	    
	    public ContentDescriptor getContentDescriptor() {
		return cd;
	    }

	    public long getContentLength() {
		return LENGTH_UNKNOWN;
	    }

	    public boolean endOfStream() {
		    return endOfStream;
	    }

	    /***************************************************************************
	     * PushBufferStream
	     ***************************************************************************/

	    int seqNo = 0;
	    long timeStamp = System.currentTimeMillis();
	    
	    public Format getFormat() {
		    return audioFormat;
	    }

	    public void read(Buffer buffer) throws IOException {
	    	synchronized (this) {
	    		//Object outdata = buffer.getData();
	    		//if (outdata == null || !(outdata.getClass() == Format.byteArray) ||
	    		//		((byte[])outdata).length < MAXSIZE) {
	    		//	outdata = new byte[MAXSIZE];
	    		//	buffer.setData(outdata);
	    		//}
	            //byte[] buff = (byte[])buffer.getData();
	
	    		
	    		int offset = 0;
	    		int bytesToRead = MAXSIZE;           
	            int bytesRead = is.read(buff,offset,bytesToRead);	            
	            
				_logger.debug("bytes read from stream: "+bytesRead);
	   
	 
	    		int totalRead = bytesRead;
	    		int count = 0;
    			offset = totalRead-1;
    			bytesToRead = MAXSIZE - totalRead;
	    		while (((totalRead % 2) != 0) && (totalRead < MAXSIZE)) {
				    int size = is.read(buff,offset,bytesToRead);
					_logger.debug("   additional bytes read from stream: "+size);
				    totalRead = totalRead + size;
				    offset = totalRead-1;
				    bytesToRead = MAXSIZE - totalRead;
				    count++;
				}
				_logger.debug("-> bytes read from stream: "+totalRead+" "+count);
				

 
	            if (bytesRead == -1) {
	            	
	            	endOfStream = true;
	            	buffer.setEOM(true);
	            	buffer.setLength(0);
	            	saveRecording();
	            } else {
	               	byte[] b1 = new byte[bytesRead];
	            	System.arraycopy(buff, 0, b1, 0, bytesRead);

					if (recording) {
				         double[] samples = (audioFormat.getEndian() ==  audioFormat.BIG_ENDIAN) ?
				                 DataUtil.bytesToValues(b1, 0, b1.length, audioFormat.getSampleSizeInBits()/8, true) :
				                 DataUtil.littleEndianBytesToValues(b1, 0, b1.length, audioFormat.getSampleSizeInBits()/8, true);
				        //for (int i =0; i<samples.length;i++) {
				        //	System.out.println(i+" : "+samples[i]);
				        //}
				                 
			            for (double value : samples) {
			                try {
			                    dos.writeShort(new Short((short) value));
			                } catch (IOException e) {
			                    e.printStackTrace();
			                }
			            }     
				                 
					}
	            	buffer.setData(b1);
    			    buffer.setFormat( audioFormat );
    			    buffer.setTimeStamp(timeStamp);
	    	  	    buffer.setSequenceNumber( seqNo );
	    		    buffer.setLength(bytesRead);
	    		    buffer.setFlags(0);
	    		    buffer.setHeader( null );
	    		    seqNo++;
	    		    timeStamp= (long) (timeStamp + (8*bytesRead/audioFormat.getSampleSizeInBits()/audioFormat.getSampleRate() ));
	            }
	    	}
	    }

	    public void setTransferHandler(BufferTransferHandler transferHandler) {
	    	synchronized (this) {
	    		this.transferHandler = transferHandler;
	    		notifyAll();
	    	}
	    }

	    void start(boolean started) {
	    	synchronized ( this ) {
	    		this.started = started;
	    		if (started && !thread.isAlive()) {
	    			thread = new Thread(this);
	    			thread.start();
	    		}
	    		notifyAll();
	    	}
            baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);
	    }

	    /***************************************************************************
	     * Runnable
	     ***************************************************************************/

	    public void run() {
	    	while (started) {
	    		synchronized (this) {
	    			while (transferHandler == null && started) {
	    				try {
	    					wait(1000);
	    				} catch (InterruptedException ie) {
	    				}
	    			} // while
	    		}

	    		if (started && transferHandler != null) {
	    			transferHandler.transferData(this);
	    			try {
	    				Thread.currentThread().sleep( 10 );
	    			} catch (InterruptedException ise) {
	    			}
	    		}
	    	} // while (started)
	    } // run

	    // Controls
	    
	    public Object [] getControls() {
	    	return controls;
	    }

	    public Object getControl(String controlType) {
	       try {
	          Class  cls = Class.forName(controlType);
	          Object cs[] = getControls();
	          for (int i = 0; i < cs.length; i++) {
	             if (cls.isInstance(cs[i]))
	                return cs[i];
	          }
	          return null;

	       } catch (Exception e) {   // no such controlType or such control
	         return null;
	       }
	    }
	    
	    
	    private void saveRecording() {
	        
	        //location of audio file (where it will be written)
	        String dumpFilePath = "c:/temp/";

	        
	        //audio format parameters
	        int bitsPerSample = 16;
	        int sampleRate = 8000;
	        boolean isBigEndian = true;
	        boolean isSigned = true;

	        
	        //create an audio format object (java sound api)
	        javax.sound.sampled.AudioFormat wavFormat = new  javax.sound.sampled.AudioFormat(sampleRate, bitsPerSample, 1, isSigned, isBigEndian);
	        AudioFileFormat.Type outputType = getTargetType("wav");
	        String wavName = dumpFilePath + getNextFreeIndex(dumpFilePath) + ".wav";
	        
	        _logger.debug("created audio Format Object "+wavFormat.toString());
	        _logger.debug("filename:" + wavName);

	        byte[] abAudioData = baos.toByteArray();
	        ByteArrayInputStream bais = new ByteArrayInputStream(abAudioData);
	        AudioInputStream ais = new AudioInputStream(bais, wavFormat, abAudioData.length / wavFormat.getFrameSize());

	        File outWavFile = new File(wavName);

	        if (AudioSystem.isFileTypeSupported(outputType, ais)) {
	            try {
	                AudioSystem.write(ais, outputType, outWavFile);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        } else {
	           System.out.println("output type not supported..."); 
	        }
	    }
	    
	    
	    /**
	     * Gets the target type.
	     * 
	     * @param extension the extension
	     * 
	     * @return the target type
	     */
	    private static AudioFileFormat.Type getTargetType(String extension) {
	        AudioFileFormat.Type[] typesSupported = AudioSystem.getAudioFileTypes();

	        for (AudioFileFormat.Type aTypesSupported : typesSupported) {
	            if (aTypesSupported.getExtension().equals(extension)) {
	                return aTypesSupported;
	            }
	        }
	        return null;
	    }
	    
	    /**
	     * Gets the next free index (a unique number for the next file name)
	     * 
	     * @param outPattern the out pattern
	     * 
	     * @return the next free index
	     */
	    private static int getNextFreeIndex(String outPattern) {
	        int fileIndex = 0;
	        while (new File(outPattern + fileIndex + ".wav").isFile())
	            fileIndex++;

	        return fileIndex;
	    }
	    
	    
	}

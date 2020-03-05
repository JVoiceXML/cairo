package org.speechforge.cairo.util;

import javax.media.Time;
import javax.media.format.AudioFormat;
import javax.media.protocol.*;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

 
public class CustomDataSource extends PushBufferDataSource {
	
    	private static Logger _logger = Logger.getLogger(CustomDataSource.class);

	    protected Object [] controls = new Object[0];
	    protected boolean started = false;
	    protected String contentType = "raw";
	    protected boolean connected = false;
	    protected Time duration = DURATION_UNKNOWN;
	    protected LiveAudioStream [] streams = null;
	    protected LiveAudioStream stream = null;
	    
		private InputStream is;
		private AudioFormat format;
	    
	    public CustomDataSource(InputStream is, AudioFormat format) {
	    	this.is = is;
	    	this.format = format;
	    }
	    
	    public String getContentType() {
		if (!connected){
	            System.err.println("Error: DataSource not connected");
	            return null;
	        }
		return contentType;
	    }

	    public void connect() throws IOException {
		 if (connected)
	            return;
		 connected = true;
	    }

	    public void disconnect() {
		    try {
	            if (started)
	                stop();
	        } catch (IOException e) {}
		    connected = false;
	    }

	    public void start() throws IOException {
		// we need to throw error if connect() has not been called
	        if (!connected)
	            throw new java.lang.Error("DataSource must be connected before it can be started");
	        if (started)
	            return;
		    started = true;
		    stream.start(true);
	    }

	    public void stop() throws IOException {
		if ((!connected) || (!started))
		    return;
		   started = false;
		   stream.start(false);
	    }

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

	    public Time getDuration() {
		    return duration;
	    }

	    public PushBufferStream [] getStreams() {
		if (streams == null) {
		    streams = new LiveAudioStream[1];
		    stream = streams[0] = new LiveAudioStream(is, format);
		}
		return streams;
	    }
	    
	}

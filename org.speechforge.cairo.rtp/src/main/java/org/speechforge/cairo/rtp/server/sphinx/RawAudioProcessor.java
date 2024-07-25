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
package org.speechforge.cairo.rtp.server.sphinx;




import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.media.format.AudioFormat;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.speechforge.cairo.util.ByteHexConverter;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;

/**
 * Processes raw audio data input and feeds it to the frontend of the Sphinx recognition engine.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class RawAudioProcessor extends BaseDataProcessor implements Runnable {
    /** Logger for this class */
    private static final Logger LOGGER =
            LogManager.getLogger(RawAudioProcessor.class);
    @S4Integer(defaultValue = 10)
    public static final String PROP_MSEC_PER_READ = "msecPerRead";
    /** List of transformed audio data */
    private LinkedBlockingQueue<Data> dataList;
    /** List of raw audio data */
    private LinkedBlockingQueue<byte[]> rawAudioList;
    private SourceAudioFormat _audioFormat;
    private AudioDataTransformer _transformer = null;
    private volatile boolean processing = false;
    private volatile boolean utteranceEndReached = false;
    private volatile byte[] _frame;
    private volatile int _framePointer = 0;
    private FileWriter _fileWriter = null;

    // Configuration data

    private int _msecPerRead;

    // Runnable variables

    private long _totalSamplesRead = 0;
    private long _startTime;
    
    long t1 =0;

    /**
     * Constructs a new RawAudioProcessor with the specified number of
     * milliseconds per read.
     * 
     * @param msecsPerRead
     *            number of milliseconds per read
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
       super.newProperties(ps);

       _msecPerRead = ps.getInt(PROP_MSEC_PER_READ);
       initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        super.initialize();
        dataList = new LinkedBlockingQueue<Data>();
        rawAudioList = new LinkedBlockingQueue<byte[]>();
    }

    /**
     * Whether this processor is currently processing audio.
     *
     * @return true if this processor is currently in the processing state.
     */
    public synchronized boolean isProcessing() {
        return processing;
    }

    /**
     * Starts processing any audio data being added via this processors
     * {@link org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor#addRawData(byte[], int, int)} method.
     * 
     * @param format format of the audio being passed to this processor
     * @throws UnsupportedEncodingException if the specified format cannot be supported
     */
    public synchronized void startProcessing(AudioFormat format) throws UnsupportedEncodingException {
        if (processing) {
            throw new IllegalStateException("RawAudioProcessor.startProcessing() cannot be called while already in processing state!");
        }

        try {
            //_fileWriter = new FileWriter("C:\\work\\cvs\\onomatopia\\cairo\\prompts\\test\\rtp.txt", false);
        } catch (Exception e) {
            LOGGER.warn(e, e);
        }


        _audioFormat = SourceAudioFormat.newInstance(_msecPerRead, format);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Frame size: " + _audioFormat.getFrameSizeInBytes() + " bytes");
        }
        utteranceEndReached = false;
        //_transformer = new AudioDataTransformer(_audioFormat, stereoToMono, selectedChannel);
        _transformer = new AudioDataTransformer(_audioFormat, "average", 0);
        _frame = new byte[_audioFormat.getFrameSizeInBytes()];
        processing = true;
        final Thread processingThread = new Thread(this);
        processingThread.start();
    }


    /**
     * Stops processing audio. This method does not return until processing
     * has been stopped and all data has been read from the audio line.
     */
    public synchronized void stopProcessing() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stopping processing: adding final frame data and end"
                    + " signal...");
        }
        processing = false;

        // add end signal
        rawAudioList.add(new byte[0]);

        if (_fileWriter != null) {
            try {
                _fileWriter.close();
                _fileWriter = null;
            } catch (IOException e){
                LOGGER.warn(e, e);
            }
        }

    }

    /**
     * Processes all audio data to be transformed and adds it to the list of
     * transformed audio data.
     */
    public void run() {
        _totalSamplesRead = 0;
        _startTime = System.currentTimeMillis();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Processing started at: " + _startTime);
        }
        
        // Insert a DataStartSignal to indicate the start of the data stream
        Data data = new DataStartSignal(_audioFormat.getSampleRate());
        dataList.add(data);
        LOGGER.debug("adding DataStartSignal");
        
        // Process all received data until end of utterance is reached
        try {
            do {
                data = transformNextRawAudio();
                if (data != null) {
                    dataList.add(data);
                    LOGGER.trace(dataList.size() + " items in data list");
                }
            } while ((data != null) && (processing));
            
        } catch (InterruptedException e) {
            LOGGER.warn(e, e);
        } 

        // Insert a DataEndSignal to indicate the end of the data stream
        data = new DataEndSignal(
                _audioFormat.calculateDurationMsecs(_totalSamplesRead));
        dataList.add(data);
        if (LOGGER.isTraceEnabled()) {
            long t2 = System.currentTimeMillis();
            LOGGER.trace("DataEndSignal added "+(t2-_startTime));
        }
    }

    private Data transformNextRawAudio() throws InterruptedException {

        LOGGER.trace("transformNextRawAudio(): retrieving data from raw audio list...");

        long tt =  rawAudioList.size();
        long t1 = System.nanoTime();
        byte[] data = rawAudioList.take();
        long t2 = System.nanoTime();
        long t = System.currentTimeMillis();
        LOGGER.trace(t+ " it took "+(t2-t1)+ " nanosecs to take an item from queue with "+tt+" elements");
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("transformNextRawAudio(): data from raw audio list, bytes=" + data.length);
        }

        long firstSampleNumber = _totalSamplesRead / _audioFormat.getChannels();
        long collectTime = _startTime + (firstSampleNumber * _audioFormat.getMsecPerRead());
        if (data.length < 1) {
        	LOGGER.trace("data length < 1");
            return null;
        }

        _totalSamplesRead += (data.length / _audioFormat.getSampleSizeInBytes());
        LOGGER.trace("read in " + data.length + " bytes " + _totalSamplesRead);
        if (data.length != _audioFormat.getFrameSizeInBytes()) {
            if (data.length % _audioFormat.getSampleSizeInBytes() != 0) {
                throw new Error("Incomplete sample read.");
            }
        }

        return _transformer.toDoubleData(data, collectTime, firstSampleNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Data getData() throws DataProcessingException {
        //getTimer().start();
        Data output = null;
        if (!utteranceEndReached) {
            try {
                LOGGER.trace("getData(): getting data from data list...");
                output = dataList.take();
                LOGGER.trace("getData(): got data from data list: " 
                        + output.getClass().getName());
            } catch (InterruptedException e){
                LOGGER.warn(e, e);
                DataProcessingException dpe =
                        new DataProcessingException(
                                "Data processing thread interrupted!");
                dpe.initCause(e);
                throw dpe;
            }
            if (output instanceof DataEndSignal) {
                utteranceEndReached = true;
            }
        } else {
            LOGGER.trace("getData(): utterance end reached, returning null.");
        }

        //getTimer().stop();

        return output;
    }

    /**
     * After processing is started on this processor this will add raw audio data to be processed.
     * 
     * @param data buffer of raw audio data to be processed
     */
    public synchronized void addRawData(byte[] data) {
        addRawData(data, 0, data.length);
    }

    /**
     * After processing is started on this processor this will add raw audio data to be processed.
     * 
     * @param data buffer of raw audio data to be processed
     * @param offset starting point in buffer to process data from
     * @param length number of bytes to be processed from buffer
     */
    public synchronized void addRawData(byte[] data, int offset, int length) {
    	try {
            addRawDataPrivate(data, offset, length);
        } catch (RuntimeException e) {
            LOGGER.debug("addRawData(): throwing exception", e);
            throw e;
        }
    }

    private synchronized void addRawDataPrivate(byte[] data, int offset, int length) {
    	//long t2 = System.nanoTime();
        //_logger.info((t2-t1)+ "  nano secs between calls");
        if (!processing) {
            throw new IllegalStateException("Attempt to add raw data when RawAudioProcessor not in processing state!");
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("addRawData(): datalength="+data.length+" offset=" + offset + ", length=" + length);
        }

        if (length < 1) {
            LOGGER.debug("addRawData(): no data to add (length < 1).");
            return;
        }

        if (data == null) {
            throw new IllegalArgumentException("Attempt to call addRawData() passing null data argument!");
        }

        if (offset + length > data.length) {
            throw new IllegalArgumentException("Attempt to call addRawData() with offset plus length greater than data length!");
        }

        if (_fileWriter != null) {
            try {
                ByteHexConverter.writeHexDigits(_fileWriter, data, offset, length);
            } catch (IOException e){
                LOGGER.warn(e, e);
            }
        }

        int dataPointer = offset;
        length = length + offset;
        if (length > data.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        while (true) {
            while (_framePointer < _frame.length && dataPointer < length) {
                _frame[_framePointer++] = data[dataPointer++];
            }
            if (_framePointer == _frame.length) {
                // the frame was filled
                rawAudioList.add(_frame);
                _frame = new byte[_frame.length];
                //_frame = new byte[_audioFormat.getFrameSizeInBytes()];
                _framePointer = 0;
            } else {
                // the data buffer was exhausted
                break;
            }
        }
         //t1 = System.nanoTime();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("remainder = " + _framePointer);
        }

    }

    public static RawAudioProcessor getInstanceForTesting(){
        RawAudioProcessor instance = new RawAudioProcessor();
        instance._msecPerRead = 10;
        instance.initialize();
        return instance;
    }

}

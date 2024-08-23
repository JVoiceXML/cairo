/*
 * JSAPI - An independent reference implementation of JSR 113.
 *
 * Copyright (C) 2017 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.speechforge.cairo.server.recog.sphinx;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioProcessor;
import org.speechforge.cairo.rtp.server.sphinx.RawAudioTransferHandler;

import edu.cmu.sphinx.api.AbstractSpeechRecognizer;
import edu.cmu.sphinx.api.Context;
import edu.cmu.sphinx.decoder.ResultListener;
import edu.cmu.sphinx.recognizer.Recognizer.State;
import edu.cmu.sphinx.recognizer.StateListener;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Recognizer implementation of Sphinx to be used within the cairo context.
 * 
 * @author Dirk Schnelle-Walka
 * @since 0.3.2
 */
public class CairoSphinxRecognizer extends AbstractSpeechRecognizer
        implements StateListener {
    /** Logger for this class. */
    private static final Logger LOGGER = LogManager
            .getLogger(CairoSphinxRecognizer.class);

    private final Object stateMonitor;
    private RawAudioTransferHandler rawAudioTransferHandler;
    private final RawAudioProcessor rawAudioProcessor;

    protected CairoSphinxRecognizer(Context context,
            RawAudioProcessor procesdor) throws IOException {
        super(context);
        rawAudioProcessor = procesdor;
        stateMonitor = new Object();
        recognizer.addStateListener(this);
    }

    public void allocate() {
        recognizer.allocate();
        waitForRecognizerState(State.READY);
    }

    public void addResultListener(final ResultListener listener) {
        recognizer.addResultListener(listener);
    }

    public void removeResultListener(final ResultListener listener) {
        recognizer.removeResultListener(listener);
    }

    public void startRecognition(PushBufferDataSource dataSource)
            throws UnsupportedEncodingException {
        PushBufferStream[] streams = dataSource.getStreams();
        if (streams.length != 1) {
            throw new IllegalArgumentException(
                "Rec engine can handle only single stream datasources, # of streams: " + streams);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting recognition on stream format: " + streams[0].getFormat());
        }
        try {
            rawAudioTransferHandler = new RawAudioTransferHandler(rawAudioProcessor);
            rawAudioTransferHandler.startProcessing(streams[0]);
        } catch (UnsupportedEncodingException e) {
            rawAudioTransferHandler = null;
            throw e;
        }
    }

    /**
     * Checks if the recognizer is currently recognizing.
     * 
     * @return {@code true} if the recognizer is currently recognizing,
     *         {@code false} otherwise.
     */
    public boolean isRecognizing() {
        return rawAudioTransferHandler != null;
    }
    
    public void stopRecognition() {
        if (rawAudioTransferHandler != null) {
            rawAudioTransferHandler.stopProcessing();
            rawAudioTransferHandler = null;
        }
    }

    public void deallocate() {
        recognizer.deallocate();
        recognizer.resetMonitors();
    }

    /**
     * {@inheritDoc} Not used but for compatibility.
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void statusChanged(State status) {
        synchronized (stateMonitor) {
            stateMonitor.notifyAll();
        }
    }

    /**
     * Wait for the recognizer to enter the given state.
     * 
     * @param status
     *            The state of the recognizer to wait for.
     */
    synchronized void waitForRecognizerState(final State status) {
        while (recognizer.getState() != status) {
            try {
                synchronized (stateMonitor) {
                    stateMonitor.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LOGGER.info("Sphinx4Recognizer in state: " + status);
    }
}

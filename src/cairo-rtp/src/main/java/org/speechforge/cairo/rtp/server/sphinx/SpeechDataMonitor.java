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

import org.speechforge.cairo.rtp.server.SpeechEventListener;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;

import org.apache.log4j.Logger;

/**

 * Monitors a stream of speech data being processed and broadcasts start-of-speech and end-of-speech events.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechDataMonitor extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataMonitor.class);

    private SpeechEventListener _speechEventListener = null;

    /**
     * TODOC
     */
    public SpeechDataMonitor() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void setSpeechEventListener(SpeechEventListener speechEventListener) {
        _speechEventListener = speechEventListener;
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();
        if (data instanceof SpeechStartSignal) {
            broadcastSpeechStartSignal();
        } else if (data instanceof SpeechEndSignal) {
            broadcastSpeechEndSignal();
        } else if (data instanceof DataStartSignal) {
            _logger.debug("<<<<<<<<<<<<<<< DataStartSignal encountered!");
        } else if (data instanceof DataEndSignal) {
            _logger.debug(">>>>>>>>>>>>>>> DataEndSignal encountered!");
        }
        return data;
    }
    
    private void broadcastSpeechStartSignal() {
        _logger.debug("*************** SpeechStartSignal encountered!");
        if (_speechEventListener != null) {
            _speechEventListener.speechStarted();
        }
    }

    private void broadcastSpeechEndSignal() {
        _logger.debug("*************** SpeechEndSignal encountered!");
        if (_speechEventListener != null) {
            _speechEventListener.speechEnded();
        }
    }

}

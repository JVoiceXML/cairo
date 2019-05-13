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
package org.speechforge.cairo.jmf;

import java.io.IOException;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Processor;
import javax.media.StartEvent;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;

/**
 * Provides a standard implementation of a {@code javax.media.ControllerListener} that can be used
 * for starting (and optionally closing) {@code javax.media.Processor} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class ProcessorStarter implements ControllerListener {

    private static Logger _logger = Logger.getLogger(ProcessorStarter.class);
    
    private boolean _handleEOM;

    /**
     * Handles EndOfMediaEvent by default.
     */
    public ProcessorStarter() {
        this(true);
    }

    /**
     * @param handleEOM if true close processor on EndOfMediaEvent
     */
    public ProcessorStarter(boolean handleEOM) {
        _handleEOM = handleEOM;
    }

    /* (non-Javadoc)
     * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
     */
    public void controllerUpdate(ControllerEvent event) {
        if (_logger.isDebugEnabled()) {
            _logger.debug("controllerUpdate(): ControllerEvent received: " + event);
        }

        try {
            if (event instanceof StartEvent) {
                Processor processor = (Processor) event.getSourceController();
                DataSource dataSource = processor.getDataOutput();
                _logger.debug("Starting data source...");
                dataSource.connect();
                dataSource.start();
            } else if (event instanceof EndOfMediaEvent) { //StopEvent) {
                if (_handleEOM) {
                    _logger.debug("controllerUpdate(): EndOfMediaEvent received: closing processor...");
                    event.getSourceController().close();
                } else {
                    _logger.debug("controllerUpdate(): ignoring EndOfMediaEvent.");
                }
            }
        } catch (IOException e) {
            _logger.warn(e, e);
        }

    }

}

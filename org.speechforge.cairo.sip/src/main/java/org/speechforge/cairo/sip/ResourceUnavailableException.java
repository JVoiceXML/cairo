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
package org.speechforge.cairo.sip;

/**
 * Thrown when a requested resource is not available to complete a SIP session initiation. 
 *
 * @author Spencer Lord {@literal <}<a href="salord@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class ResourceUnavailableException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * TODOC
     */
    public ResourceUnavailableException() {
        super();
    }

    /**
     * TODOC
     * @param message
     */
    public ResourceUnavailableException(String message) {
        super(message);
    }

    /**
     * TODOC
     * @param message
     * @param cause
     */
    public ResourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * TODOC
     * @param cause
     */
    public ResourceUnavailableException(Throwable cause) {
        super(cause);
    }

}

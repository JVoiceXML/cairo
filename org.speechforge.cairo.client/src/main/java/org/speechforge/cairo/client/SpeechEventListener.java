/*
 * cairo-client - Open source client for control of speech media resources.
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
package org.speechforge.cairo.client;

import org.speechforge.cairo.client.recog.RecognitionResult;


/**
 * Callback interface for getting recognition and synthesis/tts results.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SpeechEventListener {

	
    public enum SpeechEventType {SPEECH_MARKER,
    						SPEAK_COMPLETE,
	    					START_OF_INPUT,
	    					RECOGNITION_COMPLETE,
	    					INTERPRETATION_COMPLETE,
	    					RECORD_COMPLETE,
	    					VERIFICATION_COMPLETE,
	    					UNKNOWN}
   
    /**
     * Recognition event received.
     * 
     * @param event the mrcp event
     * @param r the recognition result
     */
    public void recognitionEventReceived(SpeechEventType event, RecognitionResult r);
    
    /**
     * Tts completed event received.
     * 
     * @param event the mrcp event
     */
    public void speechSynthEventReceived(SpeechEventType event);

    

    public enum DtmfEventType {recognitionMatch, noInputTimeout, noMatchTimeout}
    
    /**
     * Character event received.  Most typically used for DTMF (in which case valid characters include 0-9, * and #)
     * 
     * @param c the charcater received
     * @param status the event type
     */
    public void characterEventReceived(String c, DtmfEventType status);
    
}

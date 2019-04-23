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
package edu.cmu.sphinx.frontend.endpoint;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;

/**
 * Allows public access to contents of SpeechClassifiedData instances.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechClassifiedDataAccessor {

    private SpeechClassifiedDataAccessor() {
        // prevent instantiation
    }

    /**
     * Returns whether this is classified as speech.
     *
     * @param speechClassifiedData instance to check
     * @return true if this is classified as speech, false otherwise
     * @throws ClassCastException if the instance passed is not of type edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData
     */
    public static boolean isSpeech(Data speechClassifiedData) throws ClassCastException {
        return ((SpeechClassifiedData) speechClassifiedData).isSpeech();
    }

    /**
     * Returns the DoubleData contained by the SpeechClassifiedData instance passed.
     *
     * @param speechClassifiedData instance to get data from
     * @return the DoubleData contained by the SpeechClassifiedData instance passed
     * @throws ClassCastException if the instance passed is not of type edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData
     */
    public DoubleData getDoubleData(Data speechClassifiedData) {
        return ((SpeechClassifiedData) speechClassifiedData).getDoubleData();
    }

}

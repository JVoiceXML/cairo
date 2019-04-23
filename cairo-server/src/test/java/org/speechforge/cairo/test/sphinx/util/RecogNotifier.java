/**
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2006 SpeechForge - http://www.speechforge.org
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

package org.speechforge.cairo.test.sphinx.util;

import org.speechforge.cairo.server.recog.RecogListenerDecorator;
import org.speechforge.cairo.server.recog.RecognitionResult;

/**
 * Listens for recognition results and then notifies any waiting objects that the result is available.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecogNotifier extends RecogListenerDecorator {

    private RecognitionResult _result;

    public RecogNotifier() {
        super(null);  // use RecogListenerDecorator as adaptor
    }

    /**
     * @return result of recognition or null if recognition has not yet occurred.
     */
    public RecognitionResult getResult() {
        return _result;
    }

    /* (non-Javadoc)
     * @see org.speechforge.cairo.server.recog.RecogListener#recognitionComplete(org.speechforge.cairo.server.recog.RecognitionResult)
     */
    @Override
    public synchronized void recognitionComplete(RecognitionResult result) {
        _result = result;
        this.notifyAll();
    }
    
}
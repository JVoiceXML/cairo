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
package org.speechforge.cairo.test.sphinx.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.net.URL;

import junit.framework.Assert;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4String;

import org.apache.log4j.Logger;

/**
 * Sphinx data processor used for asserting values for speech data as it passes through the pipeline.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class SpeechDataAssertion extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataAssertion.class);

    @S4String(defaultValue = "speechDataFileName")
	public static final String PROP_SPEECH_DATA_FILE = "speechDataFile";
	


    private StreamTokenizer _tokenizer;
    private int _ttype;

    /**
     * TODOC
     */
    public SpeechDataAssertion() {
        super();
        // TODO Auto-generated constructor stub
    }



    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        String speechDataFile = ps.getString(PROP_SPEECH_DATA_FILE);
        if (speechDataFile == null || speechDataFile.length() < 1) {
            throw new PropertyException(ps.getInstanceName(),PROP_SPEECH_DATA_FILE, "Required property \"speechDataFile\" not specified!");
        }

        URL speechDataURL = this.getClass().getResource(speechDataFile);
        if (speechDataURL == null) {
            throw new PropertyException(ps.getInstanceName(), PROP_SPEECH_DATA_FILE, "Specified speech data file (" + speechDataFile + ") not found!");
        }

        try {
            Reader r = new BufferedReader(new InputStreamReader(speechDataURL.openStream()));
            _tokenizer = new StreamTokenizer(r);
            _tokenizer.parseNumbers();
            _ttype = _tokenizer.nextToken();
            _logger.trace(_tokenizer);
		} catch (IOException e) {
			throw (PropertyException) new PropertyException(e,ps.getInstanceName(), PROP_SPEECH_DATA_FILE, e.getMessage()).initCause(e);
		}
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.DataProcessor#initialize()
     */
    public void initialize() {
        super.initialize();
    }

    /* (non-Javadoc)
     * @see edu.cmu.sphinx.frontend.BaseDataProcessor#getData()
     */
    @Override
    public synchronized Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();

        try {
			assertData(data);
		} catch (IOException e) {
			throw new Error(e);
		}

        return data;
    }
    
    private void assertData(Data data) throws IOException {
        if (_ttype == StreamTokenizer.TT_WORD) {
            Assert.assertEquals(_tokenizer.toString(), _tokenizer.sval, data.getClass().getName());
            _ttype = _tokenizer.nextToken();
            _logger.trace(_tokenizer);
        } else {
            Assert.assertEquals(_tokenizer.toString(), DoubleData.class, data.getClass());
            double[] values = ((DoubleData) data).getValues();
            for (int i=0; i < values.length; i++) {
                String message = _tokenizer.toString();

                if (_ttype != StreamTokenizer.TT_NUMBER) {  // then throw AssertionError...
                    Assert.assertEquals(message, StreamTokenizer.TT_WORD, _ttype); // should pass (if not a number then a word)
                    Assert.assertEquals(message, _tokenizer.sval, Double.toString(values[i])); // <- should fail
                    throw new AssertionError(message); // just in case above line doesn't fail
                }

                Assert.assertEquals(message, _tokenizer.nval, values[i]);
                _ttype = _tokenizer.nextToken();
                _logger.trace(_tokenizer);
            }
        }
    }

}

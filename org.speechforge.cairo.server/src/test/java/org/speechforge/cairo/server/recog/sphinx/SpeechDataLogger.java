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
package org.speechforge.cairo.server.recog.sphinx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedDataAccessor;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4String;

import org.apache.log4j.Logger;

/**
 * Sphinx data processor for logging speech data as it passes through the pipeline.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class SpeechDataLogger extends BaseDataProcessor {

    private static Logger _logger = Logger.getLogger(SpeechDataLogger.class);

    /**
     * Property specifying the location of the log file to log speech data to.
     */
    @S4String(defaultValue = "/temp/speechdata")
    public static final String PROP_LOG_FILE_DIR = "speechDataLogFileDir";


	/**
	 * Property specifying the name of the log file to log speech data to.
	 */
    @S4String(defaultValue = "speechdata")
	public static final String PROP_LOG_FILE_NAME = "speechDataLogFileName";


    private static final String NL = System.getProperty("line.separator");

	private FileWriter _fileWriter = null;

    /**
     * Default constructor.
     */
    public SpeechDataLogger() {
        super();
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);

        File logFileDir = new File(ps.getString(PROP_LOG_FILE_DIR));
        if (!logFileDir.exists()) {
            if (!logFileDir.mkdirs()) {
                throw new PropertyException(ps.getInstanceName(), PROP_LOG_FILE_DIR, "Invalid log file dir: " + logFileDir);
            }
        } else if (!logFileDir.isDirectory()) {
            throw new PropertyException(ps.getInstanceName(), PROP_LOG_FILE_DIR, "Specified log file dir exists as file: " + logFileDir);
        }

        String logFileName = ps.getString(PROP_LOG_FILE_NAME);

        try {
			_fileWriter = new FileWriter(constructLogFile(logFileDir, logFileName), false);
		} catch (IOException e) {
			throw (PropertyException) new PropertyException(e, ps.getInstanceName(),PROP_LOG_FILE_NAME,"Failed create file");
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
    public Data getData() throws DataProcessingException {
        Data data = getPredecessor().getData();
        try {
			logData(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
            _logger.debug(e, e);
		}
        return data;
    }
    
    private void logData(Data data) throws IOException {
        if (data == null) {
            _fileWriter.append("data is null!");
            _fileWriter.append(NL);
        } else if (data instanceof Signal) {
        	_fileWriter.append(data.getClass().getName());
            _fileWriter.append(NL);
        } else if (data instanceof DoubleData) {
        	double[] values = ((DoubleData) data).getValues();
        	for (int i = 0; i < values.length; i++) {
            	_fileWriter.append(Double.toString(values[i]));
            	_fileWriter.append(NL);
        	}
        } else if (data instanceof FloatData) {
        	float[] values = ((FloatData) data).getValues();
        	for (int i = 0; i < values.length; i++) {
            	_fileWriter.append(Float.toString(values[i]));
            	_fileWriter.append(NL);
        	}
        } else if ("edu.cmu.sphinx.frontend.endpoint.SpeechClassifiedData".equals(data.getClass().getName())) {
            boolean isSpeech = SpeechClassifiedDataAccessor.isSpeech(data);
            _fileWriter.append("isSpeech? " + isSpeech);
            _fileWriter.append(NL);
        } else {
            _fileWriter.append("Unknown data type: " + data.getClass().getName());
            _fileWriter.append(NL);
        }
        _fileWriter.flush();
    }

    private static File constructLogFile(String logFileDir, String logFileName) {
        File dir = new File(logFileDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalArgumentException("invalid log file dir: " + dir);
            }
        } else if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Specified log file dir exists as file: " + dir);
        }

        return constructLogFile(dir, logFileName);
    }

    private static File constructLogFile(File logFileDir, String logFileName) {
        File logFile = new File(logFileDir, logFileName + '-' + System.currentTimeMillis() + ".txt");
        if (_logger.isDebugEnabled()) {
            try {
                URL logFileURL = logFile.toURL();
                _logger.debug("logging speech data to " + logFileURL);
            } catch (Exception e) {
                _logger.debug(logFile, e);
            }
        }
        return logFile;
    }

    public static SpeechDataLogger getInstanceForTesting(String logFileName) throws IOException {
        SpeechDataLogger instance = new SpeechDataLogger();
        instance._fileWriter = new FileWriter(constructLogFile("/temp/speechdata", logFileName), false);
        return instance;
    }

}

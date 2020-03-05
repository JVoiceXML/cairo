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

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.ProcessorModel;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;


/**
 * Provides standard implementations of common JMF functionalities.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class JMFUtil {

    private static Logger _logger = Logger.getLogger(JMFUtil.class);

    /**
     * RAW content type. This signifies content that's contained in individual
     * buffers of type Buffer and carries any format of media as specified by
     * the format attribute of the buffer.
     * 
     * @see javax.media.protocol.ContentDescriptor#RAW
     */
    public static final ContentDescriptor CONTENT_DESCRIPTOR_RAW = new ContentDescriptor(ContentDescriptor.RAW);

    /**
     * RAW RTP content type. This is similar to the RAW content type but only
     * carries buffers that contain packetized data formats supported by RTP.
     * 
     * @see javax.media.protocol.ContentDescriptor#RAW_RTP
     */

    public static final ContentDescriptor CONTENT_DESCRIPTOR_RAW_RTP = new ContentDescriptor(ContentDescriptor.RAW_RTP);

    /**
     * MediaLocator specifying the attached microphone.
     */
    //public static final MediaLocator MICROPHONE = new MediaLocator("dsound://");
    public static final MediaLocator MICROPHONE = new MediaLocator("javasound://8000");


    /**
     * @param mediaLocator
     * @param mediaFormat
     * @return
     * @throws NoDataSourceException
     * @throws IOException
     * @throws NoProcessorException
     * @throws CannotRealizeException
     */
    public static Processor createRealizedProcessor(MediaLocator mediaLocator, AudioFormat preferredMediaFormat)
      throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

        return createRealizedProcessor(mediaLocator, new AudioFormat[] {preferredMediaFormat});
    }

    /**
     * @param mediaLocator
     * @param preferredMediaFormats
     * @return
     * @throws NoDataSourceException
     * @throws IOException
     * @throws NoProcessorException
     * @throws CannotRealizeException
     */
    public static Processor createRealizedProcessor(MediaLocator mediaLocator, AudioFormat[] preferredMediaFormats)
      throws NoDataSourceException, IOException, NoProcessorException, CannotRealizeException {

        DataSource dataSource = Manager.createDataSource(mediaLocator);
        ProcessorModel pm = new ProcessorModel(dataSource, preferredMediaFormats, CONTENT_DESCRIPTOR_RAW);
        _logger.debug("Creating realized processor...");
        Processor processor = Manager.createRealizedProcessor(pm);
        _logger.debug("Processor realized.");
        return processor;
    }

    
    /**
     * @param dataSource
     * @return
     * @throws NoProcessorException
     * @throws CannotRealizeException
     * @throws IOException
     */
    public static Processor createRealizedProcessor(DataSource dataSource, AudioFormat[] preferredMediaFormats)
      throws NoProcessorException, CannotRealizeException, IOException {
        
        ProcessorModel pm = new ProcessorModel(
            dataSource,
            preferredMediaFormats,
            JMFUtil.CONTENT_DESCRIPTOR_RAW
        );
        
        _logger.debug("Creating realized processor...");
        Processor processor = Manager.createRealizedProcessor(pm);
        _logger.debug("Processor realized.");
        
        return processor;
    }

    
}

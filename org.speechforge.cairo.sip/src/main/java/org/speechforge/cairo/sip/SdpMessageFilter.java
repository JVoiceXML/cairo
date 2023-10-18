/*
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2023 switch 
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
 */
package org.speechforge.cairo.sip;

import java.util.Collection;
import java.util.Vector;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Filter for SDP messages to remove unsupported media types.
 * @author Dirk Schnelle-Walka
 *
 */
public class SdpMessageFilter {
    private final static Logger LOGGER =
            LogManager.getLogger(SdpMessageFilter.class);

        /**
         * Validates the provided SDP message.
         * @param message the message to validate
         * @return filtered descriptions
         * @throws SdpException error validating the message
         */
        public static Vector<MediaDescription> filter(final SdpMessage message) 
                throws SdpException {
            final SessionDescription sessionDescription =
                    message.getSessionDescription();
            @SuppressWarnings("unchecked")
            final Collection<MediaDescription> descriptions =
                    sessionDescription.getMediaDescriptions(true);
            final Vector<MediaDescription> filteredDescriptions =
                    new Vector<MediaDescription>();
            for (MediaDescription description : descriptions) {
                final Media media = description.getMedia();
                final String mediaType = media.getMediaType();
                if (mediaType.equals("video")) {
                    LOGGER.info("filtering " + media + " from invite");
                } else {
                    filteredDescriptions.add(description);
                }
            }
            return filteredDescriptions;
        }
}

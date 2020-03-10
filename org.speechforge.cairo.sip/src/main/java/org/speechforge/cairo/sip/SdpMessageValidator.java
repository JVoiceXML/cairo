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

import java.util.Enumeration;
import java.util.Vector;
import javax.sdp.Attribute;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;

/**
 * Validates the sdp message.  Throws exception if the message is invalid.  
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SdpMessageValidator {
    
    private static Logger _logger = Logger.getLogger(SdpMessageValidator.class);

    public static void validate(SdpMessage message)  throws SdpException {

        SessionDescription sd = null;
        String text = "";
        int problemCount = 0;
        try {
            sd = message.getSessionDescription();
            Origin origin = sd.getOrigin();
            if (origin == null) {
                text =text+"no origin line\n";
                problemCount++;
            }
            Enumeration e = sd.getMediaDescriptions(true).elements();
            while (e.hasMoreElements()) {
                MediaDescription md = (MediaDescription) e.nextElement();
                Media media = md.getMedia();
                //int port = media.getMediaPort();
                //Vector mFormats = media.getMediaFormats(true);
                Vector attributes = md.getAttributes(true);
                if ((media.getMediaType().equals("audio")) && (media.getProtocol().equals("RTP/AVP"))) {
                       // TODO: Check if the RTP Encoding in the request is supported by cairos codecs and streaming reosurces. 
                       //       Should offers be rejected if encoding not supported -- or counter-offered?  Maybe this is not a validation task
                       //       but a session negotiation task.
                } else if ((media.getMediaType().equals("application"))
                        && (media.getProtocol().equals("TCP/MRCPv2"))) {
                    for (Enumeration attrEnum = attributes.elements(); attrEnum.hasMoreElements();) {
                        Attribute attribute = (Attribute) attrEnum.nextElement();
                        if (attribute.getName().equals("setup")) {
                            // value should be "active" in request and "passive"
                            // in response
                        } else if (attribute.getName().equals("connection")) {
                            // can either be new or existing
                        } else if (attribute.getName().equals("channel")) {
                            if (attribute.getValue().endsWith(MrcpResourceType.SPEECHRECOG.toString())) {
                                //supported
                            } else if (attribute.getValue().endsWith(MrcpResourceType.SPEECHSYNTH.toString())) {
                                //supported
                            } else if (attribute.getValue().endsWith(MrcpResourceType.RECORDER.toString())) {
                               //supported
                            } else if (attribute.getValue().endsWith(MrcpResourceType.DTMFRECOG.toString())) {
                                text = text+"Cairo does not support dtmfrecog resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().endsWith(MrcpResourceType.BASICSYNTH.toString())) {
                                text = text+"Cairo does not support basicsynth resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().endsWith(MrcpResourceType.SPEAKVERIFY.toString())) {
                                text = text+"Cairo does not support speakverify resource.\n";
                                problemCount++;
                            } else {
                                text = text+"Invalid Resource type: "+attribute.getValue()+"\n";
                                problemCount++;
                            }
                        } else if (attribute.getName().equals("cmid")) {
                            // the value matches the media channel that this
                            // channel is controlling
                        } else if (attribute.getName().equals("resource")) {
                            // in the request. The values can be either
                            // speechrecog or speechsynth
                            if (attribute.getValue().equals(MrcpResourceType.SPEECHRECOG.toString())) {
                                //supportted
                            } else if (attribute.getValue().equals(MrcpResourceType.SPEECHSYNTH.toString())) {
                                //supportted
                            } else if (attribute.getValue().equals(MrcpResourceType.BASICSYNTH.toString())) {
                                text = text+"Cairo does not support basicsynth resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().equals(MrcpResourceType.SPEAKVERIFY.toString())) {
                                text = text+"Cairo does not support speakverify resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().equals(MrcpResourceType.RECORDER.toString())) {
                                //supportted
                            } else if (attribute.getValue().equals(MrcpResourceType.DTMFRECOG.toString())) {
                                text = text+"Cairo does not support dtmfrecog resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().equals(MrcpResourceType.BASICSYNTH.toString())) {
                                text = text+"Cairo does not support basicsynth resource.\n";
                                problemCount++;
                            } else if (attribute.getValue().equals(MrcpResourceType.SPEAKVERIFY.toString())) {
                                text = text+"Cairo does not support speakverify resource.\n";
                                problemCount++;
                            } else {
                                text = text+"Invalid Resource type: "+attribute.getValue()+"\n";
                                problemCount++;
                            }
                        } else {
                            //no validation for this attribute
                        }
                    }

                } else {
                    text = text + "Unrecognized media type/protocol pair in sdp message. type = "+
                                  media.getMediaType() + " proto= " + media.getProtocol()+"\n";
                    problemCount++;
                }
            }
            
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        } 
        if (problemCount>0) {
            _logger.debug("The following "+problemCount+" validation problems were found in the sdp Message\n"+ text);
            _logger.debug(sd.toString());
            throw new SdpException(text);
        }
        _logger.debug("returning from Sdp message validate method.  No problems found");
    }
    
    
}

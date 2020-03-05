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

import gov.nist.javax.sdp.MediaDescriptionImpl;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.ConnectionField;
import gov.nist.javax.sdp.fields.MediaField;
import gov.nist.javax.sdp.fields.OriginField;
import gov.nist.javax.sdp.fields.ProtoVersionField;
import gov.nist.javax.sdp.fields.SessionNameField;

import java.io.Serializable;
import java.net.InetAddress;

import java.net.UnknownHostException;
import java.util.ArrayList;

import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.sdp.Attribute;
import javax.sdp.Connection;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.Version;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;

import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;

/**
 * Encapsulates the sdp message used to to describe session (within SIP
 * messages)
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SdpMessage implements Serializable {

    private static Logger _logger = Logger.getLogger(SdpMessage.class);

    public static final String SDP_AUDIO_MEDIA = "audio";

    public static final String SDP_APPLICATION_MEDIA = "application";

    public static final String SDP_MRCP_PROTOCOL = "TCP/MRCPv2";

    public static final String SDP_RTP_PROTOCOL = "RTP/AVP";

    public static final String SDP_SYNTH_RESOURCE = "speechsynth";

    public static final String SDP_RECOG_RESOURCE = "speechrecog";
    
    public static final String SDP_RECORDER_RESOURCE = "recorder";

    public static final String SDP_SETUP_ATTR_NAME = "setup";

    public static final String SDP_RESOURCE_ATTR_NAME = "resource";

    public static final String SDP_CHANNEL_ATTR_NAME = "channel";

    public static final String SDP_CMID_ATTR_NAME = "cmid";

    public static final String SDP_MID_ATTR_NAME = "mid";

    public static final String SDP_CONNECTION_ATTR_NAME = "connection";

    public static final String SDP_NEW_CONNECTION = "new";

    public static final String SDP_EXISTING_CONNECTION = "existing";

    public static final String SDP_PASSIVE_SETUP = "passive";

    public static final String SDP_ACTIVE_SETUP = "active";

    private SessionDescription _sd;

    public SdpMessage() {
        _sd = new SessionDescriptionImpl();
    }

    public List<MediaDescription> getMrcpChannels() throws SdpException {
        String protocol = SDP_MRCP_PROTOCOL;
        return getChannels(protocol);
    }

    public List<MediaDescription> getRtpChannels() throws SdpException {
        String protocol = SDP_RTP_PROTOCOL;
        return getChannels(protocol);
    }

    
    public List<MediaDescription> getMrcpRecorderChannels() throws SdpException {
        String protocol = SDP_MRCP_PROTOCOL;
        String type = SDP_RECORDER_RESOURCE;
        return getChannels(protocol, type);
    }
    
    public List<MediaDescription> getMrcpReceiverChannels() throws SdpException {
        String protocol = SDP_MRCP_PROTOCOL;
        String type = SDP_RECOG_RESOURCE;
        return getChannels(protocol, type);
    }

    public List<MediaDescription> getMrcpTransmitterChannels() throws SdpException {
        String protocol = SDP_MRCP_PROTOCOL;
        String type = SDP_SYNTH_RESOURCE;
        return getChannels(protocol, type);
    }



    private List<MediaDescription> getChannels(String protocol) throws SdpException {
        List<MediaDescription> chans = new ArrayList<MediaDescription>();
        try {
            Enumeration e = _sd.getMediaDescriptions(true).elements();
            while (e.hasMoreElements()) {
                MediaDescription md = (MediaDescription) e.nextElement();
                if (md.getMedia().getProtocol().equals(protocol)) {
                    chans.add(md);
                }
            }
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return chans;
    }

    private List<MediaDescription> getChannels(String protocol, String type) throws SdpException {
        List<MediaDescription> chans = new ArrayList<MediaDescription>();

        try {
            Enumeration e = _sd.getMediaDescriptions(true).elements();
            while (e.hasMoreElements()) {
                MediaDescription md = (MediaDescription) e.nextElement();
                if (md.getMedia().getProtocol().equals(protocol)) {
                    // if it is a request, then the setup is passive and then
                    // there will be a resource attribute. else get the resource
                    // tytpe from
                    // the channel attribute (channel: channelid@resourcetype)
                    if (md.getAttribute(SDP_SETUP_ATTR_NAME).equalsIgnoreCase(SDP_ACTIVE_SETUP)) {
                        if (md.getAttribute(SDP_RESOURCE_ATTR_NAME).equalsIgnoreCase(type)) {
                            chans.add(md);
                        }
                    } else {
                        String channel = md.getAttribute(SDP_CHANNEL_ATTR_NAME);
                        if (channel.endsWith(type)) {
                            chans.add(md);
                        }
                    }
                }
            }
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return chans;
    } 
    
    public List<MediaDescription> getAudioChansForThisControlChan(MediaDescription control) throws SdpException {

        List<MediaDescription> chans = new ArrayList<MediaDescription>();

        String idToMatch = null;
        String protocolToMatch = null;
        String attributeNameToMatch = null;
        try {
            if (control.getMedia().getProtocol().equals(SDP_MRCP_PROTOCOL)) {
                idToMatch = control.getAttribute(SDP_CMID_ATTR_NAME);
                protocolToMatch = SDP_RTP_PROTOCOL;
                attributeNameToMatch = SDP_MID_ATTR_NAME;
                Enumeration e = _sd.getMediaDescriptions(true).elements();      
                while (e.hasMoreElements()) {
                    MediaDescription md = (MediaDescription) e.nextElement();
                    if (md.getMedia().getProtocol().equals(protocolToMatch)) {
                        if (md.getAttribute(attributeNameToMatch).equalsIgnoreCase(idToMatch)) {
                            chans.add(md);
                        }
                    }
                }            
            } else {
                _logger.error(control.toString() + " not a MRCP control channel");
                throw new SdpException(control.toString() + " not a MRCP control channel");
            }

        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return chans;
    }

    /**
     * @return the sdp session description
     */
    public SessionDescription getSessionDescription() {
        return _sd;
    }

    /**
     * @param sessionDescription
     *            the session description to set
     */
    public void setSessionDescription(SessionDescription sd) {
        this._sd = sd;
    }

    /**
     * Gets the session address.
     * 
     * @return the session address
     * 
     * @throws SdpException
     *             the sdp exception
     */
    public String getSessionAddress() throws SdpException {
        String address = null;
        try {
            address = _sd.getConnection().getAddress();
        } catch (SdpParseException e) {
            _logger.debug(e, e);
            throw e;
        }
        return address;
    }

    /**
     * Sets the session wide internet address.
     * 
     * @param address
     *            the new session address
     * 
     * @throws SdpException
     *             the sdp exception
     */
    public void setSessionAddress(String address) throws SdpException {

        Connection c = _sd.getConnection();
        try {
            if (c != null) {
                c.setAddress(address);
                c.setAddressType("IP4");
                c.setNetworkType("IN");
            } else {
                c = new ConnectionField();
                c.setAddress(address);
                c.setAddressType("IP4");
                c.setNetworkType("IN");
                _sd.setConnection(c);
            }
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
    }

 
    /**
     * Returns a new session message.
     * <p>
     * This factory method should be used by a client who wants to start a new
     * SIP transaction with a new sdp message. A origin line will be generated
     * with the the username and address that is passed in. A session id and
     * session version will be auto generated for the o-line. A Session line
     * will be generated with the session name. The rest of teh message will be
     * empty. It is the responsibility of teh client to populate the media lines
     * etc.
     * <p>
     * The cairo sdpSessionMessage implements the simpler SessionMessage
     * interface that is used by cario clients and servers.
     * 
     * @param
     * @return the sdpSessionMessage
     * @throws SdpException
     */
    public static SdpMessage createNewSdpSessionMessage(String user, String address, String sessionName)
            throws SdpException {
        SdpMessage message = new SdpMessage();
        long sessionId = (System.currentTimeMillis() / 1000) + SdpConstants.NTP_CONST;
        long sessionVersion = (System.currentTimeMillis() / 1000) + SdpConstants.NTP_CONST;

        try {
            Version v = new ProtoVersionField();
            v.setVersion(0);
            message._sd.setVersion(v);
            Origin o = new OriginField();
            o.setAddress(address);
            o.setAddressType("IP4");
            o.setNetworkType("IN");
            o.setSessionId(sessionId);
            o.setSessionVersion(sessionVersion);
            o.setUsername(user);
            message._sd.setOrigin(o);

            Connection c = new ConnectionField();
            c.setAddress(address);
            c.setAddressType("IP4");
            c.setNetworkType("IN");
            message._sd.setConnection(c);

            SessionName sn = new SessionNameField();
            sn.setValue(sessionName);
            message._sd.setSessionName(sn);

        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return message;
    }

    /**
     * Returns a session message configured with the information passed to it in
     * a sdpDescription
     * <p>
     * This factory method should be used upon receipt of a sdp message
     * (probably from the payload of a SIP message). The cairo sdpSessionMessage
     * implements the simpler SessionMessage interface that is used by cario
     * clients and servers.
     * 
     * @param sd
     *            an sdp description from a sdp message
     * @return the sdpSessionMessage
     */
    public static SdpMessage createSdpSessionMessage(SessionDescription sd) {
        SdpMessage message = new SdpMessage();
        message.setSessionDescription(sd);
        return message;
    }

    /**
     * Creates a mrcp channel sdp object for a given resource type
     * 
     * @param resourceType
     *            the resource type (speechrecog or speechsynth)
     * 
     * @return the rcp media description (As a sdp object)
     * 
     * @throws SdpException
     *             the sdp exception
     */
    public static MediaDescription createMrcpChannelRequest(MrcpResourceType resourceType)
            throws SdpException {

        MediaDescription md = new MediaDescriptionImpl();
        Media m = new MediaField();

        try {
            m.setMediaPort(9);

            m.setMediaType(SDP_APPLICATION_MEDIA);
            m.setProtocol(SDP_MRCP_PROTOCOL);
            // m.setPortCount(arg0);
            // m.setMediaFormats(arg0)
            md.setMedia(m);
            md.setAttribute(SDP_SETUP_ATTR_NAME, SDP_ACTIVE_SETUP);
            md.setAttribute(SDP_CONNECTION_ATTR_NAME, SDP_NEW_CONNECTION);
            md.setAttribute(SDP_CMID_ATTR_NAME, "1");

            if (resourceType == MrcpResourceType.SPEECHRECOG) {
                md.setAttribute(SDP_RESOURCE_ATTR_NAME, SDP_RECOG_RESOURCE);
            } else if (resourceType == MrcpResourceType.SPEECHSYNTH) {
               md.setAttribute(SDP_RESOURCE_ATTR_NAME, SDP_SYNTH_RESOURCE);
            } else if (resourceType == MrcpResourceType.RECORDER) {
                md.setAttribute(SDP_RESOURCE_ATTR_NAME, SDP_RECORDER_RESOURCE);
             }
        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return md;
    }

    /**
     * Creates a rtp channel sdp object for a given resource type
     * 
     * @param resourceType
     *            the resource type (speechrecog or speechsynth)
     * 
     * @return the rtp media desription (As a sdp object)
     * 
     * @throws SdpException
     *             the sdp exception
     */
    public static MediaDescription createRtpChannelRequest(int localPort, Vector formats) throws SdpException {

        MediaDescription md = new MediaDescriptionImpl();
        Media m = new MediaField();

        try {
            m.setMediaPort(localPort);
            m.setMediaType(SDP_AUDIO_MEDIA);
            m.setProtocol(SDP_RTP_PROTOCOL);
            m.setMediaFormats(formats);
            // m.setPortCount(arg0);
            // m.setMediaFormats(arg0)
            md.setMedia(m);

            md.setAttribute(SDP_MID_ATTR_NAME, "1");
            md.setAttribute("sendrecv", null);
            // md.setAttribute("sendonly", null);

        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return md;
    }

    /**
     * Creates a rtp channel sdp object for a given resource type
     * 
     * @param resourceType
     *            the resource type (speechrecog or speechsynth)
     * 
     * @return the rtp media desription (As a sdp object)
     * 
     * @throws SdpException
     *             the sdp exception
     */
    public static MediaDescription createRtpChannelRequest(int localPort, Vector formats, String rtpHost) throws SdpException {

        MediaDescription md = new MediaDescriptionImpl();
        Media m = new MediaField();
        Connection c = new ConnectionField();
        try {
            m.setMediaPort(localPort);
            m.setMediaType(SDP_AUDIO_MEDIA);
            m.setProtocol(SDP_RTP_PROTOCOL);
            m.setMediaFormats(formats);
            // m.setPortCount(arg0);
            // m.setMediaFormats(arg0)
            md.setMedia(m);

            md.setAttribute(SDP_MID_ATTR_NAME, "1");
            md.setAttribute("sendrecv", null);
            c.setAddress(rtpHost);
            c.setAddressType("IP4");
            c.setNetworkType("IN");
            md.setConnection(c);
            // md.setAttribute("sendonly", null);

        } catch (SdpException e) {
            _logger.debug(e, e);
            throw e;
        }
        return md;
    }   
    
}



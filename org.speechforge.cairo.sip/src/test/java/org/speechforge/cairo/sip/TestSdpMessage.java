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

import java.util.Vector;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.SessionName;

import org.junit.Assert;
import org.junit.Test;
import org.mrcp4j.MrcpResourceType;

/**
 * Unit test for SIPAgent.
 */
public class TestSdpMessage {
    /**
     * Create the test case
     */
    public TestSdpMessage() {
    }

    @Test
    public void testCreateNewSdpSessionMessage() throws SdpException {
        String user = "slord";
        String address = "a.b.com";
        String sessionName = "mySession";
        SdpMessage s = SdpMessage.createNewSdpSessionMessage(user, address, sessionName);
        // String sdpString = s.getSessionDescription().toString();
        Origin o = s.getSessionDescription().getOrigin();
        SessionName sn = s.getSessionDescription().getSessionName();
        Assert.assertEquals(o.getAddress(), address);
        Assert.assertEquals(o.getUsername(), user);
        Assert.assertEquals(sn.getValue(), sessionName);
    }

    @Test
    public void testCreateMrcpChannelRequest() throws SdpException {
        MrcpResourceType resourceType = MrcpResourceType.SPEECHRECOG;
        MediaDescription md = SdpMessage.createMrcpChannelRequest(resourceType);
        Media m = md.getMedia();
        Assert.assertEquals(m.getMediaPort(), 9);
        Assert.assertEquals(m.getMediaType(), "application");
        Assert.assertEquals(m.getProtocol(), "TCP/MRCPv2");
        // assertEquals(m.getPortCount(),1);

        Assert.assertEquals(md.getAttribute("setup"), "active");
        Assert.assertEquals(md.getAttribute("connection"), "new");
        Assert.assertEquals(md.getAttribute("resource"), "speechrecog");

        resourceType = MrcpResourceType.SPEECHSYNTH;
        md = SdpMessage.createMrcpChannelRequest(resourceType);
        m = md.getMedia();
        Assert.assertEquals(m.getMediaPort(), 9);
        Assert.assertEquals(m.getMediaType(), "application");
        Assert.assertEquals(m.getProtocol(), "TCP/MRCPv2");
        // assertEquals(m.getPortCount(),1);

        Assert.assertEquals(md.getAttribute("setup"), "active");
        Assert.assertEquals(md.getAttribute("connection"), "new");
        Assert.assertEquals(md.getAttribute("resource"), "speechsynth");
    }

    @Test
    public void testCreateRtpChannelRequest() throws SdpException {
        int localPort = 12345;
        Vector format = new Vector();
        format.add(SdpConstants.PCMU);
        MediaDescription md = SdpMessage.createRtpChannelRequest(localPort,format);
        Media m = md.getMedia();
        Assert.assertEquals(m.getMediaPort(), localPort);
        Assert.assertEquals(m.getMediaType(), "audio");
        Assert.assertEquals(m.getProtocol(), "RTP/AVP");
        // assertEquals(m.getPortCount(),1);

        Assert.assertEquals(md.getAttribute("sendrecv"), null); // bogus test...
        // assertEquals(md.getAttribute("sendonly"),null);
        // LOGGER.info(md.toString());
    }
}

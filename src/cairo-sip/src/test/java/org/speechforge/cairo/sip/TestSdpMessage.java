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

import org.mrcp4j.MrcpResourceType;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionName;
import junit.framework.Test;
import junit.framework.TestSuite;


import org.apache.log4j.Logger;

/**
 * Unit test for SIPAgent.
 */
public class TestSdpMessage extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TestSdpMessage.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TestSdpMessage(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestSdpMessage.class);
    }

    public void testCreateNewSdpSessionMessage() throws SdpException {
        debugTestName(_logger);
        String user = "slord";
        String address = "a.b.com";
        String sessionName = "mySession";
        SdpMessage s = SdpMessage.createNewSdpSessionMessage(user, address, sessionName);
        // String sdpString = s.getSessionDescription().toString();
        Origin o = s.getSessionDescription().getOrigin();
        SessionName sn = s.getSessionDescription().getSessionName();
        try {
            assertEquals(o.getAddress(), address);
            assertEquals(o.getUsername(), user);
            assertEquals(sn.getValue(), sessionName);
        } catch (SdpParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void testCreateMrcpChannelRequest() throws SdpException {
        debugTestName(_logger);

        MrcpResourceType resourceType = MrcpResourceType.SPEECHRECOG;
        MediaDescription md = SdpMessage.createMrcpChannelRequest(resourceType);
        Media m = md.getMedia();
        try {
            assertEquals(m.getMediaPort(), 9);
            assertEquals(m.getMediaType(), "application");
            assertEquals(m.getProtocol(), "TCP/MRCPv2");
            // assertEquals(m.getPortCount(),1);

            assertEquals(md.getAttribute("setup"), "active");
            assertEquals(md.getAttribute("connection"), "new");
            assertEquals(md.getAttribute("resource"), "speechrecog");

        } catch (SdpParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        resourceType = MrcpResourceType.SPEECHSYNTH;
        md = SdpMessage.createMrcpChannelRequest(resourceType);
        m = md.getMedia();
        try {
            assertEquals(m.getMediaPort(), 9);
            assertEquals(m.getMediaType(), "application");
            assertEquals(m.getProtocol(), "TCP/MRCPv2");
            // assertEquals(m.getPortCount(),1);

            assertEquals(md.getAttribute("setup"), "active");
            assertEquals(md.getAttribute("connection"), "new");
            assertEquals(md.getAttribute("resource"), "speechsynth");

        } catch (SdpParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void testCreateRtpChannelRequest() throws SdpException {
        debugTestName(_logger);

        int localPort = 12345;
        Vector format = new Vector();
        format.add(SdpConstants.PCMU);
        MediaDescription md = SdpMessage.createRtpChannelRequest(localPort,format);
        Media m = md.getMedia();
        try {
            assertEquals(m.getMediaPort(), localPort);
            assertEquals(m.getMediaType(), "audio");
            assertEquals(m.getProtocol(), "RTP/AVP");
            // assertEquals(m.getPortCount(),1);

            assertEquals(md.getAttribute("sendrecv"), null); // bogus test...
            // assertEquals(md.getAttribute("sendonly"),null);
            // System.out.println(md.toString());

        } catch (SdpParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

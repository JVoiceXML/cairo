package org.speechforge.cairo.sip;

import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Test cases for {@link SdpMessageFilter}.
 * @author Dirk Schnelle-Walka
 *
 */
public class SdpMessageFilterTest {

    /**
     * Test method for {@link org.speechforge.cairo.sip.SdpMessageFilter#filter(org.speechforge.cairo.sip.SdpMessage)}.
     * @exception Exception
     *                  test failed
     */
    @Test
    public void testValidate() throws Exception {
        final SdpFactory sdpFactory = SdpFactory.getInstance();
        final SessionDescription sd = sdpFactory
                .createSessionDescription(SdpTestMessages.INVITE_REQUEST_3);
        final SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

        //validate the sdp message (throw sdpException if the message is invalid)
        Vector<MediaDescription> descriptions =
                SdpMessageFilter.filter(sdpMessage);
        Assert.assertEquals(descriptions.size(), sd.getMediaDescriptions(true).size());
    }

    /**
     * Test method for {@link org.speechforge.cairo.sip.SdpMessageFilter#filter(org.speechforge.cairo.sip.SdpMessage)}.
     * @exception Exception
     *                  test failed
     */
    @Test
    public void testValidateWIthVideo() throws Exception {
        final SdpFactory sdpFactory = SdpFactory.getInstance();
        final SessionDescription sd = sdpFactory
                .createSessionDescription(SdpTestMessages.INVITE_REQUEST_4);
        final SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

        //validate the sdp message (throw sdpException if the message is invalid)
        Vector<MediaDescription> descriptions =
                SdpMessageFilter.filter(sdpMessage);
        Assert.assertEquals(descriptions.size(), sd.getMediaDescriptions(true).size() - 1);
    }
}

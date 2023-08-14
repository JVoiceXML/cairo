package org.speechforge.cairo.sip;

import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.junit.Test;

/**
 * Test cases for {@link SdpMessageValidator}.
 * 
 * @author Dirk Schnelle-Walka
 * @since 0.3.1
 */
public class SdpMessageValidatorTest {

    /**
     * Test method for
     * {@link org.speechforge.cairo.sip.SdpMessageValidator#validate(org.speechforge.cairo.sip.SdpMessage)}.
     * @throws SdpException test failed
     */
    @Test
    public void testValidate() throws SdpException {
        final SdpFactory sdpFactory = SdpFactory.getInstance();
        final SessionDescription sd = sdpFactory
                .createSessionDescription(SdpTestMessages.INVITE_REQUEST_3);
        final SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

        //validate the sdp message (throw sdpException if the message is invalid)
        SdpMessageValidator.validate(sdpMessage);
    }

    /**
     * Test method for
     * {@link org.speechforge.cairo.sip.SdpMessageValidator#validate(org.speechforge.cairo.sip.SdpMessage)}.
     * @throws SdpException test failed
     */
    @Test
    public void testValidateOtherConnectionOrder() throws SdpException {
        final SdpFactory sdpFactory = SdpFactory.getInstance();
        final SessionDescription sd = sdpFactory
                .createSessionDescription(SdpTestMessages.SDP_INVITE_REQUEST);
        final SdpMessage sdpMessage = SdpMessage.createSdpSessionMessage(sd);

        //validate the sdp message (throw sdpException if the message is invalid)
        SdpMessageValidator.validate(sdpMessage);
    }

}

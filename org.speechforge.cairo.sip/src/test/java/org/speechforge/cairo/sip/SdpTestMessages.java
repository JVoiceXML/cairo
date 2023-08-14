package org.speechforge.cairo.sip;

public class SdpTestMessages {

    public static final String INVITE_REQUEST_1 = "INVITE sip:mresources@server.example.com SIP/2.0\r\n"
            + "Max-Forwards:6\r\n" 
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" 
            + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314161 INVITE\r\n" 
            + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" 
            + "Content-Length:142\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842808 IN IP4 192.168.64.4\r\n" 
            + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" 
            + "c=IN IP4 10.2.17.12\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" 
            + "a=setup:active\r\n" + "a=connection:existing\r\n"
            + "a=resource:speechsynth\r\n" 
            + "a=cmid:1\r\n" 
            + "m=audio 49170 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" 
            + "a=recvonly";

    public static final String INVITE_RESPONSE_1 = " SIP/2.0 200 OK\r\n"
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" 
            + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314161 INVITE\r\n" 
            + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" 
            + "Content-Length:131\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842808 IN IP4 192.168.64.4\r\n" 
            + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" 
            + "c=IN IP4 10.2.17.11\r\n"
            + "m=application 32416 TCP/MRCPv2 1\r\n" 
            + "a=setup:passive\r\n" + "a=connection:existing\r\n"
            + "a=channel:32AECB23433801@speechsynth\r\n" 
            + "a=cmid:1\r\n" + "m=audio 48260 RTP/AVP 0\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" 
            + "a=sendonly\r\n" 
            + "a=mid:1\r\n";

    public static final String INVITE_REQUEST_2 = "INVITE sip:mresources@server.example.com SIP/2.0\r\n"
            + "Max-Forwards:6\r\n" 
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" 
            + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314163 INVITE\r\n" 
            + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" 
            + "Content-Length:142\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842809 IN IP4 192.168.64.4\r\n" 
            + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" 
            + "c=IN IP4 10.2.17.12\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" 
            + "a=setup:active\r\n" + "a=connection:existing\r\n"
            + "a=resource:speechsynth\r\n" 
            + "a=cmid:1\r\n" 
            + "m=audio 49170 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" 
            + "a=recvonly\r\n" 
            + "a=mid:1\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" 
            + "a=setup:active\r\n" 
            + "a=connection:existing\r\n"
            + "a=resource:speechrecog\r\n" 
            + "a=cmid:2\r\n" 
            + "m=audio 49180 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" 
            + "a=rtpmap:96 telephone-event/8000\r\n" 
            + "a=fmtp:96 0-15\r\n"
            + "a=sendonly\r\n";

    public static final String INVITE_RESPONSE_2 = "SIP/2.0 200 OK\r\n"
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" 
            + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314163 INVITE\r\n" 
            + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" 
            + "Content-Length:131\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842809 IN IP4 192.168.64.4\r\n" 
            + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" 
            + "c=IN IP4 10.2.17.11\r\n"
            + "m=application 32416 TCP/MRCPv2 1\r\n" 
            + "a=channel:32AECB23433801@speechsynth\r\n"
            + "a=cmid:1\r\n" 
            + "m=audio 48260 RTP/AVP 0\r\n" 
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=sendonly\r\n"
            + "a=mid:1\r\n" 
            + "m=application 32416 TCP/MRCPv2 1\r\n"
            + "a=channel:32AECB23433801@speechrecog\r\n" 
            + "a=cmid:2\r\n" + "m=audio 48260 RTP/AVP 0\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" 
            + "a=rtpmap:96 telephone-event/8000\r\n" 
            + "a=fmtp:96 0-15\r\n"
            + "a=recvonly\r\n" + "a=mid:2\r\n";

    public static final String INVITE_REQUEST_3 = "v=0\r\n" 
            + "o=slord 13760799956958020 13760799956958020"
            + " IN IP4 127.0.0.1\r\n" 
            + "s= \r\n" 
            + "c=IN IP4  127.0.0.1\r\n" 
            + "t=0 0\r\n"
            + "m=application 9  TCP/MRCPv2 1\r\n" 
            + "a=setup:active\r\n" + "a=connection:new\r\n"
            + "a=resource:speechsynth\r\n" 
            + "a=cmid:1\r\n"
            + "m=application 9  TCP/MRCPv2 1\r\n" 
            + "a=setup:active\r\n" 
            + "a=connection:new\r\n"
            + "a=resource:speechrecog\r\n" 
            + "a=cmid:1\r\n"
            + "m=audio 6022 RTP/AVP 0 96\r\n" 
            + "a=rtpmap:0 pcmu/8000\r\n"
            + "a=rtpmap:96 telephone-event/8000\r\n" 
            + "a=fmtp:96 0-15\r\n" 
            + "a=sendrecv\r\n"
            + "a=mid:1\r\n";

    public static final String INVITE_RESPONSE_3 = "v=0\r\n" 
            + "o=slord 13760799956958020 13760799956958020"
            + " IN IP4 127.0.0.1\r\n" 
            + "s= \r\n" 
            + "c=IN IP4  127.0.0.1\r\n" 
            + "t=0 0\r\n"
            + "m=application 100 TCP/MRCPv2 1\r\n" 
            + "a=setup:passive\r\n" 
            + "a=connection:new\r\n"
            + "a=channel:32AECB23433802@speechsynth\r\n" 
            + "a=cmid:1\r\n"
            + "m=application 200 TCP/MRCPv2 1\r\n" 
            + "a=setup:passive\r\n" 
            + "a=connection:new\r\n"
            + "a=channel:32AECB23433801@speechrecog\r\n" 
            + "a=cmid:1\r\n"
            + "m=audio 6022 RTP/AVP 0 96\r\n" 
            + "a=rtpmap:0 pcmu/8000\r\n"
            + "a=rtpmap:96 telephone-event/8000\r\n" 
            + "a=fmtp:96 0-15\r\n" 
            + "a=sendrecv\r\n"
            + "a=mid:1\r\n";
    
    public static final String SDP_INVITE_REQUEST = "v=0\n" + 
            "o=- 3801466427 3801466427 IN IP4 192.168.188.47\n" + 
            "s=pjmedia\n" + 
            "b=AS:84\n" + 
            "t=0 0\n" + 
            "a=X-nat:0\n" + 
            "m=audio 4002 RTP/AVP 8 0 101\n" + 
            "c=IN IP4 192.168.188.47\n" + 
            "b=TIAS:64000\n" + 
            "a=rtcp:4003 IN IP4 192.168.188.47\n" + 
            "a=sendrecv\n" + 
            "a=rtpmap:8 PCMA/8000\n" + 
            "a=rtpmap:0 PCMU/8000\n" + 
            "a=rtpmap:101 telephone-event/8000\n" + 
            "a=fmtp:101 0-16\n" + 
            "a=ssrc:849434036 cname:6a2d5b823c0b7084";
}

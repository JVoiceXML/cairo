package org.speechforge.cairo.sip;

public class SdpTestMessages {

    public static String inviteRequest1 = "INVITE sip:mresources@server.example.com SIP/2.0\r\n"
            + "Max-Forwards:6\r\n" + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314161 INVITE\r\n" + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" + "Content-Length:142\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842808 IN IP4 192.168.64.4\r\n" + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" + "c=IN IP4 10.2.17.12\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" + "a=setup:active\r\n" + "a=connection:existing\r\n"
            + "a=resource:speechsynth\r\n" + "a=cmid:1\r\n" + "m=audio 49170 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=recvonly";

    public static String inviteResposne1 = " SIP/2.0 200 OK\r\n"
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314161 INVITE\r\n" + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" + "Content-Length:131\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842808 IN IP4 192.168.64.4\r\n" + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" + "c=IN IP4 10.2.17.11\r\n"
            + "m=application 32416 TCP/MRCPv2 1\r\n" + "a=setup:passive\r\n" + "a=connection:existing\r\n"
            + "a=channel:32AECB23433801@speechsynth\r\n" + "a=cmid:1\r\n" + "m=audio 48260 RTP/AVP 0\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=sendonly\r\n" + "a=mid:1\r\n";

    public static String InviteRequest2 = "INVITE sip:mresources@server.example.com SIP/2.0\r\n"
            + "Max-Forwards:6\r\n" + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314163 INVITE\r\n" + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" + "Content-Length:142\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842809 IN IP4 192.168.64.4\r\n" + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" + "c=IN IP4 10.2.17.12\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" + "a=setup:active\r\n" + "a=connection:existing\r\n"
            + "a=resource:speechsynth\r\n" + "a=cmid:1\r\n" + "m=audio 49170 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=recvonly\r\n" + "a=mid:1\r\n"
            + "m=application 9 TCP/MRCPv2 1\r\n" + "a=setup:active\r\n" + "a=connection:existing\r\n"
            + "a=resource:speechrecog\r\n" + "a=cmid:2\r\n" + "m=audio 49180 RTP/AVP 0 96\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=rtpmap:96 telephone-event/8000\r\n" + "a=fmtp:96 0-15\r\n"
            + "a=sendonly\r\n";

    public static String InviteResponse2 = "SIP/2.0 200 OK\r\n"
            + "To:MediaServer <sip:mresources@server.example.com>\r\n"
            + "From:sarvi <sip:sarvi@example.com>;tag=1928301774\r\n" + "Call-ID:a84b4c76e66710\r\n"
            + "CSeq:314163 INVITE\r\n" + "Contact:<sip:sarvi@example.com>\r\n"
            + "Content-Type:application/sdp\r\n" + "Content-Length:131\r\n" + "v=0\r\n"
            + "o=sarvi 2890844526 2890842809 IN IP4 192.168.64.4\r\n" + "s=SDP Seminar\r\n"
            + "i=A session for processing media\r\n" + "c=IN IP4 10.2.17.11\r\n"
            + "m=application 32416 TCP/MRCPv2 1\r\n" + "a=channel:32AECB23433801@speechsynth\r\n"
            + "a=cmid:1\r\n" + "m=audio 48260 RTP/AVP 0\r\n" + "a=rtpmap:0 pcmu/8000\r\n" + "a=sendonly\r\n"
            + "a=mid:1\r\n" + "m=application 32416 TCP/MRCPv2 1\r\n"
            + "a=channel:32AECB23433801@speechrecog\r\n" + "a=cmid:2\r\n" + "m=audio 48260 RTP/AVP 0\r\n"
            + "a=rtpmap:0 pcmu/8000\r\n" + "a=rtpmap:96 telephone-event/8000\r\n" + "a=fmtp:96 0-15\r\n"
            + "a=recvonly\r\n" + "a=mid:2\r\n";

    public static String inviteRequest3 = "v=0\r\n" + "o=slord 13760799956958020 13760799956958020"
            + " IN IP4 127.0.0.1\r\n" + "s= \r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"

            + "m=application 9  TCP/MRCPv2 1\r\n" + "a=setup:active\r\n" + "a=connection:new\r\n"
            + "a=resource:speechsynth\r\n" + "a=cmid:1\r\n"

            + "m=application 9  TCP/MRCPv2 1\r\n" + "a=setup:active\r\n" + "a=connection:new\r\n"
            + "a=resource:speechrecog\r\n" + "a=cmid:1\r\n"

            + "m=audio 6022 RTP/AVP 0 96\r\n" + "a=rtpmap:0 pcmu/8000\r\n"
            + "a=rtpmap:96 telephone-event/8000\r\n" + "a=fmtp:96 0-15\r\n" + "a=sendrecv\r\n"
            + "a=mid:1\r\n";

    public static String inviteResponse3 = "v=0\r\n" + "o=slord 13760799956958020 13760799956958020"
            + " IN IP4 127.0.0.1\r\n" + "s= \r\n" + "c=IN IP4  127.0.0.1\r\n" + "t=0 0\r\n"

            + "m=application 100 TCP/MRCPv2 1\r\n" + "a=setup:passive\r\n" + "a=connection:new\r\n"
            + "a=channel:32AECB23433802@speechsynth\r\n" + "a=cmid:1\r\n"

            + "m=application 200 TCP/MRCPv2 1\r\n" + "a=setup:passive\r\n" + "a=connection:new\r\n"
            + "a=channel:32AECB23433801@speechrecog\r\n" + "a=cmid:1\r\n"

            + "m=audio 6022 RTP/AVP 0 96\r\n" + "a=rtpmap:0 pcmu/8000\r\n"
            + "a=rtpmap:96 telephone-event/8000\r\n" + "a=fmtp:96 0-15\r\n" + "a=sendrecv\r\n"
            + "a=mid:1\r\n";

}

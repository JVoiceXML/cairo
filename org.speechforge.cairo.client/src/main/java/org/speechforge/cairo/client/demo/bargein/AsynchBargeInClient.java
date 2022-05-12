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
package org.speechforge.cairo.client.demo.bargein;


import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SimpleSipAgent;
import org.speechforge.cairo.sip.SipSession;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpConstants;
import javax.sdp.SdpException;
import javax.sip.SipException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpResponse;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Demo MRCPv2 client application that plays a TTS prompt while performing speech recognition on
 * microphone input.  Prompt playback is cancelled as soon as start of speech is detected.
 * Uses the asynchronous client methods
 *
 * @author Spencer Lord{@literal <}<a href="salord@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class AsynchBargeInClient  {

    private static Logger LOGGER = Logger.getLogger(AsynchBargeInClient.class);

    private static final String BEEP_OPTION = "beep";
    private static final String LOOP_OPTION = "loop";
    private static final String PARROT_OPTION = "parrot";
    
    public static final String HELP_OPTION = "help";
    public static final String RSERVERHOST_OPTION = "rserverhost";

    private static boolean _beep = false;
    private static Toolkit _toolkit = null;
    private static boolean _loop = false;
    private static boolean _parrot = false;

    private static SimpleSipAgent sipAgent;
    private static  boolean sentBye=false;
    
    private static int _myPort = 5090;
    private static String _host = null;
    private static int _peerPort = 5050;
    private static String _mySipAddress ="sip:speechSynthClient@speechforge.org";
    private static String _cairoSipAddress="sip:cairo@speechforge.org";    

    private static NativeMediaClient mediaClient;
    

    private static Options getOptions() {


         Options options = new Options();
         Option option = new Option(HELP_OPTION, "print this message");
         options.addOption(option);

         option = new Option(RSERVERHOST_OPTION, true, "location of resource server (defaults to localhost)");
         option.setArgName("host");
         options.addOption(option);

         option = new Option(BEEP_OPTION, "play response/event timing beep");
         options.addOption(option);

         option = new Option(LOOP_OPTION, "loop recognition until quit statement recognized");
         options.addOption(option);

         option = new Option(PARROT_OPTION, "repeat back recognized utterances via TTS");
         options.addOption(option);

         return options;
    }


////////////////////////////////////
// main method
////////////////////////////////////

    public static void main(String[] args) throws Exception {
        
        // setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
        // unexpected crash (ie ctrl-c)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (mediaClient != null) {
                    mediaClient.stop();
                }
                if (!sentBye && sipAgent!=null) {
                    try {
                        sipAgent.sendBye();
                        sipAgent.dispose();

                    } catch (SipException e) {
                        LOGGER.warn(e.getMessage(), e);
                    }


                }
            }
        });

        
        //get the command line args
        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();

        if (args.length != 3 || line.hasOption(HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("BargeInClient [options] <local-rtp-port> <grammar-URL> <prompt-text>", options);
            return;
        }

        _beep = line.hasOption(BEEP_OPTION);
        if (_beep) {
            _toolkit = Toolkit.getDefaultToolkit();
        }

        _loop = line.hasOption(LOOP_OPTION);
        _parrot = line.hasOption(PARROT_OPTION);

        int localRtpPort = -1;

        try {
            localRtpPort = Integer.parseInt(args[0]);
        } catch (Exception e) {
            LOGGER.debug(e, e);
        }

        if (localRtpPort < 0 || localRtpPort >= RTPConsumer.TCP_PORT_MAX || localRtpPort % 2 != 0) {
            throw new Exception("Improper format for first command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }

        String grammar = args[1];
        URL grammarUrl = new URL(args[1]);
        String prompt = args[2];
        
        // lookup resource server
        InetAddress rserverHost = line.hasOption(RSERVERHOST_OPTION) ?
            InetAddress.getByName(line.getOptionValue(RSERVERHOST_OPTION)) : CairoUtil.getLocalHost(); 
        
        try {
            _host = CairoUtil.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            _host = "localhost";
        }
        String peerAddress = rserverHost.getHostAddress();

        // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
        sipAgent = new SimpleSipAgent(_mySipAddress, "Synth Client Sip Stack", _myPort, "UDP");

        // Construct the SDP message that will be sent in the SIP invitation
        Vector format = new Vector();
        format.add(SdpConstants.PCMU);
        SdpMessage message = SpeechClientImpl.constructResourceMessage(localRtpPort,format, _mySipAddress, _host, "Session Name");

        // Send the sip invitation (This method on the SimpleSipAgent blocks until a response is received or timeout occurs) 
        LOGGER.info("Sending a SIP invitation to the cairo server.");
        SdpMessage inviteResponse = sipAgent.sendInviteWithoutProxy(_cairoSipAddress, message, peerAddress, _peerPort);

        if (inviteResponse != null) {
            LOGGER.info("Received the SIP Response.");
        
            // Get the MRCP media channels (need the port number and the channelID that are sent
            // back from the server in the response in order to setup the MRCP channel)
            List <MediaDescription> xmitterChans = inviteResponse.getMrcpTransmitterChannels();
            int xmitterPort = xmitterChans.get(0).getMedia().getMediaPort();
            String xmitterChannelId = xmitterChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

            List <MediaDescription> receiverChans = inviteResponse.getMrcpReceiverChannels();
            MediaDescription controlChan = receiverChans.get(0);
            int receiverPort = controlChan.getMedia().getMediaPort();
            String receiverChannelId = receiverChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);


            List <MediaDescription> rtpChans = inviteResponse.getAudioChansForThisControlChan(controlChan);
            int remoteRtpPort = -1;
            if (rtpChans.size() > 0) {
                //TODO: What if there is more than 1 media channels?
                //TODO: check if there is an override for the host attribute in the m block
                //InetAddress remoteHost = InetAddress.getByName(rtpmd.get(1).getAttribute();
                remoteRtpPort =  rtpChans.get(0).getMedia().getMediaPort();
                //rtpmd.get(1).getMedia().setMediaPort(localPort);
            } else {
                LOGGER.warn("No Media channel specified in the invite request");
                //TODO:  handle no media channel in the response corresponding tp the mrcp channel (sip/sdp error)
            }   

            //construct a media client to stream the audio (both ways) and start streaming
            LOGGER.debug("Starting NativeMediaClient...");
            mediaClient = new NativeMediaClient(localRtpPort, rserverHost, remoteRtpPort);
            mediaClient.startTransmit();

            MrcpChannel recogChannel = SpeechClientImpl.createRecogChannel(receiverChannelId, rserverHost, receiverPort);
            MrcpChannel ttsChannel = SpeechClientImpl.createTtsChannel(xmitterChannelId, rserverHost, xmitterPort);
           
            
            //construct the speech client with this session
            SpeechClient _client = new SpeechClientImpl(ttsChannel, recogChannel);
            
            _client.turnOnBargeIn();
            //now we can run the demo...
            try {

                RecognitionResult result = null;

                String parrotString;
                do {
                    result = _client.playAndRecognizeBlocking(false, prompt, grammar, false);

                    if (LOGGER.isInfoEnabled()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("\n**************************************************************");
                        sb.append("\nRecognition result: ").append(result);
                        sb.append("\n**************************************************************\n");
                        LOGGER.info(sb);
                    }


                    if (result == null) {
                        parrotString = "I'm sorry, I could not understand.";
                    } else {
                       
                       if (result.isOutOfGrammar()) {
                           parrotString = "I'm sorry, I could not understand.  Your response was out of grammar.";
                       } else {
                           parrotString = result.getText(); 
                       }
                    }
                    if (_parrot) {
                        _client.playBlocking(false,parrotString);
                    }

                    //TODO: check the natural language elements in recognition result for tag:value == main:quit
                } while (_loop && !parrotString.contains("exit") && !parrotString.contains("quit"));


            } catch (Exception e){
                if (e instanceof MrcpInvocationException) {
                    MrcpResponse response = ((MrcpInvocationException) e).getResponse();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("MRCP response received:\n" + response.toString());
                    }
                }
                LOGGER.warn(e, e);
                sipAgent.sendBye();
                sipAgent.dispose();
                sentBye = true;
                System.exit(1);
            }

        } else {
            //Invitation Timeout
            LOGGER.info("Sip Invitation timed out.  Is server running?");
        }
        
        if (sipAgent != null){
            sipAgent.sendBye();
            sipAgent.dispose();
            sentBye = true;
         }
        if (mediaClient != null)
            mediaClient.stop();
        System.exit(0);
    }

}

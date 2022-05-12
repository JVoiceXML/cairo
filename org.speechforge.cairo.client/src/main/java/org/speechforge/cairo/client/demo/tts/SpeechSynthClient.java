/*
 * cairo-client - Open source client for control of speech media resources.
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
package org.speechforge.cairo.client.demo.tts;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.sip.SipException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpResponse;
import org.speechforge.cairo.client.SessionManager;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.util.CairoUtil;

/**
 * Demo MRCPv2 client application that utilizes a {@code speechsynth} resource to play a TTS prompt.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SpeechSynthClient {

    private static Logger LOGGER = Logger.getLogger(SpeechSynthClient.class);

    private static final String BEEP_OPTION = "beep";
    private static final String REPETITIONS_OPTION = "reps";
    private static final String URL_OPTION = "url";
    
    public static final String HELP_OPTION = "help";
    public static final String RSERVERHOST_OPTION = "rserverhost";

    private static boolean _beep = false;
    private static Toolkit _toolkit = null;
    private static int _repetitions = 1;
    private static boolean _url;
    
    private static SessionManager sm;

    private MrcpChannel _ttsChannel;
    private int _rep = 1;
    private static  boolean endedSession=false;

    private static NativeMediaClient _mediaClient; 
    
    private static int _myPort = 5070;
    private static String _host = null;
    private static int _peerPort = 5050;
    private static String _mySipAddress ="sip:speechSynthClient@speechforge.org";
    private static String _cairoSipAddress="sip:cairo@speechforge.org";
        
    private static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(RSERVERHOST_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("host");
        options.addOption(option);
        
        option = new Option(BEEP_OPTION, "play response/event timing beep");
        options.addOption(option);
        
        option = new Option(URL_OPTION, "play the wave or text file at the given url");
        options.addOption(option);

        option = new Option(REPETITIONS_OPTION, true, "number of times to repeat the TTS prompt");
        option.setArgName("repetitions");
        options.addOption(option);

        return options;
    }



    public static void main(String[] args) throws Exception {
 
        // setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
        // unexpected crash (ie ctrl-c)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (!endedSession && sm!=null) {
                    try {
                        _mediaClient.shutdown();
                        sm.shutdown();
                        endedSession = true;
                    } catch (SipException e) {
                        LOGGER.warn(e.getMessage(), e);
                    }

                }
            }
        }); 
        
        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();
        
        if (args.length != 2 || line.hasOption(HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SpeechSynthClient [options] <local-rtp-port> <prompt-text>", options);
            return;
        }

        if (line.hasOption(REPETITIONS_OPTION)) {
            try {
                _repetitions = Integer.parseInt(line.getOptionValue(REPETITIONS_OPTION));
            } catch (NumberFormatException e) {
                LOGGER.debug("Could not parse repetitions parameter to int!", e);
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("SpeechSynthClient [options] <prompt-text> <local-rtp-port>", options);
                return;
            }
        }

        _beep = line.hasOption(BEEP_OPTION);
        if (_beep) {
            _toolkit = Toolkit.getDefaultToolkit();
        }
        
        _url = line.hasOption(URL_OPTION);

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

        String text = args[1];
        InetAddress rserverHost = line.hasOption(RSERVERHOST_OPTION) ?
            InetAddress.getByName(line.getOptionValue(RSERVERHOST_OPTION)) : CairoUtil.getLocalHost();

        try {
            _host = CairoUtil.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            _host = "localhost";
        }
        String peerAddress = rserverHost.getHostAddress();

        //Construct the session manager and set it up

        sm = new SessionManager();
        sm.setCairoSipAddress(_cairoSipAddress);
        sm.setCairoSipHostName(peerAddress);
        sm.setCairoSipPort(_peerPort);

        sm.setPort(_myPort);
        sm.setMySipAddress(_mySipAddress);
        sm.setStackName("Test Sip Stack");
        sm.setTransport("UDP");
        sm.startup();

        //set up the mrcp channels
        SipSession session = sm.newSynthChannel(localRtpPort, _host, "Session Name");
        
        if (session != null) {
        
            //Setup a media client to receive and play the sythesized voice data streamed over the RTP channel
            LOGGER.debug("Starting NativeMediaClient for receive only...");
            _mediaClient = new NativeMediaClient(localRtpPort); 
            
            //construct the speech client with this session
            SpeechClient _client = new SpeechClientImpl(session.getTtsChannel(), null);

            // Use the MRCP channel to instruct the cairo server to sythesize voice data and send it over the
            // RTP channel as specified in teh SIP invitation
            try {
                for (int i=0; i < _repetitions; i++) {
                    if (_url) {
                       _client.playBlocking(true, text);  
                    } else {
                    	_client.playBlocking(false, text);
                    }
                }
            } catch (Exception e){
                if (e instanceof MrcpInvocationException) {
                    MrcpResponse response = ((MrcpInvocationException) e).getResponse();
                    LOGGER.warn("MRCP response received:\n" + response);
                }
                LOGGER.warn(e, e);
                sm.shutdown();
                endedSession = true;
                System.exit(1);
            }

        } else {
            //Invitation Timeout
            LOGGER.info("Sip Invitation timed out or failed.  Is server running?");
        }
        sm.shutdown();
        endedSession = true;
        System.exit(1);
    }
}

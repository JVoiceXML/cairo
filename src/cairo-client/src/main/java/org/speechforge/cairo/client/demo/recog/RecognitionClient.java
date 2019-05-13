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
package org.speechforge.cairo.client.demo.recog;

import org.speechforge.cairo.client.SessionManager;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.sip.SimpleSipAgent;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.util.CairoUtil;

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
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;


/**
 * Demo MRCPv2 client application that utilizes a {@code speechrecog} resource to perform
 * speech recognition on microphone input.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionClient  {

    private static Logger _logger = Logger.getLogger(RecognitionClient.class);

    private static final String BEEP_OPTION = "beep";
    private static final String URL_OPTION = "url";
    
    public static final String HELP_OPTION = "help";
    public static final String RSERVERHOST_OPTION = "rserverhost";
    
    
    private static SessionManager sm;
    
    private static String examplePhrase;

    private static boolean _beep = false;
    private static Toolkit _toolkit = null;
    private static boolean _url;

    private static SimpleSipAgent sipAgent;
    private static  boolean sentBye=false;

    private static int _myPort = 5080;
    private static String _host = null;
    private static int _peerPort = 5050;
    private static String _mySipAddress ="sip:speechSynthClient@speechforge.org";
    private static String _cairoSipAddress="sip:cairo@speechforge.org";
    private static NativeMediaClient mediaClient;

    public static Options getOptions() {

        Options options = new Options();
        Option option = new Option(HELP_OPTION, "print this message");
        options.addOption(option);

        option = new Option(RSERVERHOST_OPTION, true, "location of resource server (defaults to localhost)");
        option.setArgName("host");
        options.addOption(option);
        
        option = new Option(BEEP_OPTION, "play response/event timing beep");
        options.addOption(option);
        
        option = new Option(URL_OPTION, "include the grammar in mrcp message or just include the url to the grammar");
        options.addOption(option);

        return options;
    }


    public static void main(String[] args) throws Exception {
    	


        // setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
        // unexpected crash (ie ctrl-c)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                _logger.debug("Running shutdown hook");
                if (mediaClient != null)
                   mediaClient.shutdown();
                if (!sentBye && sipAgent!=null) {
                    try {
                        sm.shutdown();
                        sentBye=true;
                    } catch (SipException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        }); 
        
        CommandLineParser parser = new GnuParser();
        Options options = getOptions();
        CommandLine line = parser.parse(options, args, true);
        args = line.getArgs();

        if (args.length < 2 || args.length > 3 || line.hasOption(HELP_OPTION)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("RecognitionClient [options] <local-rtp-port> <grammar-URL> <example-phrase>", options);
            return;
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
            _logger.debug(e, e);
        }

        if (localRtpPort < 0 || localRtpPort >= RTPConsumer.TCP_PORT_MAX || localRtpPort % 2 != 0) {
            throw new Exception("Improper format for first command line argument <local-rtp-port>," +
                " should be even integer between 0 and " + RTPConsumer.TCP_PORT_MAX);
        }

        String grammarUrl = args[1];
        examplePhrase = (args.length > 2) ? args[2] : null;

        // lookup resource server
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
        SipSession session = sm.newRecogChannel(localRtpPort,_host, "Session Name");
        if (session != null) {

            _logger.debug("Starting NativeMediaClient...");
            mediaClient = new NativeMediaClient(localRtpPort, rserverHost, session.getRemoteRtpPort());
            mediaClient.startTransmit();

            //construct the speech client with this session
            SpeechClient _client = new SpeechClientImpl(null, session.getRecogChannel());
            
            //event though we are going to use a blocking call, we may also want to monitor events.
        	RecognitionClient r = new RecognitionClient();
            _client.addListener(r.new Listener());
            
            try {
            	
                if (_logger.isInfoEnabled()) {
                    if (examplePhrase == null) {
                        _logger.info("\nStart speaking now...");
                    } else {
                        _logger.info("\nStart speaking now... (e.g. \"" + examplePhrase + "\")");
                    }
                }
            	boolean hotword = false;
            	boolean attachGrammar = false;
            	long noInputTimeout = 10000;
            	RecognitionResult result = _client.recognizeBlocking(grammarUrl, hotword, attachGrammar, noInputTimeout);
                if (_logger.isInfoEnabled()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\n**************************************************************");
                    if (result != null) {
                       sb.append("\nRecognition result: ").append(result.getText());
                    } else {
                    	sb.append("\nThere was no recognition result");
                    }
                    sb.append("\n**************************************************************\n");
                    _logger.info(sb);
                }
         

            } catch (Exception e){
                if (e instanceof MrcpInvocationException) {
                    MrcpResponse response = ((MrcpInvocationException) e).getResponse();
                    if (_logger.isDebugEnabled()) {
                        _logger.debug("MRCP response received:\n" + response.toString());
                    }
                }
                _logger.warn(e, e);
                sm.shutdown();
                sentBye=true;
                System.exit(1);
            }
            sm.shutdown();
            sentBye=true;
            System.exit(1);
            
        } else {
            //Invitation Timeout
            _logger.info("Sip Invitation timed out.  Is server running?");
        }
    }
    
    private class Listener implements SpeechEventListener {

		public void characterEventReceived(String c, DtmfEventType status) {
			_logger.info("received a unexpected character receieved event. char: "+c+" Status: "+status);
        }

		public void recognitionEventReceived(SpeechEventType event, RecognitionResult r) {
			_logger.info("Received a recognition event: "+event);
       
        }

		public void speechSynthEventReceived(SpeechEventType event) {
			_logger.info("Received an unexpected synth event.  Event: "+event);
	        
        }
    }
}

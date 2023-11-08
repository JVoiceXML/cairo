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
package org.speechforge.cairo.client.demo.bargein;

import org.speechforge.cairo.rtp.NativeMediaClient;
import org.speechforge.cairo.client.SessionManager;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.util.CairoUtil;

import java.awt.Toolkit;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import javax.sip.SipException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.MrcpResponse;


/**
 * Demo MRCPv2 client application that plays a TTS prompt while performing speech recognition on
 * microphone input.  Prompt playback is cancelled as soon as start of speech is detected.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class BargeInClient  {

    private static Logger LOGGER = LogManager.getLogger(BargeInClient.class);

    private static final String BEEP_OPTION = "beep";
    private static final String LOOP_OPTION = "loop";
    private static final String PARROT_OPTION = "parrot";
    
    public static final String HELP_OPTION = "help";
    public static final String RSERVERHOST_OPTION = "rserverhost";

    private static SessionManager sm;
    
    private static boolean _beep = false;
    private static Toolkit _toolkit = null;
    private static boolean _loop = false;
    private static boolean _parrot = false;

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

    public static void main(String[] args) throws Exception {

    	// setup a shutdown hook to cleanup and send a SIP bye message even if there is a 
    	// unexpected crash (ie ctrl-c)
    	Runtime.getRuntime().addShutdownHook(new Thread() {
    		public void run() {
    			if (mediaClient != null ) {
    				mediaClient.stop();
    				mediaClient = null;
    			}
    			if (!sentBye && sm!=null) {
    				try {
    					sm.shutdown();
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


    			//Construct the session manager and set it up
    			SessionManager sm;
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
    			SipSession session = sm.newRecogAndSynthChannels(localRtpPort, _host, "Session Name"); 

    			if (session != null) {
    				//construct a media client to stream the audio (both ways) and start streaming
    				LOGGER.debug("Starting NativeMediaClient...");
    				mediaClient = new NativeMediaClient(localRtpPort, rserverHost, session.getRemoteRtpPort());
    				mediaClient.startTransmit();

    				//construct the speech client with this session
    				SpeechClient _client = new SpeechClientImpl(session.getTtsChannel(), session.getRecogChannel());
    				_client.turnOnBargeIn();

    				//event though we are going to use a blocking call, we may also want to monitor events.
    				//BargeInClient bic = new BargeInClient();
    				//_client.addListener(bic.new Listener());

    				//now we can run the demo...
    				try {
    					RecognitionResult result = null;
    					String parrotString = "";
    					do {
    						String completePrompt = parrotString+ "  " +prompt;
    						result = _client.playAndRecognizeBlocking(false,completePrompt, grammar,false);

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
    								parrotString = result.getText()+"."; 
    							}
    						}

    						//TODO: check the natural language elements in recognition result for tag:value == main:quit
    					} while (_loop && !parrotString.contains("exit") && !parrotString.contains("quit"));

    					if (_parrot) {
    						_client.playBlocking(parrotString);
    					}

    					_client.shutdown();

    				} catch (Exception e){
    					if (e instanceof MrcpInvocationException) {
    						MrcpResponse response = ((MrcpInvocationException) e).getResponse();
    						if (LOGGER.isDebugEnabled()) {
    							LOGGER.debug("MRCP response received:\n" + response.toString());
    						}
    					}
    					LOGGER.warn(e, e);
    					sm.shutdown();
    					sentBye = true;
    					System.exit(1);
    				}

    			} else {
    				//Invitation Timeout
    				LOGGER.info("Sip Invitation timed out.  Is server running?");
    			}

    			if (sm != null){
    				sm.shutdown();
    				sentBye = true;
    			}
    			if (mediaClient != null) {
    				mediaClient.stop();
    				mediaClient = null;
    			}

    			System.exit(0);
    }

    private class Listener implements SpeechEventListener {

		public void characterEventReceived(String c, DtmfEventType status) {
			LOGGER.info("received a unexpected character receieved event. char: "+c+" Status: "+status);
        }

		public void recognitionEventReceived(SpeechEventType event, RecognitionResult r) {
			LOGGER.info("Received a recognition event: "+event);
        }

		public void speechSynthEventReceived(SpeechEventType event) {
			LOGGER.info("Received a synth event.  Event: "+event);
	        
        }
    }
    
}

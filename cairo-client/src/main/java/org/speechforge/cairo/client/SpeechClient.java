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
package org.speechforge.cairo.client;

import java.io.IOException;
import java.io.Reader;

import javax.media.rtp.InvalidSessionAddressException;

import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.client.recog.RecognitionResult;


// TODO: Auto-generated Javadoc
/**
 * SpeechClient API that peovides MRCPv2 based speech recogntion capabilites.  Provides both blocking and non-blocking calls.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public interface SpeechClient {
    
    //TODO:  Re-Implement the non blocking calls.  Commented them out for the time being to get something working.
    //TODO:  Redesign the SpeechRequest object. reduce to essential components.  remove MRCP references...
    
       
    /**
     * Play the prompt using a speech sythesizer or play the audio file.
     * 
     * @param urlPrompt if this flag is set the prompt string is an url
     * @param prompt the prompt to play either a text string or a url
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws NoMediaControlChannelException 
     * @throws InvalidSessionAddressException 
     */
    public void playBlocking(boolean urlPrompt, String prompt)  throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException, InvalidSessionAddressException;

    
    /**
     * Start speech recognition with the given grammar.  This method blocks until a recognition result is returned or there is a timeout.
     * Hotword and normal bargein mode is supported.  You can also pass the grammar along in the mrcp command
     * or jend the uri of the gramamr.
     * 
     * @param grammarUrl A url to the grammar file
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * @param attachGrammar A flag indicating that the grammar should be attached to the mrcp command (else a uri is passed)
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     */
    public RecognitionResult recognizeBlocking(String grammarUrl, boolean hotword, boolean attachGrammar, long noInputTimeout)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException;


    /**
     * Start speech recognition with the given grammar.  This method blocks until a recognition result is returned or there is a timeout.
     * Hotword and normal bargein mode is supported.  You can also pass the grammar along in the mrcp command
     * or jend the uri of the gramamr.
     * 
     * @param reader the reader for the grammar
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     */
    public RecognitionResult recognizeBlocking(Reader reader, boolean hotword, long noInputTimeout)throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException;

    
    /**
     * Play the prompt and start recognition with the given grammar.  This version of play and recognize receives a url to the grammar.
     * This is a conveneience method that does the bargein processing for you.  (Starts the no input timer upon the completion of the prompt 
     * or stops the prompt upon the start of speach event).
     * 
     * @param grammarUrl A url to the grammar file
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * @param urlPrompt the url prompt. if true the prompt parameter is a url to a file containing the prompt else its the prompt itself.
     * @param prompt the prompt to play
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     * @throws InvalidSessionAddressException 
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, String grammarUrl, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException, InvalidSessionAddressException;
    
    /**
     * Play and recognize blocking.  This version of play and recognize receievs a Reader to the grammar
     * This is a conveneience method that does the bargein processing for you.  (Starts the no input timer upon the completion of the prompt 
     * or stops the prompt upon the start of speach event).
     * 
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * @param urlPrompt the url prompt. if true the prompt parameter is a url to a file containing the prompt else its the prompt itself.
     * @param prompt the prompt to play
     * @param reader the reader for the grammar
     * 
     * @return the recognition result
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     */
    public RecognitionResult playAndRecognizeBlocking(boolean urlPrompt, String prompt, Reader reader, boolean hotword) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException;
        
    
    /**
     * Turn on barge in.
     */
    public void turnOnBargeIn();
    
    /**
     * Turn off barge in.
     */
    public void turnOffBargeIn();



    /**
     * Start no input timer.
     * 
     * @return the mrcp request state
     * 
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws InterruptedException the interrupted exception
     */
    //TODO: Move the MrcpRequestState out of the interface (hide MRCP)
    public MrcpRequestState sendStartInputTimersRequest() throws MrcpInvocationException, IOException, InterruptedException;


    
    public void addListener(SpeechEventListener listener);
    
    public void removeListener(SpeechEventListener listener);
    
    /**
     * Sets the  listener.  To set the listener for methods that don't have a listener parameter.
     * 
     * @param listener the new default listener
     * 
     * @deprecated
     */
    public void setListener(SpeechEventListener listener);
    
    
    /**
     * Sets the default listener.  To set the listener for methods that don't have a listener parameter.
     * 
     * @param listener the new default listener
     * 
     * @deprecated
     */
    public void setDefaultListener(SpeechEventListener listener);
    
 
    /**
     * Queue prompt for the audio stream  This is a non-blocking call.
     * Note that if url prompt is tue, and the content type of the url is an audio file, the audio file is played on the asudio stream.
     * 
     * @param urlPormpt if true, then the prompt is a url else it is text to synthesize
     * @param prompt the prompt
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws NoMediaControlChannelException 
     */
    public SpeechRequest queuePrompt(boolean urlPormpt, String prompt)  throws IOException, MrcpInvocationException, InterruptedException, NoMediaControlChannelException;    

   
    /**
     * Enable dtmf.
     * If dtmf is already enabled,(replace old pattern and listener or throw exception?)
     * 
     * @param pattern the pattern is a regex.  Once the sequence of key clicks matches the regex, the listener is called.
     * @param listener the listener
     * @param inputTimeout the input timeout
     * @param recogTimeout the recog timeout
     */
    public void enableDtmf(String pattern, SpeechEventListener listener, long inputTimeout, long recogTimeout) ;
   
    /**
     * Disable dtmf.
     */
    public void disableDtmf();
    
    /**
     * Start speech recognition with the given grammar.  This method does not block.  The listener is called with the results.
     * Hotword and normal bargein mode is supported.  You can also pass the grammar along in the mrcp command or send the uri of the gramamr.
     * 
     * @param grammarUrl A url to the grammar file
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * @param attachGrammar A flag indicating that the grammar should be attached to the mrcp command (else a uri is passed)
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     */
    public SpeechRequest recognize(String grammarUrl, boolean hotword, boolean attachGrammar, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException ;

    /**
     * Start speech recognition with the given grammar.  This method does not block.  The listener is called with the results.
     * Hotword and normal bargein mode is supported.  You can also pass the grammar along in the mrcp command or send the uri of the gramamr.
     * 
     * @param reader the reader for the grammar
     * @param hotword a flag indicating that the recognition mode is hotword mode
     * @param attachGrammar A flag indicating that the grammar should be attached to the mrcp command (else a uri is passed)
     * 
     * @return the speech request
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     * @throws IllegalValueException the illegal value exception
     * @throws NoMediaControlChannelException 
     */
    public SpeechRequest recognize(Reader reader, boolean hotword, boolean attachGrammar, long noInputTimeout) throws IOException, MrcpInvocationException, InterruptedException, IllegalValueException, NoMediaControlChannelException ;
    
    /**
     * Cancel request.
     * 
     * @param request the request
     * @throws InterruptedException 
     * @throws IOException 
     * @throws MrcpInvocationException 
     * @throws NoMediaControlChannelException 
     */
    public void stopActiveRecognitionRequests() throws MrcpInvocationException, IOException, InterruptedException, NoMediaControlChannelException;
    
    /**
     * Shutdown. close all channels and release all resources
     * @throws InterruptedException 
     * @throws IOException 
     * @throws MrcpInvocationException 
     */
    public void shutdown() throws MrcpInvocationException, IOException, InterruptedException;
    
    /**
     * Send bargein request to the sythesizer (so it stops streaming audio)
     * 
     * @return the mrcp request state
     * 
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws MrcpInvocationException the mrcp invocation exception
     * @throws InterruptedException the interrupted exception
     */
    public MrcpRequestState sendBargeinRequest() throws IOException, MrcpInvocationException, InterruptedException;
  
}

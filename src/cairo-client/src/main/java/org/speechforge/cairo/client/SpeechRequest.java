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

import org.speechforge.cairo.client.recog.RecognitionResult;


/**
 * This object maintains the state of a speech request.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SpeechRequest {
    
    public enum RequestType {play, recognize, playAndRecognize}
    
    private long requestId;
    private boolean completed;
    private boolean blockingCall = false;
    private RecognitionResult result;
    private RequestType requestType;
    private SpeechRequest linkedRequest;
    //TODO: Add status of the call that initiated the request. 
    

    public SpeechRequest(long requestId, RequestType type, boolean completed) {
        super();
        this.requestId = requestId;
        this.requestType = type;
        this.completed = completed;
    }
    
    public SpeechRequest(long requestId, boolean completed) {
        super();
        this.requestId = requestId;
        this.completed = completed;
    }
    
    
    /**
     * @return the completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * @param completed the completed to set
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

  
    /**
     * @return the requestId
     */
    public long getRequestId() {
        return requestId;
    }
    
    /**
     * @param requestId the requestId to set
     */
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }
    
    /**
     * @return the blockingCall
     */
    public boolean isBlockingCall() {
        return blockingCall;
    }
    
    /**
     * @param blockingCall the blockingCall to set
     */
    public void setBlockingCall(boolean blockingCall) {
        this.blockingCall = blockingCall;
    }

    /**
     * @return the result
     */
    public RecognitionResult getResult() {
        return result;
    }
    
    /**
     * @param result the result to set
     */
    public void setResult(RecognitionResult result) {
        this.result = result;
    }
    
    /**
     * @return the requestType
     */
    public RequestType getRequestType() {
        return requestType;
    }
    
    /**
     * @param requestType the requestType to set
     */
    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }
    
    /**
     * @return the linkedRequest
     */
    public SpeechRequest getLinkedRequest() {
        return linkedRequest;
    }
    
    /**
     * @param linkedRequest the linkedRequest to set
     */
    public void setLinkedRequest(SpeechRequest linkedRequest) {
        this.linkedRequest = linkedRequest;
    } 
}


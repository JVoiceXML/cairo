package org.speechforge.cairo.rtp;

import java.util.Vector;
import javax.media.Format;
import javax.sound.sampled.AudioFormat;

public class AudioFormats {
    
    /***********************************************************************
     * Format types in the sdp message (in teh sip invite body) use the type to 
     * identify the format (i.e. 0 indicates 8kHZ PCMU).  
     * JMF has a format string that indiactes format.  need a way to map the two.
     * 
     * 
            PT         encoding      audio/video    clock rate    channels
            name          (A/V)          (Hz)          (audio)
    _______________________________________________________________
    0          PCMU          A              8000          1
    1          1016          A              8000          1
    2          G721          A              8000          1
    3          GSM           A              8000          1
    4          unassigned    A              8000          1
    5          DVI4          A              8000          1
    6          DVI4          A              16000         1
    7          LPC           A              8000          1
    8          PCMA          A              8000          1
    9          G722          A              8000          1
    10         L16           A              44100         2
    11         L16           A              44100         1
    12         unassigned    A
    13         unassigned    A
    14         MPA           A              90000        (see text)
    15         G728          A              8000          1
    16--23     unassigned    A
    24         unassigned    V
    25         CelB          V              90000
    26         JPEG          V              90000
    27         unassigned    V
    28         nv            V              90000
    29         unassigned    V
    30         unassigned    V
    31         H261          V              90000
    32         MPV           V              90000
    33         MP2T          AV             90000
    34--71     unassigned    ?
    72--76     reserved      N/A            N/A           N/A
    77--95     unassigned    ?
    96--127    dynamic       ?

    Table 2: Payload types (PT) for standard audio and video encodings
    ********************************************************************/

    
    private Vector requestedFormatsSDP;
    private Format[] supportedFormatsJMF;
    
    //temp hardcode ot all to ulaw
    private String PCMU_SDP = "0";
    private String PCMU_JMF = "ULAW/rtp";
    
    public void AudioFormat() {
        //TODO: determine the supported formats and save in supported formats array (for now hardcoded to ulaw)
    } 
    
    public  Vector filterOutUnSupportedFormatsInOffer() throws ResourceUnavailableException {
        //TODO: Really check the formats instead of just hardcoding
        //      Also need to take accoustic models into account.
        //      Also need to take into account what Sphinx can handle it uses PREFERRED_MEDIA_FORMAT which appears to be a third format
        //      (it think it is from the java sound api).  So we have 3: SDP, JMF AND JSAPI!
        //
        //
        //Right now hardcoded a check for format 0 (PCMU 8khz).
        //Known to be supported by JMF (and matching the accoustic models)
        //
        //
        //for (int j=0;j<PREFERRED_MEDIA_FORMATS.length;j++) {
        //    PREFERRED_MEDIA_FORMATS[j].get..
        //}
        Vector v = new Vector();
        boolean foundAtLeastOne = false;
        for (int i=0; i<requestedFormatsSDP.size(); i++) {
            //System.out.println(i+" format type is: "+requestedFormatsSDP.get(i).getClass().getCanonicalName());
            //System.out.println("   ...and it is: "+requestedFormatsSDP.get(i).toString());
            if (((String) requestedFormatsSDP.get(i)).equals(PCMU_SDP)) {
                foundAtLeastOne = true;
                v.add(requestedFormatsSDP.get(i));
            }
        }
        if (!foundAtLeastOne)
            throw new ResourceUnavailableException();
        return v;
    }
    
    public boolean isSupported(Format f) {
        if (f.getEncoding().equals(PCMU_JMF))
            return true;
        return false;
    }


    public static AudioFormats constructWithSdpVector(Vector requested) {
        AudioFormats af = new AudioFormats();
        af.setRequestedFormatsSDP(requested);
        return af;
    }


    /**
     * @return the requestedFormatsSDP
     */
    public Vector getRequestedFormatsSDP() {
        return requestedFormatsSDP;
    }


    /**
     * @param requestedFormatsSDP the requestedFormatsSDP to set
     */
    public void setRequestedFormatsSDP(Vector requestedFormatsSDP) {
        this.requestedFormatsSDP = requestedFormatsSDP;
    }


    /**
     * @return the supportedFormatsJMF
     */
    public Format[] getSupportedFormatsJMF() {
        return supportedFormatsJMF;
    }


    /**
     * @param supportedFormatsJMF the supportedFormatsJMF to set
     */
    //public void setSupportedFormatsJMF(Format[] supportedFormatsJMF) {
    //    this.supportedFormatsJMF = supportedFormatsJMF;
    //}
    
   public  static javax.media.format.AudioFormat convertToJmfFormat(AudioFormat af) {
    	
    	String encoding;
    	double sampleRate;
    	int sampleSizeInBits;
    	int channels;
    	int endian;
    	int signed;
    	int frameSizeInBytes;
    	double frameRate;
    	
    	if (af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
    		encoding = javax.media.format.AudioFormat.LINEAR;
    		signed = javax.media.format.AudioFormat.SIGNED;
    	} else if (af.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
    		encoding = javax.media.format.AudioFormat.LINEAR;
    		signed = javax.media.format.AudioFormat.UNSIGNED;
    	} else if (af.getEncoding() == AudioFormat.Encoding.ALAW) {
    		encoding = javax.media.format.AudioFormat.ALAW;
    		signed = javax.media.format.AudioFormat.UNSIGNED;
    	} else if (af.getEncoding() == AudioFormat.Encoding.ULAW) {
    		encoding = javax.media.format.AudioFormat.ULAW;
    		signed = javax.media.format.AudioFormat.UNSIGNED;
    	} else {
    		encoding = javax.media.format.AudioFormat.LINEAR;
    		signed = javax.media.format.AudioFormat.SIGNED;
    	}
    	
    	sampleRate = af.getSampleRate();
    	sampleSizeInBits = af.getSampleSizeInBits();
    	channels = af.getChannels();
    	if (af.isBigEndian() ) {
    		endian = javax.media.format.AudioFormat.BIG_ENDIAN;
    	} else {
    		endian = javax.media.format.AudioFormat.LITTLE_ENDIAN;
    	}
    	frameSizeInBytes = 8 * af.getFrameSize();
    	
    	frameRate = af.getFrameRate();
    	
    	javax.media.format.AudioFormat audioFormat = new javax.media.format.AudioFormat(encoding,
    			sampleRate,
    			sampleSizeInBits,
    			channels,
    			endian,
    			signed,
    			frameSizeInBytes,
    			frameRate,
				Format.byteArray);
		return audioFormat;

    }
    
}

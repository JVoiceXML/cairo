/**
 * 
 */
package org.speechforge.cairo.jmf.codec.audio.dtmf;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;

import com.ibm.media.codec.audio.AudioCodec;

/**
 * 
 */
public class JavaDecoder extends AudioCodec {

    public static int DTMF_PAYLOAD = 101;
    
    public static final AudioFormat DTMF_FORMAT = new AudioFormat("DTMF", 8000,
            16, 1, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
    /**
     * Constructs the encoder and init the supported formats.
     */
    public JavaDecoder()
    {
        supportedInputFormats = new AudioFormat[]
            {
              DTMF_FORMAT
            }; 

        defaultOutputFormats = new AudioFormat[]
            {
                DTMF_FORMAT
            };
        PLUGIN_NAME = "DTMF Decoder";
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {

    }
    
    @Override
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        // TODO Auto-generated method stub
        return 0;
    }

}

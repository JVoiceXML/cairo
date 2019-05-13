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
package org.speechforge.cairo.rtp.server.sphinx;

import java.io.UnsupportedEncodingException;

/**
 * Encapsulates the properties representing the format of an audio source.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class SourceAudioFormat {

    /**
     * Preferred media format for generating audio streams for recognition.
     */
    public static final javax.media.format.AudioFormat PREFERRED_MEDIA_FORMAT = getPreferredMediaFormatTwo();

    /**
     * Single element array containing {@link PREFERRED_MEDIA_FORMAT}.
     */
    public static final javax.media.format.AudioFormat[] PREFERRED_MEDIA_FORMATS = { PREFERRED_MEDIA_FORMAT };


    private javax.sound.sampled.AudioFormat _soundFormat;
    private javax.media.format.AudioFormat _mediaFormat;

    private int _msecPerRead;
    private int _sampleRate;
    private boolean _isSigned;
    private boolean _isBigEndian;
    private int _frameSizeInBytes;
    private int _sampleSizeInBytes;
    private int _channels;


    private SourceAudioFormat() {}

    // setters

    public void setSoundAudioFormat(javax.sound.sampled.AudioFormat soundFormat) {
        _mediaFormat = null;
        _soundFormat = soundFormat;
    }

    public void setMediaAudioFormat(javax.media.format.AudioFormat mediaFormat) {
        _soundFormat = null;
        _mediaFormat = mediaFormat;
    }

    public void setMsecPerRead(int msecPerRead) {
        _msecPerRead = msecPerRead;
    }

    public void setSampleSizeInBytes(int sampleSizeInBytes) {
        _sampleSizeInBytes = sampleSizeInBytes;
    }

    public void setSampleRate(int sampleRate) {
        _sampleRate = sampleRate;
    }

    public void setSigned(boolean isSigned) {
        _isSigned = isSigned;
    }

    public void setBigEndian(boolean isBigEndian) {
        _isBigEndian = isBigEndian;
    }

    public void setFrameSizeInBytes(int frameSizeInBytes) {
        _frameSizeInBytes = frameSizeInBytes;
    }

    public void setChannels(int channels) {
        _channels = channels;
    }

    // getters

    public javax.sound.sampled.AudioFormat getSoundAudioFormat() {
        return _soundFormat;
    }

    public javax.media.format.AudioFormat getMediaAudioFormat() {
        return _mediaFormat;
    }

    public int getMsecPerRead() {
        return _msecPerRead;
    }

    public int getSampleSizeInBytes() {
        return _sampleSizeInBytes;
    }

    public int getSampleRate() {
        return _sampleRate;
    }

    public boolean isSigned() {
        return _isSigned;
    }

    public boolean isBigEndian() {
        return _isBigEndian;
    }

    public int getFrameSizeInBytes() {
        return _frameSizeInBytes;
    }

    public int getChannels() {
        return _channels;
    }

    // dynamic derivation methods

    public long calculateDurationMsecs(long totalSamplesRead) {
        long duration = (long)
            (((double)totalSamplesRead/(double)_sampleRate)*1000.0);
        return duration;
    }

    public String toString() {
        if (_soundFormat != null) {
            return _soundFormat.toString();
        } else if (_mediaFormat != null) {
            return _mediaFormat.toString();
        } else {
            return null;
        }
    }

    // factory methods

    public static SourceAudioFormat newInstance(int msecPerRead, javax.sound.sampled.AudioFormat soundFormat)
      throws UnsupportedEncodingException {

        SourceAudioFormat format = new SourceAudioFormat();

        format.setSoundAudioFormat(soundFormat);
        format.setMsecPerRead(msecPerRead);

        assert (soundFormat.getSampleSizeInBits() % 8) == 0 : soundFormat.getSampleSizeInBits();
        int sampleSizeInBytes = soundFormat.getSampleSizeInBits() / 8;
        format.setSampleSizeInBytes(sampleSizeInBytes);

        format.setSampleRate((int) soundFormat.getSampleRate());

        // only support PCM encoding for now
        javax.sound.sampled.AudioFormat.Encoding encoding = soundFormat.getEncoding();
        if (javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            format.setSigned(true);
        } else if (javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            format.setSigned(false);
        } else {
            throw new UnsupportedEncodingException("Unsupported encoding: " + encoding.toString());
        }

        format.setBigEndian(soundFormat.isBigEndian());
        
        // Set the frame size depending on the sample rate.
        float secPerRead = ((float) msecPerRead) / 1000.f;
        format.setFrameSizeInBytes(sampleSizeInBytes * (int) (secPerRead * format.getSampleRate()));

        format.setChannels(soundFormat.getChannels());

        return format;
    }

    public static SourceAudioFormat newInstance(int msecPerRead, javax.media.format.AudioFormat mediaFormat)
      throws UnsupportedEncodingException {

        SourceAudioFormat format = new SourceAudioFormat();

        format.setMediaAudioFormat(mediaFormat);
        format.setMsecPerRead(msecPerRead);

        assert (mediaFormat.getSampleSizeInBits() % 8) == 0 : mediaFormat.getSampleSizeInBits();
        int sampleSizeInBytes = mediaFormat.getSampleSizeInBits() / 8;
        format.setSampleSizeInBytes(sampleSizeInBytes);

        format.setSampleRate((int) mediaFormat.getSampleRate());

        switch (mediaFormat.getSigned()) {
        case javax.media.format.AudioFormat.SIGNED:
            format.setSigned(true);
            break;

        case javax.media.format.AudioFormat.UNSIGNED:
            format.setSigned(false);
            break;

        default:
            //throw new UnsupportedEncodingException("Unsupported/unspecified signed: " + mediaFormat.toString());
        
        }

        switch (mediaFormat.getEndian()) {
        case javax.media.format.AudioFormat.BIG_ENDIAN:
            format.setBigEndian(true);
            break;

        case javax.media.format.AudioFormat.LITTLE_ENDIAN:
            format.setBigEndian(false);
            break;

        default:
            //throw new UnsupportedEncodingException("Unsupported/unspecified endian: " + mediaFormat.toString());
        
        }
        
        // Set the frame size depending on the sample rate.
        float secPerRead = ((float) msecPerRead) / 1000.f;
        format.setFrameSizeInBytes(sampleSizeInBytes * (int) (secPerRead * format.getSampleRate()));

        format.setChannels(mediaFormat.getChannels());

        return format;
    }

    public static SourceAudioFormat newInstance(int msecPerRead)
      throws UnsupportedEncodingException {
        return newInstance(msecPerRead, getRTPMediaFormat0());
    }

    private static javax.media.format.AudioFormat getRTPMediaFormat0() {

        //dvi/rtp, 8000.0 Hz, 4-bit, Mono

        return new javax.media.format.AudioFormat(
            javax.media.format.AudioFormat.DVI_RTP,
            8000.0,
            4,
            1,
            javax.media.format.AudioFormat.BIG_ENDIAN,
            javax.media.format.AudioFormat.SIGNED
        );

        //(String encoding, double sampleRate, int sampleSizeInBits, int channels, int endian, int signed)
    }

    private static javax.media.format.AudioFormat getRTPMediaFormat11() {

        //mpegaudio/rtp, 44100.0 Hz, 16-bit, Mono, BigEndian, Signed

        return new javax.media.format.AudioFormat(
            javax.media.format.AudioFormat.MPEG_RTP,
            44100.0,
            16,
            1,
            javax.media.format.AudioFormat.BIG_ENDIAN,
            javax.media.format.AudioFormat.SIGNED
        );

        //(String encoding, double sampleRate, int sampleSizeInBits, int channels, int endian, int signed)
    }

    private static javax.media.format.AudioFormat getPreferredMediaFormatOne() {

        //LINEAR, 44100.0 Hz, 16-bit, Mono, LittleEndian, Signed, 88200.0 frame rate, FrameSize=16 bits

        return new javax.media.format.AudioFormat(
            javax.media.format.AudioFormat.LINEAR,
            44100.0,
            16,
            1,
            javax.media.format.AudioFormat.BIG_ENDIAN,
            javax.media.format.AudioFormat.SIGNED,
            16,
            88200.0,
            byte[].class
        );

    }

    private static javax.media.format.AudioFormat getPreferredMediaFormatTwo() {
        return new javax.media.format.AudioFormat(
            javax.media.format.AudioFormat.LINEAR, //encoding
            8000.0,                                //sample rate
            16,                                    //sample size in bits
            1,                                     //channels
            javax.media.format.AudioFormat.LITTLE_ENDIAN,
            javax.media.format.AudioFormat.SIGNED
        );
    }

}
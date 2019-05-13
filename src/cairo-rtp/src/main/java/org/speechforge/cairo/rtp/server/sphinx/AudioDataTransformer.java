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

import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.util.DataUtil;

import org.apache.log4j.Logger;

/**
 * Transforms audio data of a specified format to {@code edu.cmu.sphinx.frontend.DoubleData}.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class AudioDataTransformer {

    private static Logger _logger = Logger.getLogger(AudioDataTransformer.class);

    public final static String STEREO_TO_MONO_AVERAGE = "average";
    public final static String STEREO_TO_MONO_SELECT_CHANNEL = "selectChannel";

    private String _stereoToMono = STEREO_TO_MONO_AVERAGE;
    private int _selectedChannel = 0;

    private SourceAudioFormat _sourceFormat;


    public AudioDataTransformer(SourceAudioFormat sourceFormat) {
        _sourceFormat = sourceFormat;
    }

    public AudioDataTransformer(SourceAudioFormat sourceFormat, String stereoToMono, int selectedChannel) {
        _sourceFormat    = sourceFormat;
        _stereoToMono    = stereoToMono;
        _selectedChannel = selectedChannel;
    }

    public DoubleData toDoubleData(byte[] data, long collectTime, long firstSampleNumber) {
            double[] samples = _sourceFormat.isBigEndian() ?
                DataUtil.bytesToValues(data, 0, data.length, _sourceFormat.getSampleSizeInBytes(), _sourceFormat.isSigned()) :
                DataUtil.littleEndianBytesToValues(data, 0, data.length, _sourceFormat.getSampleSizeInBytes(), _sourceFormat.isSigned());

            if (_sourceFormat.getChannels() > 1) {
                samples = convertStereoToMono(samples, _sourceFormat.getChannels());
            }
            if (_logger.isTraceEnabled()) {
                for (double sample : samples) {
                    _logger.trace("sample: " + sample);
                }
            }

            return new DoubleData(samples, _sourceFormat.getSampleRate(), collectTime, firstSampleNumber);
    }

    /**
     * Converts stereo audio to mono.
     *
     * @param samples the audio samples, each double in the array is one sample
     * @param channels the number of channels in the stereo audio
     */
    private double[] convertStereoToMono(double[] samples, int channels) {
        assert (samples.length % channels == 0);
        double[] finalSamples = new double[samples.length/channels];
        if (_stereoToMono.equals(STEREO_TO_MONO_AVERAGE)) {
            for (int i = 0, j = 0; i < samples.length; j++) {
                double sum = samples[i++];
                for (int c = 1; c < channels; c++) {
                    sum += samples[i++];
                }
                finalSamples[j] = sum / channels;
            }
        } else if (_stereoToMono.equals(STEREO_TO_MONO_SELECT_CHANNEL)) {
            for (int i = _selectedChannel, j = 0; i < samples.length;
                 i += channels, j++) {
                finalSamples[j] = samples[i];
            }
        } else {
            throw new Error("Unsupported stereo to mono conversion: " + _stereoToMono);
        }
        return finalSamples;
    }        

}
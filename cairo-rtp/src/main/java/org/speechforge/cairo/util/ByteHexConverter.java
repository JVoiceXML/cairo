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
package org.speechforge.cairo.util;

import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

/**
 * Provides utility methods for conversion between byte arrays and hex strings.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public final class ByteHexConverter {

    private static Logger _logger = Logger.getLogger(ByteHexConverter.class);

    /**
     * TODOC.
     * @param bytes TODOC.
     * @return TODOC.
     */
    public static String toHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            appendHexDigit(sb, bytes[i]);
        }
        return sb.toString();
    }

    /**
     * TODOC.
     * @param writer TODOC.
     * @param bytes TODOC.
     * @throws IOException If an I/O error occurs
     */
    public static void writeHexDigits(Writer writer, byte[] bytes) throws IOException {
        writeHexDigits(writer, bytes, 0, bytes.length);
    }

    /**
     * TODOC.
     * @param writer TODOC.
     * @param bytes 
     * @param offset 
     * @param length 
     * @throws IOException If an I/O error occurs
     */
    public static void writeHexDigits(Writer writer, byte[] bytes, int offset, int length)
      throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException();
        }
        if (offset < 0) {
            throw new IllegalArgumentException();
        }
        if ((offset + length) > bytes.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        for (int i = offset; i < length; i++) {
            writer.write(getFirstNibble(bytes[i]));
            writer.write(getSecondNibble(bytes[i]));
            writer.write(' ');
        }
    }

    /**
     * TODOC.
     * @param hexString TODOC.
     * @return TODOC.
     */
    public static byte[] toByteArray(String hexString) {
        char[] charArray = hexString.toCharArray();
        byte[] byteArray = new byte[charArray.length / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String hexChar = new String(charArray, i * 2, 2);
            try {
                byteArray[i] = (byte) Integer.parseInt(hexChar, 16);
            } catch (NumberFormatException e) {
                if (_logger.isDebugEnabled()) {
                    _logger.debug("hexChar=" + hexChar);
                }
                throw e;
            }
        }
        return byteArray;
    }

    ///////////////////////////////////////////
    // private methods
    ///////////////////////////////////////////

    private static StringBuffer appendHexDigit(StringBuffer sb, byte x) {
        sb.append(getFirstNibble(x));
        sb.append(getSecondNibble(x));
        return sb;
    }

    private static char getFirstNibble(byte x) {
        char c = (char) ((x >> 4) & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        return c;
    }

    private static char getSecondNibble(byte x) {
        char c = (char) (x & 0xf);
        if (c > 9) {
            c = (char) ((c - 10) + 'a');
        } else {
            c = (char) (c + '0');
        }
        return c;
    }


}

/**
 * Cairo - Open source framework for control of speech media resources.
 *
 * Copyright (C) 2006 SpeechForge - http://www.speechforge.org
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

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Provides common utility functions for cairo project classes.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 *
 */
public class CairoUtil {

    /**
     * Make default constructor private to prevent instantiation.
     */
    private CairoUtil() {
        super();
    }

    /**
     * Takes a command line argument and attempts to convert it to a URL object.
     * @param arg command line argument
     * @return argument converted to a URL object
     * @throws MalformedURLException if the command line argument provided is not well formed or is of an unknown protocol
     */
    public static URL argToURL(String arg) throws MalformedURLException {
        if (arg == null || (arg = arg.trim()).length() < 1) {
            return null;
        }

        if (arg.indexOf(':') < 0) {
            File file = new File(arg);
            return file.toURI().toURL();
        }

        return new URL(arg);
    }
    
    
	public static InetAddress getLocalHost() throws SocketException, UnknownHostException {
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			for (Enumeration en2 = networkInterface.getInetAddresses(); en2.hasMoreElements();) {
	            InetAddress addr = (InetAddress) en2.nextElement();
	            if (!addr.isLoopbackAddress()) {
	                if (addr instanceof Inet4Address) {
	                    return addr;
	                }
	            }
			}

		}
		return InetAddress.getLocalHost();
	}

}

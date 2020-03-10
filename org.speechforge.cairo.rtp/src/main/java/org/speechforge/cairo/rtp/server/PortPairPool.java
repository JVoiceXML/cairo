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
package org.speechforge.cairo.rtp.server;


import org.speechforge.cairo.rtp.RTPConsumer;
import org.speechforge.cairo.rtp.ResourceUnavailableException;


import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

/**
 * Manages pooling of TCP port pairs that are required for each RTP channel.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class PortPairPool {

    private static Logger _logger = Logger.getLogger(PortPairPool.class);

    private int[] _ports;
    private boolean[] _availables;
    private int _size;

    /**
     * TODOC
     * @param basePort port number to start incrementing port pairs from (must be even number)
     * @param numberOfPortPairs max number of port pairs to store in pool
     */
    public PortPairPool(int basePort, int numberOfPortPairs) {
        Validate.isTrue((basePort % 2 == 0), "Base port must be even, invalid port: ", basePort);
        Validate.isTrue(basePort >= 0, "Base port must not be less than zero, invalid port: ", basePort);
        Validate.isTrue(basePort < RTPConsumer.TCP_PORT_MAX, "Base port exceeds max TCP port value, invalid port: ", basePort);
        Validate.isTrue(numberOfPortPairs > 0, "Invalid number of port pairs: ", basePort);

        _size = numberOfPortPairs;
        _ports = new int[_size];
        _availables = new boolean[_size];
        for (int i = 0; i < _size; i++) {
            _ports[i] = basePort + (i * 2);
            _availables[i] = true;
        }

        if (_logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("creating new port pair pool... range: (");
            sb.append(_ports[0]).append(", ").append(_ports[0] + 1).append(") - (");
            sb.append(_ports[_size-1]).append(", ").append(_ports[_size-1] + 1).append(')');
            _logger.debug(sb);
        }
    }

    /**
     * Borrows a port from the available ports in the pool.  The port should be returned
     * when no longer in use.
     * @return the lowest available port from the pool
     * @throws ResourceUnavailableException if no ports are available in the pool
     */
    public synchronized int borrowPort() throws ResourceUnavailableException {
        for (int i = 0; i < _size; i++) {
            if (_availables[i]) {
                _availables[i] = false;
                return _ports[i];
            }
        }
        throw new ResourceUnavailableException("No port pairs available!");
    }

    /**
     * Returns a borrowed port to be made available again in the pool.  The port should
     * no longer be used once returned to the pool.
     * @param port the number of the port being returned to the pool
     * @throws IllegalArgumentException if an invalid or out of range port number is passed to the pool
     */
    public synchronized void returnPort(int port) throws IllegalArgumentException {
        if ((port % 2) != 0) {
            throw new IllegalArgumentException("Only even port values accepted, invalid port value: " + port);
        }

        int index = ( port - _ports[0] ) / 2;
        if (index < 0 || index >= _size) {
            throw new IllegalArgumentException("Invalid port value for current port pair pool: " + port);
        }

        assert (port == _ports[index]) : "port: " + port + ", _ports[" + index + "]: " + _ports[index];

        _availables[index] = true;
    }

//    static {
//        boolean assertsEnabled = false;
//        assert assertsEnabled = true; // Intentional side effect!!!
//        if (!assertsEnabled)
//            throw new RuntimeException("Asserts must be enabled!!!");
//    }

}

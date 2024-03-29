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
import org.speechforge.cairo.util.ObjectPoolUtil;


import org.apache.commons.lang.Validate;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Serves to create a pool of {@link org.speechforge.cairo.rtp.server.RTPStreamReplicator} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RTPStreamReaderFactory implements PoolableObjectFactory {

    private static Logger _logger = LogManager.getLogger(RTPStreamReaderFactory.class);

    private PortPairPool _portPairPool;

    /**
     * TODOC
     * @param basePort 
     * @param portPairPool 
     */
    private RTPStreamReaderFactory(int basePort, PortPairPool portPairPool) {
        Validate.isTrue((basePort % 2 == 0), "Base port must be even, invalid port: ", basePort);
        Validate.isTrue(basePort >= 0, "Base port must not be less than zero, invalid port: ", basePort);
        Validate.isTrue(basePort <= RTPConsumer.TCP_PORT_MAX, "Base port exceeds max TCP port value, invalid port: ", basePort);
        _portPairPool = portPairPool;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    public Object makeObject() throws Exception {
        return new RTPStreamReader(_portPairPool.borrowPort());
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
     */
    public void destroyObject(Object obj) throws Exception {
    	RTPStreamReader reader = (RTPStreamReader) obj;
    	reader.shutdown();
        _portPairPool.returnPort(reader.getPort());
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
     */
    public boolean validateObject(Object arg0) {
        //RTPStreamReader reader = (RTPStreamReplicator) obj;
        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
     */
    public void activateObject(Object arg0) throws Exception {
        //RTPStreamReader reader = (RTPStreamReplicator) obj;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
     */
    public void passivateObject(Object arg0) throws Exception {
        //RTPStreamReader reader = (RTPStreamReplicator) obj;
    }

    /**
     * TODOC
     * @param rtpBasePort base port 
     * @param maxConnects max number of connects
     * @return created pool
     */
    public static ObjectPool createObjectPool(int rtpBasePort, int maxConnects) {
        PortPairPool ppp = new PortPairPool(rtpBasePort, maxConnects);
        PoolableObjectFactory factory = new RTPStreamReaderFactory(rtpBasePort, ppp);
        GenericObjectPool.Config config = ObjectPoolUtil.getGenericObjectPoolConfig(maxConnects);
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        return objectPool;
    }

}

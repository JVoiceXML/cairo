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
package org.speechforge.cairo.server.recog.sphinx;

import java.net.URL;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.speechforge.cairo.util.pool.AbstractPoolableObjectFactory;
import org.speechforge.cairo.util.pool.ObjectPoolUtil;
import org.speechforge.cairo.util.pool.PoolableObject;

import edu.cmu.sphinx.util.props.ConfigurationManager;

/**
 * Serves to create a pool of {@link org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class SphinxRecEngineFactory extends AbstractPoolableObjectFactory {
    /** Logger instance. */
    private final static Logger LOGGER =
            LogManager.getLogger(SphinxRecEngineFactory.class);
    /** URL of the sphinx configuration file. */
    URL _sphinxConfigURL;
    /** The sphinx configuration manager. */
    ConfigurationManager _cm;
    /** Id of the last created instance. */
    private int id = 1;

    /**
     * Constructs a new object
     * @param sphinxConfigURL URL of the sphinx configuration
     */
    public SphinxRecEngineFactory(URL sphinxConfigURL) {
        _sphinxConfigURL = sphinxConfigURL;
        _cm = new ConfigurationManager(_sphinxConfigURL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        return new SphinxRecEngine(_cm, id++);
    }

    /**
     * Create a new object pool for sphinx recognizer objects.
     * @param sphinxConfigURL URL of the sphinx configuration
     * @param instances number of instances
     * @return created pool
     * @throws InstantiationException if initializing the object pool triggers
     *          an exception.
     */
    @SuppressWarnings("rawtypes")
    public static ObjectPool createObjectPool(URL sphinxConfigURL,
            int instances) throws InstantiationException {
        LOGGER.info("creating new recognizer engine pool with " + instances
                + " instances...");

        PoolableObjectFactory factory = 
                new SphinxRecEngineFactory(sphinxConfigURL);
        GenericObjectPool.Config config = 
                ObjectPoolUtil.getGenericObjectPoolConfig(instances);

        @SuppressWarnings("unchecked")
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        initPool(objectPool);
        return objectPool;
    }

}
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
package org.speechforge.cairo.server.tts;

import org.speechforge.cairo.util.pool.AbstractPoolableObjectFactory;
import org.speechforge.cairo.util.pool.ObjectPoolUtil;
import org.speechforge.cairo.util.pool.PoolableObject;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Serves to create a pool of {@link org.speechforge.cairo.server.tts.PromptGenerator} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class PromptGeneratorFactory extends AbstractPoolableObjectFactory {

    private static final Logger LOGGER = LogManager.getLogger(PromptGeneratorFactory.class);

    private String _voiceName;
    private String _speechSynthesizer;

    /**
     * Constructs a new object.
     * @param speechSynthesizer the speech synthesizer
     * @param voiceName name of the voice to use to play prompts
     */
    public PromptGeneratorFactory(String speechSynthesizer, String voiceName) {
        _speechSynthesizer = speechSynthesizer;
        _voiceName = voiceName;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        if (_speechSynthesizer == null) {
            return new PromptGenerator(_voiceName);
        }
        if(_speechSynthesizer.equals("Festival")) {
            return new FestivalPromptGenerator(_voiceName);
        } else if (_speechSynthesizer.equals("Mary")) {
            return new MaryPromptGenerator(_voiceName);
        } else {
            return new PromptGenerator(_voiceName);
        }
    }

    /**
     * TODOC
     * @param speechSynthesizer the synthesizer to use
     * @param voiceName name of the voice to use to play prompts
     * @param instances number of instances to create
     * @return created pool
     * @throws InstantiationException error creating the pool
     */
    public static ObjectPool createObjectPool(String speechSynthesizer, String voiceName, int instances)
      throws InstantiationException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("creating new prompt generator pool... instances: " + instances);
        }

        PoolableObjectFactory factory = new PromptGeneratorFactory(speechSynthesizer, voiceName);

        // TODO: adapt config to prompt generator constraints
        GenericObjectPool.Config config = ObjectPoolUtil.getGenericObjectPoolConfig(instances);
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        initPool(objectPool);
        return objectPool;
    }

}
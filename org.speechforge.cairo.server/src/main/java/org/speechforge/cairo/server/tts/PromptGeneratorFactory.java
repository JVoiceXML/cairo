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
 * Serves to create a pool of {@link org.speechforge.cairo.server.tts.FreeTTSPromptGenerator} instances.
 *
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 * @author Dirk Schnelle-Walka
 */
public class PromptGeneratorFactory extends AbstractPoolableObjectFactory {
    /** Logger instance. */
    private static final Logger LOGGER = 
            LogManager.getLogger(PromptGeneratorFactory.class);
    /** VOice name that is used by teh synthesizer. */
    private String voiceName;
    /** Name of the synthesizer to create from this object pool. */
    private String speechSynthesizer;

    /**
     * Constructs a new object.
     * @param synthesizer name of the speech synthesizer. Must be one of 
     *          {@code FreeTTS}, {@code Festival} or {@code Mary}.
     * @param voice name of the voice to use to play prompts
     */
    public PromptGeneratorFactory(String synthesizer, String voice) {
        speechSynthesizer = synthesizer;
        voiceName = voice;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoolableObject makeObject() throws Exception {
        if (speechSynthesizer == null) {
            return new FreeTTSPromptGenerator(voiceName);
        }
        if(speechSynthesizer.equals("Festival")) {
            return new FestivalPromptGenerator(voiceName);
        } else if (speechSynthesizer.equals("Mary")) {
            return new MaryPromptGenerator(voiceName);
        } else {
            return new FreeTTSPromptGenerator(voiceName);
        }
    }

    /**
     * Creates a new object pool for the provided synthesizer and voice with
     * the provided number of instances. 
     * @param speechSynthesizer the synthesizer to use
     * @param voiceName name of the voice to use to play prompts
     * @param instances number of instances to create
     * @return created pool
     * @throws InstantiationException error creating the pool
     */
    @SuppressWarnings("rawtypes")
    public static ObjectPool createObjectPool(String speechSynthesizer,
            String voiceName, int instances)
      throws InstantiationException {
        LOGGER.info("creating new prompt generator pool with " + instances
                + " instance(s)...");

        PoolableObjectFactory factory = 
                new PromptGeneratorFactory(speechSynthesizer, voiceName);

        // TODO: adapt config to prompt generator constraints
        GenericObjectPool.Config config = 
                ObjectPoolUtil.getGenericObjectPoolConfig(instances);
        @SuppressWarnings("unchecked")
        ObjectPool objectPool = new GenericObjectPool(factory, config);
        initPool(objectPool);
        return objectPool;
    }
}
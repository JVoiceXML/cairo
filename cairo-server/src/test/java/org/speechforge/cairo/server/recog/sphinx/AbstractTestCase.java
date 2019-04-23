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

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Unit test base class.
 */
public abstract class AbstractTestCase extends TestCase {

    private static Logger _logger = Logger.getLogger(AbstractTestCase.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public AbstractTestCase(String testName) {
        super(testName);
    }

    public void setUp() throws Exception {
        // configure log4j
        URL log4jURL = this.getClass().getResource("/log4j.xml");
        assertNotNull(log4jURL);
        DOMConfigurator.configure(log4jURL);
    }

//    public void testDummy() throws Exception {
//        debugTestName(_logger);
//        assertTrue(true);
//    }

    protected void debugTestName() {
        debugTestName(_logger);
    }

    protected void debugTestName(Logger logger) {
        if (logger.isDebugEnabled()) {
            logger.debug("Test: " + getName() + "()");
        }
    }

}

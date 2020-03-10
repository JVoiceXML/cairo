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
package org.speechforge.cairo.server.recog;

import org.mrcp4j.MrcpResourceType;
import org.speechforge.cairo.server.recog.sphinx.AbstractTestCase;
import org.speechforge.cairo.util.rule.RuleMatch;
import org.speechforge.cairo.sip.SdpMessage;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionName;
import junit.framework.Test;
import junit.framework.TestSuite;


import org.apache.log4j.Logger;

/**
 * Unit test for RecognitionResult.
 */
public class TestRecognitionResult extends AbstractTestCase {

    private static Logger _logger = Logger.getLogger(TestRecognitionResult.class);

    /**
     * Create the test case
     * 
     * @param testName
     *            name of the test case
     */
    public TestRecognitionResult(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(TestRecognitionResult.class);
    }

    public void testValidResultWithTwoRuleMatches()  {
        debugTestName(_logger);
        String str = "one cheeseburger and a pepsi<food:cheeseburger><drink:pepsi>";
        RecognitionResult result = null;
        try {
            result = RecognitionResult.constructResultFromString(str);
        } catch (InvalidRecognitionResultException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        RuleMatch food = new RuleMatch("food","cheeseburger");
        RuleMatch drink = new RuleMatch("drink","pepsi");
    
        assertEquals(result.getText(), "one cheeseburger and a pepsi");
        assertEquals(result.getRuleMatches().size(), 2);
        assertTrue(result.getRuleMatches().contains(food));
        assertTrue(result.getRuleMatches().contains(drink));
    }

   
    public void testNullResult()  {
        debugTestName(_logger);
        String str = null;
        RecognitionResult result = null;
        try {
            result = RecognitionResult.constructResultFromString(str);
            fail("Should raise an InvalidRecognitionResultException"); 
        } catch (InvalidRecognitionResultException e) {
        }
    }

    public void testInvalidResult1()  {
        debugTestName(_logger);
        String str = "one cheeseburger and a pepsi<food;cheeseburger><drink;pepsi>";
        RecognitionResult result = null;
        try {
            result = RecognitionResult.constructResultFromString(str);
            fail("Should raise an InvalidRecognitionResultException"); 
        } catch (InvalidRecognitionResultException e) {
        }

    }
    
}

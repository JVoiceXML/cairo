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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.speechforge.cairo.rtp.server.sphinx.SpeechDataRecorder;
import org.speechforge.cairo.util.rule.RuleMatch;


/**
 * Unit test for RecognitionResult.
 */
public class TestRecognitionResult {

    private static Logger LOGGER = Logger.getLogger(TestRecognitionResult.class);

    /**
     * Create the test case
     */
    public TestRecognitionResult() {
    }

    @Test
    public void testValidResultWithTwoRuleMatches()  {
        String str = "one cheeseburger and a pepsi<food:cheeseburger><drink:pepsi>";
        RecognitionResult result = null;
        try {
            result = RecognitionResult.constructResultFromString(str);
        } catch (InvalidRecognitionResultException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        RuleMatch food = new RuleMatch("food","cheeseburger");
        RuleMatch drink = new RuleMatch("drink","pepsi");
    
        Assert.assertEquals(result.getText(), "one cheeseburger and a pepsi");
        Assert.assertEquals(result.getRuleMatches().size(), 2);
        Assert.assertTrue(result.getRuleMatches().contains(food));
        Assert.assertTrue(result.getRuleMatches().contains(drink));
    }

   
    @Test(expected = InvalidRecognitionResultException.class)
    public void testNullResult() throws InvalidRecognitionResultException  {
        String str = null;
        RecognitionResult result = null;
        result = RecognitionResult.constructResultFromString(str);
    }

    @Test(expected = InvalidRecognitionResultException.class)
    public void testInvalidResult1() throws InvalidRecognitionResultException  {
        String str = "one cheeseburger and a pepsi<food;cheeseburger><drink;pepsi>";
        RecognitionResult result = null;
        result = RecognitionResult.constructResultFromString(str);
    }
    
}

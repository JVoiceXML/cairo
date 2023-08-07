/*
 * cairo-client - Open source client for control of speech media resources.
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
package org.speechforge.cairo.client.recog;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// TODO: Auto-generated Javadoc
/**
 * Represents the result of a completed recognition request.
 * 
 * @author Niels Godfredsen {@literal <}<a href="mailto:ngodfredsen@users.sourceforge.net">ngodfredsen@users.sourceforge.net</a>{@literal >}
 */
public class RecognitionResult {

    /** The _logger. */
    private static Logger _logger = LogManager.getLogger(RecognitionResult.class);
    
    /** The Constant tagRuleDelimiter. */
    private final static String tagRuleDelimiter = ":";
    
    /** The Constant OUTOFGRAMMAR. */
    private final static String OUTOFGRAMMAR = "<unk>";
    
    /** The oog. */
    private boolean oog;

    /** The _text. */
    private String _text;
    
    /** The _rule matches. */
    private List<RuleMatch> _ruleMatches;
  

    /**
     * Instantiates a new recognition result.
     */
    public RecognitionResult() { 
        _ruleMatches = new ArrayList<RuleMatch>();
        _text = new String();
    }
    

    /**
     * Gets the text of the recognition results.
     * 
     * @return the text
     */
    public String getText() {
        return _text;
    }


    /**
     * Gets the rule matches.
     * 
     * @return the rule matches
     */
    public List<RuleMatch> getRuleMatches() {
        return _ruleMatches;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(_text);
        if (_ruleMatches != null) {
            for (RuleMatch ruleMatch : _ruleMatches) {
                sb.append('<').append(ruleMatch.getRule());
                sb.append(':').append(ruleMatch.getTag()).append('>');
            }
        }
        return sb.toString();
    }

    

    /**
     * Construct result from string.
     * 
     * @param inputString the input string
     * 
     * @return the recognition result
     * 
     * @throws InvalidRecogResultException the invalid recognition result exception
     */
    public static  RecognitionResult constructResultFromString(String inputString) throws InvalidRecogResultException {

        if (inputString == null)
            throw new InvalidRecogResultException();
        
        RecognitionResult result = new RecognitionResult();
        if(inputString.trim().equals(OUTOFGRAMMAR)) {
            result.oog = true;
            result._text = "out of grammar";
            return result;
        }
        inputString = inputString.trim();
        int firstBracketIndex =inputString.indexOf("<");
        if (firstBracketIndex >0) {
            result._text = inputString.substring(0, firstBracketIndex);      //raw result are at the begining before the first ruleMatch
            if (result == null)
                throw new InvalidRecogResultException();
            _logger.debug(result._text);
            String theTags = inputString.substring(inputString.indexOf("<"));
            theTags = theTags.trim();
            _logger.debug(theTags);
            String ruleMatches[] = theTags.split("<|>|><");
            _logger.debug("number of rule matches: " + ruleMatches.length);
            for (int i=0; i<ruleMatches.length;i++) {
                _logger.debug("**** "+ i + "th **** " + ruleMatches[i]);
                //if ((ruleMatches[i].length() > 3) &&(ruleMatches[i].contains(tagRuleDelimiter)) ){
                if (ruleMatches[i].length() > 3  ){
                    _logger.debug(" rule match # "+i+"  " +ruleMatches[i]);
                   String rule[] = ruleMatches[i].split(tagRuleDelimiter);
                   if (rule.length == 2 ) {
                      result._ruleMatches.add(new RuleMatch(rule[0],rule[1]));
                      _logger.debug(" rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                   } else {
                       _logger.debug(" Invalid rule match # "+i+"  " + rule.length+ " "+ruleMatches[i]);
                       throw new InvalidRecogResultException();
                   }
                } else {
                    _logger.debug("Bad Tag Rule In Result: "+ruleMatches[i]);
                }
            }
            
        //there is no rule to match (just return the raw result
        } else {
            result._text = inputString;   
        }
        return result;
    }
    

    /**
     * Checks if is out of grammar.
     * 
     * @return true, if is out of grammar
     */
    public boolean isOutOfGrammar() {
        return oog;
    }
    
}

package org.speechforge.cairo.performance;

import edu.cmu.sphinx.jsapi.JSGFGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.speechforge.cairo.server.recog.GrammarLocation;
import org.speechforge.cairo.server.recog.sphinx.SphinxRecEngine;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;

/**
 * A simple Sphinx-4 application that decodes a .WAV file containing...
 * The audio format
 * itself should be PCM-linear, with the sample rate, bits per sample,
 * sign and endianness as specified in the config.xml file.
 */
public abstract class BaseRecognizerWerTest {
    
    private static Logger _logger = Logger.getLogger(BaseRecognizerWerTest.class);

   ConfigurationManager cm;
   Recognizer recognizer;
   SphinxRecEngine engine;
   JSGFGrammar jsgfGrammarManager;
    
   String grammarFileName;
    
    
   double AccumulatedWER;
   int testCount;
    
    public abstract  void setUp(URL config);  
    public abstract String recognizeAudioFile(URL audiofile);
    public abstract void processGrammarLocation(GrammarLocation grammarLocation) throws IOException;
    public abstract void shutdown();
    
    
    //test files are of this format
    // grammarfile name  (always on line 1)
    // audio file name, expected results, speaker, group (repeated N time)

    private void includeThisTestInCombinedResults(String actual, String expected, String speaker, String speakerGroup) {
        
        int wordsInExpectedResult = expected.split(" ").length;
        if (expected == null) {
            _logger.info("Null Expected result!  Check test script.");
            throw new RuntimeException();
        }
        int distance = wordsInExpectedResult;
        if (actual == null) {
            _logger.info("Null result.  Counting errors as # of words in expected result.  100% error.");
        } else {
           distance = LevenshteinDistance(actual, expected);
        }
        double WER = (double)distance/(double)wordsInExpectedResult;
        AccumulatedWER = AccumulatedWER + WER;
        testCount++;
        _logger.info(testCount+" : WER %"+100.0*WER+" ("+actual+"/"+expected+")");

        
    }
    
    private int LevenshteinDistance(String actual, String expected) {

        //char s[1..m], char t[1..n])
        String s[] = actual.split(" ");
        String t[] = expected.split(" ");

        // d is a table with m+1 rows and n+1 columns
        int[][] d = new int[s.length+1][t.length+1];

        for (int i=0;i<=s.length;i++) {
            d[i][0] = i;
        }
        for (int j=0;j<=t.length;j++) {
            d[0][j] = j;
        }

        int cost = 0;
        for (int i=1;i<=s.length;i++) {
            for (int j=1;j<=t.length;j++) {
                if (s[i-1].equals(t[j-1])) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                int del = d[i-1][j] +1;
                int ins = d[i][j-1]+1;
                int sub = d[i-1][j-1] + cost;
                if ((del < ins) && (del < sub)) {
                    d[i][j] = del;
                } else if ((ins<del) && (ins < sub)) { //could eliminate first check...
                    d[i][j] = ins;
                } else {
                    d[i][j] = sub;
                }
            }
        }
        return d[s.length][t.length];
    }

    public void runTests(String fname) {
        _logger.info("Stating up with config file: "+fname);

        try {
            BufferedReader in = new BufferedReader(new FileReader(fname));
            String str;
            int linecount = 0;
            while ((str = in.readLine()) != null) {
                linecount++;
                if (linecount == 1 ) {                // first line just get the sphinx config file
                    URL configURL = this.getClass().getResource(str);
                    _logger.info(configURL);
                    setUp(configURL);
                } else if (linecount == 2 ) {          // second  line just get the grammar file
                    URL grammarFileURL = this.getClass().getResource(str);
                    GrammarLocation grammarLocation = new GrammarLocation(grammarFileURL);
                    processGrammarLocation(grammarLocation);
                    
                } else {                        // run the test
                    String test[] = str.split(",");
                    if (test.length < 2) {
                        _logger.info("Bad test. "+test.length+" params.  need at least 2");
                        _logger.info(str);
                    } else {
                       //System.out.println("Test # "+(linecount-2)+ " "+test[1]);
                       String af = test[0];
                       String expected = test[1];
                       String speaker = test[2];
                       String speakerGroup = test[3];
                       URL audioFileURL = this.getClass().getResource(af);
                       String actual = recognizeAudioFile(audioFileURL);
                       includeThisTestInCombinedResults(actual, expected, speaker, speakerGroup);
                    }
                }
            }
            in.close();
            _logger.info("Ran "+testCount+" tests with accumulated Word Error Rate of %"+ 100.0*AccumulatedWER/testCount);
            shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }
   
}

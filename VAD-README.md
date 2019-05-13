VAD-README.md

The following class description describes a number of time-outs used by our current (Sphinx-based) VAD:

https://cmusphinx.github.io/doc/sphinx4/javadoc/edu/cmu/sphinx/frontend/endpoint/SpeechMarker.html

Converts a stream of SpeechClassifiedData objects, marked as speech and non-speech, and mark out the regions that are considered speech. This is done by inserting SPEECH_START and SPEECH_END signals into the stream.

The algorithm for inserting the two signals is as follows.

The algorithm is always in one of two states: 'in-speech' and 'out-of-speech'. If 'out-of-speech', it will read in audio until we hit audio that is speech. If we have read more than 'startSpeech' amount of continuous speech, we consider that speech has started, and insert a SPEECH_START at 'speechLeader' time before speech first started. The state of the algorithm changes to 'in-speech'.

Now consider the case when the algorithm is in 'in-speech' state. If it read an audio that is speech, it is scheduled for output. If the audio is non-speech, we read ahead until we have 'endSilence' amount of continuous non-speech. At the point we consider that speech has ended. A SPEECH_END signal is inserted at 'speechTrailer' time after the first non-speech audio. The algorithm returns to 'out-of-speech' state. If any speech audio is encountered in-between, the accounting starts all over again. While speech audio is processed delay is lowered to some minimal amount. This helps to segment both slow speech with visible delays and fast speech when delays are minimal.

Here is how to change the above params (being sudo):

Modify the Sphinx config at

/root/src/cairo/src/cairo/cairo-server/trunk/src/main/resources/config/sphinx-config.xml

then recompile Cairo by executing

bash /root/src/cairo/scripts/install.sh /opt/cairo

now restart the CB stack by running

bash ~/scripts/restart.sh

Furthermore, the no-input speech timeout as well as the too-much speech timeout are defined in the class TimerThread, method run() in

/root/src/cairo/src/cairo/cairo-server/trunk/src/main/java/org/speechforge/cairo/server/recog/sphinx/KaldiRecEngineWFST.java

After changing values, Cairo needs to be recompiled as explained above.

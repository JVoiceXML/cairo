============================
Readme for cairo-server v${project.version}
============================

Overview
--------

Cairo-server is an open source speech resource server written entirely in the Java programming language.  Cairo-server builds upon pre-existing open source speech projects such as FreeTTS and Sphinx and adds additional functionality to meet the requirements necessary for use in enterprise scale deployments of speech/telephony applications.

To achieve maximum compatibility with existing and future speech clients cairo-server has been designed from the ground up to comply with the MRCPv2 standard. (For more information on MRCPv2 please see http://tools.ietf.org/html/draft-ietf-speechsc-mrcpv2)


New Features for cairo-server v${project.version}
----------------------------------

	* Cairo has been renamed to cairo-server and re-usable SIP and RTP related functionality has been factored out into separate cairo-sip and cairo-rtp projects for use by both speech servers and speech clients.
	
	* Adds support for detection of out-of-grammar responses.
	
	* Adds support for hotword barge-in mode.
	
	* Now runs on Linux out of the box with provided shell scripts for launching speech server processes.
	
	* Adds support for direct connection from SIP softphones like Xlite by implementing the SIP REGISTER method.
	
	* Adds handling of SIP BYE requests which are used to trigger the immediate release of speech resources at the completion of an active session.

  * Recognition performance has been improved by a number of methods, including adjusting the garbage collection strategy and better start and end of speech detection (endpointing).
	
	* RTP transmission has been improved with the use of a JMF workaround to ensure 20ms packet size. 
	
	* Adds support for playback of pre-recorded prompts on the speechsynth channel.
	
	* Adds natural language processing to obtain a semantic interpretion of the utterance based on the grammar.


Limitations for cairo-server v${project.version}
---------------------------------

General limitations of this release:

  * Resource processes must be run on the same host as the resource server in order for the MRCPv2 client to locate the resource.

  * This release is limited to the following MRCPv2 resource types and resource methods and events implemented:

    +-----------------------+---------------------+-----------------------+
    | MRCPv2 Resource Type  | Methods             | Events                |
    +-----------------------+---------------------+-----------------------+
    | speechrecog           | RECOGNIZE           | START-OF-INPUT        |
    |                       | START-INPUT-TIMERS  | RECOGNITION-COMPLETE  |
    |                       | STOP                |                       |
      +-----------------------+---------------------+-----------------------+
    | speechsynth           | SPEAK               | SPEAK-COMPLETE        |
    |                       | STOP                |                       |
    |                       | BARGE-IN-OCCURRED   |                       |
    +-----------------------+---------------------+-----------------------+

Limitations of the SIP support

   * Re-invite not implemented

   * Security (authentication and encryption/SIPS) not implemented

   * Options method not implemented

Limitations for the speechrecog resource:

  * A simple Semantic interpretation of the Recognition results are provided.  Natural Language Semantics Markup Language (NLSML) is not yet supported.

  * Only Java Speech Grammar Format (JSGF) is supported for specifying grammars to be used for recognition. Speech Recognition Grammar Specification (SRGS) is not yet supported.

Limitations for the speechsynth resource:

  * Text-to-speech (TTS) requests must be specified using plain text.  Speech Synthesis Markup Language (SSML) is not yet supported.


Prerequisites
-------------

Cairo requires Java Runtime Environment (JRE) 5.0 or higher which can be downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract Cairo

  To install Cairo, extract all files from the binary distribution archive to a directory of your choosing.

2. Download and Install JMF 2.1.1

  Cairo requires Java Media Framework (JMF) version 2.1.1. which can be downloaded here:

  http://java.sun.com/products/java-media/jmf/2.1.1/download.html

  Download and run the JMF installer that corresponds to your specific operating system.  This will install jmf.jar and sound.jar to the lib/ext directory of your installed JRE(s) as well as performing the configurations specific to your operating system.

3. Install JSAPI

  Run the JSAPI installer found in the lib directory of your Cairo installation (either jsapi.exe or jsapi.sh depending upon your operating system) and accept the Sun Microsystems license agreement.  This will extract the jsapi.jar to your Cairo lib directory.
  (If you run the JSAPI installer from a different directory you will need to move the jsapi.jar from that directory to your Cairo installation's lib directory in order for it to be included in the Cairo classpath.)

  Note: Extracting jsapi.jar to the lib directory is sufficient for this single Cairo installation.  However to avoid this step during future installations you can permanently install JSAPI by moving the jsapi.jar to the lib/ext directory of your installed JRE(s).


Getting Started
---------------

Once Cairo is successfully installed you can follow the instructions below to launch the various Cairo server processes and run the demo MRCPv2 clients.


Starting the Cairo Server
-------------------------

1. Server Architecture

  Rather than being architected as a single monolithic server, Cairo is composed of a number of separate components, each performing a specific function and running in its own process space (i.e. JVM).

  Cairo can be started up in a variety of configurations depending upon the capabilities required of the individual deployment.  Out of the box, Cairo is configured to be run with one instance of each component type: a Resource Server, a Receiver Resource, and a Transmitter Resource.

  The Cairo server components have the following functions:

  +----------------------+----------------------------------------------------------+
  | Component            | Function                                                 |
  +----------------------+----------------------------------------------------------+
  | Resource Server      | Manages client connections with resources (only one of   |
  |                      | these should be running per Cairo deployment).           |
  +----------------------+----------------------------------------------------------+
  | Transmitter Resource | Responsible for all functions that generate audio data   |
  |                      | to be streamed to the client (e.g. speech synthesis).    |
  +----------------------+----------------------------------------------------------+
  | Receiver Resource    | Responsible for all functions that process audio data    |
  |                      | streamed from the client (e.g. speech recognition).      |
  +----------------------+----------------------------------------------------------+

2. Launching Server Processes

  Server process are started by passing appropriate parameters to the launch.bat script in the bin directory of your Cairo installation.  However the launch.bat script should not be invoked directly.  Instead batch files are supplied for each of the server processes present in the default configuration.

  The resource server (rserver.bat) should always be started first since the resources must register with the resource server when they become available.  Once the resource server has completed initialization you will see a message on the console that says "Server and registry bound and waiting..."
 
  Then the individual resources (transmitter1.bat and receiver1.bat) can be started in any order.  When ready, each of the resources will display a "Resource bound and waiting..." message.

  Once all three server processes have completed initialization and display a waiting message, the server cluster is ready to accept MRCPv2 client requests.


Running the Demo MRCPv2 Clients
-------------------------------

A number of demo clients are supplied with the Cairo installation.  These can either be run directly or they may be used as example code to write your own MRCPv2 client.

1. Prerequisites

  The included demo clients play synthesized speech and/or perform speech recognition on microphone input and as such require a (preferably high quality) microphone and speakers to be attached to the system executing the demos.

2. Example Grammar

  Most of the demo clients that include speech recognition functionality are configured by default to use the grammar file demo/grammar/example.gram. This grammar is in Java Speech Grammar Format (JSGF). If you are familiar with JSGF you can examine the grammar file to find out what some valid utterances are that the demos will recognize. Here are some examples of valid utterances from the example.gram grammar file:

    * "I would like sports news."
    * "Get me the weather."
    * "I would like to hear a stock quote."

  Demo clients that run in -loop mode use the grammar file demo/grammar/example-loop.gram instead. This grammar extends example.gram by adding voice commands for exiting a looping demo. The example-loop.gram grammar file adds the following recognized utterances:

    * "Exit."
    * "Quit."

3. Available Clients

  The following demo clients are included in the Cairo installation.

  +------------------+------------------------------------------------------------------------+
  | Client           | Description                                                            |
  +------------------+------------------------------------------------------------------------+
  | demo-speechsynth | MRCPv2 client application that utilizes a speechsynth resource         |
  |                  | to play a TTS prompt.                                                  |
  +------------------+------------------------------------------------------------------------+
  | demo-recog       | MRCPv2 client application that utilizes a speechrecog resource         |
  |                  | to perform speech recognition on microphone input.                     |
  +------------------+------------------------------------------------------------------------+
  | demo-bargein     | MRCPv2 client application that plays a TTS prompt while                |
  |                  | performing speech recognition on microphone input.  Prompt             |
  |                  | playback is cancelled as soon as start of speech is detected.          |
  +------------------+------------------------------------------------------------------------+
  | demo-parrot      | This is the same client as demo-bargein but run in -parrot mode        |
  |                  | so that recognized utterances are read back to the user using TTS.     |
  |                  |                                                                        |
  |                  | (Note: -loop mode is also enabled so that this demo will repeat        |
  |                  | until the phrase "quit" or "exit" is recognized.)                      |
  +------------------+------------------------------------------------------------------------+
  | demo-standalone  | Standalone application that performs speech recognition on             |
  |                  | microphone input using an embedded SphinxRecEngine instance directly   |
  |                  | (instead of by streaming audio to an MRCPv2 recognition resource).     |
  +------------------+------------------------------------------------------------------------+

  Each client can be started by running the appropriate batch script located in the demo/bin directory of your Cairo installation.  Source code for the demos is also included in the installation and can be found in the demo/src/java directory.


Further Information
-------------------

For more information please see the Cairo Project Home at http://www.speechforge.org/projects/cairo.





+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 


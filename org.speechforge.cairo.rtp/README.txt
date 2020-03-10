=========================
Readme for cairo-rtp v${project.version}
=========================

Overview
--------

The cairo-rtp library provides a simple Java API for supporting RTP audio
streaming between a media source and a media sink.

This is the first release for the cairo-rtp project.   This release provides
the necessary RTP streaming capabilities required for implementing MRCPv2
clients and servers.

Cairo-rtp is written entirely in the Java programming language and uses Sun's
Java Media Framework (JMF).


Prerequisites
-------------

Cairo-rtp requires Java Runtime Environment (JRE) 5.0 or higher which can be
downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment
variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract cairo-rtp-${project.version}.jar from the download archive and add
   it to the classpath of your application.

2. Download and install JMF 2.1.1

  Cairo-rtp requires Java Media Framework (JMF) version 2.1.1. which can be
  downloaded here:

  http://java.sun.com/products/java-media/jmf/2.1.1/download.html

  Download and run the JMF installer that corresponds to your specific
  operating system.  This will install jmf.jar and sound.jar to the lib/ext
  directory of your installed JRE(s) as well as performing the configurations
  specific to your operating system.


Getting Started
---------------

The cairo-rtp library is intendended as a common component to be used by MRCPv2
clients and servers such as cairo-client and cairo-server.   If you are using
either of those packages you will not need to use cairo-rtp directly.

If you wish to use cairo-rtp in your own client or server application the best
place to start would be to look at the the example demo code in cairo-server or
cairo-client.


Further Information
-------------------

For more information please see the Cairo Project Home at http://cairo.speechforge.org.


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 


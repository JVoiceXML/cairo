=========================
Readme for cairo-sip v${project.version}
=========================

Overview
--------

The cairo-sip library provides a simple Java API for supporting SIP/SDP message
communication between MRCPv2 clients and servers.

The Session Initiation Protocol (SIP) and the Session Description Protocol
(SDP) are critical elements of the Media Resource Control Protocol Version 2
(MRCPv2) standard as described in the MRCPv2 specification:

   "MRCPv2 is not a "stand-alone" protocol - it relies on a session management
   protocol such as the Session Initiation Protocol (SIP) to establish the
   MRCPv2 control session between the client and the server, and for rendezvous
   and capability discovery. It also depends on SIP and SDP to establish the
   media sessions and associated parameters between the media source or sink
   and the media server."


New Features for cairo-sip v${project.version}
----------------------------------

	* Adds capability to do NAT traversal. The public IP address can be specified so 
	  that SDP message body and SIP headers can be setup accordingly.

	* Fixes route header bug in SIP INVITE.
	
	* Adds rtp port and state attributes to sip Sesssion in support of the Session 
	  Manager capabilities in the cairo-client project.

	* Improves logging format of SIP/SDP activity.


Limitations for Cairo v${project.version}
--------------------------

This release provides the necessary SIP and SDP capabilities required for 
implementing MRCPv2 clients and servers.

See the General limitations section for a description of functionality not yet
supported in this release.


General limitations of this release:
------------------------------------

   * re-INVITE not yet implemented

   * REGISTER method not yet implemented.  (Cairo server does not register
     itself with a registrar.  Client must know the server's address.)

   * Secure SIP (SIPS) not yet implemented.

   * OPTIONS method not yet implemented.


Prerequisites
-------------

Cairo-sip requires Java Runtime Environment (JRE) 5.0 or higher which can be
downloaded here:

  http://java.sun.com/javase/downloads/


Installation
------------

Extract cairo-sip-${project.version}.jar from the download archive and add it to the
classpath of your application.


Getting Started
---------------

The cairo-sip library is intendended as a common component to be used by MRCPv2
clients and servers such as cairo-client and cairo-server.  If you are using
either of those packages you will not need to use cairo-sip directly.

If you wish to use cairo-sip in your own client or server application the best
place to start would be to look at the the example demo code in cairo-server or
cairo-client.


Further Information
-------------------

For more information please see the Cairo Project Home at:

http://cairo.speechforge.org


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 

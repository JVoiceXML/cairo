# Cairo

Cairo sets out to provide an enterprise grade, MRCPv2 compliant speech solution
utilizing existing open source speech resources such as FreeTTS and Sphinx-4.

## Distributables

There are four projects that make up Cairo.
  
- Cairo-server is the main Cairo project.  If you are new to Cairo, this is a good place to start.
- Cairo-client is library which you can use to build your own speech clients.  Note that you also have the option to use mrcp4j library directly to build mrcpv2 clients.  Cairo-client provides a higher level abstraction than mrcp4j.
- Cairo-sip is a library used by both cairo-server and cairo-client to do SIP processing.  It is implemented using JAIN-SIP.
- Cairo-rtp is a library used by the cairo-server and cairo-client demo to do rtp processing.  It is implemented using JMF.


## Requirements

- JAVA 8
- Gradle 6.9.1

## Starting Cairo

Cairo consists of 3 parts
1. The resource sever to manage client connections with resources (only one of these should be running per Cairo deployment).
2. The transmitter resource responsible for all functions that generate audio data to be streamed to the client (e.g. speech synthesis).
3. The receiver resource Rrsponsible for all functions that process audio data streamed from the client (e.g. speech recognition).

In order to start them cd to the cairo installation directory and start them via
1. bin\rserver -sipPort 5060 -sipTransport udp
2. bin\receiver config\cairo-config.xml receiver1
3. bin\transmitter config\cairo-config.xml transmitter1

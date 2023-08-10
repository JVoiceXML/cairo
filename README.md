# Cairo

Cairo sets out to provide an enterprise grade, MRCPv2 compliant speech solution
utilizing existing open source speech resources such as FreeTTS and Sphinx-4.

## Requirements

- JAVA 8
- Gradle 6.9.1

## Starting Cairo

Cairo consists of 3 parts
1. The resource sever
2. The transmitter resource to send TTS output
3. The receiver resource to receive ASR input

In order to start them cd to the cairo installation directory and start them via
1. bin\rserver -sipPort 5060 -sipTransport udp
2. bin\receiver config\cairo-config.xml receiver1
3. bin\transmitter config\cairo-config.xml transmitter1

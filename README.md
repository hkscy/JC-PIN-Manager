# JC-PIN-Manager

A JavaCard applet for setting the global PIN (0x11) on a card using GlobalPlatform API.

## Prerequisites
* [Apache Ant](https://ant.apache.org)
* [GlobalplatformPro](https://github.com/martinpaljak/GlobalPlatformPro)
* [openjdk 11.0.2](https://openjdk.java.net/projects/jdk/11/)

## Environment Setup
1. Initialise [JavaCard SDK binaries](https://github.com/martinpaljak/oracle_javacard_sdks) by running the following commands:

```
git submodule init
git submodule update
```
2. Ensure that your `JAVA_HOME` environment variable is set to Open JDK 11, for example on OS X you can run:

```
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

3. Install the Python prerequisites by running ``pip3 install -r requirements.txt`` or equivalent on your platform.

## To build and run the applet

1. Build the applet by running `ant` from the project root directory. 
2. Install the resulting applet onto your JavaCard using [GlobalplatformPro](https://github.com/martinpaljak/GlobalPlatformPro). Note that the CVMManagement priviledge is necessary for the applet to administrate PINs, also known as Cardholder Verification Methods (CVMs), on the card. On an unlocked card please run:

```
gp --install bin/SetupApplet.cap --privs CVMManagement         
```
Otherwise, you must specify your card keys using e.g.,

```
gp --install bin/SetupApplet.cap --privs CVMManagement --key-enc ENC-KEY --key-mac MAC-KEY --key-dek DEK-KEY      
```

3. Run the ``setup_card.py`` host application which will instruct the applet to return the number of PIN retries on the global PIN, reset the global PIN to ``3333`` and then verify the new PIN.

## Debugging
Has been tested using the Taisys SIMoME only. Please let me know if it also works on your hardware! You may need to replace the GlobalPlatform API jar file with a version which is compatible with your JavaCard.
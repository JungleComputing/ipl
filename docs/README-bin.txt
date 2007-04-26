Ibis README

NOTE:
This is a very premature release of Ibis 2.0. The code is sort of coherent,
but documentation is lacking or not up-to-date. You are really on your own
with this release.

Ibis is an open source Java grid software project of the Computer Systems
group of the Computer Science department of the Faculty of
Sciences at the Vrije Universiteit, Amsterdam, The Netherlands.
The main goal of the Ibis project is to create an efficient Java-based
platform for grid computing.

This Ibis distribution contains the following:

- The Ibis communication library (defined by the Ibis Portability Layer (IPL))
  and several implementations of this IPL.
- Satin, a package for running divide-and-conquer applications.
- RMI (Remote Method Invocation), an implementation of the Java RMI for Ibis.
- GMI (Group Method Invocation), a flexible group communication package.
- MPJ, a pure Java implementation of the Java versino of MPI.

Ibis is free software. See the file "LICENSE.txt" for copying permissions.

The Ibis software requires at least a 1.5 JDK version of Java. With a
1.4 or older version of Java, this Ibis release will not run.
Suitable versions of Java can be obtained from the web.
IBM has versions for AIX (at http://www.ibm.com/developerworks/java),
SUN has versions for Solaris, Linux, and Windows (at http://java.sun.com),
and Apple has a version for Mac OS X (at http://developer.apple.com/java).

This package does not require special installation. Just place this directory
wherever you want it. You can then try a simple test which creates a single
Ibis instance, and a client and a server thread that send messages to each
other. To do so you first need to set a couple of environment variables.
See below at the system-specific notes. You need to set JAVA_HOME to the
root of your Java installation, and IBIS_HOME to the root of your Ibis
installation. Then you can go to the Ibis bin directory and run

    ibis-run junit.textui.TestRunner ibis.ipl.impl.test.TestIbis

The programmer's manual in the docs directory ("docs/progman.pdf" or
"docs/progman/progman.html" for a HTML version) contains a detailed
description of the Ibis Application Programmer's interface (API),
illustrated with example code fragments.  It also explains how to
compile and run your Ibis application.
The javadoc of the API is available in "docs/api/index.html".

Ibis has its own web-site: http://www.cs.vu.nl/ibis/.
There, you can find more Ibis documentation, papers, application sources.

The latest Ibis source repository tree is accessible through anonymous SVN
at "https://gforge.cs.vu.nl/svn/ibis/ibis/trunk". Likewise, the latest example
applications are available at "https://gforge.cs.vu.nl/svn/ibis/apps/trunk".
You need an account on https://gforge.cs.vu.nl/ to access the repositories
there. You can create an account by clicking the 'New Account' button
on the https://gforge.cs.vu.nl/ page.
To build these applications, you first need to set some environment
variables. See the README.txt file that accompanies the Ibis applications.

The file BUGS.txt contains information for filing bug reports.

This Ibis release includes grun, a tool to start jobs on a
Globus-based Grid. See "grun/docs/README" for more details.
The grun program itself lives in "grun/bin".

This product includes software developed by the Apache Software Foundation
(http://www.apache.org/).

The BCEL copyright notice lives in "notices/LICENSE.bcel.txt".
The Log4J copyright notice lives in "notices/LICENSE.log4j.txt".

This product includes Junit (http://www.junit.org), which is
distributed under the Common Public License Version 1.0
(http://www.opensource.org/licenses/cpl.php).

This product includes software developed by the
Ant-Contrib project (http://sourceforge.net/projects/ant-contrib).
See "notices/LICENSE.ant-contrib.txt" for the ant-contrib copyright notice.

System-specific notes

Linux, Solaris, other Unix systems
    Install a recent Java SDK, at least 1.5, and set the JAVA_HOME
    environment variable to the location where it is installed,
    for example
        export JAVA_HOME=/usr/local/java/jdk1.5
    or
        set JAVA_HOME=/usr/local/java/jdk1.5
    for CSH users.
    It is probably best to add this to your .bash_profile, .profile,
    or .cshrc file (whichever gets executed when you log in to your
    system).

Mac OS X
    Set the environment variable JAVA_HOME to "/Library/Java/Home".
    You are required to install the Java SDK. See the Linux notes on
    how to set environment variables.

Windows 2000, Windows XP
    Install a recent Java SDK, at least 1.5.  This will get installed in
    for instance "c:\Program Files\Java\jdk1.5.0". You can set the
    JAVA_HOME environment variable to this path by going to the
    Control Panel, System, the "Advanced" tab, Environment variables,
    add it there and reboot your system. IBIS_HOME can be set
    similarly.

Cygwin
    See the notes on Windows 2000, Windows XP.

Ibis README

NOTE:
This is a very premature release of Ibis 2.0. The code is sort of coherent,
but manuals are lacking or not up-to-date. You are really on your own
with this release.

Ibis is an open source Java grid software project of the Computer Systems
group of the Computer Science department of the Faculty of
Sciences at the Vrije Universiteit, Amsterdam, The Netherlands.
The main goal of the Ibis project is to create an efficient Java-based
platform for grid computing.

This Ibis distribution contains the Ibis communication library (defined by the
Ibis Portability Layer (IPL)) and several implementations of this IPL.
Some example applications are provided in the "examples" directory.

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
See below at the system-specific notes. You need to set IBIS_HOME to the root
of your Ibis installation. Now, you can go to the Ibis bin directory and run

    ibis-server --events

to start an Ibis server that will print events as they occur.
Next (in another window) you can run

    ibis-run -Dibis.pool.name=blabla -Dibis.server.address=localhost junit.textui.TestRunner ibis.ipl.impl.test.TestIbis

The output should say "OK (1 test)" at the end.

The programmer's manual in the docs directory ("docs/progman.pdf")
contains a detailed description of the Ibis Application Programmer's
interface (API), illustrated with example code fragments.
It also explains how to compile and run your Ibis application.
The javadoc of the API is available in "javadoc/index.html".

Ibis has its own web-site: http://www.cs.vu.nl/ibis/.
There, you can find more Ibis documentation, papers, application sources.

The latest Ibis source repository tree is accessible through anonymous SVN
at "https://gforge.cs.vu.nl/svn/ibis/ibis/trunk". Likewise, the latest example
applications are available at "https://gforge.cs.vu.nl/svn/ibis/ipl-apps/trunk".
You need an account on https://gforge.cs.vu.nl/ to access the repositories
there. You can create an account by clicking the 'New Account' button
on the https://gforge.cs.vu.nl/ page.
To build these applications, you first need to set some environment
variables. See the README.txt file that accompanies the Ibis applications.

The file BUGS.txt contains information for filing bug reports.

There is some dispute about the pronounciation of the word "Ibis". The
file "docs/rob.mp3" shows how one of the Ibis designers feels about this
issue.

Third party libraries included with Ibis

This product includes software developed by the Apache Software Foundation
(http://www.apache.org/).

The BCEL copyright notice lives in "notices/LICENSE.bcel.txt".
The Log4J copyright notice lives in "notices/LICENSE.log4j.txt".
The Commons copyright notice lives in notices/LICENSE.apache-2.0.txt".

This product includes jstun, which is distributed with a dual license,
one of which is version 2.0 of the Apache license. It lives in
"notices/LICENSE.apache-2.0.txt".

This product includes the UPNP library from SuperBonBon Industries. Its
license lives in "notices/LICENSE.apache-2.0.txt".

This product includes the Ganymed SSH-2 library from ETHZ. Its license
lives in "notices/LICENSE.ganymed.txt".

This product includes software developed by TouchGraph LLC
(http://www.touchgraph.com/). Its license lives in "notices/LICENSE.TG.txt".

This product includes Junit (http://www.junit.org), which is
distributed under the Common Public License Version 1.0
(http://www.opensource.org/licenses/cpl.php).

System-specific notes

Windows 2000, Windows XP
    Install and use a recent Java SDK, at least 1.5.  This will get installed
    in for instance "c:\Program Files\Java\jdk1.5.0".
    You can set the IBIS_HOME environment variable going to the
    Control Panel, System, the "Advanced" tab, Environment variables,
    add it there and reboot your system.

Cygwin
    See the notes on Windows 2000, Windows XP.
    Note: there is a separate ibis-run script for Cygwin in the bin-directory,
    called ibis-run.cygwin. It is wise to move that to ibis-run.

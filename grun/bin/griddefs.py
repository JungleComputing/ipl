#
# Copyright (C) 2004  Vrije Universiteit, Amsterdam, The Netherlands.
# For full copyright and restrictions on use, see the file COPYRIGHT
# in the Grun software distribution.
#
# Author: Kees Verstoep (versto@cs.vu.nl)
#

class Grid_site:
    allsites = {}
    
    def __init__(self, name, host, jobmanager, usehostcount, filexfer, xfer_jobmanager, env = [], rslexts = {}):
        self.name       = name
        self.host       = host
        self.jobmanager = jobmanager
        self.usehostcount = usehostcount
        self.xfer       = filexfer
        # Note: may need to override xfer_host for sites that run a separate
        # storage server like EDG/LCG
        self.xfer_host  = host
        self.xfer_jobmanager = xfer_jobmanager
        self.env        = env
        self.rslexts    = rslexts
        Grid_site.allsites[name] = self
        Grid_site.allsites[host] = self

def sitediag():
    for name in Grid_site.allsites.keys():
        site = Grid_site.allsites[name]
        print site.name, "default jobmanager", site.jobmanager

def sitedefs():
    pbs    = "jobmanager-pbs"
    fork   = "jobmanager-fork"
    condor = "jobmanager-condor"
    sge    = "jobmanager-sge"
    ccs    = "jobmanager-ccs"
    gass   = "GASS" # only file xfer protocol supported right now

    ##########################################################################
    # Sites known to work:

    Grid_site("fs0",       "fs0.das2.cs.vu.nl",            sge, 1, gass, fork,
              [ "JAVA_HOME=/usr/local/sun-java/jdk1.4" ])
    Grid_site("fs1",       "fs1.das2.liacs.nl",            sge, 1, gass, fork,
              [ "JAVA_HOME=/usr/local/sun-java/jdk1.4" ])
    Grid_site("fs2",       "fs2.das2.nikhef.nl",           sge, 1, gass, fork,
              [ "JAVA_HOME=/usr/local/sun-java/jdk1.4" ])
    Grid_site("fs3",       "fs3.das2.ewi.tudelft.nl",      sge, 1, gass, fork,
              [ "JAVA_HOME=/usr/local/sun-java/jdk1.4" ])
    Grid_site("fs4",       "fs4.das2.phys.uu.nl",          sge, 1, gass, fork,
              [ "JAVA_HOME=/usr/local/sun-java/jdk1.4" ])

    # Works with and without hub
    # Default max number of 2 nodes (4 cpus) on reserved gridlab queue
    # For more nodes on skirit use "(queue=long)", but queue times are looong
    Grid_site("skirit",    "skirit.ics.muni.cz",           pbs, 0, gass, fork,
              [ "JAVA_HOME=/packages/run/jdk-1.4.2/1.4.2_04" ],
              { pbs : "(queue=gridlab)" })

    # Works with and without hub
    Grid_site("litchi",    "litchi.zib.de",               fork, 0, gass, fork,
              [ "JAVA_HOME=/usr/java/j2sdk1.4.2_03" ])

    # Works with hub, but NOT WITHOUT!
    Grid_site("matrix",    "ce.matrix.sara.nl",            pbs, 0, gass, fork,
              [ "JAVA_HOME=/usr/java/j2sdk1.4.2_04" ])

    # Works with hub, but NOT WITHOUT!
    Grid_site("packcs-e0", "packcs-e0.scai.fraunhofer.de", pbs, 1, gass, fork,
              [ "JAVA_HOME=/opt/j2re" ])

    # Works with hub, but NOT WITHOUT!
    # Some termination issue yet, though, it seems.
    # NOTE: SGE not used by default, since it allocates the same node
    # multiple times; a problem with the Globus/SGE jobmanager script I think.
    #Grid_site("n0",        "n0.hpcc.sztaki.hu",           sge, 0, gass, fork)
    Grid_site("n0",      "n0.hpcc.sztaki.hu",           condor, 0, gass, fork,
              [ "JAVA_HOME=/usr/local/java" ])

    ##########################################################################
    # Sites that are usuable, with some restrictions..

    # Works with hub, but NOT WITHOUT!
    # NOTE: Error "invalid script response" when PBS is used (sent them mail)
    #Grid_site("helix",   "helix.bcvc.lsu.edu",           pbs, 0, gass, fork)
    Grid_site("helix",     "helix.bcvc.lsu.edu",          fork, 0, gass, fork,
              [ "JAVA_HOME=/usr/java/j2sdk1.4.2_03" ])

    # Need more experience with this new one in Korea:
    Grid_site("venus",  "venus.gridcenter.or.kr",         pbs, 0, gass, fork,
	      [])

    ##########################################################################
    # Sites that are largely problematic at the moment..
    
    # Heavily firewalled plus NAT
    # With hub/routedmsgs does work together with fs0 (not TCPSplice?!)
    # Currently new job submission issue due to firewall?
    Grid_site("tc",      "tc.few.vu.nl",                   sge, 0, gass, fork,
              [ "JAVA_HOME=/usr/java/j2re1.4.2_02" ])

    # Just went over to SGE, which has the same problem as n0 above.
    # Possibly works with hub (needs more tests), but NOT WITHOUT!
    Grid_site("eltoro",     "eltoro.pcz.pl",               sge, 0, gass, fork,
              [ "JAVA_HOME=/usr/java"])

    # Works with hub, but NOT WITHOUT!
    # NOTE: PBS not working at the moment?
    Grid_site("rage1",     "rage1.man.poznan.pl",          pbs, 0, gass, fork,
              [ "JAVA_HOME=/usr/java_1.4.2_06" ])

    # Works without hub, and with hub/RoutedMsg.
    # Hangs with hub/TCPSplice?!
    # Note: ccs/fork jobmanager problems?
    Grid_site("gridentry", "gridentry.uni-paderborn.de",   ccs, 0, gass, fork,
              [])

    # Strange problems at the moment (testbed status mostly "red")
    Grid_site("peyote",  "peyote.aei.mpg.de",            pbs, 0, gass, fork,
              [ "JAVA_HOME=/usr/remote/java" ])

    # GRAM submit errors at the moment.
    # Single cpu runs work, but NameServer LEAVE fails (firewall probably)
    # Note: file operations (e.g., unpacking Ibis) are increadibly slooooow!!!
    Grid_site("hitcross",  "hitcross.lrz-muenchen.de",    fork, 0, gass, fork,
              [])

    # Often down; submits currently fail.
    Grid_site("bouscat",   "bouscat.cs.cf.ac.uk",       condor, 0, gass, fork,
              [])

    # JDK1.3 startup error:
    # could not create ibis.ipl.impl.tcp.TcpIbis, trying ibis.ipl.impl.net.NetIbis
    Grid_site("grape",     "grape.man.poznan.pl",         fork, 0, gass, fork,
              [])

    # No Java, so useless for Ibis jobs:
    Grid_site("sr8000",    "sr8000.lrz-muenchen.de",      fork, 0, gass, fork)

    # Works with and without hub
    # Onyx3 will be gone soon.
    Grid_site("onyx3",     "onyx3.zib.de",                fork, 0, gass, fork)

    # Works with and without hub
    # Often down.
    Grid_site("pclab120", "pclab120.telecom.ece.ntua.gr", fork, 0, gass, fork)
    

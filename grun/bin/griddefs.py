#
# Copyright (C) 2004  Vrije Universiteit, Amsterdam, The Netherlands.
# For full copyright and restrictions on use, see the file COPYRIGHT
# in the Grun software distribution.
#
# Author: Kees Verstoep (versto@cs.vu.nl)
#

class Grid_site:
    allsites = {}
    
    def __init__(self, name, host, jobmanager, usehostcount, filexfer, xfer_jobmanager, rslexts = {}):
        self.name       = name
        self.host       = host
        self.jobmanager = jobmanager
        self.usehostcount = usehostcount
        self.xfer       = filexfer
        # Note: may need to override xfer_host for sites that run a separate
        # storage server like EDG/LCG
        self.xfer_host  = host
        self.xfer_jobmanager = xfer_jobmanager
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

    # Defaults for some sites tested with grun (mostly DAS2 and GridLab):
    Grid_site("fs0",       "fs0.das2.cs.vu.nl",            pbs, 1, gass, fork)
    Grid_site("fs1",       "fs1.das2.liacs.nl",            pbs, 1, gass, fork)
    Grid_site("fs2",       "fs2.das2.nikhef.nl",           pbs, 1, gass, fork)
    Grid_site("fs3",       "fs3.das2.its.tudelft.nl",      pbs, 1, gass, fork)
    Grid_site("fs4",       "fs4.das2.phys.uu.nl",          pbs, 1, gass, fork)
    Grid_site("skirit",    "skirit.ics.muni.cz",           pbs, 0, gass, fork,
              { pbs : "(queue=gridlab)" })
    Grid_site("litchi",    "litchi.zib.de",               fork, 0, gass, fork)
    Grid_site("onyx3",     "onyx3.zib.de",                fork, 0, gass, fork)
    Grid_site("matrix",    "ce.matrix.sara.nl",            pbs, 0, gass, fork)
    Grid_site("packcs-e0", "packcs-e0.scai.fraunhofer.de", pbs, 1, gass, fork)
    Grid_site("n0",      "n0.hpcc.sztaki.hu",           condor, 0, gass, fork)
    Grid_site("rage1",     "rage1.man.poznan.pl",          pbs, 0, gass, fork)
    Grid_site("gridentry", "gridentry.uni-paderborn.de",   ccs, 0, gass, fork)
    Grid_site("eltoro",     "eltoro.pcz.pl",               sge, 0, gass, fork)
    Grid_site("helix",     "helix.bcvc.lsu.edu",          fork, 0, gass, fork)
    Grid_site("pclab120", "pclab120.telecom.ece.ntua.gr", fork, 0, gass, fork)
    Grid_site("tc",      "tc.few.vu.nl",                   sge, 0, gass, fork)
    Grid_site("peyote",    "peyote.aei.mpg.de",            pbs, 0, gass, fork)
    Grid_site("hitcross",  "hitcross.lrz-muenchen.de",    fork, 0, gass, fork)
    Grid_site("bouscat",   "bouscat.cs.cf.ac.uk",       condor, 0, gass, fork)
    Grid_site("grape",     "grape.man.poznan.pl",         fork, 0, gass, fork)
    Grid_site("sr8000",    "sr8000.lrz-muenchen.de",      fork, 0, gass, fork)

import os
import time

command = 'python2 seq.py --time 1000 -v %s examples/benchmark-suite/%s &'

benchmarks = [
    'qg3-09.cnf',
    'qg5-10.cnf',
    'uuf175-01.cnf',
    'uuf200-01.cnf',
    'dlx2_aa.cnf',
    'dlx2_ca.cnf',
]

solvers = [ 'SeqSolver', 'SATSolver', 'DPLLSolver' ]

for bm in benchmarks:
    for s in solvers:
        cmd = command % (s, bm )
        os.system( cmd )
        time.sleep(3)

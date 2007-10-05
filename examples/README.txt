This directory contains some Ibis example programs, organized
into a number of sub-directories (some of which may not be present in your
Ibis installation). See the README files in each sub-directory for more
details on the application.

Some low-level benchmarks and tests:

ibis/ipl/apps/tp-JavaGrande02
    This benchmark program can be used to measure latency and throughput
    for several data types.
ibis/ipl/apps/benchmarks
    Directory containing some lowlevel benchmarks.

Some applications:

ibis/ipl/apps/cell1d
    A simple implementation of cellular automata (CA).
    It uses the Ibis communication classes immediately.
ibis/ipl/apps/traffic
    A simple traffic simulator, where cars and trucks
    with individual prefered speeds travel along a 3-lane road. They overtake
    when they are blocked too much by preceding cars (and when it is
    safe).
ibis/ipl/apps/sor/explicit
    Red/black Successive Over Relaxation (SOR) is an iterative method for
    solving discretized Laplace equations on a grid.
    This implementation is an Ibis version. It distributes the grid
    row-wise among the CPUs. Each CPU exchanges one row of the matrix with
    its neighbours at the beginning of each iteration.

Other files in this directory are:

build.xml
    Ant build file for building Ibis applications.
    "ant build" (or simply: "ant") will build all applications that
    are present in this directory. "ant clean" will remove
    what "ant build" made.

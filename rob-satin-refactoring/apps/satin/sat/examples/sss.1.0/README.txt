Superscalar Suite 1.0 (SSS.1.0) in CNF Format
==========================================================================

Author:      Miroslav N. Velev
             Department of Electrical and Computer Engineering
             Carnegie Mellon University
             Pittsburgh, PA 15213, U.S.A.
             mvelev@ece.cmu.edu
             http://www.ece.cmu.edu/~mvelev

Date:        April 5, 1999

Description: Propositional logic formulas to be checked for being
             tautologies

This research was supported in part by the SRC under contract 99-DC-068.



0. Condition of Availability
----------------------------
The SSS.1.0 benchmark suite is made available, provided that any
publications that use it will list the reference:

  M.N. Velev, Superscalar Suite 1.0. Available from: http://www.ece.cmu.edu/~mvelev.

and that the authors of such publications will email
  Miroslav N. Velev (mvelev@ece.cmu.edu)
and
  Randal E. Bryant (Randy.Bryant@cs.cmu.edu)
with the best results achieved, and with enough technical details as to enable the
replication of the experiments.



1. Format Description
---------------------
The CNF format was used.
(See ftp://dimacs.rutgers.edu/pub/challenge/satisfiability/doc/satformat.tex)



2. Benchmarks
-------------
The propositional formulas were generated as described in [1]. They represent
the correctness criteria for a 1-issue DLX processor [2], as well as for
different versions of a 2-issue in-order superscalar DLX processor.


2.1 Correct Model Verification
------------------------------
The following benchmarks are ordered in increasing order of their complexity.
None should be satisfiable, i.e., there should be no counterexamples for the
correctness of each processor.

Benchmark     Processor Model Verified
---------     ----------------------------------------------------------------
dlx1_c        1-issue, 5-stage DLX processor that implements the following 
              6 instruction types: register-register, register-immediate,
              load, store, branch, jump;

The following are 2-issue superscalar DLX models with in-order execution,
having 2 pipelines of 5 stages each:

dlx2_aa       has two arithmetic pipelines (implementing register-register 
              and register-immediate instructions), such that either 1 or 2 new 
              instructions are fetched every clock cycle, conditional on the 
              second instruction in the Decode stage having (or not) a data 
              dependency on the first instruction in that stage;

dlx2_sa       can execute arithmetic and store instructions by the first 
              pipeline and arithmetic instructions by the second pipeline, so 
              that in addition to the case of the above data dependency, 
              1 instruction will be fetched also when the second instruction 
              in the Decode stage is a store (i.e., there is a structural hazard);

dlx2_la       can execute arithmetic, store, and load instructions by the first 
              pipeline and arithmetic instructions by the second pipeline, so that 
              2 load interlocks come into play now (between the instruction in 
              Execute in the first pipeline and the two instructions in Decode) 
              and 0, 1, or 2 new instructions can be fetched each cycle;

dlx2_ca       has a complete first pipeline, capable of executing the 6 instruction 
              types, and an arithmetic second pipeline, such that 0, 1, or 2 new 
              instructions can be fetched each cycle;

dlx2_cs       has a complete first pipeline, and a second pipeline that can execute 
              arithmetic and store instructions, such that 0, 1, or 2 new 
              instructions can be fetched each cycle;

dlx2_cl       has a complete first pipeline, and a second pipeline that can execute 
              arithmetic, store, and load instructions, such that 0, 1, or 2 new 
              instructions can be fetched each cycle, conditional on 4 possible load 
              interlocks (between a load in Execute in either pipeline and an 
              intruction in Decode in either pipeline) and the resolution of the 
              structural hazard of branches and jumps in Decode of pipeline two, 
              which need to wait for pipeline one;

dlx2_cc       has two complete pipelines, 4 possible load interlocks, but no 
              structural hazards, such that 0, 1, or 2 new instructions can be fetched 
              each cycle.


2.2 Incorrect Model Verification
--------------------------------
The benchmarks dlx2_cc_bug* are 40 incorrect versions of benchmark dlx2_cc.
The sequence of bug numbers does not reflect the relative complexity of the
Boolean formulas.
They should all be satisfiable, i.e., there should be a counterexample for
the correctness of each buggy processor.



References:
-----------
[1] M.N. Velev, and R.E. Bryant, "Superscalar Processor Verification 
    Using Efficient Reductions of the Logic of Equality with Uninterpreted 
    Functions to Propositional Logic", Correct Hardware 
    Design and Verification Methods (CHARME'99), September 1999.
    Available from: http://www.ece.cmu.edu/~mvelev.
[2] J.L. Hennessy, and D.A. Patterson, Computer Architecture: 
    A Quantitative Approach, 2nd edition, Morgan Kaufmann Publishers, 
    San Francisco, CA, 1996.



/****************************************************************************
* Copyright (c) 1999  Carnegie Mellon University.                           * 
* All rights reserved.                                                      *
*                                                                           *
* This benchmark suite is distributed by Carnegie Mellon University         *
* ("University") under license agreement "as is" on a nonexclusive,         *
* royalty-free basis, completely without warranty or service support.       *
* This benchmark suite is for internal use only within the licensee         *
* organization, including all divisions and subsidiaries.                   *
*                                                                           *
* The University hearby disclaims all implied warranties, including the     *
* implied warranties of merchantability and fitness for a particular        *
* purpose.  The University and its employees shall not be liable for any    *
* damages incurred by the licensee in use of the benchmark suite, including *
* direct, indirect, special, incidental, or consequential damages.          *
*                                                                           *
****************************************************************************/


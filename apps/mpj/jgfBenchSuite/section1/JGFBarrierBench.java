/**************************************************************************
*                                                                         *
*         Java Grande Forum Benchmark Suite - MPJ Version 1.0             *
*                                                                         *
*                            produced by                                  *
*                                                                         *
*                  Java Grande Benchmarking Project                       *
*                                                                         *
*                                at                                       *
*                                                                         *
*                Edinburgh Parallel Computing Centre                      *
*                                                                         * 
*                email: epcc-javagrande@epcc.ed.ac.uk                     *
*                                                                         *
*                                                                         *
*      This version copyright (c) The University of Edinburgh, 2001.      *
*                         All rights reserved.                            *
*                                                                         *
**************************************************************************/
/**************************************************************************
* Ported to MPJ:                                                          *
* Markus Bornemann                                                        * 
* Vrije Universiteit Amsterdam Department of Computer Science             *
* 19/06/2005                                                              *
**************************************************************************/

import jgfutil.*; 
import ibis.mpj.*;

public class JGFBarrierBench implements JGFSection1{

  public  static  int nprocess;
  public  static  int rank;

  private static final int INITSIZE = 1;
  private static final int MAXSIZE = 1000000;
  private static final double TARGETTIME = 10.0;

  public JGFBarrierBench(int nprocess) {
        this.nprocess=nprocess;
  }

  public void JGFrun() throws MPJException {

    int size;
    double [] time = new double[1];


/* Create the timer */ 
      if(rank==0){
        JGFInstrumentor.addTimer("Section1:Barrier", "barriers");
      }

      time[0] = 0.0;
      size=INITSIZE;

      MPJ.COMM_WORLD.barrier();

/* Start the timer */
      while (time[0] < TARGETTIME && size < MAXSIZE){
        if(rank==0){
          JGFInstrumentor.resetTimer("Section1:Barrier");
          JGFInstrumentor.startTimer("Section1:Barrier");
        }

/* Carryout the barrier operation */
        for (int k=0; k<size; k++){
          MPJ.COMM_WORLD.barrier();
        }

/* Stop the timer */
        if(rank==0){
          JGFInstrumentor.stopTimer("Section1:Barrier"); 
          time[0] = JGFInstrumentor.readTimer("Section1:Barrier"); 
          JGFInstrumentor.addOpsToTimer("Section1:Barrier",(double) size); 
        }

/* Broadcast time to the other processes */
        MPJ.COMM_WORLD.barrier();
        MPJ.COMM_WORLD.bcast(time,0,1,MPJ.DOUBLE,0);
        size *=2;
      }
 
/* Print the timing information */
        if(rank==0){
          JGFInstrumentor.printperfTimer("Section1:Barrier");
        }

  }

  public static void main(String[] argv) throws MPJException{

/* Initialise MPJ */
     MPJ.init(argv);
     rank = MPJ.COMM_WORLD.rank();
     nprocess = MPJ.COMM_WORLD.size();

     if(rank==0){
     JGFInstrumentor.printHeader(1,0,nprocess);
     }
     JGFBarrierBench ba = new JGFBarrierBench(nprocess);
     ba.JGFrun();

/* Finalise MPJ */
     MPJ.finish();

  }

}


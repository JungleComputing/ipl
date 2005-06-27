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

import java.io.*;
import jgfutil.*; 
import ibis.mpj.*;

public class JGFReduceBench implements JGFSection1{

  public  static  int nprocess;
  public  static  int rank;

  private static final int INITSIZE = 1;
  private static final int MAXSIZE =  1000000;
  private static final double TARGETTIME = 10.0;
  private static final int MLOOPSIZE = 2;
  private static final int SMAX = 5000000;
  private static final int SMIN = 4;

  public JGFReduceBench(int nprocess) {
        this.nprocess=nprocess;
  }

  public void JGFrun() throws MPJException {

    int size,i,l,m_size;
    double logsize;
    double b_time; 
    b_time = 0.0;
    double [] time = new double[1];

    m_size = 0;
    logsize = Math.log((double) SMAX) - Math.log((double) SMIN);

/* Reduce an array of doubles */

/* Create the timers */ 
      if(rank==0){
        JGFInstrumentor.addTimer("Section1:Reduce:Double", "bytes");
        JGFInstrumentor.addTimer("Section1:Reduce:Barrier", "barriers");
      }


/* loop over no of different message sizes */
    for(l=0;l<MLOOPSIZE;l++){

/* Initialize the sending data */
      m_size = (int)(Math.exp(Math.log((double)SMIN)+(double) ((double) l/(double) MLOOPSIZE*logsize)));
      double [] send_arr = new double[m_size];
      double [] recv_arr = new double[m_size];
      time[0] = 0.0;
      size=INITSIZE;

      MPJ.COMM_WORLD.barrier();

/* Start the timer */
      while (time[0] < TARGETTIME && size < MAXSIZE){
        if(rank==0){
          JGFInstrumentor.resetTimer("Section1:Reduce:Double");
          JGFInstrumentor.startTimer("Section1:Reduce:Double");
        }

/* Carryout the broadcast operation */
        for (int k=0; k<size; k++){
          MPJ.COMM_WORLD.reduce(send_arr,0,recv_arr,0,send_arr.length,MPJ.DOUBLE,MPJ.SUM,0);
          MPJ.COMM_WORLD.barrier();

        }

/* Stop the timer */
        if(rank==0){
          JGFInstrumentor.stopTimer("Section1:Reduce:Double"); 
          time[0] = JGFInstrumentor.readTimer("Section1:Reduce:Double"); 
          JGFInstrumentor.addOpsToTimer("Section1:Reduce:Double",(double) size*send_arr.length*8); 
        }

/* Broadcast time to the other processes */
        MPJ.COMM_WORLD.barrier();
        MPJ.COMM_WORLD.bcast(time,0,1,MPJ.DOUBLE,0);
        size *=2;
      }
 
        size /=2;

/* determine the cost of the Barrier, subtract the cost and write out the performance time */
      MPJ.COMM_WORLD.barrier();
      if(rank==0) {
        JGFInstrumentor.resetTimer("Section1:Reduce:Barrier");
        JGFInstrumentor.startTimer("Section1:Reduce:Barrier");
      }

      for (int k=0; k<size; k++){
        MPJ.COMM_WORLD.barrier();
      }

      if(rank==0) {
        JGFInstrumentor.stopTimer("Section1:Reduce:Barrier");
        b_time = JGFInstrumentor.readTimer("Section1:Reduce:Barrier");
        JGFInstrumentor.addTimeToTimer("Section1:Reduce:Double", -b_time);
        JGFInstrumentor.printperfTimer("Section1:Reduce:Double",send_arr.length); 
      }

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
     JGFReduceBench rd = new JGFReduceBench(nprocess);
     rd.JGFrun();

/* Finalise MPJ */
     MPJ.finish();

  }

}


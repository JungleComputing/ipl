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

public class JGFBcastBench implements JGFSection1{

  public  static  int nprocess;
  public  static  int rank;

  private static final int INITSIZE = 1;
  private static final int MAXSIZE =  1000000;
  private static final double TARGETTIME = 10.0;
  private static final int MLOOPSIZE = 25;
  private static final int SMAX = 5000000;
  private static final int SMIN = 4;

  public JGFBcastBench(int nprocess) {
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

/* Bcast an array of doubles */

/* Create the timers */ 
      if(rank==0){
        JGFInstrumentor.addTimer("Section1:Bcast:Double", "bytes");
        JGFInstrumentor.addTimer("Section1:Bcast:Barrier", "barriers");
      }


/* loop over no of different message sizes */
    for(l=0;l<MLOOPSIZE;l++){

/* Initialize the sending data */
      m_size = (int)(Math.exp(Math.log((double)SMIN)+(double) ((double) l/(double) MLOOPSIZE*logsize)));
      double [] arr = new double[m_size];
      time[0] = 0.0;
      size=INITSIZE;

      MPJ.COMM_WORLD.barrier();

/* Start the timer */
      while (time[0] < TARGETTIME && size < MAXSIZE){
        if(rank==0){
          JGFInstrumentor.resetTimer("Section1:Bcast:Double");
          JGFInstrumentor.startTimer("Section1:Bcast:Double");
        }

/* Carryout the broadcast operation */
        for (int k=0; k<size; k++){
          MPJ.COMM_WORLD.bcast(arr,0,arr.length,MPJ.DOUBLE,0);
          MPJ.COMM_WORLD.barrier();

        }

/* Stop the timer */
        if(rank==0){
          JGFInstrumentor.stopTimer("Section1:Bcast:Double"); 
          time[0] = JGFInstrumentor.readTimer("Section1:Bcast:Double"); 
          JGFInstrumentor.addOpsToTimer("Section1:Bcast:Double",(double) size*arr.length*8); 
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
        JGFInstrumentor.resetTimer("Section1:Bcast:Barrier");
        JGFInstrumentor.startTimer("Section1:Bcast:Barrier");
      }

      for (int k=0; k<size; k++){
        MPJ.COMM_WORLD.barrier();
      }

      if(rank==0) {
        JGFInstrumentor.stopTimer("Section1:Bcast:Barrier");
        b_time = JGFInstrumentor.readTimer("Section1:Bcast:Barrier");
        JGFInstrumentor.addTimeToTimer("Section1:Bcast:Double", -b_time);
        JGFInstrumentor.printperfTimer("Section1:Bcast:Double",arr.length); 
      }

    }


/* Bcast an array of objects containing a double */

/* Create the timer */
    if(rank==0){
      JGFInstrumentor.addTimer("Section1:Bcast:Object", "objects");
    }


/* loop over no of different message sizes */
    for(l=0;l<MLOOPSIZE;l++){

/* Initialize the sending data */
      m_size = (int)(Math.exp(Math.log((double)SMIN)+(double) ((double) l/(double) MLOOPSIZE*logsize)));
      obj_double [] arr_obj = new obj_double[m_size];
      for(int k=0;k<m_size;k++){
       arr_obj[k] = new obj_double(0.0);
      }
      time[0] = 0.0;
      size=INITSIZE;

      MPJ.COMM_WORLD.barrier();

/* Start the timer */
      while (time[0] < TARGETTIME && size < MAXSIZE){
        if(rank==0){
          JGFInstrumentor.resetTimer("Section1:Bcast:Object");
          JGFInstrumentor.startTimer("Section1:Bcast:Object");
        }

/* Carryout the broadcast operation */
        for (int k=0; k<size; k++){
          MPJ.COMM_WORLD.bcast(arr_obj,0,arr_obj.length,MPJ.OBJECT,0);
          MPJ.COMM_WORLD.barrier();

        }

/* Stop the timer */
        if(rank==0){
          JGFInstrumentor.stopTimer("Section1:Bcast:Object");
          time[0] = JGFInstrumentor.readTimer("Section1:Bcast:Object");
          JGFInstrumentor.addOpsToTimer("Section1:Bcast:Object",(double) size*arr_obj.length);
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
        JGFInstrumentor.resetTimer("Section1:Bcast:Barrier");
        JGFInstrumentor.startTimer("Section1:Bcast:Barrier");
      }

      for (int k=0; k<size; k++){
        MPJ.COMM_WORLD.barrier();
      }

      if(rank==0) {
        JGFInstrumentor.stopTimer("Section1:Bcast:Barrier");
        b_time = JGFInstrumentor.readTimer("Section1:Bcast:Barrier");
        JGFInstrumentor.addTimeToTimer("Section1:Bcast:Object", -b_time);
        JGFInstrumentor.printperfTimer("Section1:Bcast:Object",arr_obj.length);
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
     JGFBcastBench bc = new JGFBcastBench(nprocess);
     bc.JGFrun();

/* Finalise MPJ */
     MPJ.finish();

  }

}


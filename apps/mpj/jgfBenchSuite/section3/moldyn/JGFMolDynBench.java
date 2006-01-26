/* $Id$ */

/**************************************************************************
*                                                                         *
*             Java Grande Forum Benchmark Suite - MPJ Version 1.0         *
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


package moldyn; 

import java.io.*;
import jgfutil.*; 
import ibis.mpj.*;

public class JGFMolDynBench extends md implements JGFSection3 {

//   int size;

  public static int nprocess;
  public static int rank;

  public JGFMolDynBench(int nprocess, int rank) {
        this.nprocess=nprocess;
        this.rank=rank;
  }

  public void JGFsetsize(int size){
    this.size = size;
  }

  public void JGFinitialise(){

      initialise();

  }

  public void JGFapplication() throws MPJException{ 

    MPJ.COMM_WORLD.barrier();
    if(rank==0) {
      JGFInstrumentor.startTimer("Section3:MolDyn:Run");  
    }

    runiters();

    MPJ.COMM_WORLD.barrier();
    if(rank==0) {
      JGFInstrumentor.stopTimer("Section3:MolDyn:Run");  
    }
  } 


  public void JGFvalidate(){
  	//  commented out, because we don't know the ref_value of a problem size of 19
  	/*  double refval[] = {1731.4306625334357,7397.392307839352};
      double dev = Math.abs(ek - refval[size]);
      if (dev > 1.0e-12 ){
        System.out.println("Validation failed");
        System.out.println("Kinetic Energy = " + ek + "  " + dev + "  " + size);
      }*/
  }

  public void JGFtidyup(){    

    one = null;
    System.gc();
  }


  public void JGFrun(int size) throws MPJException{

    if(rank==0) {
      JGFInstrumentor.addTimer("Section3:MolDyn:Total", "Solutions",size);
      JGFInstrumentor.addTimer("Section3:MolDyn:Run", "Interactions",size);
    }

    JGFsetsize(size); 

    if(rank==0) {
      JGFInstrumentor.startTimer("Section3:MolDyn:Total");
    }

    JGFinitialise(); 
    JGFapplication(); 
    JGFvalidate(); 
    JGFtidyup(); 

    if(rank==0) {
      JGFInstrumentor.stopTimer("Section3:MolDyn:Total");
    
      JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Run", (double) interactions);
      JGFInstrumentor.addOpsToTimer("Section3:MolDyn:Total", 1);

      JGFInstrumentor.printTimer("Section3:MolDyn:Run"); 
      JGFInstrumentor.printTimer("Section3:MolDyn:Total"); 
    }

  }


}
 

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
*      adapted from SciMark 2.0, author Roldan Pozo (pozo@cam.nist.gov)   *
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

package sparsematmult;
import jgfutil.*;
import ibis.mpj.*;

public class SparseMatmult
{

  public static double ytotal = 0.0;

	/* 10 iterations used to make kernel have roughly
		same granulairty as other Scimark kernels. */

	public static void test( double y[], double val[], int row[],
				int col[], double x[], int NUM_ITERATIONS, int buf_row[], double p_y[]) throws MPJException
	{
        int nz = val.length;


        MPJ.COMM_WORLD.barrier();
        if(JGFSparseMatmultBench.rank==0){
          JGFInstrumentor.startTimer("Section2:SparseMatmult:Kernel"); 
        }

		for (int reps=0; reps<NUM_ITERATIONS; reps++)
		{
			for (int i=0; i<nz; i++)
			{
      			p_y[ row[i] ] += x[ col[i] ] * val[i];
			}
                // create updated copy on each process 
                MPJ.COMM_WORLD.allreduce(p_y,0,y,0,y.length,MPJ.DOUBLE,MPJ.SUM);
		}

        MPJ.COMM_WORLD.barrier();

        if(JGFSparseMatmultBench.rank==0){
          JGFInstrumentor.stopTimer("Section2:SparseMatmult:Kernel"); 
        } 

        if(JGFSparseMatmultBench.rank==0){
          for (int i=0; i<buf_row.length; i++) {
            ytotal += y[ buf_row[i] ];
          }
        }

	}
}

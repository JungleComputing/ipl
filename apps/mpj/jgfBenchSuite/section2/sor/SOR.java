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

package sor; 
import jgfutil.*; 
import ibis.mpj.*;

public class SOR
{

  public static double Gtotal = 0.0;

	public static final void SORrun(double omega, double p_G[][], int num_iterations, double G[][]) throws MPJException
	{

		int M = p_G.length;
		int N = p_G[0].length;
               
		double omega_over_four = omega * 0.25;
		double one_minus_omega = 1.0 - omega;
                
                int ilow,ihigh; 
                int rm_length;

		// update interior points
		//
		int Mm1 = M-1;
		int Nm1 = N-1;

                ilow = 0;
                ihigh = Mm1 + 1;
               
                MPJ.COMM_WORLD.barrier();
                if(JGFSORBench.rank==0){ 
                  JGFInstrumentor.startTimer("Section2:SOR:Kernel"); 
                }

		for (int p=0; p<2*num_iterations; p++) {
                 for (int i=ilow+(p%2); i<ihigh; i=i+2) {

                   if(i!=0){
		     double [] Gi = p_G[i];
		     double [] Gim1 = p_G[i-1];

                     if((i==1)&&JGFSORBench.rank==0) {

                     } else if((i==ihigh-1)&&JGFSORBench.rank==(JGFSORBench.nprocess-1)) {

                     } else if(((i==2)&&JGFSORBench.rank==0)||((i==1)&&(JGFSORBench.rank!=0))) {

		       double [] Gip1 = p_G[i+1];
                       for (int j=1; j<Nm1; j=j+2){
                         Gi[j] = omega_over_four * (Gim1[j] + Gip1[j] + Gi[j-1]
                                 + Gi[j+1]) + one_minus_omega * Gi[j];
                       }
                     } else if (((i==ihigh-2)&&JGFSORBench.rank==(JGFSORBench.nprocess-1))||
                               ((i==ihigh-1)&&(JGFSORBench.rank!=(JGFSORBench.nprocess-1)))){
                       double [] Gim2 = p_G[i-2];

                       for (int j=1; j<Nm1; j=j+2){
                         if((j+1) != Nm1) {
                           Gim1[j+1]=omega_over_four * (Gim2[j+1] + Gi[j+1] + Gim1[j]
                                     + Gim1[j+2]) + one_minus_omega * Gim1[j+1];
                         }
                       }

                     } else {
                       double [] Gip1 = p_G[i+1];
                       double [] Gim2 = p_G[i-2];

                       for (int j=1; j<Nm1; j=j+2){
                         Gi[j] = omega_over_four * (Gim1[j] + Gip1[j] + Gi[j-1]
                                 + Gi[j+1]) + one_minus_omega * Gi[j];

                         if((j+1) != Nm1) {
                           Gim1[j+1]=omega_over_four * (Gim2[j+1] + Gi[j+1] + Gim1[j]
                                     + Gim1[j+2]) + one_minus_omega * Gim1[j+1];
                         }
                       }
                     }

                 
                   }

		 }

/* Do the halo swaps */

                 if(JGFSORBench.rank!=JGFSORBench.nprocess-1){
                   MPJ.COMM_WORLD.sendrecv(p_G[p_G.length-2],0,p_G[p_G.length-2].length,MPJ.DOUBLE,
                   JGFSORBench.rank+1,1,
                   p_G[p_G.length-1],0, p_G[p_G.length-1].length,MPJ.DOUBLE,JGFSORBench.rank+1,2);
                 }
                 if(JGFSORBench.rank!=0){
                   MPJ.COMM_WORLD.sendrecv(p_G[1],0,p_G[1].length,MPJ.DOUBLE,JGFSORBench.rank-1,2,
                   p_G[0],0,p_G[0].length,MPJ.DOUBLE,JGFSORBench.rank-1,1);
                 }

		}

 

/* Send all data back to G */

                MPJ.COMM_WORLD.barrier();
                System.gc();
                if(JGFSORBench.rank==0) {

                  for(int i=1;i<p_G.length-1;i++){
                    for(int j=0;j<G[0].length;j++){
                      G[i-1][j] = p_G[i][j];
                    }
                  }

                  for(int k=1;k<JGFSORBench.nprocess;k++){
                    if(k==(JGFSORBench.nprocess-1)){
                     rm_length = JGFSORBench.rem_p_row;
                    } else {
                     rm_length = JGFSORBench.p_row;
                    }
                    MPJ.COMM_WORLD.recv(G,k*JGFSORBench.p_row,rm_length,MPJ.OBJECT,k,k);
                    System.gc();
                  }


                } else {

                 for(int k=1;k<JGFSORBench.nprocess;k++){
                  if(JGFSORBench.rank==k) {
                    MPJ.COMM_WORLD.ssend(p_G,1,JGFSORBench.p_row,MPJ.OBJECT,0,JGFSORBench.rank);
                  }
                 }
                }

                MPJ.COMM_WORLD.barrier();
                if(JGFSORBench.rank==0){ 
                  JGFInstrumentor.stopTimer("Section2:SOR:Kernel");
                }

/* Determine Gtotal on process 0 */

                if(JGFSORBench.rank==0){
                  for (int i=1; i<G.length-1; i++) {
                   for (int j=1; j<G[0].length-1; j++) {
                    Gtotal += G[i][j];
                   }
                  }  
                }               

 
	}
}
			

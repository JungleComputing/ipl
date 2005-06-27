/****************************************************************************

 Object version :
    Sang Lim(slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    11/28/98
****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class alltoallO {
  static public void test() throws MPJException {
    final int MAXLEN = 10;

    int i,j,k,l;
    int myself,tasks;
 
    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();

    int out[][] = new int[MAXLEN*tasks][MAXLEN]; 
    int in[][]  = new int[MAXLEN*tasks][];

    for(k=0;k<MAXLEN;k++)
      for(i=0;i<MAXLEN*tasks;i++)  
        out[i][k] = k;   
    
    MPJ.COMM_WORLD.alltoall(out,0,MAXLEN,MPJ.OBJECT,in,0,MAXLEN,MPJ.OBJECT);

    for(k=0;k<MAXLEN*tasks;k++) 
      for(l=0; l<MAXLEN; l++){
 	if(in[k][l] != l) {
	  System.out.println("bad answer in["+k+"]["+l+"] = "+
		  	       in[k][l]+" should be "+l);
          break; 
	}
      }

    MPJ.COMM_WORLD.barrier();
    if(myself==0)  System.out.println("AllToAllO TEST COMPLETE\n");
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  }
}



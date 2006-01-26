/* $Id$ */

/***************************************************************************
 Object version :
    Sang Lim(slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    11/1/98
****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class bcastO {
  static public void test() throws MPJException {
    final int MAXLEN = 100; 

    int root=0,i,j,k;
    int out[][] = new int[MAXLEN][MAXLEN];
    int myself,tasks;
    double time;

    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();

    if(myself == root){
      for (i = 0; i<MAXLEN; i++)    
        for (k = 0; k < MAXLEN ;k++)
          out[i][k] = k;
    }
    else{
      for (i = 0; i<MAXLEN; i++)    
        for (k = 0; k < MAXLEN ;k++)
          out[i][k] = k+1;
    }

    MPJ.COMM_WORLD.bcast(out,0,MAXLEN,MPJ.OBJECT,root);
      
    for(k=0;k<MAXLEN;k++) {
      for(i=0;i<MAXLEN; i++)
	if(out[i][k] != k) {  
	  System.out.println("bad answer out["+i+"]["+k+"] = "+
			       out[i][k]+" should be "+k);
	  break; 
	}
    }

    MPJ.COMM_WORLD.barrier();
    if(myself == 0)  System.out.println("BcastO TEST COMPLETE\n");
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
      
    MPJ.finish();
  }
}

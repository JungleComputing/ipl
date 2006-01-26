/* $Id$ */

/****************************************************************************

 Object version :
    Sang Lim(slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    11/30/98
****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class allgatherO {
  static public void test() throws MPJException {
    final int MAXLEN = 6;

    int root,i,j,k,l,m;
    int myself,tasks;
    
    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();
    
    int out[][] = new int[MAXLEN][3];
    int in[][]  = new int[MAXLEN*tasks][];

    for (j = 0; j < tasks; j++){
      if (j == myself)
        for (l=0;l<MAXLEN;l++)
          for (i = 0; i < 3; i++)
            out[l][i] = l+j*MAXLEN;
    }

    MPJ.COMM_WORLD.allgather(out, 0, MAXLEN, MPJ.OBJECT,
		             in,  0, MAXLEN, MPJ.OBJECT);

    for (j = 0; j < tasks; j++){
      if (j == myself){
	for (l=0;l<MAXLEN*tasks;l++)
          for (i = 0; i < 3; i++){
            if(in[l][i] != l)
              System.out.println("Recived data : "+in[l][i]+" at proc. "+j+"  in recive buffer["+l+"]["+i+"] should be : "+ l);
	  }
      }
      MPJ.COMM_WORLD.barrier(); 
    }
    MPJ.COMM_WORLD.barrier(); 
    if(myself == 0) System.out.println("AllgatherO TEST COMPLETE\n"); 
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  }
}


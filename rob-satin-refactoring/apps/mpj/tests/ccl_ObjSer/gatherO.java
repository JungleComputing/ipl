/* $Id$ */

/***************************************************************************

 Object version :
    Sang Lim(slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    11/15/98
****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;
 
class gatherO {
  static public void test() throws MPJException {
    
    int root=0,i,j,k,l;
    int myself,tasks;
 
    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();
 
    int out[][] = new int[6][3];
    int in[][]  = new int[6*tasks][];

    for (j = 0; j < tasks; j++){
      if (j == myself)
        for (l=0;l<6;l++)
          for (i = 0; i < 3; i++)
            out[l][i] = l+j*6;
    }

    
    MPJ.COMM_WORLD.gather(out,0,6,MPJ.OBJECT,in,0,6,MPJ.OBJECT,root);

    if (root == myself){
      for (l=0;l<6*tasks;l++)
	for (i = 0; i < 3; i++)
          if (in[l][i] != l)
            System.out.println("Recived data : "+in[l][i]+" at recive buffer["+l+"]["+i+"] should be : "+ l);
    }

    MPJ.COMM_WORLD.barrier();
    if(myself == root)  System.out.println("GatherO TEST COMPLETE\n");
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
    
    test();

    MPJ.finish();
  }
}

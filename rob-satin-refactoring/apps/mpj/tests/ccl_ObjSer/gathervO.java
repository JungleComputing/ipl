/* $Id$ */

/*
 MPI-Java version :
    Sang Lim (slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    12/1/98
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;
 
class gathervO {
  static public void test() throws MPJException {
    final int MAXLEN = 10;
 
    int root,i,j,k;
    int myself,tasks,stride=15;
 
    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();

    int out[][] = new int[MAXLEN][MAXLEN];
    int in[][]  = new int[MAXLEN*stride*tasks][MAXLEN];
    int ans[] = {1,2,3, 4, 5, 6, 7, 8, 9, 10, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,1, 2, 3, 4, 5, 0 ,0, 0, 0, 0,  0, 0, 0, 0, 0};

    int dis[] = new int[MAXLEN];
    int rcount[] = new int[MAXLEN];

    for (i = 0; i<MAXLEN*stride*tasks;i++)
      for (j = 0; j < MAXLEN; j++)
        in[i][j] = 0;

    for (i = 0; i<MAXLEN;i++)
      for (j = 0; j < MAXLEN; j++){
        dis[i] = i*stride;
        rcount[i] = 5;
        out[j][i] = j+1;
    }
    rcount[0] = 10;

    if (myself == 0)
       MPJ.COMM_WORLD.gatherv(out,0,10,MPJ.OBJECT,
                              in ,0,rcount,dis,MPJ.OBJECT,0);
    else 
       MPJ.COMM_WORLD.gatherv(out,0,5,MPJ.OBJECT,
                              in ,0,rcount,dis,MPJ.OBJECT,0);

    if(myself==0){
       for(j=0; j<MAXLEN;j++){
         for(i=0; i<tasks*stride; i++)
           if (ans[i]!=in[i][j])
             System.out.println("recived data : "+in[i][j]+"at ["+i+"]["+j+"] should be : "+ans[i]);
       }
    }
    
    MPJ.COMM_WORLD.barrier();
    if(myself == 0)  System.out.println("GathervO TEST COMPLETE\n");
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
        
    MPJ.finish();
  }
}

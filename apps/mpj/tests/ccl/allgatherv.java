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
 
class allgatherv {
  static public void test() throws MPJException {
    final int MAXLEN = 10;
 
    int root,i,j,k;
    int myself,tasks,stride=15;
 
    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();

    int out[] = new int[MAXLEN];
    int in[]  = new int[MAXLEN*stride*tasks];
    int dis[] = new int[MAXLEN];
    int rcount[] = new int[MAXLEN];
    int ans[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    for (i = 0; i<MAXLEN;i++){
       dis[i] = i*stride;
       rcount[i] = 5;
       out[i] = i;
    }
    rcount[0] = 10;

    for (i = 0; i<MAXLEN*tasks*stride;i++){
       in[i] = 0;
    }

    if (myself == 0)
       MPJ.COMM_WORLD.allgatherv(out,0,10,MPJ.INT,
                              in ,0,rcount,dis,MPJ.INT);
    else 
       MPJ.COMM_WORLD.allgatherv(out,0,5,MPJ.INT,
                              in ,0,rcount,dis,MPJ.INT);
    
    for(i=0; i<tasks*stride; i++)
      if (ans[i]!=in[i])
        System.out.println("recived data : "+in[i]+"at ["+i+"] should be : "+ans[i]+" on proc. : "+myself);

      MPJ.COMM_WORLD.barrier();

    if(myself == 0)  System.out.println("Allgatherv TEST COMPLETE\n");
  
  }  

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
   
    test();  
      
    MPJ.finish();
  }
}

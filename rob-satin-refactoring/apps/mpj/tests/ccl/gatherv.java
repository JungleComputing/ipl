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
 
class gatherv {
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
    int ans[] = {1, 1, 1, 1, 1, 1, 1,1, 1, 1, 0,0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    for (i = 0; i<MAXLEN;i++){
       dis[i] = i*stride;
       rcount[i] = 5;
       out[i] = 1;
    }
    rcount[0] = 10;

    for (i = 0; i<MAXLEN*tasks*stride;i++){
       in[i] = 0;
    }
    
    if (myself == 0)
       MPJ.COMM_WORLD.gatherv(out,0,10,MPJ.INT,
                              in ,0,rcount,dis,MPJ.INT,0);
    else 
       MPJ.COMM_WORLD.gatherv(out,0,5,MPJ.INT,
                              in ,0,rcount,dis,MPJ.INT,0);

    if(myself==0){
       for(i=0; i<tasks*stride; i++)
         if (ans[i]!=in[i])
             System.out.println("recived data : "+in[i]+"at ["+i+"] should be : "+ans[i]);
    }
    
    MPJ.COMM_WORLD.barrier();
    if(myself == 0)  System.out.println("Gatherv TEST COMPLETE\n");
  
  }
  
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
    
    test();
    
    MPJ.finish();
    
  }
}

/****************************************************************************

 MESSAGE PASSING INTERFACE TEST CASE SUITE

 Copyright IBM Corp. 1995

 IBM Corp. hereby grants a non-exclusive license to use, copy, modify, and
 distribute this software for any purpose and without fee provided that the
 above copyright notice and the following paragraphs appear in all copies.

 IBM Corp. makes no representation that the test cases comprising this
 suite are correct or are an accurate representation of any standard.

 In no event shall IBM be liable to any party for direct, indirect, special
 incidental, or consequential damage arising out of the use of this software
 even if IBM Corp. has been advised of the possibility of such damage.

 IBM CORP. SPECIFICALLY DISCLAIMS ANY WARRANTIES INCLUDING, BUT NOT LIMITED
 TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS" BASIS AND IBM
 CORP. HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 ENHANCEMENTS, OR MODIFICATIONS.

****************************************************************************

 These test cases reflect an interpretation of the MPI Standard.  They are
 are, in most cases, unit tests of specific MPI behaviors.  If a user of any
 test case from this set believes that the MPI Standard requires behavior
 different than that implied by the test case we would appreciate feedback.

 Comments may be sent to:
    Richard Treumann
    treumann@kgn.ibm.com

****************************************************************************

 MPI-Java version :
    Sung-Hoon Ko(shko@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    03/22/98

****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;


class graph {
  static public void test() throws MPJException {
    int index[] = {2,3,4,6};
    int edges[]={1,3,0,3,0,2};
    int me,tasks;
    boolean reorder;
    int i,num,start,type,nnodes,nedges;
    int neighbors[] = new int[4];

 
    me = MPJ.COMM_WORLD.rank();
    tasks =MPJ.COMM_WORLD.size(); 

    if(tasks != 4) { 
      System.out.println("MUST RUN WITH 4 TASKS"); 
      System.exit(0); 
    }

    reorder = false;
    Graphcomm comm = MPJ.COMM_WORLD.createGraph(index,edges,reorder);


    type = comm.topoTest();
    if(type != MPJ.GRAPH)  
      System.out.println
	("ERROR in MPJ_Topo_test, type = "+type+
	 ", should be "+MPJ.GRAPH+" (MPJ.GRAPH)");
    

    index = comm.get().index;
    nnodes = comm.get().index.length;
    nedges = comm.get().edges.length;
    if(nnodes != 4 || nedges != 6)
      System.out.println
        ("ERROR in MPJ_Graphdims_get, nnodes= "+nnodes+
         ", nedges = "+nedges+", should be 4, 6");
 
 
    index = comm.get().index;
    edges = comm.get().edges;
    if(index[0] != 2 || index[1] != 3 || index[2] != 4 || index[3] != 6)
      System.out.println
        ("ERROR in MPJ_Graph_get: index = "+index[0]+", "+index[1]+
         ", "+index[2]+", "+index[3]+", should be 2,3,4,6");
    if(edges[0] != 1 || edges[1] != 3 || edges[2] != 0 || edges[3] != 3 
       || edges[4] != 0 || edges[5] != 2) 
      System.out.println
        ("ERROR in MPJ_Graph_get: edges = "+edges[0]+", "+edges[1]+
         ", "+edges[2]+", "+edges[3]+", "+edges[4]+", "+edges[5]+
         ", should be 1,3,0,3,0,2");

 

    nnodes = comm.neighbours(me).length;
    int k = (me==0 || me==3) ? 1 : 0;
    if(nnodes != 1 + k)
      System.out.println
        ("ERROR in MPJ_Graph_neighbors_count: count = "+
         nnodes+", should be 1+(me==0|me==3)");
 
    

    neighbors = comm.neighbours(me);
    start = (me==0) ? 0 : index[me-1];
    num = (me==0) ? index[0] : index[me] - start;
    for(i=0;i<num;i++)  {
      if(neighbors[i] != edges[start+i])
        System.out.println
          ("ERROR in MPJ_Graph_neighbors: wrong neighbor on task "+
           me+" index "+i+", neighbor = "+neighbors[i]+
           ", should be "+edges[start+i]);
    }
    
 
    Graphcomm comm2 = MPJ.COMM_WORLD.createGraph(index,edges,reorder);
 

    comm.barrier();    
    if(me==0) System.out.println("Graph TEST COMPLETE\n");
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();

  }
}
  
  

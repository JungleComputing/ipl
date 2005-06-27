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

class cart {
  static public void test() throws MPJException {

    final int MAXDIMS = 10;    
    int tasks,me,type,ndims;

    
    int rank,src,dest,rc;
    int      cnt=0, i;


  
    Comm comms[] = new Comm[20];

    Group gid = MPJ.COMM_WORLD.group();

    tasks = gid.size();
    if(tasks != 6)  { 
      System.out.println("MUST RUN WITH 6 TASKS"); 
      System.exit(0); 
    }

 
/* test non-periodic topology */
    int dims2[] = new int[2];
 
    dims2[0] = 0;  dims2[1] = 0;
    Cartcomm.dimsCreate(tasks,dims2);
    if(dims2[0] != 3 || dims2[1] != 2)
      System.out.println
	("ERROR in MPJ_Dims_create, dims = "+dims2[0]+","+dims2[1]+
	 ", should be 3, 2");

    boolean periods2[] = new boolean[2];
    periods2[0] = false;
    periods2[1] = false;
    Cartcomm comm = MPJ.COMM_WORLD.createCart(dims2,periods2,false);
    comms[cnt++] = comm;
    me = comm.rank();
 
    type = comm.topoTest();
    if(type != MPJ.CART)  
      System.out.println("ERROR in MPJ_Topo_test, type = "+
			 type+", should be "+MPJ.CART);
 
    ndims = comm.get().dims.length;
      if(ndims != 2) 
	System.out.println("ERROR in MPJ_Cartdim_get, ndims = "+
			   ndims+", should be 2");
 
    int coords2[]  = new int[2];
    dims2    = comm.get().dims;
    periods2 = comm.get().periods;
    coords2  = comm.get().coords;

    if(dims2[0] != 3 ||dims2[1] != 2)
      System.out.println
	("ERROR in MPJ_Cart_get, dims = "+dims2[0]+","+dims2[1]+
	 ", should be 3, 2");
    if(periods2[0] != false || periods2[1] != false) 
      System.out.println("WRONG PERIODS!");
    if(coords2[0] != me/2 || coords2[1] != me%2) { 
      System.out.println
	("ERROR in MPJ_Cart_get, coords = "+coords2[0]+","+
	 coords2[1]+", should be "+(me/2)+","+(me%2));
      System.exit(0); 
    }
    

    rank = comm.rank(coords2);
    if(rank != me)  
      System.out.println("ERROR in MPJ_Cart_rank, rank = "+
			 rank+", should be "+me);
    

    coords2 = comm.coords(rank);
    if(coords2[0] != me/2 || coords2[1] != me%2) {
      System.out.println
	("ERROR in MPJ_Cart_coords, coords = "+coords2[0]+","+
	 coords2[1]+", should be "+(me/2)+", "+(me%2));
      System.exit(0); 
    }
 

    src = comm.shift(0, 5).rankSource;
    dest = comm.shift(0, 5).rankDest;

    if(src != MPJ.PROC_NULL || dest != MPJ.PROC_NULL)
      System.out.println
	("ERROR in MPJ_Cart_shift, src/dest = "+src+","+dest+
	 ", should be "+MPJ.PROC_NULL+", "+MPJ.PROC_NULL);
 

    src = comm.shift(0, 1).rankSource;
    dest = comm.shift(0, 1).rankDest;
    
    if(me/2<2 && dest != me+2)
      System.out.println
	("ERROR in MPJ_Cart_shift, dest = "+dest+
	 ", should be "+(me+2));
 
    if(me/2>0 && src != me-2)
      System.out.println
	("ERROR in MPJ_Cart_shift, src = "+src+
	 ", should be "+(me-2));
 

    src = comm.shift(1, -1).rankSource;
    dest = comm.shift(1, -1).rankDest;
     
    if((me%2==1) && (dest != me-1))
      System.out.println
	("ERROR in MPJ_Cart_shift, dest = "+dest+
	 ", should be "+(me-1));
    if(me%2==1 && src != MPJ.PROC_NULL)
      System.out.println
	("ERROR in MPJ_Cart_shift, src = "+src+
	 ", should be "+MPJ.PROC_NULL);
    if(me%2==0 && src != me+1)
      System.out.println
	("ERROR in MPJ_Cart_shift, src = "+src+
	 ", should be "+(me+1));
    if(me%2==0 && dest != MPJ.PROC_NULL)
      System.out.println
	("ERROR in MPJ_Cart_shift, dest = "+dest+
	 ", should be "+MPJ.PROC_NULL);

/* test periodic topology */
 
    dims2[0] = 2;  dims2[1] = 0;
    Cartcomm.dimsCreate(tasks,dims2);
    if(dims2[0] != 2 || dims2[1] != 3)
      System.out.println
	("ERROR in MPJ_Dims_create, dims = "+dims2[0]+","+dims2[1]+
	 ", should be 2, 3");

    
    periods2[0] = true;
    periods2[1] = true;
    comm = MPJ.COMM_WORLD.createCart(dims2,periods2,false);
    comms[cnt++] = comm;
    me = comm.rank();
    coords2[0] = me/3; coords2[1] = me%3;
    rank = comm.rank(coords2);
    if(rank != me)  
      System.out.println
	("ERROR in MPJ_Cart_rank, rank = "+rank+", should be "+me);
    

    coords2 = comm.coords(rank);
    if(coords2[0] != me/3 || coords2[1] != me%3)
      System.out.println
        ("ERROR in MPJ_Cart_coords, coords = "+coords2[0]+","+
         coords2[1]+", should be "+(me/3)+","+(me%3));
 
    
    src = comm.shift(0, 5).rankSource;
    dest = comm.shift(0, 5).rankDest;     
    if(src != (me+3)%6 || dest != (me+3)%6)
      System.out.println
        ("ERROR in MPJ_Cart_shift, src/dest = "+src+", "+dest+
         ", should be "+(me+3)+", "+(me+3));

    
    src = comm.shift(1, -1).rankSource;
    dest = comm.shift(1, -1).rankDest;         
    int k = (me%3==0) ? 1:0;
      if(dest != (me-1)+3*k)
      System.out.println
        ("ERROR in MPJ_Cart_shift, dest = "+dest+
         ", should be "+((me-1+3)%3));

      k = (me%3==2) ? 1:0;
      if(src != (me+1)-3*k)
      System.out.println
        ("ERROR in MPJ_Cart_shift, src = "+src+
         ", should be "+((me+1+3)%3));

    
    dims2[0] = 1; 
    comm = MPJ.COMM_WORLD.createCart(dims2,periods2,false);
    comms[cnt++] = comm;
    

    MPJ.COMM_WORLD.barrier();    
    if(me==0) System.out.println("Cart TEST COMPLETE\n");
    
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();

  }
}

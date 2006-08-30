/* $Id$ */

/****************************************************************************

 MPI-Java version :
    Sung-Hoon Ko(shko@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    03/22/98

****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;
 
 
class map {
  static public void test() throws MPJException {
    final int NUM_DIMS = 2;
    int       rank, size, i;
    int       errors=0;
    int        dims[] = new int[NUM_DIMS];
    boolean periods[] = new boolean[NUM_DIMS];
    int       new_rank;


    rank = MPJ.COMM_WORLD.rank();
    size = MPJ.COMM_WORLD.size(); 

    /* Clear dims array and get dims for topology */
    for(i=0;i<NUM_DIMS;i++) { dims[i] = 0; periods[i] = false; }
    Cartcomm.dimsCreate(size,dims);

    Cartcomm intcomm = MPJ.COMM_WORLD.createCart(dims,periods,false);
    /* Look at what rankings a cartesian topology MIGHT have */
    new_rank = intcomm.map(dims, periods);

    /* Check that all new ranks are used exactly once */
    int rbuf[] = new int[size];
    int sbuf[] = new int[size];
    

    for (i=0; i<size; i++) 
      sbuf[i] = 0;
    sbuf[new_rank] = 1;
    MPJ.COMM_WORLD.reduce(sbuf,0,rbuf,0,size,MPJ.INT,MPJ.SUM,0);
    if (rank == 0) {
      for (i=0; i<size; i++) {
	if (rbuf[i] != 1) {
	  errors++;
	  System.out.println("Rank "+i+" used "+rbuf[i]+" times");
	}
      }
      if (errors == 0) 
	System.out.println( "Map test passed\n" );
    }
    
  }
  
  static public void main(String[] args) throws MPJException {


    MPJ.init(args);

    test();
        
    MPJ.finish();
  }
}


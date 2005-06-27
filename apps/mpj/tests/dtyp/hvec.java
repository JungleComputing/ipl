// This test is from MPICH test suite.

/* The original version of this was sent by  
   empierce@tribble.llnl.gov (Elsie M. Pierce) 
   I've modified it to fit the automated tests requirements
 */
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

class hvec {
  static public void test() throws MPJException {
    int my_rank;
    
    my_rank = MPJ.COMM_WORLD.rank();
     

    Datatype messtyp, messtyp2;
    int root=0;
    int i, istat, big_offset;
    final int DL = 32;
      

    int dar[] = new int[DL];
      
    for (i=0; i<DL; i++)
      dar[i] = my_rank;

    int count	= 2;
    int bllen	= 3;
    int gap	= 1;
    int str	= bllen + gap;

    messtyp = MPJ.INT.vector(count, bllen, str);
    messtyp.commit();
    MPJ.COMM_WORLD.bcast(dar, 0, 1, messtyp, root);
    if (my_rank==1) {
      System.out.println("  0 = Sent, 1 = Not Sent");
      System.out.println("  Vector Type with Gap : ");
    }
    if (my_rank == 1) {
      for (i=0; i<DL; i++) 
	System.out.print(dar[i]+" ");
      System.out.println();
      System.out.println();
    }



    for (big_offset = -1; big_offset <= 2 ; big_offset++) {
      if (my_rank==1)
	System.out.println
	  (" Three of above vector types combined, with offset = " +
	   big_offset + " ints");

      for (i=0; i<DL; i++)
	dar[i] = my_rank;

      count = 3;
      int ext = messtyp.extent();
      
      messtyp2 = messtyp.hvector(count, 1, ext + big_offset);
      messtyp2.commit();
      MPJ.COMM_WORLD.bcast(dar, 0, 1, messtyp2, root);

      MPJ.COMM_WORLD.barrier();

      if (my_rank == 1) {
	for (i=0; i<DL; i++) 
	  System.out.print(dar[i]+" ");
	System.out.println();
	System.out.println();
      }
    }

  }
  
  static public void main(String[] args) throws MPJException {   
    MPJ.init(args);   

    test();
    
    MPJ.finish();
  }
}



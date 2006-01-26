/* $Id$ */

// a test program from MPICH test suite.
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

class pack {
  static public void test() throws MPJException {
  /*
      Check pack/unpack of mixed datatypes.
*/

    final int BUF_SIZE = 100;

    int myrank;
    byte buffer[] = new byte[BUF_SIZE];
    int n[]    = new int[1];
    int size[] = new int[1];
    int src, dest, errcnt, errs;
    double a[] = new double[1];
    double b[] = new double[1];
    int pos;
    int apos[] = new int[1];


    Status status;


    myrank = MPJ.COMM_WORLD.rank();

    src	   = 0;
    dest   = 1;
    
    errcnt = 0;
    if (myrank == src) {
      pos  = 0;
      n[0] = 10;
      a[0] = 1.1;
      b[0] = 2.2;
      pos = MPJ.COMM_WORLD.pack(n,0,1,MPJ.INT,buffer,pos);

      pos = MPJ.COMM_WORLD.pack(a,0,1,MPJ.DOUBLE,buffer,pos);

      pos = MPJ.COMM_WORLD.pack(b,0,1,MPJ.DOUBLE,buffer,pos);

      apos[0] = pos;
      MPJ.COMM_WORLD.send(apos, 0, 1, MPJ.INT, dest, 999);

      MPJ.COMM_WORLD.send(buffer, 0, pos, MPJ.PACKED, dest, 99);
    }
    else if (myrank == dest){
      status = MPJ.COMM_WORLD.recv(size, 0, 1, MPJ.INT, src, 999);

      status = MPJ.COMM_WORLD.recv(buffer,0,size[0],MPJ.PACKED,src,99);

      pos = 0;
      
      pos = MPJ.COMM_WORLD.unpack(buffer,pos,n,0,1,MPJ.INT);
 
      pos = MPJ.COMM_WORLD.unpack(buffer,pos,a,0,1,MPJ.DOUBLE);

      pos = MPJ.COMM_WORLD.unpack(buffer,pos,b,0,1,MPJ.DOUBLE);
      
      /* Check results */
      if (n[0] != 10) { 
	errcnt++;
	System.out.println
	  ("Wrong value for n; got "+n[0]+" expected 10");
      }
      if (a[0] != 1.1) { 
	errcnt++;
	System.out.println
	  ("Wrong value for a; got "+a[0]+" expected 1.1");
      }
      if (b[0] != 2.2) { 
	errcnt++;
	System.out.println
	  ("Wrong value for b; got "+b[0]+" expected 2.2");
      }
    }

    if(myrank == 0)  System.out.println("Pack TEST COMPLETE\n");    

  }
  
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
      
    MPJ.finish();
  }
}

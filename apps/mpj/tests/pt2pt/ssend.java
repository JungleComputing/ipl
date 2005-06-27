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

class ssend {
  static public void test() throws MPJException {

    char buf[] = new char[10];
    int len,tasks,me,i;
    Status status;
    double time, timeoffset;
    double timeBuf[] = new double[1]; 
    double timeoffsetBuf[] = new double[1];

    /* This test makes assumptions about the global nature of MPI_WTIME that
      are not required by MPI, and may falsely signal an error */

    len = buf.length;

    me = MPJ.COMM_WORLD.rank();
    MPJ.COMM_WORLD.barrier();
    
    if(me==0) {
      /* First, roughly synchronize the clocks */
      MPJ.COMM_WORLD.recv(timeoffsetBuf,0,1,MPJ.DOUBLE,1,1);
      timeoffset = timeoffsetBuf[0];
      timeoffset = MPJ.wtime() - timeoffset;

      MPJ.COMM_WORLD.ssend(buf,0,len,MPJ.CHAR,1,1);

      time = MPJ.wtime() - timeoffset;
      timeBuf[0] = time;
      MPJ.COMM_WORLD.ssend(timeBuf,0,1,MPJ.DOUBLE,1,2);
    } else if(me == 1) {
      time = MPJ.wtime();
      timeBuf[0] = time;

      MPJ.COMM_WORLD.ssend(timeBuf,0,1,MPJ.DOUBLE,0,1);

      for(i=0;i<3000000;i++) ;

      MPJ.COMM_WORLD.recv(buf,0,len,MPJ.CHAR,0,1);
      MPJ.COMM_WORLD.recv(timeBuf,0,1,MPJ.DOUBLE,0,2);
      time = timeBuf[0];
      time = time - MPJ.wtime();
      if(time < 0) time = -time;
      if(time > .1) 
	System.out.println("ERROR: MPJ_Ssend did not synchronize"); 

	  // Don't understand exactly what this is *meant* to do, but on
          // general principles it seems dubious one could make an effective
          // test of the MPI spec this way...  DBC.
    }

    MPJ.COMM_WORLD.barrier();

    if(me == 0)  System.out.println("Ssend TEST COMPLETE\n");  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  }
}




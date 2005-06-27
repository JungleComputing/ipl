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

 Object version :
    Sang Lim(slim@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    08/10/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class ssendO {
  static public void main(String[] args) throws MPJException {
    
    test a[] = new test[10];
    test b[] = new test[10];

    int len,tasks,me,i;
    Status status;
    double time, timeoffset;
    double timeBuf[] = new double[1]; 
    double timeoffsetBuf[] = new double[1];

    /* This test makes assumptions about the global nature of MPI_WTIME that
      are not required by MPI, and may falsely signal an error */

    len = a.length;

    MPJ.init(args);
    me = MPJ.COMM_WORLD.rank();
    MPJ.COMM_WORLD.barrier();
    
    for(i = 0; i < 10;i++){
       a[i]   = new test();
       b[i]   = new test();
       a[i].a = i;
       b[i].a = 0;
    }
    if(me==0) {
      /* First, roughly synchronize the clocks */
      MPJ.COMM_WORLD.recv(timeoffsetBuf,0,1,MPJ.DOUBLE,1,1);
      timeoffset = timeoffsetBuf[0];
      timeoffset = MPJ.wtime() - timeoffset;

      MPJ.COMM_WORLD.ssend(a,0,len,MPJ.OBJECT,1,1);

      time = MPJ.wtime() - timeoffset;
      timeBuf[0] = time;

    } else if(me == 1) {
      time = MPJ.wtime();
      timeBuf[0] = time;

      MPJ.COMM_WORLD.ssend(timeBuf,0,1,MPJ.DOUBLE,0,1);

      for(i=0;i<3000000;i++) ;

      MPJ.COMM_WORLD.recv(b,0,len,MPJ.OBJECT,0,1);
   
      time = timeBuf[0];
      time = time - MPJ.wtime();
      if(time < 0) time = -time;
    }

    MPJ.COMM_WORLD.barrier();

    if(me == 0)  System.out.println("SsendO TEST COMPLETE\n");
    MPJ.finish();
  }
}




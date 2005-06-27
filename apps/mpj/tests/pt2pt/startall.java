/*
****************************************************************************

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
 
class startall {

  static int me,tasks,i,bytes;
  static int mebuf[] = new int[1];
  static int data[];
  static byte buf[];
 

  static Prequest req[];
  static Status stats[];
 

  static void wstart() throws MPJException {
    for(i=0;i<tasks;i++)  data[i] = -1;
    
    Prequest.startAll(req);

    stats = Request.waitAll(req);
    
    for(i=0;i<tasks;i++)
      if(data[i] != i)
        System.out.println
          ("ERROR in Startall: data is "+data[i]+", should be "+i);
    for(i=1;i<2*tasks;i+=2) {
      bytes = stats[i].getCount(MPJ.BYTE);
      if(bytes != 1)
        System.out.println
          ("ERROR in Waitall: status.getCount() = "+bytes+", should be 1");
    }
  }

  ////////////////////////////////////////////////////////////////////

  static public void test() throws MPJException {
    me = MPJ.COMM_WORLD.rank();
    tasks =MPJ.COMM_WORLD.size(); 
 
    data = new int[tasks];
    int intsize = 4;
    
    // we do not have bsend_overhead in MPJ :-)
    //buf = new byte[tasks * (intsize+MPI.BSEND_OVERHEAD)];
    buf = new byte[tasks * (intsize)];
    
    req = new Prequest[2*tasks];
    stats = new Status[2*tasks];

    MPJ.bufferAttach(buf);
  
    mebuf[0] = me;
    for(i=0;i<tasks;i++)  {
      req[2*i] = MPJ.COMM_WORLD.sendInit(mebuf,0,1,MPJ.INT,i,1);     
      req[2*i+1] = MPJ.COMM_WORLD.recvInit(data,i,1,MPJ.INT,i,1);
    }
    wstart();


    for(i=0;i<tasks;i++)  {
      req[2*i] = MPJ.COMM_WORLD.sendInit(mebuf,0,1,MPJ.INT,i,1);      
      req[2*i+1] = MPJ.COMM_WORLD.recvInit(data,i,1,MPJ.INT,i,1);
    }
    wstart();


    for(i=0;i<tasks;i++)  {
      req[2*i] = MPJ.COMM_WORLD.bsendInit(mebuf,0,1,MPJ.INT,i,1);
      req[2*i+1] = MPJ.COMM_WORLD.recvInit(data,i,1,MPJ.INT,i,1);
    }
    wstart();
 

    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("StartAll TEST COMPLETE\n");
  
  }

  static public void main(String[] args) throws MPJException {

    MPJ.init(args);
    
    test();
    
    MPJ.finish();
  }
}

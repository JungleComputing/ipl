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
    09/5/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;
 

class waitanyO {
  static public void main(String[] args) throws MPJException {

    int me,tasks,i,index;

    test a[] = new test[1];
    test b[] = new test[10];

    MPJ.init(args);
    me = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size(); 
 
    int data[] = new int[tasks];
    Request req[] = new Request[tasks];
    Status status;
  
    for (i = 0; i < 10;i++){
       b[i] = new test();
       b[i].a = -1;
    }

    a[0] = new test();
    a[0].a = me;

    if(me == 1) 
      MPJ.COMM_WORLD.send(a,0,1,MPJ.OBJECT,0,1);
    else {
      req[0] = null;//MPI.REQUEST_NULL;
      for(i=1;i<tasks;i++)  
	req[i] = MPJ.COMM_WORLD.irecv(b,i,1,MPJ.OBJECT,i,1);
	
      for(i=1;i<tasks;i++)  {
	status = Request.waitAny(req);

	if(!req[status.getIndex()].isVoid())
	  System.out.println
	    ("ERROR(3) in MPJ_Waitany: reqest not set to NULL");
      }
    }

    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("WaitanyO TEST COMPLETE\n");
    MPJ.finish();
  }
}



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
    09/17/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;
 
class waitsomeO {
  static public void main(String[] args) throws MPJException {

    int me,tasks,i,done,outcount;
    boolean flag;
    test mebuf[] = new test[1];
    mebuf[0] = new test();
    mebuf[0].a = 444;

    MPJ.init(args);
    me = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size(); 
 
    test data[]  = new test[tasks];
    for (int j = 0; j<tasks;j++){
        data[j] = new test();
        data[j].a = -1;
    }
    Request req[]   = new Request[tasks];  
    Status status[] = new Status[tasks];  
  
    if(me == 1) 
      MPJ.COMM_WORLD.send(mebuf,0,1,MPJ.OBJECT,0,1);
    else {
      req[0] = null;//MPI.REQUEST_NULL;
      for(i=1;i<tasks;i++)  
	req[i] = MPJ.COMM_WORLD.irecv(data,i,1,MPJ.OBJECT,i,1);

      done = 0; 
      while(done < tasks-1)  {
	status = Request.waitSome(req);
	outcount = status.length;

	if(outcount == 0)
	  System.out.println("ERROR(2) in Waitsome: outcount = 0");
	for(i=0;i<outcount;i++)  {
	  done++;

	  if(!req[status[i].getIndex()].isVoid())
	    System.out.println
	      (i+", "+outcount+", "+status[i].getIndex()+", "+req[status[i].getIndex()]+
	       " ERROR(4) in MPJ_Waitsome: reqest not set to NULL");
	}
      }
    }

    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("WaitsomeO TEST COMPLETE\n");
    MPJ.finish();
  }
}

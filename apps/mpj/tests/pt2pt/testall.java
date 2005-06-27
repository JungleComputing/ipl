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
 

class testall {
  static public void test() throws MPJException {
    int me,tasks,i;
    int mebuf[] = new int[1];
    boolean flag;


    me = MPJ.COMM_WORLD.rank();
    tasks =MPJ.COMM_WORLD.size(); 

    int data[] = new int[tasks];
    Request   req[] = new Request[tasks];
    Status status[] = new Status[tasks]; 

    mebuf[0] = me;
    if(me > 0) 
      MPJ.COMM_WORLD.send(mebuf,0,1,MPJ.INT,0,1);
    else {
      //req[0] = MPJ.REQUEST_NULL;  
      for(i=1;i<tasks;i++)  
	req[i] = MPJ.COMM_WORLD.irecv(data,i,1,MPJ.INT,i,1);


      flag = false; 
      while(flag == false)  {
	for(i=1;i<tasks;i++)  {
	  if(req[i].isVoid())
	    System.out.println("ERROR(2) in MPJ_Testall: incorrect status");
	}
	status = Request.testAll(req);
	if(status == null) flag = false;
	else               flag = true;
      }


      for(i=1;i<tasks;i++)  {
	if(!req[i].isVoid())
	  System.out.println("ERROR(3) in Testall: request not set to NULL");
	if(status[i].getSource() != i)
	  System.out.println
	    ("ERROR(4) in Testall: request prematurely set to NULL");
	if(data[i] != i)
	  System.out.println("ERROR(5) in MPJ_Testall: incorrect data");
      }
      

      status = Request.testAll(req);
      if(status == null)
	System.out.println
	  ("ERROR(6) in MPJ_Testall: status(flag) is not set");


    }


    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("Testall TEST COMPLETE\n");
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
        
    MPJ.finish();
  }
}

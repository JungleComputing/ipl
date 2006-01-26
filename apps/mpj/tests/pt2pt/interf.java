/* $Id$ */

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
 
class interf {
  static public void test() throws MPJException {
 
    int me,tasks;
    int val1[] = new int[1];
    int val2[] = new int[1];

    Status status;
    Request request1,request2;



    me = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size(); 

    Intracomm my_comm = (Intracomm) MPJ.COMM_WORLD.clone();

    if(me==0)  {
      val1[0] = 1;
      MPJ.COMM_WORLD.send(val1,0,1,MPJ.INT,1,1);

      val2[0] = 2;
      my_comm.send(val2,0,1,MPJ.INT,1,1);
    } 
    else if(me == 1)  {      
      request1 = my_comm.irecv(val1,0,1,MPJ.INT,0,1);
      //my_comm.recv(val1,0,1,MPJ.INT,0,1);
      request2 = MPJ.COMM_WORLD.irecv(val2,0,1,MPJ.INT,0,1);
      // MPJ.COMM_WORLD.recv(val2,0,1,MPJ.INT,0,1);
     
      status = request1.Wait();

      status = request2.Wait();

      if(val1[0] != 2 || val2[0] != 1) { 
	System.out.println
	  ("ERROR, messages were exchanged between different communicators--");
	System.out.println("val1[0]="+val1[0]+", val2[0]="+val2[0]); 
      }
    }

    my_comm.barrier();
    if(me == 0)  System.out.println("Interf TEST COMPLETE.\n"); 
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);    
    
    test();
     
    MPJ.finish();

  }
}



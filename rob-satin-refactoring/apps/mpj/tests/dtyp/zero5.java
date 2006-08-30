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
    09/10/99

****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class zero5 {
  static public void test() throws MPJException {
      final int MSZ = 10; 
    int myself,tasks;
 
    int i;
    int ii[] = new int[MSZ];
    int check[] = new int[MSZ];
    int numtasks, me;
    int len;
    int error=0;
    int count1,count2,count3;
    int aob[] = new int[3];
    int aod[] = new int[3];



    Datatype newtype;
    Datatype aot[] = new Datatype[3];
    Status status;


    myself = MPJ.COMM_WORLD.rank();

    numtasks = MPJ.COMM_WORLD.size();
    me = MPJ.COMM_WORLD.rank();
	
    if ((numtasks < 2)) {
      System.out.println("this testcase requires 2 tasks.");
      MPJ.COMM_WORLD.abort(me);
    }
	
    if ((numtasks > 2) && (me > 1)) { 
      System.out.println("Testcase uses two tasks, extraneous task #"+me+" exited.");
      MPJ.finish();
      System.exit(0);
    }


    for (i=0;i<MSZ;i++) {
      check[i] = i;
    }
    check[1] = -1;
    check[5] = -1;
    check[8] = -1;
    check[9] = -1;
    aot[0] = MPJ.INT;   aob[0] = 0; aod[0] = 2; 
    aot[1] = MPJ.INT;   aob[1] = 1; aod[1] = 0;
    aot[2] = MPJ.INT;   aob[2] = 2; aod[2] = 2;
  
    newtype = Datatype.struct(aob,aod,aot);
    newtype.commit();
	
    if(myself == 0)  {
      for (i=0;i<MSZ;i++) {
	ii[i] = i;
      }
      MPJ.COMM_WORLD.send(ii,0,2,newtype,1,0);
    } else if(myself == 1) {
      for (i=0;i<MSZ; i++) {
	ii[i] = -1;
      }
      status = MPJ.COMM_WORLD.recv(ii,0,2,newtype,0,0);
      for (i=0;i<MSZ;i++) {
	if (ii[i] != check[i]) error++;
      }
      if (error > 0) {
	System.out.println("FAILURE: Results below.");
	for (i=0;i<MSZ;i++) {
	  System.out.println("ii["+i+"]="+ii[i]);
	}
      } else {
	System.out.println("SUCCESS with sent message.");
      }

      count1 = status.getCount(newtype);
      count2 = status.getElements(newtype);
      if ( (count1==2) && (count2==6) ) 
	System.out.println("Success with Get_count & Get_elements.");
      else
	System.out.println("Should be 2, 6 but is "+count1+", "+count2);

    }

  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();

    MPJ.finish();
  }
}

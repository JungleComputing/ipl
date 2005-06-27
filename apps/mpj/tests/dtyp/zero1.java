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
 
class zero1 {
  static public void test() throws MPJException {
    int myself,tasks;
 
    int ii[] = new int[1];
    int numtasks, me;
    int count1,count2,count3;
    int len[] = new int[0];
    int disp[] = new int[0];



    Datatype type[] = new Datatype[0];
    Datatype newtype;
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


    newtype = Datatype.struct(len,disp,type);
    newtype.commit();

    if(myself == 0)  {
      ii[0] = 2;
      MPJ.COMM_WORLD.send(ii,0,100,newtype,1,0);
    } else if(myself == 1) {
      ii[0] = 0;
      status = MPJ.COMM_WORLD.recv(ii,0,100,newtype,0,0);
      if(ii[0] != 0)
	System.out.println("ERROR!");
      count1 = status.getCount(newtype);
      count2 = status.getElements(newtype);
	    
      if ( (count1==100) &&
	  (count2==MPJ.UNDEFINED) )
	System.out.println("Success\n");
      else
	System.out.println("Should be 100, MPJ.UNDEFINED but is "+count1+", "+count2);
    } 


  
  }


  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  }
}

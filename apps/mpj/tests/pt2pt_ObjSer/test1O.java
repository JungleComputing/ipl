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
    07/29/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/
import ibis.mpj.*;
 
class test1O {
  static public void main(String[] args) throws MPJException {
    test outmsg[] = new test[1];
    test inmsg[] = new test[1];
    int i,me,flag=0;
    Status status=null;
    Request msgid;

    MPJ.init(args);
    me = MPJ.COMM_WORLD.rank();
  
    if(me == 1) { 
      outmsg[0] = new test();
      outmsg[0].a = 5;
      MPJ.COMM_WORLD.send(outmsg,0,1,MPJ.OBJECT,0,1);
    }

    if(me == 0) {
     inmsg[0] = new test();
     inmsg[0].a = 0;
     msgid = MPJ.COMM_WORLD.irecv(inmsg, 0, 1, MPJ.OBJECT,
                                  MPJ.ANY_SOURCE, MPJ.ANY_TAG);

     while(status == null)
	status = msgid.test();

      if(inmsg[0].a != 5 || status.getSource() != 1 || status.getTag() != 1)
	System.out.println
	  ("ERROR inmsg[0]="+inmsg[0].a+", src="+status.getSource()+
	   ", tag="+status.getTag()+", should be 5,1,1");
    }


    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("Test1O TEST COMPLETE\n");
    MPJ.finish();
  }
}

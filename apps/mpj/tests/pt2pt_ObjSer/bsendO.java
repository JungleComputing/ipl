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
    07/18/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/
import ibis.mpj.*;

class bsendO {
  static public void main(String[] args) throws MPJException {
    /* Note that the buffer sizes must include the BSEND_OVERHEAD;
       these values are probably sizeof(int) too large */

    int len,tasks,me,i,size,rc;
    Status status;

    MPJ.init(args);
    me=MPJ.COMM_WORLD.rank();

    test datatest[] = new test[10];
    test recdata[]  = new test[10];
    test a[]        = new test[1000];
    test b[]        = new test[1000];

    int intsize = 4;
    
    // MPJ has no bsend overhead
    //byte buf1[] = new byte[1000*intsize+MPI.BSEND_OVERHEAD];
    //byte buf100[] = new byte[100000*intsize+MPI.BSEND_OVERHEAD];
    byte buf1[] = new byte[1000*intsize];
    byte buf100[] = new byte[100000*intsize];

	   
	   
	   // No obvious rationale to this.
           // Probably bsend is always unsafe for objects.  dbc.

    for(i = 0; i< 10; i++){
       datatest[i]   = new test();
       recdata[i]    = new test();
       datatest[i].a = 1;
       recdata[i].a  = 0;
    }

    if(me==0) {
      
      MPJ.bufferAttach(buf1);  
      MPJ.COMM_WORLD.bsend(datatest,0,10,MPJ.OBJECT,1,1);

      MPJ.bufferDetach();

      MPJ.bufferAttach(buf100);

      MPJ.COMM_WORLD.barrier();

/* test to see if large array is REALLY being buffered */
      for(i=0;i<1000;i++){
         a[i]   = new test();
         a[i].a = 1;
         b[i]   = new test();
         b[i].a = 0;
      }

      MPJ.COMM_WORLD.bsend(a,0,1000,MPJ.OBJECT,1,1);

      MPJ.COMM_WORLD.recv(b,0,1000,MPJ.OBJECT,1,2);

      for(i=0;i<1000;i++)
	if(b[i].a != 2)  
	  System.out.println
	    ("ERROR, incorrect data["+i+"]="+b[i].a+", task 0");

    } else if(me == 1) {
      MPJ.COMM_WORLD.recv(recdata,0,10,MPJ.OBJECT,0,1);
      MPJ.COMM_WORLD.barrier();

      MPJ.bufferAttach(buf100);

/* test to see if large array is REALLY being buffered */
      for(i=0;i<1000;i++){
         a[i]   = new test();
         a[i].a = 2;
         b[i]   = new test();
         b[i].a = 0;
      }
      MPJ.COMM_WORLD.bsend(a,0,1000,MPJ.OBJECT,0,2);

      MPJ.COMM_WORLD.recv(b,0,1000,MPJ.OBJECT,0,1);

      for(i=0;i<1000;i++)
	if(b[i].a != 1)  
	  System.out.println
	    ("ERROR , incorrect data["+i+"]="+b[i].a+", task 1");
    }

    if ((me != 1) && (me != 0)) {
    	MPJ.COMM_WORLD.barrier();
    }
    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("BsendO TEST COMPLETE\n");
    MPJ.finish();
  }
}



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
    07/23/98
****************************************************************************
*/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class isendO {

  static int tasks,bytes,i; 
  static byte buf[] = new byte[10000];
  static Request req[];
  static Status stats[];

  static    test a[] = new test[1];
  static    test b[] = new test[10];
     
 
  static void wstart()
  throws MPJException {
    stats = Request.waitAll(req);

    for(i=0;i<tasks;i++)
      if(b[i].a != i)
	System.out.println
	  ("ERROR : data is"+b[i].a+", should be"+i);
  }
  
  public static void main(String[] args) throws MPJException {
    a[0] = new test();
    for(i=0;i<10;i++)
      b[i] = new test();

    MPJ.init(args);
    a[0].a=MPJ.COMM_WORLD.rank();
    tasks=MPJ.COMM_WORLD.size();

    req = new Request[2*tasks];
    stats = new Status[2*tasks];  

    MPJ.bufferAttach(buf);


    if(a[0].a == 0)  System.out.println("> Testing Isend/Irecv...");
    for(i=0;i<tasks;i++)  b[i].a = -1;
    for(i=0;i<tasks;i++)  {
       req[2*i]=MPJ.COMM_WORLD.isend(a,0,1,MPJ.OBJECT,i,1);
       req[2*i+1]=MPJ.COMM_WORLD.irecv(b,i,1,MPJ.OBJECT,i,1);
    }
    wstart();

    if(a[0].a == 0)  System.out.println("> Testing Issend/Irecv...");
    for(i=0;i<tasks;i++)  b[i].a = -1;
    for(i=0;i<tasks;i++)  {
      req[2*i]=MPJ.COMM_WORLD.issend(a,0,1,MPJ.OBJECT,i,1);
      req[2*i+1]=MPJ.COMM_WORLD.irecv(b,i,1,MPJ.OBJECT,i,1);
    }
    wstart();

    if(a[0].a == 0)  System.out.println("> Testing Irecv/Irsend..."); 
    for(i=0;i<tasks;i++)  b[i].a = -1;
    for(i=0;i<tasks;i++)
      req[2*i+1]=MPJ.COMM_WORLD.irecv(b,i,1,MPJ.OBJECT,i,1);
    MPJ.COMM_WORLD.barrier();
    for(i=0;i<tasks;i++)
      req[2*i]=MPJ.COMM_WORLD.irsend(a,0,1,MPJ.OBJECT,i,1);
    wstart();

    if(a[0].a == 0)  System.out.println("> Testing Ibsend/Irecv...");
    for(i=0;i<tasks;i++)  b[i].a = -1;
    for(i=0;i<tasks;i++)  {
      req[2*i]=MPJ.COMM_WORLD.ibsend(a,0,1,MPJ.OBJECT,i,1);
      req[2*i+1]=MPJ.COMM_WORLD.irecv(b,i,1,MPJ.OBJECT,i,1);
    }
    wstart(); 

    MPJ.COMM_WORLD.barrier();
    if(a[0].a == 0)  System.out.println("IsendO TEST COMPLETE\n");
    MPJ.finish();
  }
}




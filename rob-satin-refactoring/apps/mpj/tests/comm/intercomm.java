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

****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class intercomm {

  static int newsize,me,size,color,key,
             local_lead,remote_lead,newme,othersum;
  static int mebuf[] = new int[1];
  static int sum[]  = new int[1];
  static int newsum[] = new int[1];
  static boolean flag;
  static Intracomm comm,mergecomm;
  static Intercomm intercomm; 
  static Status status;
  static Group newgid;
  


  static void inter_tests() throws MPJException {
    flag = intercomm.testInter();
    if(flag != true)
      System.out.println
	("ERROR in MPJ_Comm_test_inter: flag = "+flag+", should be 1");

    
    newsize = intercomm.remoteSize();    
    if(newsize != size/2)
      System.out.println
	("ERROR in MPJ_Comm_remote_size: size = "+newsize+
	 ", should be "+(size/2));
 
   
    newgid = intercomm.remoteGroup();
    newsize = newgid.size();
    if(newsize != size/2)
      System.out.println
	("ERROR in MPJ_Comm_remote_group: size = "+newsize+
	 ", should be "+(size/2));

 
    newsum[0] = sum[0];
    status = intercomm.sendrecvReplace(newsum,0,1,MPJ.INT,newme,7,newme,7);
    othersum = size/2*(size/2-1);
    if(me%2 == 0)  othersum += size/2;
    if(othersum != newsum[0])
      System.out.println
	("ERROR in Intercomm_create, sum = "+othersum+", should be "+newsum);
 

    boolean high = (color==1) ? true : false;
    Intracomm mergecomm = intercomm.merge(high);
    mebuf[0] = me;
    mergecomm.allreduce(mebuf,0,newsum,0,1,MPJ.INT,MPJ.SUM);
    if(newsum[0] != size*(size-1)/2)
      System.out.println
	("ERROR in MPJ_Intercomm_merge: sum = "+newsum[0]+
	 ", should be "+size*(size-1)/2);
  }
 
  ////////////////////////////////////////////////////////////

  
  static public void test() throws MPJException {
    Intracomm comm1, comm2;

    me = MPJ.COMM_WORLD.rank();
    size = MPJ.COMM_WORLD.size(); 
 
    if(size%2==1) { 
      System.out.println("MUST RUN WITH EVEN NUMBER OF TASKS"); 
      System.exit(0); 
    }
 
    key = me;
    color = me%2;
    comm = MPJ.COMM_WORLD.split(color,key);
    comm1 = comm;
    flag = comm.testInter();
    if(flag != false)
      System.out.println
	("ERROR in MPJ_Comm_test_inter: flag = "+flag+", should be false");
    newme = comm.rank();

    mebuf[0] = me;
    comm.allreduce(mebuf,0,sum,0,1,MPJ.INT,MPJ.SUM);
 

    local_lead = 0;
    remote_lead = (color==1) ? 0 : 1;
    intercomm = MPJ.COMM_WORLD.createIntercomm(comm,local_lead,remote_lead,5);
    inter_tests();
 

    Intercomm incomm = (Intercomm) intercomm.clone();
    intercomm = incomm;
    inter_tests();
 

    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("Intercomm TEST COMPLETE\n");

  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);    

    test();
    
    MPJ.finish();
  }
}

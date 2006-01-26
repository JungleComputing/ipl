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

class compare {
  static public void test() throws MPJException {
    Intracomm comm1,comm2;
    int me,result,color,key;

    me = MPJ.COMM_WORLD.rank();
 
    comm1 = (Intracomm) MPJ.COMM_WORLD.clone();

    result = Comm.compare(comm1,comm1);
    if(result != MPJ.IDENT)
      System.out.println
	("ERROR in MPJ_Comm_compare, result = "+result+
	 ", should be "+(MPJ.IDENT)+"(MPJ_IDENT)");
    

    result = Comm.compare(MPJ.COMM_WORLD,comm1);
    if(result != MPJ.CONGRUENT)
      System.out.println
	("ERROR in MPJ_Comm_compare, result = "+result+
	 ", should be "+(MPJ.CONGRUENT)+"(MPJ_CONGRUENT)");
    

    color = 1;
    key = -me;
    comm2 = comm1.split(color,key);
    result = Comm.compare(comm1,comm2);
    if(result != MPJ.SIMILAR)
      System.out.println
	("ERROR in MPJ_Comm_compare, result = "+result+
	 ", should be "+(MPJ.SIMILAR)+"(MPJ_SIMILAR)");
    

    color = me;
    comm2 = comm1.split(color,key);
    result = Comm.compare(comm1,comm2);
    if(result != MPJ.UNEQUAL)
      System.out.println
	("ERROR in MPJ_Comm_compare, result = "+result+
	 ", should be "+(MPJ.UNEQUAL)+"(MPJ_UNEQUAL)"); 


    if(me == 0)  System.out.println("Compare TEST COMPLETE\n");
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  }
}

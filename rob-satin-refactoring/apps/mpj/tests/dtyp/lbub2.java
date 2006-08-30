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
 
class lbub2 {
  static public void test() throws MPJException {
  
    int error=0;
    int numtasks,me;
    int extent,lb,ub;
    int aob[] = new int[3], aod[] = new int[3];
    int [] bob = new int [2], bod = new int [2] ;



    Datatype newtype,newtype2,newtype3,newtype4,newtype5;
    Datatype newtype6,newtype7,newtype8,newtype9;
    Datatype aot[] = new Datatype[3];
    Datatype bot[] = new Datatype[2];

    me = MPJ.COMM_WORLD.rank();
    numtasks = MPJ.COMM_WORLD.size();

 
    if ((numtasks != 1) && (me!=0)) { 
      System.out.println
        ("Testcase uses one task, extraneous task #"+me+" exited.");
      MPJ.finish();
      System.exit(0);
    }

    newtype = MPJ.INT.contiguous(4);
    newtype.commit();    

    aot[0] = newtype; aod[0] = 3; aob[0] = 1;
    aot[1] = MPJ.UB; aod[1] = 100; aob[1] = 1;
    aot[2] = MPJ.LB; aod[2] = 0; aob[2] = 1;
    newtype2 = Datatype.struct(aob,aod,aot);
    newtype2.commit();

    extent = newtype2.extent();
    lb = newtype2.lb();
    ub = newtype2.ub();
    if ((extent!=100) | (lb!=0) | (ub!=100)) {
      error++;
      System.out.println("Should be: Extent = 100, lb = 0, ub = 100.");
      System.out.println("Is:        Extent = "+extent+
                         ", lb = "+lb+", ub = "+ub);
    }

    bot[0] = newtype; bod[0] = 65; bob[0] = 1;
    bot[1] = MPJ.INT; bod[1] = 97; bob[1] = 1;
    newtype3 = Datatype.struct(bob,bod,bot);
    newtype3.commit();

    extent = newtype3.extent();
    lb = newtype3.lb();
    ub = newtype3.ub();
    if ((extent!=33) | (lb!=65) | (ub!=98)) {
      error++;
      System.out.println("Should be: Extent = 33, lb = 65, ub = 98.");
      System.out.println("Is:        Extent = "+extent+
                         ", lb = "+lb+", ub = "+ub);
    }

    aot[0] = newtype; aod[0] = 32; aob[0] = 1;
    aot[1] = MPJ.LB; aod[1] = 3; aob[1] = 1;
    aot[2] = MPJ.UB; aod[2] = 94; aob[2] = 1;
    newtype4 = Datatype.struct(aob,aod,aot);
    newtype4.commit();

    extent = newtype4.extent();
    lb = newtype4.lb();
    ub = newtype4.ub();
    if ((extent!=91) | (lb!=3) | (ub!=94)) {
      error++;
      System.out.println("Should be: Extent = 91, lb = 3, ub = 94.");
      System.out.println("Is:        Extent = "+extent+
                         ", lb = "+lb+", ub = "+ub);
    }

    aot[0] = newtype; aod[0] = 13; aob[0] = 2;
    aot[1] = MPJ.LB; aod[1] = -3; aob[1] = 1;
    aot[2] = MPJ.UB; aod[2] = 96; aob[2] = 1;
    newtype5 = Datatype.struct(aob,aod,aot);
    newtype5.commit();

    extent = newtype5.extent();
    lb = newtype5.lb();
    ub = newtype5.ub();
    if ((extent!=99) | (lb!=-3) | (ub!=96)) {
      error++;
      System.out.println("Should be: Extent = 99, lb = -3, ub = 96.");
      System.out.println("Is:        Extent = "+extent+
                         ", lb = "+lb+", ub = "+ub);
    }


    aot[0] = newtype; aod[0] = 5; aob[0] = 2;
    aot[1] = MPJ.LB; aod[1] = -3; aob[1] = 1;
    aot[2] = MPJ.UB; aod[2] = 86; aob[2] = 1;
    newtype6 = Datatype.struct(aob,aod,aot);
    newtype6.commit();

    extent = newtype6.extent();
    lb = newtype6.lb();
    ub = newtype6.ub();
    if ((extent!=89) | (lb!=-3) | (ub!=86)) {
      error++;
      System.out.println("Should be: Extent = 89, lb = -3, ub = 86.");
      System.out.println("Is:        Extent = "+extent+
                         ", lb = "+lb+", ub = "+ub);
    }


    if (error == 0)
      System.out.println("Upper bound/lower bound/extent test passed.\n");
    else
      System.out.println("ERRORS in bounds/extent test.\n");

  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();
  } 
}

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
class getcount {
  static public void test() throws MPJException {
    int me,count;
    byte   dataBYTE[]   = new byte[5];
    char   dataCHAR[]   = new char[5];
    int    dataINT[]    = new int[5];
    float  dataFLOAT[]  = new float[5];
    double dataDOUBLE[] = new double[5];
    short  dataSHORT[]  = new short[5];
    long   dataLONG[]   = new long[5];
    Status status;
    Status st2;


    me=MPJ.COMM_WORLD.rank();
    
    if(me == 0)  {
      MPJ.COMM_WORLD.send(dataBYTE,0,5,MPJ.BYTE,1,1);
      
      MPJ.COMM_WORLD.send(dataCHAR,0,5,MPJ.CHAR,1,1);

      MPJ.COMM_WORLD.send(dataINT,0,5,MPJ.INT,1,1);

      MPJ.COMM_WORLD.send(dataFLOAT,0,5,MPJ.FLOAT,1,1);

      MPJ.COMM_WORLD.send(dataDOUBLE,0,5,MPJ.DOUBLE,1,1);

      MPJ.COMM_WORLD.send(dataSHORT,0,5,MPJ.SHORT,1,1);

      MPJ.COMM_WORLD.send(dataLONG,0,5,MPJ.LONG,1,1);

    } else if(me == 1)  {
      status = MPJ.COMM_WORLD.recv(dataBYTE,0,5,MPJ.BYTE,0,1);
      count = status.getCount(MPJ.BYTE);
      if(count != 5) 
	System.out.println
	  ("ERROR(1) in Get_count(MPJ.BYTE), count = "+count+", should be 5");
	 
      
      status = MPJ.COMM_WORLD.recv(dataCHAR,0,5,MPJ.CHAR,0,1);
      count = status.getCount(MPJ.CHAR);
      if(count != 5) 
	System.out.println
	  ("ERROR(2) in Get_count(MPJ.CHAR), count = "+count+", should be 5");

      status = MPJ.COMM_WORLD.recv(dataINT,0,5,MPJ.INT,0,1);
      count = status.getCount(MPJ.INT);
      if(count != 5) 
	System.out.println
	  ("ERROR(3) in Get_count(MPJ.INT), count = "+count+", should be 5");

      status = MPJ.COMM_WORLD.recv(dataFLOAT,0,5,MPJ.FLOAT,0,1);
      count = status.getCount(MPJ.FLOAT);
      if(count != 5) 
	System.out.println
	  ("ERROR(4) in Get_count(MPJ.FLOAT), count = "+count+", should be 5");


      status = MPJ.COMM_WORLD.recv(dataDOUBLE,0,5,MPJ.DOUBLE,0,1);
      count = status.getCount(MPJ.DOUBLE);
      if(count != 5) 
	System.out.println
	  ("ERROR(5) in Get_count(MPJ.DOUBLE), count = "+count+", should be 5");


      status = MPJ.COMM_WORLD.recv(dataSHORT,0,5,MPJ.SHORT,0,1);
      count = status.getCount(MPJ.SHORT);
      if(count != 5) 
	System.out.println
	  ("ERROR(6) in Get_count(MPJ.SHORT), count = "+count+", should be 5");


      status = MPJ.COMM_WORLD.recv(dataLONG,0,5,MPJ.LONG,0,1);
      count = status.getCount(MPJ.LONG);
      if(count != 5) 
	System.out.println
	  ("ERROR(7) in Get_count(MPJ.LONG), count = "+count+", should be 5");

    }

    MPJ.COMM_WORLD.barrier();
    if(me == 1)  System.out.println("getCount TEST COMPLETE.\n");
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);    
    
    test();
    
    MPJ.finish();
  }  
}

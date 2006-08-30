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


class probe {
  static public void test() throws MPJException {

    int me,i,cnt,src,tag,tasks;
    int data[] = new int[1];
    Intracomm comm;
    Status status;
 
    comm = MPJ.COMM_WORLD;
    me = comm.rank();
    tasks = comm.size(); 


    /* probe for specific source, tag */
    if(me > 0) {
      data[0] = me;
      comm.send(data,0,1,MPJ.INT,0,me);
    } else  {
      for(i=1;i<tasks;i++)  {
	status = comm.probe(i,i);

	src = status.getSource();
	if(src != i)
	  System.out.println
	    ("ERROR in MPJ_Probe(1): src = "+src+", should be "+i);


	tag = status.getTag();
	if(tag != i)
	  System.out.println
	    ("ERROR in MPJ_Probe(1): tag = "+tag+", should be "+i);


	cnt = status.getCount(MPJ.INT);
	if(cnt != 1) {
	  System.out.println
	    ("ERROR in MPJ_Probe(1): cnt = "+cnt+", should be 1");
	  System.exit(0);   
	}


	status = comm.recv(data,0,cnt,MPJ.INT,src,tag);
	if(data[0] != i) 
	  System.out.println
	    ("ERROR in MPJ_Recv(1), data = "+data[0]+", should be "+i);
      }
    }
 

    /* probe for specific source, tag = MPJ_ANY_TAG */
    if(me > 0) {
      data[0] = me;
      comm.send(data,0,1,MPJ.INT,0,me);
    } else  {
      for(i=1;i<tasks;i++)  {
	status = comm.probe(i,MPJ.ANY_TAG);


	src = status.getSource();
	if(src != i)
	  System.out.println
	    ("ERROR in MPJ_Probe(2): src = "+src+", should be "+i);


	tag =status.getTag(); 
	if(tag != i)
	  System.out.println
	    ("ERROR in MPJ_Probe(2): tag = "+tag+", should be "+i);


	cnt = status.getCount(MPJ.INT);
	if(cnt != 1) 
	  System.out.println
	    ("ERROR in MPJ_Probe(2): cnt = "+cnt+", should be 1");


	status = comm.recv(data,0,cnt,MPJ.INT,src,tag);
	if(data[0] != i) 
	  System.out.println
	    ("ERROR in MPJ_Recv(2), data = "+data[0]+", should be "+i);
      }
    }
 

    /* probe for specific tag, source = MPJ_ANY_SOURCE */
    if(me > 0) {
      data[0] = me;
      comm.send(data,0,1,MPJ.INT,0,me);   
    } else  {
      for(i=1;i<tasks;i++)  {
	status = comm.probe(MPJ.ANY_SOURCE,i);
	

	src = status.getSource();
	if(src != i)
	  System.out.println
	    ("ERROR in MPJ_Probe(3): src = "+src+", should be "+i);	    


	tag =status.getTag();
	if(tag != i)
	  System.out.println
            ("ERROR in MPJ_Probe(3): tag = "+tag+", should be "+i);


	cnt = status.getCount(MPJ.INT);
	if(cnt != 1) 
	  System.out.println
	    ("ERROR in MPJ_Probe(3): cnt = "+cnt+", should be 1");


        status = comm.recv(data,0,cnt,MPJ.INT,src,tag);
	if(data[0] != i) 
	  System.out.println
            ("ERROR in MPJ_Recv(3), data = "+data[0]+", should be "+i);
      }      
    }
 

    /* probe for source = MPI_ANY_SOURCE, tag = MPI_ANY_TAG */
    if(me > 0) {
      data[0] = me;
      comm.send(data,0,1,MPJ.INT,0,me);
    } else  {
      for(i=1;i<tasks;i++)  {
        status = comm.probe(MPJ.ANY_SOURCE,MPJ.ANY_TAG);	

        src = status.getSource();
        tag =status.getTag();
	if(src != tag)
	  System.out.println
            ("ERROR in MPJ_Probe(4): tag = "+tag+", should be "+src);


        cnt = status.getCount(MPJ.INT);
	if(cnt != 1) System.out.println
            ("ERROR in MPJ_Probe(4): cnt = "+cnt+", should be 1");


        status = comm.recv(data,0,cnt,MPJ.INT,src,tag);	
	if(data[0] != src)
	  System.out.println
            ("ERROR in MPJ_Recv(4), data = "+data+", should be "+src);
      }
    }
 
    comm.barrier();
    if(me == 0) System.out.println("Probe TEST COMPLETE\n");
     
  
  }

  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
    
    test();
    
    MPJ.finish();
  }
}

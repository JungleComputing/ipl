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

class group {
  static public void test() throws MPJException {
    int tasks,me,size,rank,i,result,rc;
    int       cnt = 0;
    

    
    Group group1,group2,group3,newgroup;
    Group groups[] = new Group[20];
    Comm newcomm;
    

    tasks = MPJ.COMM_WORLD.size();
    int ranks1[] = new int[tasks/2];
    int ranks2[] = new int[tasks/2];
    int ranks3[] = new int[tasks];
    
    if(tasks < 2) { 
      System.out.println("MUST HAVE AT LEAST 2 TASKS"); 
      System.exit(0); }
    
    me = MPJ.COMM_WORLD.rank();
    
    group1 = MPJ.COMM_WORLD.group();
    groups[cnt++] = group1;
    
    size = group1.size();
    
    if(size != tasks)  
      System.out.println
	("ERROR in MPJ_Group_size, size = "+size+", should be "+tasks);


    rank = group1.rank();

    if(rank != me)  
      System.out.println
	("ERROR in MPJ_Group_rank, rank = "+rank+", should be "+me);

    for(i=0;i<tasks/2;i++)  ranks1[i] = i;

    newgroup = group1.incl(ranks1);

    /* newgroup freed below */
    size = newgroup.size();
    if(size != (tasks/2))  
      System.out.println
	("ERROR in MPJ_Group_size, size = "+size+", should be "+(tasks/2));


    result = Group.compare(newgroup,newgroup);
    if(result != MPJ.IDENT)  
      System.out.println
	("ERROR in MPJ_Group_compare (1), result = "+result+
	 ", should be "+MPJ.IDENT);


    result = Group.compare(newgroup,group1);
    if(result != MPJ.UNEQUAL)  
      System.out.println
	("ERROR in MPJ_Group_compare (2), result = "+result+
	 ", should be "+MPJ.UNEQUAL);


    group2 = Group.union(group1,newgroup);
    groups[cnt++] = group2;
    result = Group.compare(group1,group2);
    if(result != MPJ.IDENT)  
      System.out.println
	("ERROR in MPJ_Group_compare (3), result = "+result+
	 ", should be "+MPJ.IDENT);


    group2 = Group.intersection(newgroup,group1);
    groups[cnt++] = group2;
    result = Group.compare(group2,newgroup);
    if(result != MPJ.IDENT)  
      System.out.println
	("ERROR in MPJ_Group_compare (4), result = "+result+
	 ", should be "+MPJ.IDENT);
   

    group2 = Group.difference(group1,newgroup);
    groups[cnt++] = group2;
    size = group2.size();
    if(size != (tasks/2))
      System.out.println
	("ERROR in MPJ_Group_size, size = "+size+
	 ", should be "+(tasks/2));


    for(i=0;i<size;i++)  ranks1[i] = i;
    ranks2 = group1.translateRanks(group2,ranks1);
    for(i=0;i<size;i++) {
      if(ranks2[i] != (tasks/2 + i))  
	System.out.println("ERROR in MPJ_Group_translate_ranks.");
    }

    
    newcomm = MPJ.COMM_WORLD.create(newgroup);
    if(newcomm != null)  {
      group3 = newcomm.group();
      groups[cnt++] = group3;
      result = Group.compare(group3,newgroup);
      if(result != MPJ.IDENT)  
	System.out.println
	  ("ERROR in MPJ_Group_compare (4.5) , result = "+result+
	   ", should be "+MPJ.IDENT);
    }
  

    group3 = group1.excl(ranks1);
    groups[cnt++] = group3;
    result = Group.compare(group2,group3);
    if(result != MPJ.IDENT)  
      System.out.println
	("ERROR in MPJ_Group_compare (5) , result = "+result+
	 ", should be "+MPJ.IDENT);
    


    for(i=0;i<tasks;i++)  ranks3[tasks-1-i] = i;
    group3 = group1.incl(ranks3);
    groups[cnt++] = group3;
    result = Group.compare(group1,group3);
    if(result != MPJ.SIMILAR)  
      System.out.println
	("ERROR in MPJ_Group_compare (6), result = "+result+
	 ", should be "+MPJ.SIMILAR);


    MPJ.COMM_WORLD.barrier();
    if(me == 0)  System.out.println("Group TEST COMPLETE\n"); 
  
  }
  
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);

    test();
    
    MPJ.finish();

  }
}

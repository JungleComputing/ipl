
/****************************************************************************

 MPI-Java version :
    Sung-Hoon Ko(shko@npac.syr.edu)
    Northeast Parallel Architectures Center at Syracuse University
    09/14/99

****************************************************************************/
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class allreduce_maxminloc {
  static public void test() throws MPJException {
    int count, errcnt = 0, gerr = 0, size, rank;
    Comm comm;
    
    Comm comms[] = new Comm[10];
    int  ncomm, i, world_rank;
    
    world_rank = MPJ.COMM_WORLD.rank();
 
    comm = MPJ.COMM_WORLD;
    size = comm.size();
    rank = comm.rank();
    
    count = 10;
    
    
    /* Test Maxloc */

    // int //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MAXLOC with MPJ.INT2..." );
    
    int in[] = new int[2*count];
    int out[] = new int[2*count];
    int sol[] = new int[2*count];
    
    int fnderr=0;

    for (i=0; i<count; i++) {
      in[i*2]     = (rank + i);
      in[i*2+1] = rank ;
	
      sol[i*2]     = size - 1 + i;
      sol[i*2+1] = size - 1;

      out[i*2]     = 0;
      out[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in,0,out,0,count,MPJ.INT2,MPJ.MAXLOC);
    
    for (i=0; i<count; i++) { 
      if ( out[i*2] != sol[i*2] || 
	   out[i*2+1] != sol[i*2+1] ) {
	errcnt++; fnderr++; 
	System.out.println(world_rank+" Expected ("+sol[i*2]+","+sol[i*2+1]+") got ("+out[i*2]+","+out[i*2+1]+")");
      }
    }
    
    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.INT2 and op MPJ.MAXLOC ("+fnderr+" of "+count+" wrong)");
    
    
    // long //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MAXLOC with MPJ.LONG2..." );    

    fnderr=0;

    long in_l[] = new long[2*count];
    long out_l[] = new long[2*count];
    long sol_l[] = new long[2*count];

    for (i=0; i<count; i++) {
      in_l[i*2]     = (rank + i);
      in_l[i*2+1] = rank ;
        
      sol_l[i*2]     = (size - 1 + i);
      sol_l[i*2+1] = (size - 1);

      out_l[i*2]     = 0;
      out_l[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_l,0,out_l,0,count,MPJ.LONG2,MPJ.MAXLOC);
 

    for (i=0; i<count; i++) { 
      if ( out_l[i*2] != sol_l[i*2] || 
           out_l[i*2+1] != sol_l[i*2+1] ) {
        errcnt++; fnderr++; 
        System.out.println(world_rank+" Expected ("+sol_l[i*2]+","+sol_l[i*2+1]+") got ("+out_l[i*2]+","+out_l[i*2+1]+")");
      }
    }

    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.LONG2 and op MPJ.MAXLOC ("+fnderr+" of "+count+" wrong)");

    
    // short //////////////////////////////////////////
    MPJ.COMM_WORLD.barrier();
    if (world_rank == 0) System.out.println( "Testing MPJ.MAXLOC with MPJ.SHORT2..." );    

    fnderr=0;

    short in_s[] = new short[2*count];
    short out_s[] = new short[2*count];
    short sol_s[] = new short[2*count];

    for (i=0; i<count; i++) {
      in_s[i*2]     = (short)(rank + i);
      in_s[i*2+1] = (short)rank ;

      sol_s[i*2]     = (short)(size - 1 + i);
      sol_s[i*2+1] = (short)(size - 1);

      out_s[i*2]     = 0;
      out_s[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_s,0,out_s,0,count,MPJ.SHORT2,MPJ.MAXLOC);
 

    for (i=0; i<count; i++) { 
      if ( out_s[i*2] != sol_s[i*2] || 
           out_s[i*2+1] != sol_s[i*2+1] ) {
        errcnt++; fnderr++; 
        System.out.println(world_rank+" Expected ("+sol_s[i*2]+","+sol_s[i*2+1]+")got ("+out_s[i*2]+","+out_s[i*2+1]+")");
      }
    }

    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.SHORT2 and op MPJ.MAXLOC ("+fnderr+" of "+count+" wrong)");
    

    // float //////////////////////////////////////////
    MPJ.COMM_WORLD.barrier();
    if (world_rank == 0) System.out.println( "Testing MPJ.MAXLOC with MPJ.FLOAT2..." );    

    fnderr=0;

    float in_f[] = new float[2*count];
    float out_f[] = new float[2*count];
    float sol_f[] = new float[2*count];

    for (i=0; i<count; i++) {
      in_f[i*2]     = (rank + i);
      in_f[i*2+1] = rank ;
        
      sol_f[i*2]     = (size - 1 + i);
      sol_f[i*2+1] = (size - 1);

      out_f[i*2]     = 0;
      out_f[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_f,0,out_f,0,count,MPJ.FLOAT2,MPJ.MAXLOC);
 

    for (i=0; i<count; i++) { 
      if ( out_f[i*2] != sol_f[i*2] || 
           out_f[i*2+1] != sol_f[i*2+1] ) {
        errcnt++; fnderr++; 
        System.out.println(world_rank+" Expected ("+sol_f[i*2]+","+sol_f[i*2+1]+")got ("+out_f[i*2]+","+out_f[i*2+1]+")");
      }
    }

    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.FLOAT and op MPJ.MAXLOC ("+fnderr+" of "+count+" wrong)");


    // double //////////////////////////////////////////
    MPJ.COMM_WORLD.barrier();
    if (world_rank == 0) System.out.println( "Testing MPJ.MAXLOC with MPJ.DOUBLE2..." );    

    fnderr=0;

    double in_d[] = new double[2*count];
    double out_d[] = new double[2*count];
    double sol_d[] = new double[2*count];

    for (i=0; i<count; i++) {
      in_d[i*2]     = (rank + i);
      in_d[i*2+1] = rank ;

      sol_d[i*2]     = (size - 1 + i);
      sol_d[i*2+1] = (size - 1);

      out_d[i*2]     = 0;
      out_d[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_d,0,out_d,0,count,MPJ.DOUBLE2,MPJ.MAXLOC);
 
    for (i=0; i<count; i++) { 
      if ( out_d[i*2] != sol_d[i*2] || 
           out_d[i*2+1] != sol_d[i*2+1] ) {
        errcnt++; fnderr++; 
        System.out.println(world_rank+" Expected ("+sol_d[i*2]+","+sol_d[i*2+1]+")got ("+out_d[i*2]+","+out_d[i*2+1]+")");
      }
    }

    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.DOUBLE and op MPJ.MAXLOC ("+fnderr+" of "+count+" wrong)");




    gerr += errcnt;
    if (errcnt > 0)
	System.out.println( "Found "+errcnt+" errors on "+rank+" for MPJ_MAXLOC\n");
    errcnt = 0;
    

    ///////////////////////////////////////////////////////
    // Test minloc 
    //////////////////////////////////////////////////////

    // int //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MINLOC with MPJ.INT2..." );
    
    //struct int_test { int a; int b; } *in, *out, *sol;
    int in_mn_i[] = new int[2*count];
    int out_mn_i[] = new int[2*count];
    int sol_mn_i[] = new int[2*count];
    
    fnderr=0;

    for (i=0; i<count; i++) {
      in_mn_i[i*2]     = (rank + i);
      in_mn_i[i*2+1] = rank ;
	
      sol_mn_i[i*2]     = i;
      sol_mn_i[i*2+1] = 0;

      out_mn_i[i*2]     = 0;
      out_mn_i[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_mn_i,0,out_mn_i,0,count,MPJ.INT2,MPJ.MINLOC);
    
    for (i=0; i<count; i++) { 
      if ( out_mn_i[i*2] != sol_mn_i[i*2] || 
	   out_mn_i[i*2+1] != sol_mn_i[i*2+1] ) {
	errcnt++; fnderr++; 
	System.out.println(world_rank+" Expected ("+sol_mn_i[i*2]+","+sol_mn_i[i*2+1]+") got ("+out_mn_i[i*2]+","+out_mn_i[i*2+1]+")");
      }
    }
    
    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.INT2 and op MPJ.MINLOC ("+fnderr+" of "+count+" wrong)");
    
    
    // long //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MINLOC with MPJ.LONG2..." );
    
    long in_mn_l[] = new long[2*count];
    long out_mn_l[] = new long[2*count];
    long sol_mn_l[] = new long[2*count];
    
    fnderr=0;

    for (i=0; i<count; i++) {
      in_mn_l[i*2]     = (rank + i);
      in_mn_l[i*2+1] = rank ;
	
      sol_mn_l[i*2]     = i;
      sol_mn_l[i*2+1] = 0;

      out_mn_l[i*2]     = 0;
      out_mn_l[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_mn_l,0,out_mn_l,0,count,MPJ.LONG2,MPJ.MINLOC);
    
    for (i=0; i<count; i++) { 
      if ( out_mn_l[i*2] != sol_mn_l[i*2] || 
	   out_mn_l[i*2+1] != sol_mn_l[i*2+1] ) {
	errcnt++; fnderr++; 
	System.out.println(world_rank+" Expected ("+sol_mn_l[i*2]+","+sol_mn_l[i*2+1]+") got ("+out_mn_l[i*2]+","+out_mn_l[i*2+1]+")");
      }
    }
    
    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.INT2 and op MPJ.MINLOC ("+fnderr+" of "+count+" wrong)");
    
    


    // short //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MINLOC with MPJ.SHORT2..." );
    
    short in_mn_s[] = new short[2*count];
    short out_mn_s[] = new short[2*count];
    short sol_mn_s[] = new short[2*count];
    
    fnderr=0;

    for (i=0; i<count; i++) {
      in_mn_s[i*2]     = (short)(rank + i);
      in_mn_s[i*2+1] = (short)rank ;
	
      sol_mn_s[i*2]     = (short)i;
      sol_mn_s[i*2+1] = 0;

      out_mn_s[i*2]     = 0;
      out_mn_s[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_mn_s,0,out_mn_s,0,count,MPJ.SHORT2,MPJ.MINLOC);
    
    for (i=0; i<count; i++) { 
      if ( out_mn_s[i*2] != sol_mn_s[i*2] || 
	   out_mn_s[i*2+1] != sol_mn_s[i*2+1] ) {
	errcnt++; fnderr++; 
	System.out.println(world_rank+" Expected ("+sol_mn_s[i*2]+","+sol_mn_s[i*2+1]+") got ("+out_mn_s[i*2]+","+out_mn_s[i*2+1]+")");
      }
    }
    
    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.SHORT2 and op MPJ.MINLOC ("+fnderr+" of "+count+" wrong)");


    // double //////////////////////////////////////////
    if (world_rank == 0) System.out.println( "Testing MPJ.MINLOC with MPJ.DOUBLE2..." );
    
    double in_mn_d[] = new double[2*count];
    double out_mn_d[] = new double[2*count];
    double sol_mn_d[] = new double[2*count];
    
    fnderr=0;

    for (i=0; i<count; i++) {
      in_mn_d[i*2]     = (rank + i);
      in_mn_d[i*2+1] = rank ;
	
      sol_mn_d[i*2]     = i;
      sol_mn_d[i*2+1] = 0;

      out_mn_d[i*2]     = 0;
      out_mn_d[i*2+1] = -1;
    }
    MPJ.COMM_WORLD.allreduce(in_mn_d,0,out_mn_d,0,count,MPJ.DOUBLE2,MPJ.MINLOC);
    
    for (i=0; i<count; i++) { 
      if ( out_mn_d[i*2] != sol_mn_d[i*2] || 
	   out_mn_d[i*2+1] != sol_mn_d[i*2+1] ) {
	errcnt++; fnderr++; 
	System.out.println(world_rank+" Expected ("+sol_mn_d[i*2]+","+sol_mn_d[i*2+1]+") got ("+out_mn_d[i*2]+","+out_mn_d[i*2+1]+")");
      }
    }
    
    if (fnderr > 0) System.out.println(world_rank+" Error for type MPJ.DOUBLE2 and op MPJ.MINLOC ("+fnderr+" of "+count+" wrong)");


    gerr += errcnt;
    if (errcnt > 0)
	System.out.println( "Found "+errcnt+" errors on "+rank+" for MPJ_MINLOC");
    errcnt = 0;

  
    if (gerr > 0) {
	System.out.println( "Found "+gerr+" errors overall on "+rank);
    }
    else 
	if(world_rank == 0)
	    System.out.println("\nAllreduce Max-Min-Loc TEST COMPLETE.\n");
    
    
    MPJ.COMM_WORLD.barrier();   
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
    
    test();

    MPJ.finish( );
  }
}

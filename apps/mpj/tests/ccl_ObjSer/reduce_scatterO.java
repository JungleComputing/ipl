/* $Id$ */

/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;



class reduce_scatterO {
  static public void test() throws MPJException {
    final int MAXLEN = 100;
 
    complexNum out[] = new complexNum[900];
    complexNum in[]  = new complexNum[900];

    int i,j,k;
    int myself,tasks;
    int recvcounts[] = new int[128];
    boolean bool=false;

    myself = MPJ.COMM_WORLD.rank();
    tasks = MPJ.COMM_WORLD.size();
 
    for(j=MAXLEN*tasks;j<=MAXLEN*tasks;j*=10)  {
      for(i=0;i<tasks;i++)  recvcounts[i] = j;
      for(i=0;i<j*tasks;i++){
        in[i] = new complexNum();
        out[i] = new complexNum();
        out[i].realPart = i;
        out[i].imaginPart = i;
      }

      //complexAdd cadd = new complexAdd();
      //Op op = new Op(cadd, bool);
      complexAdd op = new complexAdd(bool);
      
      MPJ.COMM_WORLD.reduceScatter(out,0,in,0,recvcounts,MPJ.OBJECT,op);

      for(k=0;k<j;k++) {
	if(in[k].realPart != tasks*(myself*j+k)) {  
	  System.out.println
	    ("bad answer ("+in[k].realPart+") at index "+k+" of "+j+
	     "(should be "+tasks*(myself*j+k)+")"); 
	  break; 
	}
      }
    }

    MPJ.COMM_WORLD.barrier();
    if(myself == 0)  System.out.println("Reduce_scatter TEST COMPLETE\n");
  
  }
  
  static public void main(String[] args) throws MPJException {
    MPJ.init(args);
    
    test();
    
    MPJ.finish();
  }
}

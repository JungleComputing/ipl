package ibis.group;

public class Personalizer { 

} 

/*

  interface X extends GroupInterface { 
           void put(double [] d, int i);
	   Data get(int i);
  } 

  stub.foo(d, i) => personalized_foo(d, i, ParameterVector p []) 

  ParameterVector { 
      p1, p2, p3, p4 etc.
      writeInt(int num, val);
  } 
  
  void personalize(double [] d, int i, ParameterVector p [], int size) { 
  
         for (int i=0;i<size;i++) { 
	     p[i].writeSubArray(0, d, i*(d.length/size), (i+1)*(d.length/size));
	     p[i].writeInt(1, i/size);
	 } 
  } 
  

 */

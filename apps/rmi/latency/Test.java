import ibis.rmi.*;

public class Test implements myServer { 

	int i;

	public Test() { 
		super();
	} 

	public void foo() { 
//		System.out.println("foo");
//		i++;
	} 
	
	public int bar() { 
		System.out.println("bar");
		return 42;
	}
} 

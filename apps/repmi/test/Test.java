import ibis.repmi.ReplicatedObject;

public class Test extends ReplicatedObject implements myRep { 

	int i;

	public Test() { 
		super();
	} 

	public void foo() { 
		System.out.println("foo");
		i++;
	} 
	
	public int bar() { 
		System.out.println("bar");
		return 42;
	}
} 

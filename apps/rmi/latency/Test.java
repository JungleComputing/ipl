import ibis.rmi.*;

public class Test extends ibis.rmi.server.UnicastRemoteObject implements myServer { 

	int i;

	public Test() throws RemoteException { 
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

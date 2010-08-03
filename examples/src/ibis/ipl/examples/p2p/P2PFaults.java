package ibis.ipl.examples.p2p;

import java.util.Random;

import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.PortType;

public class P2PFaults {
	PortType portType = new PortType(
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_EXPLICIT,
			PortType.CONNECTION_MANY_TO_MANY, PortType.CONNECTION_DOWNCALLS);

	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT);
	Ibis ibis;
	
	public void run() throws IbisCreationFailedException, InterruptedException {
		ibis = IbisFactory.createIbis(ibisCapabilities, null, portType);
		
		System.out.println("ID: " + ibis.identifier());
		
		Thread.sleep(10000);
		
	    Random randomGenerator = new Random();
	    int randomInt = randomGenerator.nextInt(100);
	    if (randomInt < 10) {
	    	System.out.println("I failed!");
	    	System.exit(0);
	    }
	    
	    Thread.sleep(10000);
	    
	    System.out.println("ID: " + ibis.identifier());
	}
	
	public static void main(String[] args) {
		try {
			new P2PFaults().run();
		} catch (IbisCreationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

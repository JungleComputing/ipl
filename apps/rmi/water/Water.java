import java.rmi.*;
import java.net.*;
import java.io.*;
import java.rmi.registry.*;
import ibis.util.PoolInfo;

public class Water{

    WaterMaster master;
    String hostName, masterName;
    int hostNr, nHosts;
    Registry registry;
    String inputfile;


    Water(){
	hostNr = 0;
	nHosts = 0;
	hostName = "";
	masterName = "";
	inputfile = "random.in";
    }

    private void startRMI(){
	//start registry
	try{
	    registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
	}catch(RemoteException e){
	    try{
		registry = LocateRegistry.getRegistry();
	    }catch(Exception d){
		System.out.println(" Failed to locate or get registry ");
		System.exit(1);
	    }
	}
	//start security manager
	if(System.getSecurityManager() == null)
	    System.setSecurityManager(new RMISecurityManager());
    }

    private void createMaster(){
	try{
	    master = new WaterMaster(nHosts, inputfile);
	}catch(Exception e){
	    e.printStackTrace();
	    System.out.println(" Failed to create master");
	    System.exit(1);
	}
	new Thread(master).start();
    }

    public void start(String[] args){

	PoolInfo info = null;
	
	try {
	    info = PoolInfo.createPoolInfo();
	} catch(Exception e) {
	    System.err.println("Oops: " + e);
            e.printStackTrace();
            System.err.println("Problem with PoolInfo");
	    System.exit(1);
	}

	if (args.length == 0) {
	} else if (args.length == 1) {
	    inputfile = args[0];
	} else {
	    System.out.println("Usage: water [<inputfile>]");
	    System.exit(1);
	}
	hostNr = info.rank();
	nHosts = info.size();
	hostName = info.hostName(hostNr);
	startRMI();
	masterName = info.hostName(0);
	if (hostNr == 0) createMaster();

	//start a slave
	try{
	    new WaterWorker(hostName, masterName, nHosts).start(info);
	}catch(Exception e){
            e.printStackTrace();
            System.err.println("Problem with WaterWorker");
        } 
    }
        
    public static void main(String[] args) {
	new Water().start(args);
	System.exit(0);
    }
}

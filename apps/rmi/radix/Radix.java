import java.rmi.*;
import java.net.*;
import java.io.*;
import java.rmi.registry.*;
import ibis.util.PoolInfo;
import ibis.ipl.IbisException;

public class Radix{
    
    static final int MAX_PROCESSORS = 64;
    static final int MAX_RADIX = 4096;
    static final int DEFAULT_RADIX = 1024;
    static final int DEFAULT_LOG2 = 10;
    static final int DEFAULT_NUMBER_OF_KEYS = 1000;

    int radix, num_Keys, log2_Radix;
    boolean doStats, testResult, doPrint;
    String hostname, mastername;
    int nhosts, host;
    RadixMaster master;
    Registry registry;
    DataOutputStream output = null;
    PoolInfo d = null;
    String output_file = null;
 
    Radix() throws IbisException {
	d = new PoolInfo();
        nhosts = d.size();
        host  = d.rank();
        hostname = d.hostName(host);
        mastername = d.hostName(0);
        registry = null;
        master = null;
        radix = DEFAULT_RADIX;
        num_Keys = DEFAULT_NUMBER_OF_KEYS;
        log2_Radix = DEFAULT_LOG2;
    }
    
    public void usage(){
	System.out.println(" Radix <options>");
	System.out.println(" options are:");
	System.out.println(" -rR : R = radix for sorting. Must be power of 2.");
	System.out.println(" -nN : N = number of key's to sort.");
	System.out.println(" -s  : Print individual processor timing statistics.");
	System.out.println(" -t  : Check to make sure all keys are sorted. ");
	System.out.println(" -o  : Print out sorted keys.");
	System.out.println(" -h  : Print out commandline options.");
	System.out.println("Default: Radix -r1024 -n1000");
    }

    void parseCommandLine(String[] argv){
	String sub = new String(""); 
	if(argv.length > 0){
	    for(int i = 0; i < argv.length; i++){
		if(argv[i].startsWith("-r")){
		    try{
			sub = argv[i].substring(2);
			radix = Integer.parseInt(sub);
			log2_Radix = log2Radix();
		    }catch(Exception e){
			System.out.println(e.getMessage());
			usage();
			System.exit(1);
		    }
		}else if(argv[i].startsWith("-n")){
		    try{
			sub = argv[i].substring(2);
			num_Keys = Integer.parseInt(sub);
		    }catch(Exception e){
			System.out.println(e.getMessage());
			usage();
			System.exit(1);
		    }
		}else if(argv[i].startsWith("-s")){
		    doStats = true;
		}else if(argv[i].startsWith("-t")){
		    testResult = true;
		} else if(argv[i].startsWith("-o")){
		    doPrint = true;
		}else if(argv[i].startsWith("-h")){
		    usage();
		    System.exit(1);
		}else {
		    System.out.println("Wrong Argument");
		    usage();
		    System.exit(1);
		} 
	    }
	}	
    }
	
    public int log2Radix(){
	double d1 = Math.log(radix);
	double d2 = Math.log(2);
	double t = d1/d2;
	int tmp = (int)(t);
	if((double)tmp != t){
	    System.out.println(" Radix must be a power of 2");
	    System.exit(1);
	}
	return tmp;	
    }

    private void startRMI(){
        //start registry
	
	if (host == 0) {
            try{
                registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            }catch(RemoteException e){
                try{
                    registry = LocateRegistry.getRegistry();
                }catch (Exception d){
                    System.out.println("Failed to locate or get registry");
                    System.exit(1);
                }	
            }	
	}
        //start security manager
        if(System.getSecurityManager() == null)
            System.setSecurityManager(new RMISecurityManager());
    }

 
    private Thread createMaster(){
	Thread t;
        try{
            master = new RadixMaster(d, num_Keys, nhosts, radix, doStats, testResult, doPrint);
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Failed to create master");
            System.exit(1);
        }
	t = new Thread(master);
	t.start();
	return t;
    }
   
    void start(String argv[]){
	SlaveSort l;
	Thread masterThread = null;
        parseCommandLine(argv);
        startRMI();
        if (host == 0) {
	    masterThread = createMaster();
	}
	else {
	    try {
		 Thread.sleep(2000);
	    } catch (Exception e) {
	    }
	}
	
        //start a slave     
	try{
            l = new SlaveSort(hostname, mastername, host);
	    if (host == 0) master.this_cpus_slave = l;
	    l.start();
        }catch(Exception e){
            e.printStackTrace();
            System.err.println("Problem with RadixWorker");
        }   
	if (host == 0) {
	    try {
	        masterThread.join(10000);
	    } catch (Exception e) {
            	e.printStackTrace();
            	System.err.println("Problem with waiting for master");
	    }
	}
    }     
     
    public static void main(String argv[]){
	try {
	    new Radix().start(argv);        
	} catch(Exception e) {
	    System.err.println("Oops: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
	System.exit(0);
    }
}

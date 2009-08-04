package ibis.ipl.registry.bt;

import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.registry.statistics.Statistics;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
//import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.bluetooth.*;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnectionNotifier;
//import javax.microedition.io.Connector;
//import javax.microedition.io.StreamConnectionNotifier;
import java.util.Vector;

public class Registry extends ibis.ipl.registry.Registry implements Runnable, DiscoveryListener {
//public class Registry implements ibis.ipl.Registry, Runnable {

    //bluetooth 
    private LocalDevice localDevice; // local Bluetooth Manager
    private DiscoveryAgent discoveryAgent; // discovery agent
    private final String myServiceUUID = "2d26618601fb47c28d9f10b8ec891363";
    private final UUID MYSERVICEUUID_UUID = new UUID(myServiceUUID, false);
    private final String connURL = "btspp://localhost:"+MYSERVICEUUID_UUID.toString() + ";name=Ibis";
	private final Logger logger = LoggerFactory.getLogger(Registry.class);
	
	private boolean scan;

    private final IbisCapabilities capabilities;

    private final TypedProperties properties;

    private final String poolName;

    private IbisIdentifier identifier;
    private final Object inquiryCompletedEvent = new Object();
    private final Object serviceSearchSemaphore = new Object();
  //  private final Statistics statistics;

//    private final MemberSet members;

  //  private final ElectionSet elections;

    private final Upcaller upcaller;

    // data structures the user can poll

    private final ArrayList<IbisIdentifier> joinedIbises;
    private final ArrayList<IbisIdentifier> leftIbises;
    private final ArrayList<IbisIdentifier> diedIbises;
    

    
//    private final ArrayList<String> signals;
//
    private boolean stopped;
    
    Vector<RemoteDevice> discoveredDevices;
    Vector<RemoteDevice> remoteDevices;
    Vector<RemoteDevice> joinedDevices;
    Vector<String> discoveredServices;
    Map<RemoteDevice, String> scannedDevices;
    
    /**
     * Creates a Gossip Registry.
     * 
     * @param eventHandler
     *            Registry handler to pass events to.
     * @param userProperties
     *            properties of this registry.
     * @param ibisData
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @param applicationTag
     *            A tag provided by the application constructing this ibis.
     * @param authenticationObject
     * @param ibisImplementationIdentifier
     *            the identification of this ibis implementation, including
     *            version, class and such. Must be identical for all Ibises in
     *            a single poolName.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities,
            RegistryEventHandler eventHandler, Properties userProperties,
            byte[] ibisData, String implementationVersion, Credentials credentials,
            byte[] applicationTag
            )
            throws IbisConfigurationException, IOException,
            IbisConfigurationException {
        this.capabilities = capabilities;
        localDevice = null;
        discoveryAgent = null;
        discoveredDevices = new Vector<RemoteDevice>();
        remoteDevices = new Vector<RemoteDevice>();
        joinedDevices = new Vector<RemoteDevice>();
        discoveredServices = new Vector<String>();
        scannedDevices = new HashMap<RemoteDevice, String>();
        scan = true;
        
        localDevice = LocalDevice.getLocalDevice();
        //FIX this...
        //localDevice.setDiscoverable(DiscoveryAgent.GIAC);
        discoveryAgent = localDevice.getDiscoveryAgent();
        
        if (capabilities
                .hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
            throw new IbisConfigurationException(
                    "Bluetooth registry does not support totally ordered membership");
        }

        if (capabilities.hasCapability(IbisCapabilities.CLOSED_WORLD)) {
            throw new IbisConfigurationException(
                    "Bluetooth registry does not support closed world");
        }

       if (capabilities.hasCapability(IbisCapabilities.ELECTIONS_STRICT)) {
            throw new IbisConfigurationException(
                    "Bluetooth registry does not support strict elections");
       }

        if (capabilities.hasCapability(IbisCapabilities.TERMINATION)) {
            throw new IbisConfigurationException(
                    "Bluetooth registry does not support termination");
        }

        properties = RegistryProperties.getHardcodedProperties();
        properties.addProperties(userProperties);

        if ((capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE))
                && eventHandler == null) {
            joinedIbises = new ArrayList<IbisIdentifier>();
            leftIbises = new ArrayList<IbisIdentifier>();
            diedIbises = new ArrayList<IbisIdentifier>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
        }

//        if (capabilities.hasCapability(IbisCapabilities.SIGNALS)
//               && eventHandler == null) {
//            signals = new ArrayList<String>();
//        } else {
//            signals = null;
//        }

        if (eventHandler != null) {
            upcaller = new Upcaller(eventHandler);
        } else {
            upcaller = null;
        }

        //FIX ID
        java.util.UUID id = java.util.UUID.randomUUID();

        poolName = properties.getProperty(IbisProperties.POOL_NAME);
        scan = properties.getProperty(IbisProperties.BT_NOSCAN) == null;
        
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.POOL_NAME + " is not specified");
       }
        
        Location location = Location.defaultLocation(properties, null);
//
//        if (properties.getBooleanProperty(RegistryProperties.STATISTICS)) {
//            statistics = new Statistics(Protocol.OPCODE_NAMES);
//            statistics.setID(id.toString() + "@" + location.toString(), poolName);
//
//            long interval = properties
//                    .getIntProperty(RegistryProperties.STATISTICS_INTERVAL) * 1000;
//
//            statistics.startWriting(interval);
//        } else {
//            statistics = null;
//        }
//
//        members = new MemberSet(properties, this, statistics);
//        elections = new ElectionSet(properties, this);
//
        //connURL = conenctionURL, not advertise URL
        

        
        String connURL = "btspp://localhost:"+MYSERVICEUUID_UUID.toString() + ";name=Ibis";
        StreamConnectionNotifier streamConnNotifier = (StreamConnectionNotifier) Connector.open(connURL);
        connURL = localDevice.getRecord(streamConnNotifier).getConnectionURL(0, false);
        connURL = "btspp://" + localDevice.getBluetoothAddress() + ":1;authenticate=false;encrypt=false;master=false";
        System.out.println("Ibis id (in registry)= " + connURL);
        identifier = new IbisIdentifier(connURL, connURL.getBytes(), 
                null, location, poolName, applicationTag);

//        members.start();
//
//        boolean printMembers = properties
//                .getBooleanProperty(RegistryProperties.PRINT_MEMBERS);
//
//        if (printMembers) {
//            new MemberPrinter(members);
//        }
//
        stopped = false;
        ThreadPool.createNew(this, "pool management thread");

        logger.debug("registry for " + identifier + " initiated");
        
    }


    public void setIbisIdentifier(IbisIdentifier newId) {
        identifier = newId;
    }
    
    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    public IbisIdentifier elect(String electionName) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

//        IbisIdentifier[] candidates = elections.elect(electionName);

//        return members.getFirstLiving(candidates);
        return null;
    }

    public IbisIdentifier elect(String electionName, long timeoutMillis)
            throws IOException {
            throw new IbisConfigurationException("No election support requested");
    }

    public IbisIdentifier getElectionResult(String election) throws IOException {
    	throw new IbisConfigurationException("No election support requested");
      }

    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) throws IOException {
           throw new IbisConfigurationException("No election support requested");
    }

    public void maybeDead(ibis.ipl.IbisIdentifier suspect) throws IOException {
        try {
//            members.maybeDead((IbisIdentifier) suspect);
        } catch (ClassCastException e) {
            logger.error("illegal ibis identifier given: " + e);
        }
    }

    public void assumeDead(ibis.ipl.IbisIdentifier deceased) throws IOException {
//        try {
//            members.assumeDead((IbisIdentifier) deceased);
//        } catch (ClassCastException e) {
//            logger.error("illegal ibis identifier given: " + e);
//        }
    }

    public void signal(String signal,
            ibis.ipl.IbisIdentifier... ibisIdentifiers) throws IOException {
            throw new IbisConfigurationException("No string support requested");
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result = joinedIbises
                .toArray(new ibis.ipl.IbisIdentifier[0]);
        joinedIbises.clear();
        return result;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] result = leftIbises
                .toArray(new ibis.ipl.IbisIdentifier[0]);
        leftIbises.clear();
        return result;
    }

    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result = diedIbises
                .toArray(new ibis.ipl.IbisIdentifier[0]);
        diedIbises.clear();
        return result;
    }

    public synchronized String[] receivedSignals() {
//        if (signals == null) {
//            throw new IbisConfigurationException(
//                    "Registry downcalls not configured");
//        }
//
//       String[] result = signals.toArray(new String[0]);
//       signals.clear();
//       return result;
        return new String[0];    	
    }

    public int getPoolSize() {
        throw new IbisConfigurationException(
                "getPoolSize not supported by gossip registry");
    }

    public synchronized void waitUntilPoolClosed() {
        throw new IbisConfigurationException(
                "waitForAll not supported by gossip registry");

    }

    public void enableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.enableEvents();
    }

    public void disableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.disableEvents();
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        throw new IbisConfigurationException(
                "Sequence numbers not supported by" + "gossip registry");
    }

    public Map<String, String> managementProperties() {
        // no properties (as of yet)
        return new HashMap<String, String>();
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        String result = managementProperties().get(key);

        if (result == null) {
            throw new NoSuchPropertyException(key + " is not a valid property");
        }
        return result;
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "Bt registry does not have any properties that can be set");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "Bt registry does not have any properties that can be set");
    }

    public void printManagementProperties(PrintStream stream) {
        // NOTHING
    }

//    // functions called by pool to tell the registry an event has occured
//
//    synchronized void ibisJoined(IbisIdentifier ibis) {
//        if (joinedIbises != null) {
//            joinedIbises.add(ibis);
//        }
//
//        if (upcaller != null) {
//            upcaller.ibisJoined(ibis);
//        }
//    }
//
//    synchronized void ibisLeft(IbisIdentifier ibis) {
//        if (leftIbises != null) {
//            leftIbises.add(ibis);
//        }
//
//        if (upcaller != null) {
//            upcaller.ibisLeft(ibis);
//        }
//    }
//
//    synchronized void ibisDied(IbisIdentifier ibis) {
//        if (diedIbises != null) {
//            diedIbises.add(ibis);
//        }
//
//        if (upcaller != null) {
//            upcaller.ibisDied(ibis);
//        }
//    }
//
//    synchronized void signal(String signal, IbisIdentifier source) {
//        if (signals != null) {
//            signals.add(signal);
//        }
//
//        if (upcaller != null) {
//            upcaller.signal(signal, source);
//        }
//    }
//
//    synchronized void electionResult(String name, IbisIdentifier winner) {
//        if (upcaller != null) {
//            upcaller.electionResult(name, winner);
//        }
//    }
//
    public boolean isStopped() {
        return stopped;
    }

    public String getPoolName() {
        return poolName;
    }

    @Override
    public void leave() throws IOException {
        logger.debug("leaving: setting stopped state");
        synchronized (this) {
            stopped = true;
//            notifyAll();
        }
//        logger.debug("leaving: telling pool we are leaving");
//        members.leave(identifier);
//        members.leave();
//
//        // logger.debug("leaving: broadcasting leave");
//        commHandler.broadcastLeave();
//         
//        logger.debug("leaving: writing statistics");
//        if (statistics != null) {
//            statistics.write();
//            statistics.end();
//        }
        logger.debug("leaving: done!");
    }

    public void run() {
        long interval = properties
                .getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 5000;

  
         
        while (!isStopped()) {
        	if(scan){
        	System.out.println("BtRegistry scanning devices");
        	btInitiateDeviceSearch();
            synchronized (inquiryCompletedEvent) {
                try {
                	inquiryCompletedEvent.wait();
                } catch (InterruptedException e) {
                    System.out.println("wait interrupted...");
                }
            }        	

        	btInitiateServiceSearch();        	        
        	Set<RemoteDevice> devs = scannedDevices.keySet();
        	for (int i=0; i<devs.size(); i++) {
                RemoteDevice rd = (RemoteDevice)devs.toArray()[i];
                if(!joinedDevices.contains(rd)){
                	joinedDevices.add(rd);
                	if(scannedDevices.get(rd) == null || joinedIbises == null)
                		System.out.println("NULL IN SCANNED DEVS?!?!");
                	joinedIbises.add(new IbisIdentifier(scannedDevices.get(rd),
                			scannedDevices.get(rd).getBytes(),
                			null,null,poolName,null)); 
                }        	
        	}
        	}
        	
        	try{
        	Thread.sleep((long)(5000 * Math.random()));
        	}catch(Exception e){}
        }
        
    }

    // DiscoveryListener Callbacks ///////////////////////
    /**
     * deviceDiscovered() is called by the DiscoveryAgent when
     * it discovers a device during an inquiry.
     */
    public void deviceDiscovered(
            javax.bluetooth.RemoteDevice remoteDevice,
            javax.bluetooth.DeviceClass deviceClass) {
    	if(remoteDevice.getBluetoothAddress().equals("00116758A92E") || 
    			remoteDevice.getBluetoothAddress().equals("0016CB28D391")){
    		System.out.print("D");
    		discoveredDevices.add(remoteDevice);
    	}
    }

    public void inquiryCompleted(int type) {
        int i, s;
        s = discoveredDevices.size();
        if (s > 0) {
            for (i=0; i<s; i++) {
                RemoteDevice rd = (RemoteDevice) 
                    discoveredDevices.elementAt(i);
                if(!remoteDevices.contains(rd)){
                    remoteDevices.add(rd);
                }
            }
        }
        synchronized(inquiryCompletedEvent){
            inquiryCompletedEvent.notifyAll();
        }
    }

    /**
     * btInitiateDeviceSearch() kicks off the device discovery
     */
    public void btInitiateDeviceSearch() {
        int i, s;
        
        remoteDevices.clear();
        discoveredDevices.clear();
        
        RemoteDevice[] cachedDevices =
            discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
        if (cachedDevices != null) {
            s = cachedDevices.length;
            for (i=0; i<s; i++) {
                remoteDevices.add(cachedDevices[i]);
            }
        }
        
        RemoteDevice[] preknownDevices =
            discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
        if (preknownDevices != null) {
            s = preknownDevices.length;
            for (i=0; i<s; i++) {
                remoteDevices.add(preknownDevices[i]);
            }
        }
        
        boolean inquiryStarted = false;
        try {
            inquiryStarted = 
                discoveryAgent.startInquiry(
                    DiscoveryAgent.GIAC, this);
        } catch(BluetoothStateException bse) {
            System.out.println("Inquiry Failed");
            return;
        }

        if (inquiryStarted != true) {
            System.out.println("Inquiry Failed");
        }
    }

    /**
     * btInitiateServiceSearch() kicks off the service discovery
     */
    public void btInitiateServiceSearch() {
        int s, i;
        UUID[] uuidSet = new UUID[1];
        uuidSet[0] = new UUID("2d26618601fb47c28d9f10b8ec891363", false);
        int[] attrSet = {0x0100, 0x0002};  
        
        discoveredServices.clear();

        // Initiate the service search on the remote device
        s = remoteDevices.size();
        if (s>0){
            for (i=0; i<s; i++) {
                RemoteDevice rd = (RemoteDevice) remoteDevices.elementAt(i);
                try {
                	discoveryAgent.searchServices(attrSet, uuidSet, rd, (DiscoveryListener)this);
                    synchronized (serviceSearchSemaphore) {
                        serviceSearchSemaphore.wait();
                    }
                    
                    if(discoveredServices.size()>0){
                    	scannedDevices.put(rd, discoveredServices.elementAt(0) );
                    }
                } catch (InterruptedException ie) {
                    // Ignore
                } catch (BluetoothStateException bse) {
                    // ...
                    System.err.println("Service Search Failed");
                    return;
                }
            }
        }
    }
    
    public void servicesDiscovered(int transID,
            javax.bluetooth.ServiceRecord[] serviceRecord) {
    	if(serviceRecord != null){
    		System.out.print("S");    		
    		for(int i=0;i<serviceRecord.length;i++)
    			discoveredServices.add(serviceRecord[i].getConnectionURL(0, false));
    	}
   }
    public void serviceSearchCompleted
            (int transID, int responseCode) {
        synchronized (serviceSearchSemaphore) {
            serviceSearchSemaphore.notifyAll();
        }
    }

    
    public boolean hasTerminated() {
        throw new IbisConfigurationException(
                "gossip registry does not support termination");
    }

    public boolean isClosed() {
        throw new IbisConfigurationException(
                "gossip registry does not support closed world");
    }

    public void terminate() throws IOException {
        throw new IbisConfigurationException(
                "gossip registry does not support termination");
    }

    public ibis.ipl.IbisIdentifier waitUntilTerminated() {
        throw new IbisConfigurationException(
                "gossip registry does not support termination");
    }
    
    @Override
    public IbisIdentifier getRandomPoolMember() {
        throw new IbisConfigurationException(
        "gossip registry does not support random member selection");
    }    
}

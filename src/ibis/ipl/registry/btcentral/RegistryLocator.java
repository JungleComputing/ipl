package ibis.ipl.registry.btcentral;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.bluetooth.*;

public class RegistryLocator {

		
	public static final Vector<String> serviceFound = new Vector<String>();

	public static String findAny(String uuid) throws IOException, InterruptedException {
        UUID serviceUUID = new UUID(uuid, false);    	
        long start = System.currentTimeMillis();
    	String serv = LocalDevice.getLocalDevice().getDiscoveryAgent().selectService(serviceUUID, 
    									ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
    	//System.out.println("Regsitry inquiry took: " + (System.currentTimeMillis()  - start) + "ms");
   		return serv;    		
	}
    
	public static Map<String, String> findAll(String uuid) throws IOException, InterruptedException {

        UUID serviceUUID = new UUID(uuid, false);    	
        final Object serviceSearchCompletedEvent = new Object();
        UUID[] searchUuidSet = new UUID[] { serviceUUID };
        DiscoveryAgent discoveryAgent = LocalDevice.getLocalDevice().getDiscoveryAgent();
        long start = System.currentTimeMillis();
        
        Vector<RemoteDevice> devicesDiscovered = RemoteDeviceDiscovery.getDevices();
        
        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {}
            public void inquiryCompleted(int discType) {}

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                for (int i = 0; i < servRecord.length; i++) {
                    String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url != null)
                    	serviceFound.add(url);
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
                synchronized(serviceSearchCompletedEvent){
                    serviceSearchCompletedEvent.notifyAll();
                }
            }
        };

        HashMap<String, String> ret = new HashMap<String, String>();
        for(Enumeration<RemoteDevice> en = devicesDiscovered.elements(); en.hasMoreElements(); ) {        	
        	serviceFound.clear();
        	RemoteDevice btDevice = (RemoteDevice)en.nextElement();
            //System.out.println("Scanning device " + btDevice.getBluetoothAddress());
            synchronized(serviceSearchCompletedEvent) {
            	discoveryAgent.searchServices(null, searchUuidSet, btDevice, listener);
                serviceSearchCompletedEvent.wait();
                if(serviceFound.size() != 0)
                	ret.put(btDevice.getBluetoothAddress(),(String)serviceFound.elementAt(0));
            }
        }
        System.out.println("Regsitry inquiry took: " + (System.currentTimeMillis()  - start) + "ms");
       	return ret;
    }

}

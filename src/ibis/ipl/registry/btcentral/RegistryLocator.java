package ibis.ipl.registry.btcentral;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.bluetooth.*;

public class RegistryLocator {

		
	public static final Vector<String> serviceFound = new Vector<String>();

    public static String find(String uuid) throws IOException, InterruptedException {

        // First run RemoteDeviceDiscovery and use discoved device
        RemoteDeviceDiscovery.main(null);

        serviceFound.clear();

        UUID serviceUUID = new UUID(uuid, false);

        final Object serviceSearchCompletedEvent = new Object();

        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
            }

            public void inquiryCompleted(int discType) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                for (int i = 0; i < servRecord.length; i++) {
                    String url = servRecord[i].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                    if (url == null) {
                        continue;
                    }
                    serviceFound.add(url);
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
                synchronized(serviceSearchCompletedEvent){
                    serviceSearchCompletedEvent.notifyAll();
                }
            }

        };

        UUID[] searchUuidSet = new UUID[] { serviceUUID };
        int[] attrIDs =  new int[] {
        		0x0001,
        		0x0002,
        		0x0003,
                0x0100 // Service name                
        };

        for(Enumeration<RemoteDevice> en = RemoteDeviceDiscovery.devicesDiscovered.elements(); en.hasMoreElements(); ) {        	
        	serviceFound.clear();
        	RemoteDevice btDevice = (RemoteDevice)en.nextElement();
            System.out.println("Scanning device " + btDevice.getBluetoothAddress());
            synchronized(serviceSearchCompletedEvent) {
                LocalDevice.getLocalDevice().getDiscoveryAgent().searchServices(attrIDs, searchUuidSet, btDevice, listener);
                serviceSearchCompletedEvent.wait();
                if(serviceFound.size() != 0)
                	return (String)serviceFound.elementAt(0);                
            }
        }
       	return null;
    }

}

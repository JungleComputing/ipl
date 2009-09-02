package ibis.ipl.registry.btcentral;

import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;

class RemoteDeviceDiscoveryListener implements  DiscoveryListener{
	
	Vector<RemoteDevice> devicesDiscovered = new Vector<RemoteDevice>();
	Object lock;
		
	public RemoteDeviceDiscoveryListener(Object lock){
		this.lock = lock;
	}
	
	public Vector<RemoteDevice> getResult(){
		return devicesDiscovered; 
	}
	
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        devicesDiscovered.addElement(btDevice);
    }

    public void inquiryCompleted(int discType) {
        synchronized(lock){
        	lock.notifyAll();
        }
    }

    public void serviceSearchCompleted(int transID, int respCode) {}
    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}
}


public class RemoteDeviceDiscovery {
	
    public static synchronized Vector<RemoteDevice> getDevices() throws IOException, InterruptedException {


      	//Vector<RemoteDevice> devicesDiscovered = new Vector<RemoteDevice>();
        final Object inquiryCompletedEvent = new Object();

        //devicesDiscovered.clear();
       /* DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                devicesDiscovered.addElement(btDevice);
            }

            public void inquiryCompleted(int discType) {
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {}
            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}
        };*/
        
        RemoteDeviceDiscoveryListener listener = new RemoteDeviceDiscoveryListener(inquiryCompletedEvent);
        synchronized(inquiryCompletedEvent) {    
            boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
            if (started)
                inquiryCompletedEvent.wait();            
        }
        return listener.getResult();
    }

}

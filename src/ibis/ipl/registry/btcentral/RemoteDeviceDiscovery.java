package ibis.ipl.registry.btcentral;

import java.io.IOException;
import java.util.Vector;
import javax.bluetooth.*;

/**
 * Minimal Device Discovery example.
 */
public class RemoteDeviceDiscovery {

    public static final Vector/*<RemoteDevice>*/ devicesDiscovered = new Vector();

    public static void main(String[] args) throws IOException, InterruptedException {

        final Object inquiryCompletedEvent = new Object();

        devicesDiscovered.clear();

        DiscoveryListener listener = new DiscoveryListener() {

            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                devicesDiscovered.addElement(btDevice);
            }

            public void inquiryCompleted(int discType) {
                synchronized(inquiryCompletedEvent){
                    inquiryCompletedEvent.notifyAll();
                }
            }

            public void serviceSearchCompleted(int transID, int respCode) {
            }

            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
            }
        };

        synchronized(inquiryCompletedEvent) {
            boolean started = LocalDevice.getLocalDevice().getDiscoveryAgent().startInquiry(DiscoveryAgent.GIAC, listener);
            if (started) {
                inquiryCompletedEvent.wait();
                //System.out.println(devicesDiscovered.size() +  " device(s) found");
            }
        }
    }

}

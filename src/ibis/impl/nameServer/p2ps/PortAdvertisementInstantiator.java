package ibis.impl.nameServer.p2ps;

import org.jdom.Element;
import p2ps.discovery.Advertisement;
import p2ps.discovery.AdvertisementInstantiator;
import p2ps.peer.Peer;

import java.io.IOException;

public class PortAdvertisementInstantiator implements AdvertisementInstantiator {
    /**
     * @return the type of advertisement instantiated
     */
    public String getType() {
        return PortAdvertisement.TYPE;
    }

    /**
     * @return a advertisement generated from the specified document
     */
    public Advertisement createAdvertisement(Object envelope, Peer peer) 
    	throws IOException {
        if (!(envelope instanceof Element))
            throw (new RuntimeException("Invalid Envelope: JDOM Element expected"));

        return new PortAdvertisement((Element) envelope);
    }

    /**
     * @return a new advertisement instance
     */
    public Advertisement newAdvertisement(String peerid, String advertid) 
    throws IOException {
        return new PortAdvertisement(advertid, peerid);
    }

}
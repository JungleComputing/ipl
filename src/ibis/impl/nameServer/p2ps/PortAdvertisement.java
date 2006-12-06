package ibis.impl.nameServer.p2ps;

import org.apache.log4j.Logger;
import org.jdom.Element;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.io.IOException;

/**
 * An implementation of the Port Advertisement
 *
 * @author Nick Palmer
 * @version $Revision: 1.15 $
 * @created 27 Sept. 2006
 * @date $Date: 2006/05/11 15:43:34 $ modified by $Author: spxinw $
 */

public class PortAdvertisement extends BaseAdvertisement implements PortTags {

	public static final String TYPE = TYPE_TAG + ADVERTISEMENT_TYPE;
	
    private static Logger logger
    = ibis.util.GetLogger.getLogger(PortAdvertisement.class.getName());

    private String portName;
    private ReceivePortIdentifier portId;    
    
    public PortAdvertisement(String advertId, String peerId) {
    	super(advertId, peerId);
    }

    public PortAdvertisement(Element root) throws IOException {
    	super(root);
        
    	Element elem;
    	
        elem = root.getChild(PORT_NAME_TAG);
        if (elem != null)
            portName = elem.getText();
        
        elem = root.getChild(PORT_ID_TAG);
        if (elem != null && elem.getText() != null) {
        	try {
        		portId = (ReceivePortIdentifier)Base64.decodeToObject(elem.getText());
        	}
        	catch (Exception e) {
        		logger.error("Unable to unserialize Receive Port Instance!");
        	}
        }
    }


    /**
     * @return the type for this advertisement
     */
    public String getType() {
        return TYPE;
    }

    /**
     * @return the name of the port
     */
    public String getPortName() {
        return portName;
    }

    /**
     * Sets the name of the port
     */
    public void setPortName(String portName) {
        this.portName = portName;
    }

    /**
     * @return the ReceivePortIdentifier
     */
    public ReceivePortIdentifier getPortId() {
        return portId;
    }

    /**
     * Sets the IbisIdentifier
     */
    public void setPortId(ReceivePortIdentifier id) {
        portId = id;
    }
    
    /**
     * Output the advert as an xml document
     */
    public Object getXMLAdvert() throws IOException {
        Element root = super.getXMLAdvert(TYPE);

        Element elem;
        
        elem = new Element(PORT_NAME_TAG);
        elem.addContent(portName);
        root.addContent(elem);
   
        elem = new Element(PORT_ID_TAG);
        elem.addContent(Base64.encodeObject(portId));
        root.addContent(elem);        
        
        return root;
    }

}

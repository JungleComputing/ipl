package ibis.impl.nameServer.p2ps;

import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.*;
import ibis.ipl.*;

/**
 * An implementation of the Port Query
 *
 * @author Nick Palmer
 * @version $Revision: 1.11 $
 * @created 27 Sept. 2006
 * @date $Date: 2006/05/11 15:43:34 $ modified by $Author: spxinw $
 */

public class PortQuery extends BaseQuery implements PortTags {
	public static final String TYPE = TYPE_TAG + QUERY_TYPE;

    private String queryPortName;

    private static Logger logger
    = ibis.util.GetLogger.getLogger(PortQuery.class.getName());    
    
    public PortQuery(String advertId, String peerId) {
    	super(advertId, peerId);
    }

    public PortQuery(Element root) {
    	super(root);
    	
        Element elem;

        elem = root.getChild(PORT_NAME_TAG);
        if (elem != null)
            queryPortName = elem.getText();
    }

    /**
     * @return the type for this advertisement
     */
    public String getType() {
        return TYPE;
    }

    /**
     * @return the type of advertisement this query is interested in (e.g.
     *         PipeAdvertisement)
     */
    public String getQueryType() {
        return PortAdvertisement.TYPE;
    }

    /**
     * @return the id of the port this query is interested in (null if any)
     */
    public String getQueryPortName() {
        return queryPortName;
    }

    /**
     * Sets the id of the port this query is interested in (null if any)
     */
    public void setQueryPortName(String portName) {
        this.queryPortName = portName;
    }

    /**
     * Output the advert as an xml document
     */
    public Object getXMLAdvert() throws IOException {
        Element root = super.getXMLAdvert(TYPE);

        Element elem = new Element(QUERY_TAG);
        elem.addContent(PortAdvertisement.TYPE);
        root.addContent(elem);
        
        if (queryPortName != null) {
            elem = new Element(PORT_NAME_TAG);
            elem.addContent(queryPortName);
            root.addContent(elem);
        }

        org.jdom.output.XMLOutputter op = new org.jdom.output.XMLOutputter();
        
        logger.debug("Advert XML is:" + op.outputString(root));
        
        return root;
    }

}

package ibis.impl.nameServer.p2ps;

import org.jdom.Element;

import p2ps.discovery.Query;

import java.util.List;

public abstract class BaseQuery extends BaseAdvertisement implements Query {

    private String[] replyuri = new String[0];
    
    private String queryPeerId;
	
    protected BaseQuery(String advertId, String peerId) {
    	super(advertId, peerId);
    }
    
    protected BaseQuery(Element root) {
    	super(root);
    	
        if (queryPeerId != null) {
            Element elem = new Element(QUERY_PEER_ID_TAG);
            elem.addContent(queryPeerId);
            root.addContent(elem);
        }

        List list = root.getChildren(REPLY_URI_TAG);
        Element[] elems = (Element[]) list.toArray(new Element[list.size()]);
        replyuri = new String[elems.length];

        for (int count = 0; count < elems.length; count++)
            replyuri[count] = elems[count].getText();
    }

    /**
     * @return the id of the peer this query is interested in (null if any)
     */
    public String getQueryPeerID() {
        return queryPeerId;
    }

    /**
     * Sets the id of the peer this query is interested in (null if any)
     */
    public void setQueryPeerID(String id) {
        queryPeerId = id;
    }

    /**
     * @return optional endpoint uri for the query reply.
     */
    public String[] getReplyURIs() {
        return replyuri;
    }
    
    /**
     * Ssets the optional endpoint uri for the query reply.
     */
    public void setReplyURIs(String[] replyuri) {
        this.replyuri = replyuri;
    }
    
    protected Element getXMLAdvert(String type) {
        Element root = super.getXMLAdvert(type);

        Element elem = new Element(QUERY_PEER_ID_TAG);
        elem.addContent(queryPeerId);
        root.addContent(elem);
        
        for (int count = 0; count < replyuri.length; count++) {
            elem = new Element(REPLY_URI_TAG);
            elem.addContent(replyuri[count]);
            root.addContent(elem);
        }
        
        return root;
    }
}
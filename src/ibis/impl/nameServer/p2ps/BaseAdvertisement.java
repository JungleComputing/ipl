package ibis.impl.nameServer.p2ps;

import org.jdom.Element;

import p2ps.discovery.Advertisement;

public abstract class BaseAdvertisement implements BaseTags, Advertisement {
    private String advertId;
    private String peerId;
    
    protected BaseAdvertisement(String advertId, String peerId) {
    	this.advertId = advertId;
    	this.peerId = peerId;
    }
    
    protected BaseAdvertisement(Element root) {
        Element elem = root.getChild(ADVERT_ID_TAG);
        if (elem != null)
            advertId = elem.getText();

        elem = root.getChild(PEER_ID_TAG);
        if (elem != null)
            peerId = elem.getText();
    }
    
	private BaseAdvertisement() { }
	
	public String getAdvertID() {
        return advertId;
	}
	
	
	public String getPeerID() {
		return peerId;
	}

    /**
     * Sets the peer id for this object
     */
    public void setPeerID(String id) {
        this.peerId = id;
    }
    
    protected Element getXMLAdvert(String type) {
        Element root = new Element(type);
        
        Element elem = new Element(ADVERT_ID_TAG);
        elem.addContent(getAdvertID());
        root.addContent(elem);

        elem = new Element(PEER_ID_TAG);
        elem.addContent(getPeerID());
        root.addContent(elem);
        
        return root;
    }
}
package ibis.impl.nameServer.p2ps;

public interface BaseTags {
	public static final String QUERY_TYPE = "Query";
	public static final String ADVERTISEMENT_TYPE = "Advertisement";

    public static final String QUERY_START_TAG = "q";
    public static final String ADVERTISEMENT_START_TAG = "a";
    
    public static final String QUERY_PEER_ID_TAG = QUERY_START_TAG + "QueryPeerId";
}
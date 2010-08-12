package ibis.ipl.impl.stacking.sns;

public final class SNSProperties {
	
    public static final String PREFIX = "ibis.sns.";
    
    public static final String IMPLEMENTATION = PREFIX + "implementation";
    public static final String APPLICATION_NAME = PREFIX + "applicationname";

    public static final String FACEBOOK = "facebook";
	public static final String SESSIONKEY = PREFIX + FACEBOOK + ".sessionkey";
	public static final String UID = PREFIX + FACEBOOK + ".uid";
	public static final String SECRET = PREFIX + FACEBOOK + ".secret";
	
	public static final String HYVES = "hyves";
		
	public static final String AUTH_MSG = "AUTHENTICATION";
	public static final String KEY_EXCHANGE_MSG = "KEY_EXCHANGE";
 
	public static final String KEYSTORE_NAME = PREFIX + "keystorename";
	public static final String KEYSTORE_PASSWORD= PREFIX + "keystorepassword";
	public static final String KEYSTORE_ALIAS = PREFIX + "keystorealias";
}
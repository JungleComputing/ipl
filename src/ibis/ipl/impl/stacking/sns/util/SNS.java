package ibis.ipl.impl.stacking.sns.util;

/**
 * An interface to build SNS object that is created using {@link ibis.ipl.impl.stacking.sns.SNSFactory}.
 * 
 */

public interface SNS {

	/**
	 * Returns the name of the SNS Object
	 * 
	 */
	public String Name();
	
	/**
	 * Returns the userID that is used for logging at the SNS site
	 * 
	 */	
	public String UserID();
	
	/**
	 * Returns the uniqueID that identifies the SNS object.
	 * The uniqueID is different in for every SNS objects.
	 */
	public String UniqueID();
	
	public boolean isFriend(String uid);
	
	public void sendMessage(String UID, String content);
	public void sendMessage(String[] UID, String content);
	public void sendMessage(String UID, String title, String content);
	public void sendMessage(String[] UID, String title, String key);
	
	public String readMessage(String UID);
	public String readMessage(String UID, String title);
	
}

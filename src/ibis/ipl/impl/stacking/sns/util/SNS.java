package ibis.ipl.impl.stacking.sns.util;

public interface SNS {

	public String SNSName();
	public String SNSUID();
	
	public boolean isFriend(String uid);	
	public void sendAuthenticationRequest(String UID, String key);
	public String getAuthenticationRequest(String UID);
	public void sendSecretKey(String otherUID, String string);
	public String getSecretKey(String otherUID);
}

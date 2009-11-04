package ibis.ipl.impl.stacking.sns.util;

public interface SNS {
	public boolean isAuthenticated();
	public String getAuthenticationRequest(String uid);
	public String snsName();
	public String userID();
	public boolean isFriend(String uid);
	public void sendAuthenticationRequest(String uid, String key);
}

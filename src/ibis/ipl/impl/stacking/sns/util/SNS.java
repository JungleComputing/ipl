package ibis.ipl.impl.stacking.sns.util;

public interface SNS {
	public boolean getSession();
	public boolean isAuthenticated();
	public String getAuthenticationRequest();
	public void sendAuthenticationRequest(String key);
	public String snsName();
	public String userID();
	public boolean isFriend(String string);
}

package ibis.ipl.impl.stacking.sns.util;

import ibis.ipl.IbisIdentifier;

public interface SNS {
	public boolean getSession();
	public boolean isAuthenticated();
	public IbisIdentifier[] getAuthenticationRequest();
	public void sendAuthenticationRequest(IbisIdentifier SNSIbisIdent);
	public String name();
	public String username();
	public boolean isFriend(String string);
}

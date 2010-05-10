package ibis.ipl.impl.stacking.sns.hyves;

import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.impl.stacking.sns.SNSProperties;
import ibis.ipl.impl.stacking.sns.util.SNS;

import java.util.Properties;

public class Hyves implements SNS{

	public Hyves(Properties properties) throws IbisCreationFailedException {
        
			throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
		
	}
	
	@Override
	public String Name() {
		return SNSProperties.HYVES;
	}

	@Override
	public String UniqueID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String UserID() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isFriend(String uid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String readMessage(String UID) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void sendMessage(String UID, String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String readMessage(String UID, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessage(String[] UID, String content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendMessage(String UID, String title, String content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendMessage(String[] UID, String title, String key) {
		// TODO Auto-generated method stub
		
	}
}

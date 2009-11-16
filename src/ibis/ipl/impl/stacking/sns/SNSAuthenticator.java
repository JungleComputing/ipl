package ibis.ipl.impl.stacking.sns;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.stacking.sns.util.SNS;

public class SNSAuthenticator implements Runnable{
	IbisIdentifier applicantID;
	SNSIbis ibis;
	RegistryEventHandler handler;
	
	
	public SNSAuthenticator(IbisIdentifier id, SNSIbis ibis, RegistryEventHandler h) {
		this.applicantID = id; 
		this.ibis = ibis;
		this.handler = h;		
	}

	@Override
	public void run() {
		String applicantSNSID = applicantID.tagAsString();
		
		String[] snsIDPairs = applicantSNSID.split(",");
		
		for(String snsIDPair : snsIDPairs) {
			String[] pair = snsIDPair.split(":");
			String snsName = pair[0];
			String snsUID = pair[1];
			String snsKey = pair[2];
			if(ibis.snsImplementations.containsKey(snsName)) {
				SNS sns = ibis.snsImplementations.get(snsName);
				System.out.println("SNSIbis : Found matched SNS name : " + snsName);				
				System.out.println("SNSIbis : isFriends ? : " + sns.isFriend(snsUID));
				
				if (sns.isFriend(snsUID)){
					if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
						//TO BE DONE
					}		
					else if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY)) {
						String uniqueKey = ibis.snsUniqueKeys.get(snsName);
						
						System.out.println("SNSIbis : " + snsUID + " --- " + uniqueKey);
						sns.sendAuthenticationRequest(snsUID, uniqueKey);
												
						//wait until key is received
						String applicantKey = null;
						int retry = 0;
						do {
							applicantKey = sns.getAuthenticationRequest(snsUID);							
														
							try {
								this.wait(10000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							retry = retry++;
						}
						while (applicantKey == null && retry < 3);
							
						if(applicantKey == snsKey)  {
							returnCallBack();						
							break;
						}
						
						//result = true;

					}
					else if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)){												
						returnCallBack();
						break;
					}
				}
			}			
		}
	}
	
	public void returnCallBack() {
		ibis.allowedIbisIdent.add(applicantID);
		
	    if (handler != null) {
	    	handler.joined(applicantID);
	    }
	}
}

/*
public boolean SNSApplicationTagCheck(IbisIdentifier applicantID) throws IOException{
	boolean result = false;
	
	String applicantSnsID = applicantID.tagAsString();		
	System.out.println("SNSIbis : applicantSnsID = " + applicantSnsID);
	
	String[] snsIDPairs = applicantSnsID.split(",");
	
	for(String snsIDPair : snsIDPairs) {
		String[] pair = snsIDPair.split(":");
		String snsName = pair[0];
		String snsUID = pair[1];
		String snsKey = pair[2];
		
		if(snsImplementations.containsKey(snsName)) {
			SNS sns = snsImplementations.get(snsName);
			System.out.println("SNSIbis : Found matched SNS name : " + snsName);				
			System.out.println("SNSIbis : isFriends ? : " + sns.isFriend(snsUID));
			
			if (sns.isFriend(snsUID)){					
				
				
				if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
					
					Base64 base = new Base64();
					String groupKey;	
					byte[] encodedKey;

					IbisIdentifier masterID = mIbis.registry().getElectionResult("Master", 10000);
					while (masterID == null) {
						masterID = mIbis.registry().elect("Master");
					}
					
					if (mIbis.identifier() == masterID) {
						SecretKey key = crypto.getSecretKey();
						encodedKey = key.getEncoded();							

						groupKey = Base64.encodeBase64String(encodedKey);
						
						sns.sendAuthenticationRequest(snsUID, groupKey);
					}
					else {
						//wait until key is received							
						int retry = 0;
						do {
							groupKey = sns.getAuthenticationRequest(snsUID);							
														
							try {
								wait(10000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							retry = retry++;
						}
						while (groupKey == null && retry < 3);
					}
				
					if(groupKey != null) {
						encodedKey = Base64.decodeBase64(groupKey);
						
						crypto.initialize(encodedKey);
						key = crypto.getSecretKey();

						result = true;
						break;
					}
					
				}		
				else if (capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY)) {
					String uniqueKey = snsUniqueKeys.get(snsName);
					
					System.out.println("SNSIbis : " + snsUID + " --- " + uniqueKey);
					sns.sendAuthenticationRequest(snsUID, uniqueKey);
					
					
					
					/*
					//wait until key is received
					String applicantKey = null;
					int retry = 0;
					do {
						applicantKey = sns.getAuthenticationRequest(snsUID);							
													
						try {
							wait(10000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						retry = retry++;
					}
					while (applicantKey == null && retry < 3);
						
					if(applicantKey == snsKey)  {
						result = true;
						break;
					}
					
					result = true;
					break;
				}
				else if (capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)){
					result = true;
					break;
				}
			}
		}			
	}
	
	return result;
}
*/
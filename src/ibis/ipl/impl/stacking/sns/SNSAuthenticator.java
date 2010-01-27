package ibis.ipl.impl.stacking.sns;

import java.io.IOException;

import javax.crypto.SecretKey;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.stacking.sns.util.SNS;

import org.apache.commons.codec.binary.Base64;

public class SNSAuthenticator implements Runnable {
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
		        
				synchronized(sns) {
					if (true){//sns.isFriend(snsUID)){												
						if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
							String uniqueKey = ibis.snsUniqueKeys.get(snsName);

							sns.sendAuthenticationRequest(snsUID, uniqueKey);

							//wait until key is received
							String authenticationKey = null;
							
							for (int retry = 0; retry < 10; retry++) {
								authenticationKey = sns.getAuthenticationRequest(snsUID);	
								
								if(authenticationKey != null) {
									if(authenticationKey.equals(snsKey))  {
										handleGroupKey(sns, snsUID);						
										break;
									}
								}
								
								try {
									sns.wait(30000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}		
						else if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY)) {
							String uniqueKey = ibis.snsUniqueKeys.get(snsName);
							
							System.out.println("SNSIbis : " + snsUID + " --- " + uniqueKey);
							sns.sendAuthenticationRequest(snsUID, uniqueKey);

							//wait until key is received
							String authenticationKey = null;
							
							for (int retry = 0; retry < 10; retry++) {
								authenticationKey = sns.getAuthenticationRequest(snsUID);
								System.out.println("SNSAuthenticator : SNSKey " + snsKey + ", AuthKey " + authenticationKey);
								
								if(authenticationKey != null) {
									if(authenticationKey.equals(snsKey))  {
										returnCallBack();						
										break;
									}
								}
								
								try {
									sns.wait(30000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						else if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)){												
							returnCallBack();
							break;
						}
					}
				}
			}			
		}
	}

	public void handleGroupKey(SNS sns, String otherUID) {
		IbisIdentifier master = null;
		String groupKey;	
		
		try {
			master = ibis.mIbis.registry().getElectionResult("GroupKeyManager", 10000);
			while (master == null) {
				master = ibis.mIbis.registry().elect("GroupKeyManager");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (master.compareTo(ibis.identifier()) == 0) {		
			SecretKey secretKey = ibis.encryption.getSecretKey();
			groupKey = new String(Base64.encodeBase64(secretKey.getEncoded()));
			
			System.out.println("Sending key : " + groupKey + " to newly joined Ibis");
			sns.sendSecretKey(otherUID, groupKey);
			returnCallBack();
		}
		else {
			System.out.println("receiving key");
			for (int retry = 0; retry < 10; retry++) {
				String secretKeyString = sns.getSecretKey(otherUID);	
				
				if(secretKeyString != null)  {
					byte[] encodedKey = Base64.decodeBase64(secretKeyString.getBytes());	
					ibis.encryption.initialize(encodedKey);

					System.out.println("Received secret key : " + secretKeyString + " from the manager");
					
					returnCallBack();						
					break;
				}
											
				try {
					sns.wait(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}	
	}
	
	public void returnCallBack() {
		synchronized(ibis.allowedIbisIdent) {
			ibis.allowedIbisIdent.add(applicantID);
		}
		
		synchronized(handler) {
		    if (handler != null) {
		    	handler.joined(applicantID);
		    }
		}
	}	
}
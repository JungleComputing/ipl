package ibis.ipl.impl.stacking.sns;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;

import javax.crypto.SecretKey;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.stacking.sns.util.SNS;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SNSAuthenticator implements Runnable {
	IbisIdentifier applicantID;
	SNSIbis ibis;
	RegistryEventHandler handler;
	
	private static final Logger logger = LoggerFactory.getLogger(SNSAuthenticator.class);
	
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
			if(ibis.SNSList.containsKey(snsName)) {
				SNS sns = ibis.SNSList.get(snsName);
		        
				synchronized(sns) {
					
					if (sns.isFriend(snsUID)){
						
						if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)){
				            if (logger.isDebugEnabled()) {
				                logger.debug("SNSAuthenticator: selected capability " + SNSIbisCapabilities.SNS_FRIENDS_ONLY);
				            }
				            
							returnCallBack();
							break;
						}
						else {
							sns.sendMessage(snsUID, SNSProperties.AUTH_MSG, sns.UniqueID());
							
							//wait until key is received
							String authenticationKey = null;
							
							for (int retry = 0; retry < 10; retry++) {
								authenticationKey = sns.readMessage(snsUID, SNSProperties.AUTH_MSG);								
								if(authenticationKey != null) {
									
									if(authenticationKey.equals(snsKey))  {

										if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
											
								            if (logger.isDebugEnabled()) {
								                logger.debug("SNSAuthenticator: selected capability " + SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY);
								            }
										
											handleGroupKey(sns, snsUID);						
											break;
										}
										else if (ibis.capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY)) {
											
								            if (logger.isDebugEnabled()) {
								                logger.debug("SNSAuthenticator: selected capability " + SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY);
								            }
								            
											returnCallBack();						
											break;								            
										}
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
			//ibis.mIbis.keystore()
			char[] password = "password".toCharArray();
		    PasswordProtection keyStorePassword = new PasswordProtection(password);
		    SecretKeyEntry skEntry;
			try {
				skEntry = (SecretKeyEntry) ibis.mIbis.keystore().getEntry("ALIAS", keyStorePassword);
			    SecretKey secretKey = skEntry.getSecretKey();
			    
				//SecretKey secretKey = ibis.encryption.getSecretKey();
				groupKey = new String(Base64.encodeBase64(secretKey.getEncoded()));
				
				System.out.println("Sending key : " + groupKey + " to newly joined Ibis");
				sns.sendMessage(otherUID, SNSProperties.KEY_EXCHANGE_MSG, groupKey);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecoverableEntryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			

			returnCallBack();
		}
		else {
			System.out.println("receiving key");
			for (int retry = 0; retry < 10; retry++) {
				String secretKeyString = sns.readMessage(otherUID, SNSProperties.KEY_EXCHANGE_MSG);	
				
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
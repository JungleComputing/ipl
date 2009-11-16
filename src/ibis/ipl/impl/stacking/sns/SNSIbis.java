package ibis.ipl.impl.stacking.sns;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.stacking.sns.facebook.Facebook;
import ibis.ipl.impl.stacking.sns.util.SNS;
import ibis.ipl.impl.stacking.sns.util.SNSEncryption;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

public class SNSIbis implements Ibis{
    Ibis mIbis;
    PortType[] portTypes;
    IbisCapabilities capabilities;
    //SNSEncryption crypto;

	ArrayList<IbisIdentifier> allowedIbisIdent = new ArrayList<IbisIdentifier>();
	
	HashMap<String, SNS> snsImplementations = new HashMap<String,SNS>();
	HashMap<String, String> snsUserIDs = new HashMap<String,String>();
	HashMap<String, String> snsUniqueKeys = new HashMap<String,String>();
	//HashMap<IbisIdentifier, SecretKey> keyStore = new HashMap<IbisIdentifier,SecretKey>();

	//SecretKey key = null;
	String snsAppTag = null;
	
    static final IbisCapabilities SNSibisCapabilities = new IbisCapabilities(
        SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY,
        SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY,
        SNSIbisCapabilities.SNS_FRIENDS_ONLY
    );
	
	//SNSImpl snsImpl= new SNSImpl();	
	//SNSID snsIDs = new SNSID(null);
	//SNSID ApplicantSNSid = new SNSID(null);
	
    public SNSIbis(IbisFactory factory, RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, 
            PortType[] portTypes, String specifiedSubImplementation,
            SNSIbisStarter snsIbisStarter)
            throws IbisCreationFailedException {
    	
    	System.out.println("SNS Ibis initializing");
    	
    	if (specifiedSubImplementation == null) {
            throw new IbisCreationFailedException("SNSIbis: child Ibis implementation not specified");
        }
    	
        EventHandler h = null;
        if (registryEventHandler != null) {
            h = new EventHandler(registryEventHandler, this);
        }
        else {
        	h = new EventHandler(null, this);
        }
        
        //String appName = userProperties.getProperty(SNSProperties.APPLICATION_NAME);
    	
    	//if (capabilities.matchCapabilities(ibisCapabilities)){
    	if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY) ||
    		capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY) ||
    		capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)) {
    		
    		this.capabilities = capabilities;    	
	    	String[] caps = (capabilities.unmatchedCapabilities(SNSibisCapabilities)).getCapabilities();
	    	capabilities = new IbisCapabilities(caps);    	
    	}
    	else {
    		throw new IbisCreationFailedException("SNSIbis: SNS capabilities is not specified"); 
    	}
    	                
        String SNSImplementation = userProperties.getProperty(SNSProperties.IMPLEMENTATION);        
        if (SNSImplementation != null) {
            String[] snsNames = SNSImplementation.split(",");

            for (String snsName : snsNames){
            	//SNS sns = createSNS(snsName, userProperties);
            	SNS sns = null;

            	sns = SNSFactory.createSNS(snsName, userProperties);
            	
            	if(sns != null){
            		snsImplementations.put(snsName, sns);
            		
            		snsUserIDs.put(snsName, sns.userID());
            		
            		String uniquekey = UUID.randomUUID().toString();
            		snsUniqueKeys.put(snsName, uniquekey);

            		//"facebook:123456:key1,hyves:54321:key2" 
            		
            		//NEED TO PUT APPLICATION NAME
            		if (snsAppTag == null) {
            			snsAppTag = snsName + ":" + sns.userID() + ":" + uniquekey;
            		}
            		else {
            			snsAppTag = "," + snsName + ":" + sns.userID() + ":" + uniquekey;
            		}            		
            	}
            	else {
            		throw new IbisCreationFailedException("SNSIbis: SNS implementation is not found"); 
            	}
            }
        }
        else {
    		throw new IbisCreationFailedException("SNSIbis: SNS implementation is not specified"); 
    	}
        
        /*
        if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
        	crypto = new SNSEncryption();
        	crypto.initialize();        	
        }
        */        
        

        
    	//Replace application tag with SNS application tag
        mIbis = factory.createIbis(h, capabilities, userProperties, credentials, snsAppTag.getBytes(), portTypes, specifiedSubImplementation);
    }    
	
	private class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler h, SNSIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {		
			boolean result = false;
        	
			if (id == ibis.identifier()) {
				//Don't need to authenticate myself
				result = true;
			}
			else {
				/*
				try {
					result = SNSApplicationTagCheck(id);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				*/
				//SNSAuthenticator SNSAuthRun = new SNSAuthenticator(ibis, h);
				Thread SNSAuthThread = new Thread(new SNSAuthenticator(id, ibis, h));
				SNSAuthThread.start();
				
				//System.out.println("SNSIbis : Application tag check finished with result : " + result);
			}
			/*
			if (result){
				allowedIbisIdent.add(id);
			}
			
            if ((h != null) && result) {
				System.out.println("SNSIbis : callback returned");
                h.joined(id);
            }
            */
        }

        public void left(IbisIdentifier id) {
        	removeIbisSendPort(id);
            if (h != null) {
                h.left(id);
            }
        }

        public void died(IbisIdentifier id) {
        	removeIbisSendPort(id);
            if (h != null) {
                h.died(id);
            }
        }

        public void gotSignal(String s, IbisIdentifier id) {      	
            if (h != null) {
                h.gotSignal(s, id);
            }
        }

        public void electionResult(String electionName, IbisIdentifier winner) {
            if (h != null) {
                h.electionResult(electionName, winner);
            }
        }

        public void poolClosed() {
            if (h != null) {
                h.poolClosed();
            }
        }

        public void poolTerminated(IbisIdentifier source) {
            if (h != null) {
                h.poolTerminated(source);
            }
        }
    }
    /*
    public SNS createSNS(String name, Properties properties) throws IbisCreationFailedException{
    	SNS sns = null;
    	
    	//Make a factory out of this ?
    	if(name.equals("facebook")) {
    		
    		
            String sessionKey = properties.getProperty("sns.facebook.sessionkey");
    		String secretGenerated = properties.getProperty("sns.facebook.secretGenerated");
    		String uid = properties.getProperty("sns.facebook.uid");
 		
    		if (sessionKey != null && secretGenerated != null && uid != null) {    		
    			sns = new Facebook(uid, sessionKey, secretGenerated);
    		}
    		else {
    			throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
    		}
    	}
    	//else if(name.equals("hyves") {}
    	
    	return sns;
    }
    */
	public void removeIbisSendPort(IbisIdentifier ibisIdentifier) {
		allowedIbisIdent.remove(ibisIdentifier);
		System.out.println("Removing sendport from " + ibisIdentifier.toString());
	}	
   
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
						/*
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
						*/
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
						*/
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
	
    @Override
	public ReceivePort createReceivePort(PortType portType,	String portName) 
			throws IOException {
		return createReceivePort(portType, portName, null, null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,	String portName, MessageUpcall messageUpcall)
			throws IOException {
		return createReceivePort(portType, portName, messageUpcall, null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,	String portName, ReceivePortConnectUpcall receivePortConnectUpcall)
			throws IOException {
		return createReceivePort(portType, portName, null, receivePortConnectUpcall, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,	String receivePortName, MessageUpcall messageUpcall,
			ReceivePortConnectUpcall receivePortConnectUpcall, Properties properties) 
			throws IOException {

		return new SNSReceivePort(portType, this, receivePortName, messageUpcall, receivePortConnectUpcall, properties);
	}

	@Override
	public SendPort createSendPort(PortType portType) throws IOException {
        return createSendPort(portType, null, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String portName)
			throws IOException {
        return createSendPort(portType, portName, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String portName, SendPortDisconnectUpcall sendPortDisconnectUpcall, Properties properties) 
			throws IOException {

        return new SNSSendPort(portType, this, portName, sendPortDisconnectUpcall, properties);
	}

    @Override
	public void end() throws IOException {
		mIbis.end();		
	}

	@Override
	public String getVersion() {
		return "SNSIbis version : " + mIbis.getVersion();
	}

	@Override
	public IbisIdentifier identifier() {		
		return mIbis.identifier();
	}

	@Override
	public void poll() throws IOException {
		mIbis.poll();
		
	}

	@Override
	public Properties properties() {
		return mIbis.properties();
	}

	@Override
	public Registry registry() {
		return mIbis.registry();
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return mIbis.getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {		
		return mIbis.managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		mIbis.printManagementProperties(stream);		
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		mIbis.setManagementProperties(properties);		
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		mIbis.setManagementProperty(key, value);
		
	}
}

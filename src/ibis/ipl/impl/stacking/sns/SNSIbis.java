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
import ibis.ipl.impl.stacking.sns.util.SNS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starting point to create SNS Ibis
 */
public class SNSIbis implements Ibis{
    Ibis mIbis;
    PortType[] portTypes;
    IbisCapabilities capabilities, refinedcapabilities; 
  
    ArrayList<IbisIdentifier> allowedIbisIdent = new ArrayList<IbisIdentifier>();
	HashMap<String, SNS> SNSList = new HashMap<String,SNS>();

	String SNSTag;
    SNSIbisApplicationTag SNSIbisTag;
    String keystoreName;
    String keystorePassword;
    String keystoreAlias;
	
    static final IbisCapabilities SNSibisCapabilities = new IbisCapabilities(
        SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY,
        SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY,
        SNSIbisCapabilities.SNS_FRIENDS_ONLY
    );
	
    private static final Logger logger = LoggerFactory.getLogger(SNSIbis.class);
	
    public SNSIbis(IbisFactory factory, RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, 
            PortType[] portTypes, String specifiedSubImplementation,
            SNSIbisStarter snsIbisStarter)
            throws IbisCreationFailedException {
    	
    	
    	if (specifiedSubImplementation == null) {
            throw new IbisCreationFailedException("SNSIbis: child Ibis implementation not specified");
        }
    	
    	if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY) ||
        		capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY) ||
        		capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)) {
        		
    		this.capabilities = capabilities;
	    	String[] caps = (capabilities.unmatchedCapabilities(SNSibisCapabilities)).getCapabilities();
	    	refinedcapabilities = new IbisCapabilities(caps);
    	} else if ((capabilities.getCapabilitiesWithPrefix(SNSProperties.PREFIX)).getCapabilities().length > 1) {
    		throw new IbisCreationFailedException("SNSIbis: more than one SNS capabilities is specified");
    	} else {
    		throw new IbisCreationFailedException("SNSIbis: SNS capabilities is not specified"); 
    	}
    	
    	//Build SNS Objects
    	initializeSNS(userProperties, applicationTag);
        
    	//Create Ibis Instance
    	try {
			mIbis = factory.createIbis(new EventHandler(registryEventHandler, this), refinedcapabilities, userProperties, credentials, SNSIbisTag.getBytes(), portTypes, specifiedSubImplementation);
		} catch (IOException e) {
			throw new IbisCreationFailedException("SNSIbis: Failed to create SNS Ibis");
		}
		
		//Initialize Keystore 
        if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: Preparing KeyStore");
            }
            
        	keystoreName = userProperties.getProperty(SNSProperties.KEYSTORE_NAME);
        	keystorePassword = userProperties.getProperty(SNSProperties.KEYSTORE_PASSWORD);
        	keystoreAlias = userProperties.getProperty(SNSProperties.KEYSTORE_ALIAS);
        	
        	KeyStore ks;
		  	File from_file = new File(keystoreName);
		    if (!from_file.exists()) {			  	
				try {
					ks = KeyStore.getInstance("JCEKS");
				    ks.load(null, keystorePassword.toCharArray());
				    
				    KeyGenerator kg = KeyGenerator.getInstance("DES");
				    kg.init(new SecureRandom());
				    SecretKey mySecretKey = kg.generateKey();
				    
				    SecretKeyEntry skEntry = new SecretKeyEntry(mySecretKey);
				    PasswordProtection keyStorePassword = new PasswordProtection(keystorePassword.toCharArray());
				    ks.setEntry(keystoreAlias, skEntry, keyStorePassword);
				    	    
				    FileOutputStream fos = new FileOutputStream(keystoreName);
				    ks.store(fos, keystorePassword.toCharArray());
				    fos.close();
				} catch (Exception e) {
					throw new IbisCreationFailedException("SNSIbis: Failed to create new KeyStore");
				}
		    } else {
		    	try {
					ks = KeyStore.getInstance("JCEKS");
					
			    	FileInputStream fis = new FileInputStream(keystoreName);
				    ks.load(fis, keystorePassword.toCharArray());
				    fis.close();
				    
				    PasswordProtection keyStorePassword = new PasswordProtection(keystorePassword.toCharArray());
				    SecretKeyEntry skEntry = (SecretKeyEntry) ks.getEntry(keystoreAlias, keyStorePassword);
				    SecretKey key = skEntry.getSecretKey();
			    
				} catch (Exception e) {
					throw new IbisCreationFailedException("SNSIbis: Failed to load existing KeyStore");
				}
		    }
		    
		    mIbis.setKeystore(ks);
        }
        
    }    
	
    /**
     * This method initializes SNS objects based on the Ibis properties
     * @exception IbisCreationFailedException is thrown when an SNS object cannot be created.
     */
	private void initializeSNS(Properties userProperties, byte[] applicationTag ) throws IbisCreationFailedException {
        String SNSImplementation = userProperties.getProperty(SNSProperties.IMPLEMENTATION);        
        if (SNSImplementation != null) {
            String[] snsNames = SNSImplementation.split(",");

            for (String snsName : snsNames){
            	SNS sns = SNSFactory.createSNS(snsName, userProperties);
            	
            	if(sns != null){
            		SNSList.put(snsName, sns);
                  	
            		if (SNSTag == null) {
            			SNSTag = snsName + ":" + sns.UserID() + ":" + sns.UniqueID();
            		}
            		else {
            			SNSTag = "," + snsName + ":" + sns.UserID() + ":" + sns.UniqueID();
            		}            		
            	}
            	else {
            		throw new IbisCreationFailedException("SNSIbis: SNS implementation is not found"); 
            	}
            }
            
            SNSIbisTag = new SNSIbisApplicationTag(SNSTag.getBytes(), applicationTag);
            
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: SNSTag " + SNSTag);
            }            
        }
        else {
    		throw new IbisCreationFailedException("SNSIbis: SNS implementation is not specified"); 
    	}
		
	}

    /**
     * Class to handle Registry events
     * In case of join event, an SNSAuthenticator thread is created to do the authentication  
     */
	public class EventHandler implements RegistryEventHandler {
        RegistryEventHandler REHandler;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler handler, SNSIbis ibis) {
            this.REHandler = handler;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
            	logger.debug("SNSIbis: my Ibis " + ibis.identifier().name());
                logger.debug("SNSIbis: new Ibis " + id.name() + " has joined");
            }
        	
//			if (id.compareTo(ibis.identifier()) == 0) { 
			if (ibis.identifier().equals(id)) { 
				//DONT NEED TO AUTHENTICATE MYSELF	
				synchronized(allowedIbisIdent) {
					allowedIbisIdent.add(id);
				}
				
				if (REHandler != null) {
					synchronized (REHandler) {
						REHandler.joined(id);
				    }
				}
			}
			else {
				if (REHandler != null) {
					synchronized (REHandler) {
						REHandler.joined(id);
				    }
				}

				Thread SNSAuthThread = new Thread(new SNSAuthenticator(id, ibis, REHandler));
				SNSAuthThread.start();
			}
        }

        public void left(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: new Ibis " + id.tagAsString() + " has left");
            }
        	
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.remove(id);
    		}
    		
    		if (REHandler != null) {
    			synchronized (REHandler) {
    				REHandler.left(id);
	            }
    		}
        }

        public void died(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: new Ibis " + id.tagAsString() + " has died");
            }
            
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.remove(id);
    		}
   
    		if (REHandler != null) {
    			synchronized (REHandler) {
    				REHandler.died(id);
	            }
    		}
        }

        public void gotSignal(String s, IbisIdentifier id) {   
        	if (REHandler != null) {
        		synchronized (REHandler) {
        			REHandler.gotSignal(s, id);
	            }
    		}
        }

        public void electionResult(String electionName, IbisIdentifier winner) {
        	if (REHandler != null) {
        		synchronized (REHandler) {
        			REHandler.electionResult(electionName, winner);
	            }
    		}
        }

        public void poolClosed() {
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.clear();
    		}
    		
    		if (REHandler != null) {
    			synchronized (REHandler) {
    				REHandler.poolClosed();
	            }
    		}
        }

        public void poolTerminated(IbisIdentifier source) {
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.clear();
    		}
        	
    		if (REHandler != null) {
    			synchronized (REHandler) {
    				REHandler.poolTerminated(source);
	            }
    		}
        }
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
		return new SNSIbisIdentifier(mIbis.identifier());
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
		
//		USED FOR BENCHMARK
//		return new SNSRegistry(this);
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

	@Override
	public KeyStore keystore() {
		return mIbis.keystore();
	}

	@Override
	public void setKeystore(KeyStore k) {
		mIbis.setKeystore(k);
	}
}


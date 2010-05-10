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
import ibis.ipl.impl.stacking.sns.util.SNSEncryption;

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

public class SNSIbis implements Ibis{
    Ibis mIbis;
    PortType[] portTypes;
    IbisCapabilities capabilities, refinedcapabilities; 
    SNSEncryption encryption;  
  
    ArrayList<IbisIdentifier> allowedIbisIdent = new ArrayList<IbisIdentifier>();
	
	HashMap<String, SNS> SNSList = new HashMap<String,SNS>();
	//HashMap<String, String> snsUserIDs = new HashMap<String,String>();
	//HashMap<String, String> snsUniqueKeys = new HashMap<String,String>();

	String snsAppTag;
	
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
    	
        if (logger.isDebugEnabled()) {
            logger.debug("SNSIbis: Creating SNSIbis");
        }
    	
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

    	if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY) ||
    		capabilities.hasCapability(SNSIbisCapabilities.SNS_AUTHENTICATED_FRIENDS_ONLY) ||
    		capabilities.hasCapability(SNSIbisCapabilities.SNS_FRIENDS_ONLY)) {
    		
    		this.capabilities = capabilities;    	
	    	String[] caps = (capabilities.unmatchedCapabilities(SNSibisCapabilities)).getCapabilities();
	    	refinedcapabilities = new IbisCapabilities(caps);    	
    	}
    	else {
    		throw new IbisCreationFailedException("SNSIbis: SNS capabilities is not specified"); 
    	}
    	                
        String SNSImplementation = userProperties.getProperty(SNSProperties.IMPLEMENTATION);        
        if (SNSImplementation != null) {
            String[] snsNames = SNSImplementation.split(",");

            for (String snsName : snsNames){
            	SNS sns = SNSFactory.createSNS(snsName, userProperties);
            	
            	if(sns != null){
            		SNSList.put(snsName, sns);
            		
            		//snsUserIDs.put(snsName, sns.SNSUID());
            		
            		//String uniquekey = UUID.randomUUID().toString();
            		//snsUniqueKeys.put(snsName, uniquekey);
           		
            		//PUT THE SNS CREDENTIALS IN IBIS APPLICATION TAG
            		if (snsAppTag == null) {
            			snsAppTag = snsName + ":" + sns.UserID() + ":" + sns.UniqueID();
            		}
            		else {
            			snsAppTag = "," + snsName + ":" + sns.UserID() + ":" + sns.UniqueID();
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
        
        if (applicationTag != null){
        	logger.warn("SNSIbis: Applicationtag is suppressed");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("SNSIbis: SNSTag " + snsAppTag);
        }                
        
    	//PUT REFINED IBIS CAPS AND OVERWRITE IBIS APPLICATION TAG
        mIbis = factory.createIbis(h, refinedcapabilities, userProperties, credentials, snsAppTag.getBytes(), portTypes, specifiedSubImplementation);

        if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: Preparing KeyStore");
            }
        	            
            char[] password = "password".toCharArray();
        	KeyStore ks;
		  	File from_file = new File("KEYSTORE");
		    if (!from_file.exists()) {			  	
				try {
					ks = KeyStore.getInstance("JCEKS");
				    ks.load(null, password);
				    
				    KeyGenerator kg = KeyGenerator.getInstance("DES");
				    kg.init(new SecureRandom());
				    SecretKey mySecretKey = kg.generateKey();
				    
				    SecretKeyEntry skEntry = new SecretKeyEntry(mySecretKey);
				    PasswordProtection keyStorePassword = new PasswordProtection(password);
				    ks.setEntry("ALIAS", skEntry, keyStorePassword);
				    	    
				    FileOutputStream fos = new FileOutputStream("KEYSTORE");
				    ks.store(fos, password);
				    fos.close();
				} catch (Exception e) {
					throw new IbisCreationFailedException("SNSIbis: Failed to create new KeyStore");
				}
		    } else {
		    	try {
					ks = KeyStore.getInstance("JCEKS");
					
			    	FileInputStream fis = new FileInputStream("KEYSTORE");
				    ks.load(fis, password);
				    fis.close();
				    
				    PasswordProtection keyStorePassword = new PasswordProtection(password);
				    SecretKeyEntry skEntry = (SecretKeyEntry) ks.getEntry("ALIAS", keyStorePassword);
				    SecretKey key = skEntry.getSecretKey();

				} catch (Exception e) {
					throw new IbisCreationFailedException("SNSIbis: Failed to load existing KeyStore");
				}
		    }
		    
		    mIbis.setKeystore(ks);
        }
        
    }    
	
	public class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler h, SNSIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
            	logger.debug("SNSIbis: my Ibis " + ibis.identifier().tagAsString());
                logger.debug("SNSIbis: new Ibis " + id.tagAsString() + "has joined");
            }
        	
			if (id.compareTo(ibis.identifier()) == 0) { 
				//DONT NEED TO AUTHENTICATE MYSELF	
				synchronized(allowedIbisIdent) {
					allowedIbisIdent.add(id);
				}
				
				synchronized (h) {
				    if (h != null) {
				    	h.joined(id);
				    }
				}
			}
			else {
				Thread SNSAuthThread = new Thread(new SNSAuthenticator(id, ibis, h));
				SNSAuthThread.start();
			}
        }

        public void left(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: new Ibis " + id.tagAsString() + "has left");
            }
        	
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.remove(id);
    		}
    		
    		synchronized (h) {
	            if (h != null) {
	                h.left(id);
	            }
    		}
        }

        public void died(IbisIdentifier id) {
            if (logger.isDebugEnabled()) {
                logger.debug("SNSIbis: new Ibis " + id.tagAsString() + "has died");
            }
            
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.remove(id);
    		}
   
    		synchronized (h) {
	            if (h != null) {
	                h.died(id);
	            }
    		}
        }

        public void gotSignal(String s, IbisIdentifier id) {   
    		synchronized (h) {
	            if (h != null) {
	                h.gotSignal(s, id);
	            }
    		}
        }

        public void electionResult(String electionName, IbisIdentifier winner) {
    		synchronized (h) {
	            if (h != null) {
	                h.electionResult(electionName, winner);
	            }
    		}
        }

        public void poolClosed() {
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.clear();
    		}
    		
    		synchronized (h) {
	        	if (h != null) {
	                h.poolClosed();
	            }
    		}
        }

        public void poolTerminated(IbisIdentifier source) {
    		synchronized(allowedIbisIdent){
    			allowedIbisIdent.clear();
    		}
        	
    		synchronized (h) {
	        	if (h != null) {
	                h.poolTerminated(source);
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

	@Override
	public KeyStore keystore() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setKeystore(KeyStore k) {
		// TODO Auto-generated method stub		
	}
}

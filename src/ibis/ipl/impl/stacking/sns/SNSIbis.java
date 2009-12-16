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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNSIbis implements Ibis{
    Ibis mIbis;
    PortType[] portTypes;
    IbisCapabilities capabilities, refinedcapabilities;
    SNSEncryption encryption;  

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
	
    private static final Logger logger = LoggerFactory.getLogger("ibis.ipl.impl.SNSIbis");
	
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
            		snsImplementations.put(snsName, sns);
            		
            		snsUserIDs.put(snsName, sns.SNSUID());
            		
            		String uniquekey = UUID.randomUUID().toString();
            		snsUniqueKeys.put(snsName, uniquekey);

            		//"facebook:123456:key1,hyves:54321:key2" 
            		
            		//NEED TO PUT APPLICATION NAME
            		if (snsAppTag == null) {
            			snsAppTag = snsName + ":" + sns.SNSUID() + ":" + uniquekey;
            		}
            		else {
            			snsAppTag = "," + snsName + ":" + sns.SNSUID() + ":" + uniquekey;
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
        
        if (capabilities.hasCapability(SNSIbisCapabilities.SNS_ENCRYPTED_COMM_ONLY)) {
        	encryption = new SNSEncryption();
        	encryption.initialize();
        }
        
    	//Replace application tag with SNS application tag
        mIbis = factory.createIbis(h, refinedcapabilities, userProperties, credentials, snsAppTag.getBytes(), portTypes, specifiedSubImplementation);
    }    
	
	public class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler h, SNSIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {
        	System.out.println("Other Ibis" + id.tagAsString());
        	System.out.println("SNS Ibis" + ibis.identifier().tagAsString());
        	
			if (id.compareTo(ibis.identifier()) == 0) { //Don't need to authenticate myself
				
				/*
				ibis.allowedIbisIdent.add(id);
			    if (h != null) {
			    	h.joined(id);
			    }
			    */
			}
			else {
				Thread SNSAuthThread = new Thread(new SNSAuthenticator(id, ibis, h));
				SNSAuthThread.start();
			}
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

	public void removeIbisSendPort(IbisIdentifier ibisIdentifier) {
		allowedIbisIdent.remove(ibisIdentifier);
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

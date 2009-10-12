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
import ibis.ipl.impl.stacking.sns.util.SNSID;
import ibis.ipl.impl.stacking.sns.util.SNSImpl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.trilead.ssh2.log.Logger;

public class SNSIbis implements Ibis{
    Ibis mIbis;
    PortType[] portTypes;
    IbisCapabilities capabilities;

	ArrayList<IbisIdentifier> allowedIbisIdent = new ArrayList<IbisIdentifier>();
	
	HashMap<String, SNS> snsImplementations = new HashMap<String,SNS>();
	HashMap<String, String> snsUserIDs = new HashMap<String,String>();
	
	//SNSImpl snsImpl= new SNSImpl();	
	//SNSID snsIDs = new SNSID(null);
	//SNSID ApplicantSNSid = new SNSID(null);
	
	private class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler h, SNSIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {		
			boolean result = false;
        	
			if (id == mIbis.identifier()) {
				result = true;
			}
			else {
				result = SNSApplicationTagCheck(id);
			}
			
			if (result){
				allowedIbisIdent.add(id);
			}
			
            if ((h != null) && result) {
                h.joined(id);
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
	
    public SNSIbis(IbisFactory factory, RegistryEventHandler registryEventHandler,
            Properties userProperties, IbisCapabilities capabilities,
            Credentials credentials, byte[] applicationTag, 
            PortType[] portTypes, String specifiedSubImplementation,
            SNSIbisStarter snsIbisStarter)
            throws IbisCreationFailedException {
    	
    	if (specifiedSubImplementation == null) {
            throw new IbisCreationFailedException("SNSIbis: child Ibis implementation not specified");
        }
    	
    	if (applicationTag == null){
            throw new IbisCreationFailedException("SNSIbis: application tag is not specified");    		
    	}
        
        EventHandler h = null;
        if (registryEventHandler != null) {
            h = new EventHandler(registryEventHandler, this);
        }
        else {
        	h = new EventHandler(null, this);
        }
        
        String SNSImplementation = userProperties.getProperty(SNSProperties.IMPLEMENTATION);        
        if (SNSImplementation != null) {
            String[] snsNames = SNSImplementation.split(",");

            for (String snsName : snsNames){
            	SNS sns = createSNS(snsName, userProperties);
            	if(sns != null){
            		snsImplementations.put(snsName, sns);
            		
            		snsUserIDs.put(snsName, sns.userID());
            	}
            	else {
            		throw new IbisCreationFailedException("SNSIbis: SNS implementation is not found"); 
            	}
            }
        }
    	    	    	
        mIbis = factory.createIbis(h, capabilities, userProperties, credentials, applicationTag, portTypes, specifiedSubImplementation);
    }    
	
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
    
	public void removeIbisSendPort(IbisIdentifier ibisIdentifier) {
		allowedIbisIdent.remove(ibisIdentifier);
		System.out.println("Removing sendport from " + ibisIdentifier.toString());
	}	
   
	public boolean SNSApplicationTagCheck(IbisIdentifier applicantID){
		boolean result = false;
		
		//ApplicantSNSid.readByteArray(id.tag());
		//parse id.tag()
		//"facebook:123456,hyves:54321"
		String applicantSnsID = applicantID.tagAsString();
		
		System.out.println("applicantSnsID : " + applicantSnsID);
		
		String[] snsIDPairs = applicantSnsID.split(",");
		
		for(String snsIDPair : snsIDPairs) {
			String[] pair = snsIDPair.split(":");
			String snsName = pair[0];
			String snsUID = pair[1];
			
			if(snsImplementations.containsKey(snsName)) {
				SNS sns = snsImplementations.get(snsName);
				
				if (sns.isFriend(snsUID)){
					//check authentication key from SNS.
					//Get SNS authentication
					result = true;
					break;
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

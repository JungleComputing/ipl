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
import ibis.ipl.impl.stacking.sns.util.SNSID;

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
	
	SNS sns;
	SNSID LocalSNSid = new SNSID(null);
	SNSID ApplicantSNSid = new SNSID(null);
	
	private class EventHandler implements RegistryEventHandler {
        RegistryEventHandler h;
        SNSIbis ibis;

        EventHandler(RegistryEventHandler h, SNSIbis ibis) {
            this.h = h;
            this.ibis = ibis;
        }        

        public void joined(IbisIdentifier id) {		
			boolean result = false;
        	
			result = SNSApplicationTagCheck(id);

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
        	System.out.println("Getting signal");
        	
			SNSAuthenticationCheck();

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
        
        
        //this.portTypes = portTypes;    
        
        //List<PortType> requiredPortTypes = new ArrayList<PortType>();        
		//requiredPortTypes.add(new PortType(PortType.CONNECTION_UPCALLS));
		/*
        for (PortType portType: portTypes) {
            if (snsPortType(portType)) {
            	requiredPortTypes.add(portType);
            }
            else {
            	ArrayList<String> caps = portType.getCapabilities();
            	caps.
            	PortType pt = new PortType( PortType.CONNECTION_UPCALLS );
            }            
        }
        */
    	mIbis = factory.createIbis(h, capabilities,
                userProperties, credentials, applicationTag, portTypes,//requiredPortTypes.toArray(new PortType[requiredPortTypes.size()]),
                specifiedSubImplementation);
    	
    	LocalSNSid.readByteArray(mIbis.identifier().tag());
    }    

	public void addIbisSendPort(IbisIdentifier ibisIdentifier, String name)	{
		allowedIbisIdent.add(ibisIdentifier);
		System.out.println("Adding known sendport from " + ibisIdentifier.toString() + " " + name);
	}
	
	public void removeIbisSendPort(IbisIdentifier ibisIdentifier) {
		allowedIbisIdent.remove(ibisIdentifier);
		System.out.println("Removing sendport from " + ibisIdentifier.toString());
	}	
	
	public void putImplemetation(SNS sns) {
		this.sns = sns;				
	}
	/*
	public void putSNSID(SNSID SNSid) {
		this.LocalSNSid = SNSid;
	}
*/
	/*
    private static boolean snsPortType(PortType tp) {
        return (tp.hasCapability(PortType.CONNECTION_UPCALLS));
    }
	*/
	
	public void SNSAuthenticationCheck() {
		IbisIdentifier[] ibisIdentifiers = sns.getAuthenticationRequest();
		for (IbisIdentifier ibisIdentifier: ibisIdentifiers) {
			allowedIbisIdent.add(ibisIdentifier);
		}
	}
    
	public boolean SNSApplicationTagCheck(IbisIdentifier id){
		boolean result = false;
		
		ApplicantSNSid.readByteArray(id.tag());
			
		for( String SNSName : LocalSNSid.getAllSNSNames()){			
			if(ApplicantSNSid.containSNS(SNSName)) {

				//for every instance of sns
				if (sns.isFriend(ApplicantSNSid.getSNSAlias(SNSName))){
					result = true;
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
		/*
		matchPortType(portType);
        if (receivePortConnectUpcall != null
                && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
            throw new IbisConfigurationException(
                    "connection upcalls not supported by this porttype");
        }
        */
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
		/*
		matchPortType(portType);
        if (sendPortDisconnectUpcall != null
                && !portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
            throw new IbisConfigurationException(
                    "connection upcalls not supported by this porttype");
        }
		*/
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
		//return new SNSRegistry(this);
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

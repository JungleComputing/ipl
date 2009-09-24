package ibis.ipl.impl.stacking.sns;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.stacking.sns.util.SNS;

public class SNSReceivePort implements ReceivePort{
	
	final ReceivePort base;
	static SNSIbis ibis;
	
    public SNSReceivePort(PortType type, SNSIbis ibis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties)
            throws IOException {
    	
        if (connectUpcall != null) {
            connectUpcall = new ConnectUpcaller(this, connectUpcall);
        }        
        else
        	connectUpcall = new ConnectUpcaller(this);
       
        this.ibis = ibis;
       
        base = ibis.mIbis.createReceivePort(type, name, upcall, connectUpcall, properties);
    }
	
    private static final class ConnectUpcaller implements ReceivePortConnectUpcall {
		SNSReceivePort port;
		ReceivePortConnectUpcall upcaller;
		
		public ConnectUpcaller(SNSReceivePort snsReceivePort,
		        ReceivePortConnectUpcall upcaller) {
		    this.port = snsReceivePort;
		    this.upcaller = upcaller;
		}
		
		public ConnectUpcaller(SNSReceivePort snsReceivePort) {
		    this.port = snsReceivePort;
		}
		
		public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
			IbisIdentifier id = applicant.ibisIdentifier();
			
			if(ibis.allowedIbisIdent.contains(id)) {
				if(upcaller == null) {
					return true;
				}
				else {
					return upcaller.gotConnection(port, applicant);
				}
			}
			else {
				return false;					
			}
			
			/*
			
			
			System.out.println(applicantID.getSNSAlias("Facebook"));

			if(ibis.allowedSendPort.contains(id)) {
				result = true;
			}
			else {
				//GET IBIS IDENTIFIER FROM SNS
				SNSAuthenticationCheck();

				if(ibis.allowedSendPort.contains(id)) {
					result = true;
				}
				else {
					result = false;
				}
			}
			 */
			
		}
		
		public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe, Throwable reason) {
		    upcaller.lostConnection(port, johnDoe, reason);
		}
    }

	@Override
	public void close() throws IOException {
		base.close();	
	}

	@Override
	public void close(long timeoutMillis) throws IOException {
		base.close(timeoutMillis);		
	}

	@Override
	public SendPortIdentifier[] connectedTo() {
		return base.connectedTo();
	}

	@Override
	public void disableConnections() {
		base.disableConnections();		
	}

	@Override
	public void disableMessageUpcalls() {
		base.disableMessageUpcalls();		
	}

	@Override
	public void enableConnections() {
		base.enableConnections();		
	}

	@Override
	public void enableMessageUpcalls() {
		base.enableMessageUpcalls();	
	}

	@Override
	public PortType getPortType() {
		return base.getPortType();
	}

	@Override
	public ReceivePortIdentifier identifier() {
		
		return base.identifier();
	}

	@Override
	public SendPortIdentifier[] lostConnections() {
		return base.lostConnections();
	}

	@Override
	public String name() {
		return base.name();
	}

	@Override
	public SendPortIdentifier[] newConnections() {
		return base.newConnections();
	}

	@Override
	public ReadMessage poll() throws IOException {
        ReadMessage m = base.poll();
        if (m != null) {
            m = new SNSReadMessage(m, this);
        }
        return m;
	}

	@Override
	public ReadMessage receive() throws IOException {
		return receive(0);
	}

	public ReadMessage receive(long timeoutMillis) throws IOException {
		return new SNSReadMessage(base.receive(timeoutMillis), this);
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return base.getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {
		return base.managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		base.printManagementProperties(stream);
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		base.setManagementProperties(properties);
		
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		base.setManagementProperty(key, value);		
	}

}

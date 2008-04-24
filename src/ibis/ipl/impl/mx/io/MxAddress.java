package ibis.ipl.impl.mx.io;
import java.io.Serializable;

public class MxAddress implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final long nic_id;
	final String hostname;
	final int endpoint_id;

	/* hostname can be empty, Nic ID and Endpoint ID always set */	
	public MxAddress(String hostname, long nic_id, int endpoint_id) {
		this.hostname = hostname;
		this.nic_id = nic_id;
		this.endpoint_id = endpoint_id;
	}

	public MxAddress(String hostname, int endpoint_id) {
		this.hostname = hostname;
		this.nic_id = NioMx.jmx_getNicID(hostname);
		this.endpoint_id = endpoint_id;
	}
	
	public MxAddress(long nic_id, int endpoint_id) {
		this.hostname = null;
		this.nic_id = nic_id;
		this.endpoint_id = endpoint_id;
	}
	
	public String getHostname() {
		return hostname;
	}

	public int getEndpoint_id() {
		return endpoint_id;
	}

	public long getNic_id() {
		return nic_id;
	}
	
//	@Override
	public String toString() {
		if (hostname == null) {
			return Long.toHexString(nic_id) + ":" + endpoint_id + "(no hostname)";
		} else {
			return hostname + ":" + endpoint_id;
		}
	}
}

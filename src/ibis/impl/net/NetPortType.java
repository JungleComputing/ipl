package ibis.ipl.impl.net;

import ibis.ipl.ConnectUpcall;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.Upcall;

class NetPortType implements PortType {
	/*
	static final byte SERIALIZATION_SUN   = 0;
	static final byte SERIALIZATION_MANTA = 1;
	*/
	private String 		 name 	     	   = null;
	private StaticProperties staticProperties  = null;
	private NetIbis          ibis              = null; 
	// private byte             serializationType = SERIALIZATION_SUN;
	private NetDriver        driver            = null;

	public NetPortType (NetIbis ibis, String name, StaticProperties sp)
		throws IbisIOException {
		this.ibis             = ibis;
		this.name             = name;
		this.staticProperties = sp;

		driver = ibis.getDriver(sp.find("/:Driver"));
	}

	/**
	 * Returns the topmost driver for this type.
	 */
	public NetDriver getDriver() {
		return driver;
	}
	
	/**
	 * Returns a reference to the owning Ibis instance.
	 */
	public NetIbis getIbis() {
		return ibis;
	}
	/*
	public byte serializationType() {
		return serializationType;
	}
	*/
	/**
	 * {@inheritDoc}
	 */
	public String name() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public StaticProperties properties() {
		return staticProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(String name) throws IbisIOException {
		return new NetSendPort(this, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort() throws IbisIOException {
		return new NetSendPort(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name) throws IbisIOException {
		NetReceivePort nrp = new NetReceivePort(this, name);
		ibis.receivePortNameServerClient().bind(name, nrp);

		return nrp;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u)
		throws IbisIOException {
		NetReceivePort nrp = new NetReceivePort(this, name, u);
		ibis.receivePortNameServerClient().bind(name, nrp);

		return nrp;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String        name,
					     ConnectUpcall cU)
		throws IbisIOException {
		__.unimplemented__("createReceivePort(..., ConnectUpcall)");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String    	   name,
					     Upcall    	   u,
					     ConnectUpcall cU)
		throws IbisIOException {
		__.unimplemented__("createReceivePort(..., ConnectUpcall)");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(PortType other) {
		if (!(other instanceof NetPortType)) {
			return false;
		}

		NetPortType temp = (NetPortType) other;

		return name.equals(temp.name);
	}

	/**
	 * Special decoding function for boolean properties.
	 */
	public boolean getBooleanProperty(String  name,
					  boolean def) {
		boolean result = def;
		String  value  = staticProperties.find(name);

		if (value != null) {
			if (value.equals(String.valueOf(true))) {
				result = true;
			} else if (value.equals(String.valueOf(false))) {
				result = false;
			} else {
				__.abort__("invalid property value '"+value+"', should be "+String.valueOf(true)+" or "+String.valueOf(false));
			}
		}

		return result;
	}
}

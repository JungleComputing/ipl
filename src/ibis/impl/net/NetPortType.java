package ibis.ipl.impl.net;

import ibis.ipl.ConnectUpcall;
import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.io.Replacer;
import ibis.ipl.Upcall;
import ibis.util.Input;

import java.util.Enumeration;
import java.util.HashMap;

public final class NetPortType implements PortType {
	private String 		 name 	     	   = null;
	private StaticProperties staticProperties  = null;
	private NetIbis          ibis              = null;
        private NetPropertyTree  propertyTree      = null;
        private HashMap          propertyCache     = null;

	private String readKey(Input in) {
		// Skip comment lines starting with a '#' at col 0
		if (!in.eof() && !in.eoln() && in.nextChar() == '#') {
			return null;
		}

		// Skip empty lines
		if (in.eoln()) {
			return null;
		}

		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && in.nextChar() != '=' && !Character.isWhitespace(in.nextChar())) {
			s.append(in.readChar());
		}

		return s.toString();
	}

	private String readVal(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln()) {
			s.append(in.readChar());
		}

		return s.toString();
	}
	private void readProperties(Input in, StaticProperties sp) {
		while(!in.eof()) {
                        in.skipWhiteSpace();
			String key = readKey(in);

			if (key == null) {
				in.readln();
				continue;
			}
                        in.skipWhiteSpace();			
			in.readChar();
			in.skipWhiteSpace();
			String val = readVal(in);
			in.readln();

			try {
                                if (sp.find(key) == null) {
                                        //System.err.println("NetPortType: default setting ["+key+"] = ["+val+"]");
                                        sp.add(key, val);
                                }
			} catch (Exception e) {
				System.err.println("error adding property (" + key + "," + val + ")");
				System.exit(1);
			}
		}
	}

        private Input tryOpen(String s) {
                Input in = null;
                try {
                        in = new Input(s);
                } catch (Exception e) {
                                //
                }
                return in;
        }
        

	public NetPortType (NetIbis ibis, String name, StaticProperties sp)
		throws NetIbisException {
		this.ibis             = ibis;
		this.name             = name;
                this.propertyTree     = new NetPropertyTree();
                this.propertyCache    = new HashMap();
		this.staticProperties = sp;
                
                /* Completes the static properties with default values from configuration file*/
                {        
                        Input  in = null;
                        String filename = "";

                        in = tryOpen(filename = "net_port_type_defaults.txt");
                        
                        if (in != null) {
                                try {
                                        //System.err.println("NetPortType: reading defaults port settings from "+filename);
                                        readProperties(in, sp);
                                } catch (Exception e) {
				// nothing
                                }
                        }
                }

                /* Completes the static properties with default values */
                {
                        if (sp.find("/:Driver") == null) {
                                //System.err.println("NetPortType: internal default setting /:Driver = gen");
                                try {
                                        sp.add("/:Driver", "gen");
                                } catch (Exception e) {
                                        System.err.println("error adding property (/:Driver, gen)");
                                        System.exit(1);
                                }

                                if (sp.find("/gen:Driver") == null) {
                                        //System.err.println("NetPortType: internal default setting /gen:Driver = def");
                                        try {
                                                        
                                                sp.add("/gen:Driver", "def");
                                        } catch (Exception e) {
                                                System.err.println("error adding property (/gen:Driver, def)");
                                                System.exit(1);
                                        }
                                }
                        }
                }
                
                
                /* Builds the property tree */
                {
                        Enumeration e = sp.keys();

                        while (e.hasMoreElements()) { 
                                String key   = (String) e.nextElement();		       			
                                String value = sp.find(key);

                                propertyTree.put(key, value);
                        }
		} 
                
	}

	/**
	 * Returns a reference to the owning Ibis instance.
	 */
	public NetIbis getIbis() {
		return ibis;
	}

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
	public SendPort createSendPort(String name) throws NetIbisException {
		return new NetSendPort(this, null, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(Replacer r) throws NetIbisException {
		return new NetSendPort(this, r);
	}

	public SendPort createSendPort(String name, Replacer r) throws NetIbisException {
		return new NetSendPort(this, r, name);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort() throws NetIbisException {
		return new NetSendPort(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name) throws NetIbisException {
		NetReceivePort nrp = new NetReceivePort(this, name);

                try {
                        ibis.receivePortNameServerClient().bind(name, nrp);
                } catch (ibis.ipl.IbisIOException e) {
                        throw new NetIbisException(e);
                }

		return nrp;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u)
		throws NetIbisException {
		NetReceivePort nrp = new NetReceivePort(this, name, u);
                
                try {
                        ibis.receivePortNameServerClient().bind(name, nrp);
                } catch (ibis.ipl.IbisIOException e) {
                        throw new NetIbisException(e);
                }

		return nrp;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String        name,
					     ConnectUpcall cU)
		throws NetIbisException {
		__.unimplemented__("createReceivePort(..., ConnectUpcall)");
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String    	   name,
					     Upcall    	   u,
					     ConnectUpcall cU)
		throws NetIbisException {
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






        private Object getProperty(String context, String name) {
                if (context != null) {
                        name = context+":"+name;
                }
                
                Object result = null;
                
                if (propertyCache.containsKey(name)) {
                        result =  propertyCache.get(name);
                } else {
                        result = propertyTree.get(name);
                        propertyCache.put(name, result);
                }

                return result;
        }
        

	/**
	 * Special decoding function for boolean properties.
	 */
	public Boolean getBooleanStringProperty(String context, String name, Boolean defaultValue) {
		Boolean result = defaultValue;
		String  value  = (String)getProperty(context, name);

		if (value != null) {
                        value.toLowerCase();
			if (value.equals(String.valueOf(true))) {
				result = new Boolean(true);
			} else if (value.equals(String.valueOf(false))) {
				result = new Boolean(false);
			} else {
				__.abort__("invalid property value '"+value+"', should be "+String.valueOf(true)+" or "+String.valueOf(false));
			}
		}

		return result;
	}

	public boolean getBooleanStringProperty(String context, String name, boolean defaultValue) {
		boolean result = defaultValue;
		String  value  = (String)getProperty(context, name);

		if (value != null) {
                        value.toLowerCase();
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
        

        public String getStringProperty(String context, String name, String defaultValue) {
                String result = defaultValue;
                String value  = (String)getProperty(context, name);

                if (value != null) {
                        result = value;
                }

                return result;
        }

	public Boolean getBooleanStringProperty(String context, String name) {
                return getBooleanStringProperty(context, name, null);
        }
        
        public String getStringProperty(String context, String name) {
                return getStringProperty(context, name, null);
        }
        
}

package ibis.ipl.impl.net;

import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Upcall;
import ibis.ipl.IbisException;
import ibis.ipl.Replacer;

import ibis.util.Input;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import java.io.IOException;

/**
 * Provide a NetIbis'specific implementation of the {@link PortType} interface.
 */
public final class NetPortType implements PortType {

        /**
         * Store the type name.
         */
	private String 		 name 	     	   = null;

        /**
         * Store the properties.
         */
	private StaticProperties staticProperties  = null;

        /**
         * Reference the {@link NetIbis} instance.
         */
	private NetIbis          ibis              = null;

        /**
         * Store the sorted version of the {@link #staticProperties
         * properties}.
         */
        private NetPropertyTree  propertyTree      = null;

        /**
         * Cache the read accesses to the {@linkplain #propertyTree
         * property tree}.
         */
        private HashMap          propertyCache     = null;

        /**
         * Read a property key.
         *
         * @param in the {@link Input} from which the key is read.
         * @param return the property key or <code>null</code>.
         */
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

        /**
         * Read a property value.
         *
         * @param in the {@link Input} from which the value is read.
         * @param return the property value.
         */
	private String readVal(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln()) {
			s.append(in.readChar());
		}

		return s.toString();
	}

        /**
         * Read a port-type property.
         *
         * If the property is already set, the new value is <b>ignored</b>.
         *
         * @param in the {@link Input} from which the property is read.
         * @param sp the property pool to which the property will be stored.
         */
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
				throw new Error("error adding property (" + key + "," + val + ")", e);
			}
		}
	}

        /**
         * Wrap the file opening operation.
         *
         * @param s the full pathname of the file to open.
         * @return the {@link Input} attached to the file.
         */
        private Input tryOpen(String s) {
                Input in = null;
                try {
                        in = new Input(s);
                } catch (Exception e) {
                                //
                }
                return in;
        }

        /**
         * Complete the static properties with default values from configuration file.
         */
        private void readDefaultProperties() {
                Input  in = null;
                String filename = null;

                Properties p = System.getProperties();
                filename = p.getProperty("netibis.config.filename");
                if (filename == null) {
                        filename = "net_port_type_defaults.txt";
                        in = tryOpen(filename);
                        if (in == null) {
                                String userhome = p.getProperty("user.home");
                                String fileseparator = p.getProperty("file.separator");

                                if (userhome != null) {
                                        if (fileseparator == null) {
                                                fileseparator = "/";
                                        }

                                        filename = userhome+fileseparator+filename;
                                        in = tryOpen(filename);
                                }
                        }
                } else {
                        in = tryOpen(filename);
                }

                if (in != null) {
                        try {
                                readProperties(in, staticProperties);
                        } catch (Exception e) {
				// nothing
                        }
                }
        }

        /**
         * Completes the static properties with default values.
         */
        private void setDefaultProperties() {
                if (staticProperties.find("/:Driver") == null) {
                        //System.err.println("NetPortType: internal default setting /:Driver = gen");
                        try {
                                staticProperties.add("/:Driver", "gen");
                        } catch (Exception e) {
                                throw new Error("error adding property (/:Driver, gen)", e);
                        }

                        if (staticProperties.find("/gen:Driver") == null) {
                                //System.err.println("NetPortType: internal default setting /gen:Driver = def");
                                try {

                                        staticProperties.add("/gen:Driver", "def");
                                } catch (Exception e) {
                                        throw new Error("error adding property (/gen:Driver, def)", e);
                                }
                        }
                }
        }
        /**
         * Build the property tree.
         */
        private void buildPropertyTree() {
                Enumeration e = staticProperties.keys();

                while (e.hasMoreElements()) {
                        String key   = (String) e.nextElement();
                        String value = staticProperties.find(key);

                        propertyTree.put(key, value);
                }
        }

        /**
         * Construct a port type.
         *
         * @param ibis a reference to the {@link NetIbis} instance.
         * @param name the unique name of the type.
         * @param sp the runtime-defined properties of the type.
         * @exception IOException if the operation fails.
         */
	public NetPortType (NetIbis ibis, String name, StaticProperties sp) throws IOException {
		this.ibis             = ibis;
		this.name             = name;
                this.propertyTree     = new NetPropertyTree();
                this.propertyCache    = new HashMap();
		this.staticProperties = sp != null ? sp : new StaticProperties();

                readDefaultProperties();
                setDefaultProperties();
                buildPropertyTree();
	}

	/**
	 * Return a reference to the owning {@link NetIbis} instance.
         *
         * @return a reference to the owning {@link NetIbis} instance.
	 */
	public NetIbis getIbis() {
		return ibis;
	}

	/**
	 * {@inheritDoc}
	 */
	public String name() {
		if (name != null) {
			return name;
		}
		return "__anonymous__";
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
	public SendPort createSendPort(String name) throws IOException {
		return new NetSendPort(this, null, name, null, false);
	}

	public SendPort createSendPort(String name, boolean connectionAdministration) throws IOException {
		return new NetSendPort(this, null, name, null, connectionAdministration);
	}

        public SendPort createSendPort(String name, SendPortConnectUpcall spcu) throws IOException {
                return new NetSendPort(this, null, name, spcu, true);
        }

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort() throws IOException {
		return new NetSendPort(this, null, null, null, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(Replacer r) throws IOException {
		return new NetSendPort(this, r, null, null, false);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(String name, Replacer r) throws IOException {
		return new NetSendPort(this, r, name, null, false);
	}

	public SendPort createSendPort(boolean connectionAdministration) throws IOException {
		return new NetSendPort(this, null, null, null, connectionAdministration);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(Replacer r, boolean connectionAdministration) throws IOException {
		return new NetSendPort(this, r, null, null, connectionAdministration);
	}

	/**
	 * {@inheritDoc}
	 */
	public SendPort createSendPort(String name, Replacer r, boolean connectionAdministration) 
		throws IOException {
		return new NetSendPort(this, r, name, null, connectionAdministration);
	}

	public SendPort createSendPort(Replacer r, SendPortConnectUpcall spcu) throws IOException {
                return new NetSendPort(this, r, null, spcu, true);
        }

        public SendPort createSendPort(String name, Replacer r, SendPortConnectUpcall spcu) throws IOException {
                return new NetSendPort(this, r, name, spcu, true);
        }

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name) throws IOException {
                return createReceivePort(name, null, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u) throws IOException {
		return createReceivePort(name, u, null);
	}


	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, boolean connectionAdministration) 
		throws IOException {
                return createReceivePort(name, null, null, connectionAdministration);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u, boolean connectionAdministration) 
		throws IOException {
		return createReceivePort(name, u, null, connectionAdministration);
	}


	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, ReceivePortConnectUpcall rpcu) throws IOException {
                return createReceivePort(name, null, rpcu, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u, ReceivePortConnectUpcall rpcu) throws IOException {
		return createReceivePort(name, u, rpcu, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReceivePort createReceivePort(String name, Upcall u, ReceivePortConnectUpcall rpcu,
					     boolean connectionAdministration) throws IOException {
		NetReceivePort nrp = new NetReceivePort(this, name, u, rpcu, connectionAdministration);

		ibis.receivePortNameServerClient().bind(name, nrp);

		return nrp;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object other) {
		if (other == null || !(other instanceof NetPortType)) {
			return false;
		}

		NetPortType temp = (NetPortType) other;

		if (temp == this) return true;

		return name().equals(temp.name()) && ibis.equals(temp.ibis);
	}

	/**
	 * @inheritDoc
	 */
	public int hashCode() {
		return name().hashCode() + ibis.hashCode();
	}

        /**
         * Lookup a property.
         *
         * The first time a property is accessed, it is looked up in
         * the {@link #propertyTree} and the result is stored in the
         * {@link #propertyCache}. Subsequent accesses directly
         * retrieve the property value from the {@link
         * #propertyCache}.
         *
         * @param context the context string to used for prefixing the
         * property name (may also be seen as the property name's
         * namespace). The actual property name will be <code>context+":"+name</code>
         * @param name the name of the property.
         * @return the direct or inherited property value, or
         * <code>null</code> if the property is not found.
         */
        private Object getProperty(String context, String name) {
                if (context != null) {
                        name = context+":"+name;
                }

                Object result = null;

                if (propertyCache.containsKey(name)) {
                        result = propertyCache.get(name);
                } else {
                        result = propertyTree.get(name);
                        propertyCache.put(name, result);
                }

                return result;
        }

	/**
	 * Lookup a {@link Boolean} property.
         *
         * @param context @see #getProperty.
         * @param name @see #getProperty.
         * @param defaultValue the value to return if the property is not found.
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

	/**
	 * Lookup a <code>boolean</code> property.
         *
         * @param context @see #getProperty.
         * @param name @see #getProperty.
         * @param defaultValue the value to return if the property is not found.
	 */
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

	/**
	 * Lookup a {@link String} property.
         *
         * @param context @see #getProperty.
         * @param name @see #getProperty.
         * @param defaultValue the value to return if the property is not found.
	 */
        public String getStringProperty(String context, String name, String defaultValue) {
                String result = defaultValue;
                String value  = (String)getProperty(context, name);

                if (value != null) {
                        result = value;
                }

                return result;
        }

	/**
	 * Lookup a <code>boolean</code> property.
         *
         * @param context @see #getProperty.
         * @param name @see #getProperty.
	 */
	public Boolean getBooleanStringProperty(String context, String name) {
                return getBooleanStringProperty(context, name, null);
        }

	/**
	 * Lookup a {@link String} property.
         *
         * @param context @see #getProperty.
         * @param name @see #getProperty.
	 */
        public String getStringProperty(String context, String name) {
                return getStringProperty(context, name, null);
        }

}

package ibis.impl.net;

import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * Provide a NetIbis'specific implementation of the {@link PortType} interface.
 */
public final class NetPortType extends PortType {

    /** Stripped down Input.java ...
     *  Code and dutch comments are by Matty Huntjens.
     */
    static class Input extends BufferedInputStream {

        protected static final int END_OF_FILE = -1, CODE_NULL_CHARACTER = 0;

        protected String lineSeparator;

        protected int lengthLineSeparator;

        protected boolean endOfFile;

        /* CONSTRUCTORS */

        // Deze constructor initialiseert het Input object als lezende van
        // de InputStream in.
        public Input(InputStream in) {
            super(in);
            lineSeparator = try_getProperty("line.separator");
            lengthLineSeparator = lineSeparator.length();
            endOfFile = false;
        }

        // Deze constructor initialiseert het Input object als lezende van
        // standard input.
        public Input() {
            this(System.in);
        }

        // Deze constructor initialiseert het Input object als lezende van
        // de file met naam "name".
        public Input(String name) throws FileNotFoundException {
            this(new FileInputStream(name));
        }

        /* FINALIZE */

        protected void finalize() {
            try_close(in);
            try_finalize();
        }

        /* CLASS METHODS */

        // retourneert het aantal characters in een number
        // Deze methode moet static zijn om in de constante declaraties gebruikt
        // te kunnen worden.
        protected static int length(long i) {
            return String.valueOf(i).length();
        }

        /* METHODS TO HIDE TRY STATEMENTS */

        protected void try_close(InputStream in) {
            try {
                close();
            } catch (IOException e) {
                throw new Error("I/O error\n" + e);
            }
        }

        protected void try_finalize() {
            try {
                super.finalize();
            } catch (Throwable e) {
                throw new Error("throwable thrown\n" + e);
            }
        }

        protected String try_getProperty(String key) {
            try {
                return System.getProperty(key);
            } catch (SecurityException e) {
                throw new Error("security exception\n" + e);
            }
        }

        protected void try_reset() {
            try {
                reset();
            } catch (IOException e) {
                throw new Error("IO exception\n" + e);
            }
        }

        protected boolean bufEmpty() {
            return pos >= count;
        }

        protected int try_read() {
            /* N.B. Als de methode read() de waarde END_OF_FILE retourneert, zal,
             bij inlezen van standaard invoer (het toetsenbord), een tweede 
             aanroep van read() niet tot gevolg hebben dat er weer de waarde
             END_OF_FILE geretourneerd wordt, maar dat er gewacht zal worden
             op invoer.
             Om dit te voorkomen is de boolean endOfFile ingevoerd.
             */
            try {
                int charCode = bufEmpty() && endOfFile ? CODE_NULL_CHARACTER
                        : read();
                if (charCode == END_OF_FILE) {
                    endOfFile = true;
                    charCode = CODE_NULL_CHARACTER;
                }
                return charCode;
            } catch (IOException e) {
                throw new Error("IO exception\n" + e);
            }
        }

        /* BASIC INPUT METHODS */

        // Indien readChar() wordt aangeroepen bij end-of-file,
        // leidt dit tot een foutmelding en een crash.
        // Anders leest readChar() een char in en retourneert dit.
        public char readChar() {
            if (eof()) {
                throw new Error(this + ": end-of-file error");
            }

            return (char) try_read();
        }

        public char nextChar() {
            return nextNCharacters(1).charAt(0);
        }

        protected String nextNCharacters(int n) {
            StringBuffer s = new StringBuffer();
            int codeChar;

            mark(n);
            for (int i = 0; i < n; i++) {
                s.append((char) try_read());
            }
            try_reset();

            return s.toString();
        }

        public boolean eoln() {
            return nextNCharacters(lengthLineSeparator).equals(lineSeparator);
        }

        public boolean eof() {
            if (!bufEmpty()) {
                return false;
            }

            // de buffer is leeg

            if (endOfFile) {
                return true;
            }

            mark(1);
            try_read(); // try_read() zet endOfFile op true indien end-of-file
            try_reset();

            return endOfFile;
        }

        public void readln() {
            // Skip alle characters voor de end-of-line.
            while (!eoln()) {
                readChar();
            }

            // Skip de end-of-line.
            skipCharacters(lengthLineSeparator);
        }

        public void skipWhiteSpace() {
            while (!eof() && Character.isWhitespace(nextChar())) {
                readChar();
            }
        }

        protected void skipCharacters(int n) {
            for (int i = 0; i < n; i++) {
                readChar();
            }
        }
    }

    /**
     * Store the type name.
     */
    private String name = null;

    /**
     * Cache if the type is "Numbered"
     */
    private boolean numbered = false;

    /**
     * Store the properties.
     */
    private StaticProperties staticProperties = null;

    /**
     * Reference the {@link ibis.impl.net.NetIbis} instance.
     */
    private NetIbis ibis = null;

    /**
     * Store the sorted version of the {@link #staticProperties
     * properties}.
     */
    private NetPropertyTree propertyTree = null;

    /**
     * Cache the read accesses to the {@linkplain #propertyTree
     * property tree}.
     */
    private HashMap propertyCache = null;

    /**
     * Read a property key.
     *
     * @param in the {@link Input} from which the key is read.
     * @return the property key or <code>null</code>.
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
        while (!in.eof() && !in.eoln() && in.nextChar() != '='
                && !Character.isWhitespace(in.nextChar())) {
            s.append(in.readChar());
        }

        return s.toString();
    }

    /**
     * Read a property value.
     *
     * @param in the {@link Input} from which the value is read.
     * @return the property value.
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
        while (!in.eof()) {
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
                throw new Error("error adding property (" + key + "," + val
                        + ")", e);
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
        Input in = null;
        String filename = null;

        Properties p = System.getProperties();
        filename = p.getProperty(NetIbis.cfg_filenm);
        if (filename == null) {
            filename = p.getProperty(NetIbis.cfg_file);
        }
        if (filename == null) {
            filename = p.getProperty("netibis.config.filename");
        }
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

                    filename = userhome + fileseparator + filename;
                    in = tryOpen(filename);
                }
            }
        } else {
            in = tryOpen(filename);
        }
        // System.err.println("Try to read from property file \"" + filename + "\" fd " + in);

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
                    throw new Error("error adding property (/gen:Driver, def)",
                            e);
                }
            }
        }
    }

    /**
     * Build the property tree.
     */
    private void buildPropertyTree() {
        Set e = staticProperties.propertyNames();
        Iterator i = e.iterator();

        while (i.hasNext()) {
            String key = (String) i.next();
            String value = staticProperties.find(key);

            propertyTree.put(key, value);
        }
    }

    /**
     * Construct a port type.
     *
     * @param ibis a reference to the {@link ibis.impl.net.NetIbis} instance.
     * @param name the unique name of the type.
     * @param sp the runtime-defined properties of the type.
     * @exception IOException if the operation fails.
     */
    public NetPortType(NetIbis ibis, String name, StaticProperties sp)
            throws IOException {
        this.ibis = ibis;
        this.name = name;
        this.propertyTree = new NetPropertyTree();
        this.propertyCache = new HashMap();
        this.staticProperties = sp != null ? sp : new StaticProperties();

        readDefaultProperties();
        setDefaultProperties();
        buildPropertyTree();

        numbered = staticProperties.isProp("communication", "Numbered");
        if (numbered) {
            System.err.println(this + ": switch on numbered communication");
        }
    }

    /**
     * Return a reference to the owning {@link ibis.impl.net.NetIbis} instance.
     *
     * @return a reference to the owning {@link ibis.impl.net.NetIbis} instance.
     */
    public NetIbis getIbis() {
        return ibis;
    }

    public String name() {
        return name;
    }

    public boolean numbered() {
        return numbered;
    }

    public boolean oneToMany() {
        return staticProperties.isProp("communication", "OneToMany");
    }

    public boolean manyToOne() {
        return staticProperties.isProp("communication", "ManyToOne");
    }

    public StaticProperties properties() {
        return staticProperties;
    }

    /**
     * Return whether this PortType supports only one incoming connection
     * per port.
     */
    public boolean inputSingletonOnly() {
        StaticProperties prop = properties();
        boolean singletonOnly = TypedProperties.booleanProperty(
                NetIbis.port_single, true)
                && !prop.isProp("communication", "ManyToOne")
                && !prop.isProp("communication", "Poll")
                && !prop.isProp("communication", "ReceiveTimeout");
        if (false && singletonOnly) {
            System.err.println(this + ": set Poller.singletonOnly to "
                    + singletonOnly);
            System.err.println(this + ": property ManyToOne "
                    + prop.isProp("communication", "ManyToOne"));
        }
        return singletonOnly;
    }

    /**
     * Return whether this PortType supports only one outgoing connection
     * per port.
     */
    public boolean outputSingletonOnly() {
        StaticProperties prop = properties();
        boolean singletonOnly = TypedProperties.booleanProperty(
                NetIbis.port_single, true)
                && !prop.isProp("communication", "OneToMany");
        if (false && singletonOnly) {
            System.err.println(this + ": property OneToMany "
                    + prop.isProp("communication", "OneToMany"));
        }
        return singletonOnly;
    }

    public SendPort createSendPort(String name, SendPortConnectUpcall spcu,
            boolean connectionAdministration) throws IOException {
        return new NetSendPort(this, name, spcu, connectionAdministration);
    }

    public ReceivePort createReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall rpcu, boolean connectionAdministration)
            throws IOException {
        NetReceivePort nrp = new NetReceivePort(this, name, u, rpcu,
                connectionAdministration);

        ibis.registry().bind(name, nrp.identifier());

        return nrp;
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof NetPortType)) {
            return false;
        }

        NetPortType temp = (NetPortType) other;

        if (temp == this)
            return true;

        return name.equals(temp.name) && ibis.equals(temp.ibis);
    }

    public int hashCode() {
        return name.hashCode() + ibis.hashCode();
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
            name = context + ":" + name;
        }
        name = name.toLowerCase();

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
    public Boolean getBooleanStringProperty(String context, String name,
            Boolean defaultValue) {
        Boolean result = defaultValue;
        String value = (String) getProperty(context, name);

        if (value != null) {
            value = value.toLowerCase();
            if (value.equals(String.valueOf(true))) {
                result = Boolean.valueOf(true);
            } else if (value.equals(String.valueOf(false))) {
                result = Boolean.valueOf(false);
            } else {
                __
                        .abort__("invalid property value '" + value
                                + "', should be " + String.valueOf(true)
                                + " or " + String.valueOf(false));
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
    public boolean getBooleanStringProperty(String context, String name,
            boolean defaultValue) {
        boolean result = defaultValue;
        String value = (String) getProperty(context, name);

        if (value != null) {
            value = value.toLowerCase();
            if (value.equals(String.valueOf(true))) {
                result = true;
            } else if (value.equals(String.valueOf(false))) {
                result = false;
            } else {
                __
                        .abort__("invalid property value '" + value
                                + "', should be " + String.valueOf(true)
                                + " or " + String.valueOf(false));
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
    public String getStringProperty(String context, String name,
            String defaultValue) {
        String result = defaultValue;
        String value = (String) getProperty(context, name);

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
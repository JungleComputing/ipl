/* $Id$ */

package ibis.ipl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Container for the properties of an {@link ibis.ipl.Ibis Ibis} or a
 * {@link ibis.ipl.PortType PortType}.
 *
 * A property consists of a name (key), and its value, a set of strings.
 * for instance: name: serialization, value: {sun}.
 * The properties are static, that is, once a property is set, its value
 * cannot be changed.
 * The value of a property is usually given as a string, which is then
 * split into words (a word separator is one of
 * " ,\t\n\r\f"). These words can be queried as well, by means of the
 * {@link #isProp(String, String)} method. For instance,
 * <code>isProp("communication", "OneToMany")</code> could be a query to
 * find out if multicast is supported (or required, when a porttype is
 * created).
 */
public final class StaticProperties implements java.io.Serializable {

    /** Set containing the category names. */
    private static Set<String> category_names;

    /** User defined properties. */
    private static final StaticProperties user_properties;

    /** User defined properties that are to be merged. */
    private static final StaticProperties user_merge_properties;

    /** Ibis properties. */
    private static final StaticProperties ibis_properties;

    /** Maps property names to property values. */
    private transient HashMap<String, Property> mappings
            = new HashMap<String, Property>();

    /** Default properties. */
    private transient final StaticProperties defaults;

    private transient byte[] codedForm = null;

    /**
     * Container class for properties that are associated with a key.
     */
    private static class Property {
        private HashSet<String> h;

        /**
         * Creates a <code>Property</code> from the specified string.
         * The string is tokenized, and each token is added to the 
         * property set.
         * @param v the property set as a string.
         * @param toLower true when the value should be converted to lowercase
         */
        public Property(String v, boolean toLower) {
            if (v != null) {
                StringTokenizer st = new StringTokenizer(v, " ,\t\n\r\f");
                h = new HashSet<String>();
                while (st.hasMoreTokens()) {
                    String s = toLower ? st.nextToken().toLowerCase() : st.nextToken();
                    h.add(s);
                }
            }
        }

        public Property(String v) {
            this(v, true);
        }

        /**
         * Creates an empty <code>Property</code>.
         */
        public Property() {
            h = new HashSet<String>();
        }

        /**
         * Creates a property from the specified set.
         */
        Property(HashSet<String> h) {
            this.h = h;
        }

        /**
         * Returns <code>true</code> if the property set contains the
         * specified string.
         * @return <code>true</code> if the property set contains the
         * specified string, <code>false</code> otherwise.
         */
        public boolean hasProp(String s) {
            if (s == null) {
                return false;
            }
            return h.contains(s);
        }

        /**
         * Returns the value of this property, as a comma-separated string.
         * @return the value of this property, as a string.
         */
        public String getValue() {
            String s = null;
            Iterator i = h.iterator();
            while (i.hasNext()) {
                String str = (String) i.next();
                if (s == null) {
                    s = "" + str;
                } else {
                    s += ", " + str;
                }
            }
            if (s == null) {
                return "";
            }
            return s;
        }

        /**
         * Allows access to the propertyset as a set.
         * @return the property set.
         */
        public HashSet<String> getSet() {
            return h;
        }
    }

    /**
     * Initialize from the global ibis-properties file, which defines
     * the properties.
     */
    static {
        // First, read the ibis-properties file.
        try {
            ibis_properties = new StaticProperties();
            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(
                    "ibis-properties");
            if (in == null) {
                throw new IOException("Could not open ibis-properties");
            }
            ibis_properties.load(in);
            in.close();
        } catch (IOException e) {
            System.err.println("IO exception during ibis-properties read");
            e.printStackTrace();
            System.exit(1);
            // And, because javac does not understand exit ...
            throw new Error("Got IOException", e);
        }

        // Then, find the property categories.
        category_names = ibis_properties.findSet("PropertyCategories");
        if (category_names == null) {
            System.err.println("no PropertyCategories in ibis-properties!");
            System.exit(1);
        }
        ibis_properties.addImpliedProperties();

        // Now compute user-defined properties.
        // These are derived from system properties with names starting with
        // "ibis.". The "ibis." prefix is stripped, and if the rest of the
        // name is a category name, the property is added to the user
        // properties.

        user_properties = new StaticProperties();
        user_merge_properties = new StaticProperties();
        Properties sysprops = System.getProperties();
        Enumeration e = sysprops.propertyNames();

        while (e.hasMoreElements()) {
            String name = ((String) e.nextElement());
            String prop = sysprops.getProperty(name);

            name = name.toLowerCase();
            if (name.length() >= 5 && name.substring(0, 5).equals("ibis.")) {
                String n = name.substring(5);

                if (category_names.contains(n) || n.equals("name")
                        || n.equals("verbose")) {
                    user_properties.add(n, prop);
                } else {
                    // Allow for properties like ibis.communication.numbered
                    Iterator i = category_names.iterator();
                    while (i.hasNext()) {
                        String catName = (String) i.next();
                        if (n.startsWith(catName + ".")) {
                            Set<String> catValues
                                    = ibis_properties.findSet(catName);
                            Iterator j = catValues.iterator();
                            while (j.hasNext()) {
                                String catValue = (String) j.next();
                                if (n.equals(catName + "." + catValue)) {
                                    Property p = (Property) 
                                            user_merge_properties.mappings.get(
                                                    catName);
                                    if (p == null) {
                                        p = new Property();
                                        user_merge_properties.mappings.put(
                                                catName, p);
                                    }
                                    HashSet<String> h = p.getSet();
                                    h.add(catValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates an empty property set.
     */
    public StaticProperties() {
        defaults = null;
    }

    /**
     * Creates an empty property set with the specified defaults.
     * @param sp the specified defaults.
     */
    public StaticProperties(StaticProperties sp) {
        defaults = sp;
    }

    /**
     * Creates a property set from a serialized form.
     * @param codedForm the serialized form, as produced by the
     * {@link #toBytes()} method.
     * @exception IOException is thrown in case of trouble.
     */
    public StaticProperties(byte[] codedForm) throws IOException {
        this(codedForm, 0, codedForm.length);
        this.codedForm = codedForm;
    }

    /**
     * Creates a property set from a serialized form.
     * @param codedForm contains the serialized form, as produced by the
     * {@link #toBytes()} method.
     * @param offset offset where input for this method starts.
     * @param length length of input for this method.
     * @exception IOException is thrown in case of trouble.
     */
    public StaticProperties(byte[] codedForm, int offset, int length)
            throws IOException {
        this(new DataInputStream(
                new ByteArrayInputStream(codedForm, offset, length)));
    }

    /**
     * Creates a property set by reading it from the specified input stream.
     * @param dis the input stream to read from.
     * @exception IOException is thrown in case of trouble.
     */
    public StaticProperties(DataInput dis) throws IOException {
        defaults = null;
        doRead(dis);
    }

    /**
     * Returns the property set represented as a byte array.
     * @return the byte array.
     * @exception IOException is thrown in case of trouble.
     */
    public byte[] toBytes() throws IOException {
        if (codedForm == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            doWrite(dos);
            dos.close();
            codedForm = bos.toByteArray();
        }
        return codedForm;
    }

    /**
     * Writes the property set to the specified output stream.
     * @param dos the output stream to write to.
     * @exception IOException is thrown in case of trouble.
     */
    public void writeTo(DataOutput dos) throws IOException {
        toBytes();
        dos.write(codedForm);
    }

    /**
     * Returns the set of property names.
     * @return the set of property names.
     */
    public Set<String> propertyNames() {
        if (defaults == null) {
            return mappings.keySet();
        }
        HashSet<String> h = new HashSet<String>(mappings.keySet());
        defaults.addNames(h);
        return h;
    }

    /**
     * Returns the number of property names in this property set.
     * @return the number of property names.
     */
    public int size() {
        if (defaults == null) {
            return mappings.size();
        }
        return propertyNames().size();
    }

    /**
     * Adds property names to the specified set.
     * @param h the set.
     */
    private void addNames(HashSet<String> h) {
        h.addAll(mappings.keySet());
        if (defaults != null) {
            defaults.addNames(h);
        }
    }

    /**
     * Returns the static properties as derived from the system properties
     * provided by the user running the application.
     * @return the user-provided static properties.
     */
    public static StaticProperties userProperties() {
        return user_properties;
    }

    /**
     * Combines the specified properties with system properties as
     * specified by the user, giving preference to the latter.
     * @return the combined static properties.
     */
    public StaticProperties combineWithUserProps() {
        return combine(userProperties()).merge(user_merge_properties);
    }

    /**
     * Combines the properties with the specified properties,
     * giving preference to the latter.
     * This means: choose the specified property if it is "more specific" or
     * conflicts with "this" properties.
     * 
     * @param p the properties to combine with.
     * @return the combined static properties.
     */
    public StaticProperties combine(StaticProperties p) {
        StaticProperties combined = new StaticProperties(this);

        Set<String> e = p.propertyNames();
        Iterator i = e.iterator();

        while (i.hasNext()) {
            String name = ((String) i.next());
            String prop = p.find(name);
            if (name.equals("serialization")
                    && p.isProp("serialization", "object")) {
                // in fact, object may not make things more specific.
                if (isProp("serialization", "sun")
                        ^ isProp("serialization", "ibis")) {
                    // there already was either sun or ibis serialization
                    // specified.
                    continue;
                }
            }

            combined.addLiteral(name, prop);
        }
        return combined;
    }

    /**
     * Merges the properties with the specified properties. Merging means
     * that the result is a union, in contrast to what
     * {@link #combine(StaticProperties)} does.
     * 
     * @param p the properties to merge with.
     * @return the merged static properties.
     */
    public StaticProperties merge(StaticProperties p) {
        StaticProperties merged = copy();

        Set<String> e = p.mappings.keySet();
        Iterator i = e.iterator();
        while (i.hasNext()) {
            String cat = (String) i.next();
            if (category_names.contains(cat)) {
                Property p1 = (Property) p.mappings.get(cat);
                Property p2 = (Property) merged.mappings.get(cat);
                if (p2 == null) {
                    p2 = new Property();
                    merged.mappings.put(cat, p2);
                }
                p2.getSet().addAll(p1.getSet());
            }
        }

        return merged;
    }

    /**
     * Matches the current properties with the static properties
     * supplied. We have a match if the current properties are a subset
     * of the specified properties.
     * @param sp the static properties to be matched with.
     * @return <code>true</code> if we have a match,
     * <code>false</code> otherwise.
     */
    public boolean matchProperties(StaticProperties sp) {
        Iterator i = category_names.iterator();

        while (i.hasNext()) {
            String cat = (String) i.next();
            Set<String> v1 = findSet(cat);
            Set<String> v2 = sp.findSet(cat);
            if (!v2.containsAll(v1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Matches the current properties with the static properties
     * supplied. We have a match if the current properties are a subset
     * of the specified properties.
     * @param sp the static properties to be matched with.
     * @return the properties that don't match.
     */
    public StaticProperties unmatchedProperties(StaticProperties sp) {
        Iterator i = category_names.iterator();
        StaticProperties p = new StaticProperties();

        while (i.hasNext()) {
            String cat = (String) i.next();
            Set<String> v1 = findSet(cat);
            Set<String> v2 = sp.findSet(cat);
            if (!v2.containsAll(v1)) {
                HashSet<String> h = new HashSet<String>(v1);
                h.removeAll(v2);
                p.add(cat, (new Property(h)).getValue());
            }
        }
        return p;
    }

    /**
     * Adds a key/value pair to the properties.
     * If the key is already bound, an
     * {@link java.lang.RuntimeException RuntimeException}
     * is thrown. If either the key or the value is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     *
     * @param key the key to be bound.
     * @param value the value to bind to the key.
     * @exception RuntimeException is thrown when the key is already bound.
     * @exception NullPointerException is thrown when either key or value
     *  is <code>null</code>.
     */
    public void add(String key, String value) {
        key = key.toLowerCase();
        if (mappings.containsKey(key)) {
            throw new RuntimeException("Property " + key
                    + " already exists");
        }
        mappings.put(key, new Property(value));
    }

    /**
     * Adds a key/value pair to the properties.
     * If the key is already bound, an
     * {@link java.lang.RuntimeException RuntimeException}
     * is thrown. If either the key or the value is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * The value is NOT converted to lowercase.
     *
     * @param key the key to be bound.
     * @param value the value to bind to the key.
     * @exception RuntimeException is thrown when the key is already bound.
     * @exception NullPointerException is thrown when either key or value
     *  is <code>null</code>.
     */
    public void addLiteral(String key, String value) {
        key = key.toLowerCase();
        if (mappings.containsKey(key)) {
            throw new RuntimeException("Property " + key
                    + " already exists");
        }
        mappings.put(key, new Property(value, false));
    }

    /**
     * Returns the property associated with the specified key.
     * @param key the property category.
     * @return the Property structure, or null if not present.
     */
    private Property getProp(String key) {
        Property p = (Property) mappings.get(key);
        if (p == null && defaults != null) {
            return defaults.getProp(key);
        }
        return p;

    }

    /**
     * Returns the value associated with the specified key,
     * or <code>null</code>.
     * This is a synonym for {@link #find(String)} 
     * @param key the specified key.
     * @return the value associated with the specified key.
     */
    public String getProperty(String key) {
        return find(key);
    }

    /**
     * Returns the value associated with the specified key,
     * or <code>null</code>.
     * @param key the specified key.
     * @return the value associated with the specified key.
     */
    public String find(String key) {
        Property p = getProp(key.toLowerCase());
        if (p != null) {
            return p.getValue();
        }
        return null;
    }

    /**
     * Returns the value associated with the specified key, as a set.
     * @param key the specified key.
     * @return the value associated with the specified key.
     */
    public Set<String> findSet(String key) {
        Property p = getProp(key.toLowerCase());
        if (p != null) {
            return new HashSet<String>(p.getSet());
        }
        return new HashSet<String>();
    }

    /**
     * Returns <code>true<code> if the specified property category has the
     * specified property set.
     * @param cat the property category.
     * @param prop the property.
     * @return <code>true</code> if the property is set,
     * <code>false</code> otherwise.
     */
    public boolean isProp(String cat, String prop) {
        Property p = getProp(cat.toLowerCase());
        if (p == null) {
            return false;
        }
        return p.hasProp(prop.toLowerCase());
    }

    /**
     * Creates and returns a clone of this.
     * @return a clone.
     */
    public Object clone() {
        StaticProperties sp = new StaticProperties(defaults);

        Set<String> e = mappings.keySet();
        Iterator i = e.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String value = find(key);
            sp.addLiteral(key, value);
        }
        return sp;
    }

    /**
     * Returns all key/value pairs as a string.
     * The format is: a newline-separated list of
     * key = value pairs.
     * @return the key/value pairs as a string, or an
     * empty string if there are no key/value pairs.
     */
    public String toString() {

        StringBuffer result = new StringBuffer("");

        Set<String> e = propertyNames();
        Iterator i = e.iterator();
        while (i.hasNext()) {
            String key = (String) i.next();
            String value = find(key);

            result.append(key);
            result.append(" = ");
            result.append(value);
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * Reads the properties from the specified <code>InputStream</code>.
     * @param in the <code>InputStream</code>.
     * @exception IOException is thrown when an IO error occurs.
     */
    public void load(InputStream in) throws IOException {
        Properties p = new Properties();
        p.load(in);
        Enumeration e = p.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = p.getProperty(key);
            add(key.toLowerCase(), value);
        }
    }

    /**
     * Reads and returns the properties from the specified file name, which is
     * searched for in the classpath.
     * @param name the file name.
     * @exception IOException is thrown when an IO error occurs.
     */
    public static StaticProperties load(String name) throws IOException {
        StaticProperties p = new StaticProperties();
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(
                name);
        if (in == null) {
            throw new IOException("Could not open " + name);
        }
        p.load(in);
        in.close();
        p.addImpliedProperties();
        return p;
    }

    /**
     * Adds implied properties to the properties set. This method
     * DOES change the properties, but it is private.
     */
    private void addImpliedProperties() {
        Set<String> e = mappings.keySet();
        Iterator i = e.iterator();
        while (i.hasNext()) {
            String cat = (String) i.next();
            if (category_names.contains(cat)) {
                Property p = (Property) mappings.get(cat);
                HashSet<String> h = p.getSet();
                boolean changed;
                do {
                    changed = false;
                    Iterator i2 = new HashSet<String>(h).iterator();
                    while (i2.hasNext()) {
                        String n = (String) i2.next();
                        Set<String> implied = ibis_properties.findSet(n);
                        if (!h.containsAll(implied)) {
                            changed = true;
                            h.addAll(implied);
                        }
                    }
                } while (changed);
            }
        }
    }

    /**
     * Returns a copy of the properties.
     * @return the copy.
     */
    public StaticProperties copy() {
        Set<String> keys = propertyNames();
        Iterator i = keys.iterator();
        StaticProperties p = new StaticProperties();

        while (i.hasNext()) {
            String s = (String) i.next();
            String prop = getProperty(s);
            p.addLiteral(s, prop);
        }
        return p;
    }

    /**
     * Returns <code>true</code> if <code>other</code> represents the same
     * property set.
     * @param other the object to compare with.
     * @return <code>true</code> if equal, <code>false</code> otherwise.
     */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof StaticProperties)) {
            return false;
        }
        StaticProperties o = (StaticProperties) other;
        Set<String> keys1 = propertyNames();
        Set<String> keys2 = o.propertyNames();
        if (!keys1.equals(keys2)) {
            return false;
        }
        Iterator i = keys1.iterator();
        while (i.hasNext()) {
            String s = (String) i.next();
            Set<String> s1 = findSet(s);
            Set<String> s2 = o.findSet(s);
            if (!s1.equals(s2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the hashcode of this property set.
     * @return the hashcode.
     */
    public int hashCode() {
        return propertyNames().hashCode();
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        doWrite(out);
    }

    private void doWrite(DataOutput out) throws IOException {
        Set<String> keys = propertyNames();
        out.writeInt(keys.size());
        for (Iterator i = keys.iterator(); i.hasNext();) {
            String key = (String) i.next();
            String val = find(key);
            out.writeUTF(key);
            out.writeUTF(val);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException {
        doRead(in);
    }

    private void doRead(DataInput in) throws IOException {
        int sz = in.readInt();
        mappings = new HashMap<String, Property>();
        for (int i = 0; i < sz; i++) {
            String key = in.readUTF();
            String val = in.readUTF();
            addLiteral(key, val);
        }
    }
}

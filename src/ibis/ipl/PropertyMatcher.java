package ibis.ipl;

import ibis.ipl.StaticProperties;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;

/**
 */

class PropertyMatcher {

    //
    // Communication properties.
    //

    /** When an Ibis implementation supports one-to-one communication.
     *  (if it does not support this, what DOES it support?)
     */
    private static final int OneToOne 			= 0x00000001;

    /** When an Ibis implementation supports multicast. **/
    private static final int OneToMany 			= 0x00000002;

    /** When an Ibis implementation supports connections from
     *  multiple sendports to a single receiveport.
     */
    private static final int ManyToOne 			= 0x00000004;

    /** Extra bit indicating total ordering. **/
    private static final int TotallyOrdered		= 0x00000008;

    /** When an Ibis implementation supports totally ordered multicast. */
    private static final int TotallyOrderedManyToOne	=
	TotallyOrdered | ManyToOne;

    /** When an Ibis implementation supports reliable communication. **/
    private static final int Reliable			= 0x00000010;

    /** When an Ibis implementation supports unreliable communication.
     *  Note: an Ibis implementation may support both unreliable and
     *  reliable communication.
     */
    private static final int Unreliable			= 0x00000020;

    /** When the user does not have to poll to get upcalls. **/
    private static final int NoPollForUpcalls		= 0x00000040;

    /** When the Ibis implementation supports upcalls. **/
    private static final int Upcalls			= 0x00000080;

    /** When the Ibis implementation supports explicit receipt. **/
    private static final int ExplicitReceipt		= 0x00000100;

    /** When the Ibis implementation supports connection administration. **/
    private static final int ConnectionAdministration	= 0x00000200;

    /** When the Ibis implementation supports connection upcalls. **/
    private static final int ConnectionUpcalls		= 0x00000400;

    //
    // World model properties.
    //

    /** When an Ibis implementation supports open-world. */
    private static final int OpenWorld 			= 0x00000800;

    /** When an Ibis implementation supports closed-world. */
    private static final int ClosedWorld		= 0x00001000;

    //
    // Serialization properties
    //

    /** When an Ibis implementation supports bytes and arrays of bytes. **/
    private static final int Bytes			= 0x00002000;

    /** When an Ibis implementation supports all primitive types and
     *  arrays of primitive types.
     */
    private static final int Data			= 0x00004000 | Bytes;

    /** When an Ibis implementation supports Ibis serialization. **/
    private static final int Ibis			= 0x00008000 | Data;

    /** When an Ibis implementation supports Sun serialization. **/
    private static final int Sun			= 0x00010000 | Data;

    /** Stores a property name with its value. **/
    private static class Prop {
	/** The property name. **/
	public String name;

	/** The property value. **/
	public int    val;
	
	/** Defines a <code>Prop</code> object with specified name and value.
	 *  @param name the name of this property.
	 *  @param val  the value of this property.
	 */
	Prop(String name, int val) {
	    this.name = name;
	    this.val = val;
	}
    }

    /** Serialization properties array. */
    private static final Prop[] SerializationProperties =
	new Prop[] { new Prop("bytes", Bytes),
		     new Prop("data",  Data),
		     new Prop("sun",   Sun),
		     new Prop("ibis",  Ibis)};

    /** World properties array. */
    private static final Prop[] WorldProperties =
	new Prop[] { new Prop("open",   OpenWorld),
		     new Prop("closed", ClosedWorld)};

    /** Communication properties array. */
    private static final Prop[] CommunicationProperties =
	new Prop[] { new Prop("OneToOne",	  OneToOne),
		     new Prop("OneToMany",	  OneToMany),
		     new Prop("ManyToOne",	  ManyToOne),
		     new Prop("TotallyOrderedManyToOne",
			      TotallyOrderedManyToOne),
		     new Prop("Reliable",	  Reliable),
		     new Prop("Unreliable",	  Unreliable),
		     new Prop("NoPollForUpcalls", NoPollForUpcalls),
		     new Prop("Upcalls",	  Upcalls),
		     new Prop("ExplicitReceipt",  ExplicitReceipt),
		     new Prop("ConnectionAdministration",
			      ConnectionAdministration),
		     new Prop("ConnectionUpcalls",ConnectionUpcalls)};

    private final int summary;

    /**
     * Creates a property matcher for the static properties specified.
     * The properties specified are the requirements to an Ibis
     * implementation.
     * @param sp the static properties.
     */
    public PropertyMatcher(StaticProperties sp) {
	summary = getProperties(sp, true);
    }

    /**
     * Returns a summary of the properties, associated with the given name and
     * properties array, from the given static properties.
     * @param sp the static properties.
     * @param p the props array.
     * @param name the name of the static property to extract a summary of.
     * @return the properties, as an int.
     */
    private static int extractProperties(StaticProperties sp,
					 Prop[] p,
					 String name) {
	String values = sp.find(name);
	int prop = 0;
	if (values != null) {
	    StringTokenizer t = new StringTokenizer(values,
						    ", \t\n\r\f");
	    while (t.hasMoreTokens()) {
		String s = t.nextToken();
		for (int i = 0; i < p.length; i++) {
		    if (s.equalsIgnoreCase(p[i].name)) {
			prop |= p[i].val;
			break;
		    }
		}
		// Unrecognized properties silently ignored?
	    }
	}
	return prop;
    }

    /**
     * Returns a summary of the specified properties, including possible
     * user-overrides.
     * @param sp the static properties.
     * @param allow_overrides if set, allows for overrides.
     * @return the properties, as an int.
     */
    private static int getProperties(StaticProperties sp,
				     boolean allow_overrides) {
	StaticProperties syssp = new StaticProperties(sp);

	if (allow_overrides) {
	    Properties p = System.getProperties();
	    Enumeration e = p.propertyNames();

	    while (e.hasMoreElements()) {
		String name = (String) e.nextElement();
		String prop = p.getProperty(name);
		if (name.substring(0,5).equals("ibis.")) {
		    String n = name.substring(5);
		    if (n.equals("world") ||
			n.equals("communication") ||
			n.equals("serialization")) {
			syssp.add(n, prop);
		    }
		}
	    }
	}

	return extractProperties(syssp,
				 CommunicationProperties,
				 "communication") |
	       extractProperties(syssp,
				 WorldProperties,
				 "world") |
	       extractProperties(syssp,
				 SerializationProperties,
				 "serialization");
    }

    /**
     * Matches the current required properties with the static properties
     * supplied.
     * @param sp the static properties to be matched with.
     * @return true if we have a match, false otherwise.
     */
    public boolean matchProperties(StaticProperties sp) {
	// Maybe build a cache of computed summaries?
	int props = getProperties(sp, false);

//	System.out.println("reqprops = " + summary + ", props = " + props);

	return ((props & summary) == summary);
    }
}

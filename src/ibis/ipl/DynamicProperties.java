package ibis.ipl;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Dynamic properties associated with a send or receive port.
 * Each port may have a set of dynamic properties associated with it,
 * for instance a buffer size. The details are Ibis-implementation-specific,
 * so any application depending on this is not portable across different Ibis
 * implementations. 
 * The default implementation does nothing.
 */
public class DynamicProperties {
    /**
     * Empty dynamic properties set. May be used by
     * {@link ibis.ipl.ReceivePort#properties() ReceivePort.properties()}
     * or {@link ibis.ipl.SendPort#properties() SendPort.properties()}
     * when these ports don't implement specific dynamic properties.
     */
    public static final DynamicProperties NoDynamicProperties = 
	new DynamicProperties();

    /**
     * Sets the property associated with the specified key to the
     * specified value.
     * The default implementation does nothing.
     * @param key the property to be set.
     * @param value the value for this property.
     * @exception IbisException may be thrown when a property is set to an
     * illegal value.
     */

    public void set(String key, Object value) throws IbisException {
    	/* do nothing */
    }

    /**
     * Returns the object associated with the specified key.
     * The default implementation just returns null.
     * @param key the property key.
     * @return the object associated with the specified key, or
     * <code>null</code> if not present.
     */
    public Object find(String key) {
	return null;
    }

    /**
     * Returns an enumeration for all current keys in this properties.
     * The default implementation returns an empty enumeration.
     * @return an enumeration for all current keys.
     */
    public Enumeration keys() {
	return new Enumeration() {
	    public boolean hasMoreElements() {
		return false;
	    }

	    public Object nextElement() {
		throw new NoSuchElementException("DynamicProperties keys()");
	    }
	};
    }
} 

/* $Id$ */

package ibis.satin;

/**
 * This interface is a marker interface for methods that change the value
 * of a Satin shared object.
 * A shared object that has methods that change the value of the object
 * must mark these methods as write methods. This is accomplished by
 * putting these methods in an interface that extends this marker interface.
 * The shared object class must implement this interface.
 */
public interface WriteMethodsInterface { 
	/* just a marker interface, no methods */
}

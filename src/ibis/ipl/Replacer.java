package ibis.ipl;

/** Replacer interface, for RMI support.
**/

public interface Replacer { 

	/**
	 * Replace an object. To be used when serializing an object, to determine
	 * if the object should be replaced with a stub. If so, the replace method
	 * returns the stub, otherwise it returns the parameter object.
	 **/
	public Object replace(Object v);
} 

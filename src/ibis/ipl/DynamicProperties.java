package ibis.ipl;

import java.util.Enumeration;

/**
 * None of the Ibis implementations do anything with this.
 * What should it do???
 */
public interface DynamicProperties { 
	public void set(String key, Object value) throws IbisException;
	public Object find(String key);
	public Enumeration keys();
} 

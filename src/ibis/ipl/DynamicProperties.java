package ibis.ipl;

import java.util.Enumeration;

public interface DynamicProperties { 

	public void set(String key, Object value) throws IbisException;
	public Object find(String key);
	public Enumeration keys();
} 

package ibis.ipl;

import java.util.Enumeration;
import java.util.Hashtable;

public class StaticProperties {

	private final Hashtable data = new Hashtable();

        public void add(String key, String value) throws IbisException { 
		if (data.containsKey(key)) { 
			throw new IbisException("Property already exists");
		} else { 
			try { 
				data.put(key, value);
			} catch (NullPointerException e) { 
				if (key == null) { 
					throw new IbisException("Property needs a key");
				} else { 
					throw new IbisException("Property needs a value");
				}
			} 
		} 
	}
	
	public String find(String key) { 
		return (String) data.get(key);
	} 
	
	public Enumeration keys() { 
		return data.keys();
	} 		      

	public int size() { 
		return data.size();
	}

	public boolean equals(Object other) {

		if (!(other instanceof StaticProperties)) { 
			return false;
		}

		StaticProperties temp = (StaticProperties) other;

		if (data.size() != temp.data.size()) { 
			return false;
		}

		Enumeration e = data.keys();

		while (e.hasMoreElements()) { 
			String key = (String) e.nextElement();		       			
			String value1 = (String) data.get(key);
			String value2 = (String) temp.data.get(key);

			if (!value1.equals(value2)) { 
				return false;
			}
		} 
		
		return true;
	} 

	public String toString() { 

		StringBuffer result = new StringBuffer("");

		Enumeration e = data.keys();

		while (e.hasMoreElements()) { 
			String key = (String) e.nextElement();		       			
			String value = (String) data.get(key);
			
			result.append(key);
			result.append(" = ");
			result.append(value);
			result.append("\n");			
		} 
		
		return result.toString();
	} 
}

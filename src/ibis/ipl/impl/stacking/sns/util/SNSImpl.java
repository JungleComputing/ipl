package ibis.ipl.impl.stacking.sns.util;

import java.util.HashMap;

public class SNSImpl {
   	private HashMap<String, SNS> SNSIMPL = new HashMap<String,SNS>();
   	
	public void addSNSImpl(String name, SNS impl ){
		SNSIMPL.put(name, impl);
	}
	
	public boolean containSNS(String name){
		return  SNSIMPL.containsKey(name);
	}
	
	public String[] getAllSNSNames() {
		return SNSIMPL.keySet().toArray(new String[SNSIMPL.size()]);
	}

	public SNS getSNSImpl(String name){
		return SNSIMPL.get(name);		
	}
}

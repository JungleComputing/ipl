package ibis.ipl.impl.stacking.sns.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

public class SNSID {
   	private HashMap<String, String> SNSids = new HashMap<String,String>();
   	private String key;
    	
   	public SNSID(String key){
   		this.key = key;
   	}
   	
	public void addSNSID(String SNSName, String SNSAlias){
		SNSids.put(SNSName, SNSAlias);
	}
	
	public String getSNSAlias(String SNSName){
		return SNSids.get(SNSName); 
	}  
	
	public boolean containSNS(String SNSName){
		return  SNSids.containsKey(SNSName);
	}
	
	public String[] getAllSNSNames() {
		return SNSids.keySet().toArray(new String[SNSids.size()]);
	}
	
	public String getKey(){
		return key;
	}
	
	public byte[] toByteArray ()
	{
	  byte[] bytes = null;
	  ByteArrayOutputStream bos = new ByteArrayOutputStream();
	  try {
	    ObjectOutputStream oos = new ObjectOutputStream(bos); 
	    oos.writeObject(SNSids);
	    oos.flush(); 
	    oos.close(); 
	    bos.close();
	    bytes = bos.toByteArray ();
	  }
	  catch (IOException ex) {
	    //TODO: Handle the exception
	  }
	  return bytes;
	}

	@SuppressWarnings("unchecked")
	public void readByteArray (byte[] bytes)
	{
		HashMap<String, String> obj = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream (bytes);
			ObjectInputStream ois = new ObjectInputStream (bis);
			obj = (HashMap<String, String>) ois.readObject();
		}
		catch (IOException ex) {
			//TODO: Handle the exception
		}
		catch (ClassNotFoundException ex) {
			//TODO: Handle the exception
		}

		SNSids = obj;
	}
}

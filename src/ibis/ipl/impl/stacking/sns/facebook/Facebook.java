package ibis.ipl.impl.stacking.sns.facebook;

import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.impl.stacking.sns.SNSProperties;
import ibis.ipl.impl.stacking.sns.util.SNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Facebook implements SNS{
	private String UID;
	private String UniqueID;
	private String applicationName;

	private FacebookPostMethod FPM;
	//private ArrayList<String> UIDList = new ArrayList<String>();
	private JSONObject responseObject;
	
	private static final Logger logger = LoggerFactory.getLogger(Facebook.class);
		
	public Facebook(Properties properties) throws IbisCreationFailedException {
        String sessionKey = properties.getProperty("sns.facebook.sessionkey");
		String secret = properties.getProperty("sns.facebook.secret");
		this.UID = properties.getProperty("sns.facebook.uid");
		this.applicationName = properties.getProperty(SNSProperties.APPLICATION_NAME);
		this.UniqueID = UUID.randomUUID().toString();
		
		if (sessionKey != null && 
				secret != null && 
			this.UID != null &&
			this.applicationName != null	) {
				FPM = new FacebookPostMethod(sessionKey, secret);
			
	            if (logger.isDebugEnabled()) {
	                logger.debug("Facebook: New SNS Object " + SNSProperties.FACEBOOK + " - " + 
	                		sessionKey + " " + 
	                		secret + " " +
	                		UID + " " +
	                		applicationName + " " +
	                		UniqueID);
	            }
				
		}
		else {
			throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
		}
	}

	@Override
	public String Name() {
		return SNSProperties.FACEBOOK;
	}

	@Override
	public String UserID() {
		return UID;
	}

	@Override
	public String UniqueID() {
		return UniqueID;
	}
	
	@Override
	public boolean isFriend(String otherUID) {
		String result = "false";
		
		try {
	        List <NameValuePair> Params = new ArrayList <NameValuePair>();
			Params.add(new BasicNameValuePair("uids1", UID));
			Params.add(new BasicNameValuePair("uids2", otherUID));

			responseObject = FPM.JSONcall("Friends.areFriends", Params);
			JSONArray entries = responseObject.getJSONArray("returnValue");
			JSONObject entry = entries.getJSONObject(0);
			
			result = entry.getString("are_friends");
					
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (result.equals("true"))
			return true;
		else
			return false;
	}

	@Override
	public void sendMessage(String otherUID, String content) {
		sendMessage(otherUID, null, content);
	}
	
	@Override
	public void sendMessage(String[] otherUID, String content) {
		sendMessage(otherUID, null, content);		
	}

	@Override
	public void sendMessage(String[] UID, String title, String content) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void sendMessage(String otherUID, String title, String content) {
		List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("uid", UID));
		Params.add(new BasicNameValuePair("title", FacebookProperties.MESSAGE_UIDKEY + FacebookProperties.DELIMITER + otherUID));
		Params.add(new BasicNameValuePair("content", content));
		
		try {			
			responseObject = FPM.JSONcall("Notes.create", Params);
			System.out.println("FaceBook :" + responseObject.toString());
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	@Override
	public String readMessage(String otherUID) {
		return readMessage(otherUID, null);
	}
	
	@Override
	public String readMessage(String otherUID, String title) {
		long timestamp = 0;
		String authKey = null;
		String query = "SELECT uid,created_time,title,content FROM note WHERE uid="+otherUID;
		
        List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("query", query ));
		
		try {
			responseObject = FPM.JSONcall("Fql.query", Params);
			JSONArray notes = responseObject.getJSONArray("returnValue");

			for(int i = 0; i < notes.length(); i ++){
				JSONObject note = notes.getJSONObject(i);
				
				String noteTitle = note.getString("title");
				String[] pair = noteTitle.split(FacebookProperties.DELIMITER);
				if (pair.length == 2) {
					String messageKey = pair[0];
					String recipientUID = pair[1];	
					
					if (messageKey.equals(FacebookProperties.MESSAGE_UIDKEY) && recipientUID.equals(UID)) 
					{		
						if (authKey == null || timestamp < note.getLong("created_time")){
							timestamp = note.getLong("created_time");
							authKey = note.getString("content");
						}				
					}
				}
			}

		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return authKey;
	}
}
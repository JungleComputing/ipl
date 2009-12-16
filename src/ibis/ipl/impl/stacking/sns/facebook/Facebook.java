package ibis.ipl.impl.stacking.sns.facebook;

import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.impl.stacking.sns.SNSProperties;
import ibis.ipl.impl.stacking.sns.util.SNS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Facebook implements SNS{
	private String UID;
	private String applicationName;

	private FacebookPostMethod FPM;
	//private ArrayList<String> UIDList = new ArrayList<String>();
	private JSONObject responseObject;
	
	public Facebook(Properties properties) throws IbisCreationFailedException {
        String sessionKey = properties.getProperty("sns.facebook.sessionkey");
		String secret = properties.getProperty("sns.facebook.secret");
		this.UID = properties.getProperty("sns.facebook.uid");
		this.applicationName = properties.getProperty(SNSProperties.APPLICATION_NAME);
		
		if (sessionKey != null && 
				secret != null && 
			this.UID != null &&
			this.applicationName != null	) {
				FPM = new FacebookPostMethod(sessionKey, secret);
		}
		else {
			throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
		}
	}

	/*
	public List<String> getAllFriends(){
		try {			 
			JSONObject responseObject = FPM.JSONcall("Friends.get");
			JSONArray responseArray = responseObject.getJSONArray("returnValue");
				
			for (int i = 0; i < responseArray.length(); i++) {
				UIDList.add(responseArray.getString(i));
			}		
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return UIDList;
	}	
	 */

	@Override
	public boolean isFriend(String otherUID) {	
	//public boolean areFriends(String otherUID){
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

	/*
	@Override
	public boolean isFriend(String otherUID) {
		if(UIDList.isEmpty()){
			getAllFriends();
		}
		
		return UIDList.contains(otherUID);
		
		//return areFriends();
	}
	*/

	@Override
	public void sendAuthenticationRequest(String otherUID, String content) {
		List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("uid", UID));
		Params.add(new BasicNameValuePair("title", FacebookVariables.MESSAGE_UIDKEY + FacebookVariables.DELIMITER + otherUID));
		Params.add(new BasicNameValuePair("content", content));
		
		try {
			responseObject = FPM.JSONcall("Notes.create", Params);
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
	public String getAuthenticationRequest(String otherUID) {
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
				
				System.out.println("note = " + note.toString());
				String noteTitle = note.getString("title");
				String[] pair = noteTitle.split(FacebookVariables.DELIMITER);
				if (pair.length == 2) {
					String messageKey = pair[0];
					String recipientUID = pair[1];	
					
					if (messageKey.equals(FacebookVariables.MESSAGE_UIDKEY) && recipientUID.equals(UID)) {//note.getString("title").equals(UID)){ //note.getString("uid").equals(otherUID) && 
						
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
	
	@Override
	public void sendSecretKey(String otherUID, String content) {
		List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("uid", UID));
		Params.add(new BasicNameValuePair("title", FacebookVariables.MESSAGE_SECRETKEY + FacebookVariables.DELIMITER + otherUID));
		Params.add(new BasicNameValuePair("content", content));
		
		try {
			responseObject = FPM.JSONcall("Notes.create", Params);
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
	public String getSecretKey(String otherUID) {
		long timestamp = 0;
		String secretKey = null;
		String query = "SELECT uid,created_time,title,content FROM note WHERE uid="+otherUID;
		
        List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("query", query ));
		
		try {
			responseObject = FPM.JSONcall("Fql.query", Params);
			JSONArray notes = responseObject.getJSONArray("returnValue");

			for(int i = 0; i < notes.length(); i ++){
				JSONObject note = notes.getJSONObject(i);
				
				System.out.println("note = " + note.toString());
				
				String noteTitle = note.getString("title");
				String[] pair = noteTitle.split(FacebookVariables.DELIMITER);
				if (pair.length == 2) {
					String messageKey = pair[0];
					String recipientUID = pair[1];				
					
					if (messageKey.equals(FacebookVariables.MESSAGE_SECRETKEY) && recipientUID.equals(UID)){					
						if (secretKey == null || timestamp < note.getLong("created_time")){
							timestamp = note.getLong("created_time");
							secretKey = note.getString("content");
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
		
		return secretKey;
	}

	@Override
	public String SNSName() {
		return "facebook";
	}

	@Override
	public String SNSUID() {
		return UID;
	}
}

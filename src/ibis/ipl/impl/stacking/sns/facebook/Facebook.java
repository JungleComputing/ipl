package ibis.ipl.impl.stacking.sns.facebook;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.stacking.sns.util.SNS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Facebook implements SNS{
	private static final String SECRET = "67620b30a597b403f4ea64b0acb6db81";
	private static final String API_KEY = "32b1d5aa847cfc5c9322628b18f1d267";
	
	private String token;
	private ArrayList<String> UIDList = new ArrayList<String>();
	private String sessionKey = null;
	private String secretGenerated = null;
	private String uid = null;
	private boolean isAuthenticated = false;

	JSONObject obj = new JSONObject();
	JSONArray objArray = new JSONArray();

	FacebookPostMethod FPM = new FacebookPostMethod(SECRET, API_KEY);

	public Facebook(String uid, String sessionKey, String secretGenerated ) {
		this.uid = uid;
		this.sessionKey = sessionKey;
		this.secretGenerated = secretGenerated;
		
		//if uid == or null throw exception
		
		FPM.setSessionKey(sessionKey);
		FPM.setSecretGenerated(secretGenerated);
	}
	
	public List getAllFriends(){
		try {
			obj = FPM.JSONcall("Friends.get");
			obj.toJSONArray(objArray);

			objArray = obj.getJSONArray("returnValue");
				
			for (int i = 0; i < objArray.length(); i++) {
				UIDList.add(objArray.getString(i));
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
	
	public boolean areFriends(String otherUID){
		String result = null;
		
		try {
	        List <NameValuePair> Params = new ArrayList <NameValuePair>();
			Params.add(new BasicNameValuePair("uids1", uid));
			Params.add(new BasicNameValuePair("uids2", otherUID));

			obj = FPM.JSONcall("Friends.AreFriends", Params);
			result = obj.getString("are_friends");
					
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (result == "true")
			return true;
		else
			return false;
	}

	@Override
	public boolean isAuthenticated() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFriend(String otherUID) {
		if(UIDList.isEmpty()){
			getAllFriends();
		}
		
		return UIDList.contains(otherUID);
		
		//return areFriends();
	}

	@Override
	public void sendAuthenticationRequest(String uid, String key) {
        List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("to_ids", uid));
		Params.add(new BasicNameValuePair("notification", key));
		
		try {
			obj = FPM.JSONcall("Notifications.send", Params);
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
	public String getAuthenticationRequest(String uid) {
		return null;
	}

	@Override
	public String snsName() {
		return "facebook";
	}

	@Override
	public String userID() {
		return uid;
	}
}

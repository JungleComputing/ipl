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

	private ArrayList<String> UIDList = new ArrayList<String>();
	private String userID = null;
	private String appName;
	//private boolean isAuthenticated = false;

	JSONObject obj; // = new JSONObject();
	JSONArray objArray; // = new JSONArray();

	FacebookPostMethod FPM;

	/*
	public Facebook(String uid, String sessionKey, String secretGenerated) {
		this.uid = uid;
		this.appName =;
		//if uid == null throw exception
		FPM = new FacebookPostMethod(sessionKey, secretGenerated);
	}
	*/
	
	public Facebook(Properties properties) throws IbisCreationFailedException {
        String sessionKey = properties.getProperty("sns.facebook.sessionkey");
		String secretGenerated = properties.getProperty("sns.facebook.secretGenerated");
		this.userID = properties.getProperty("sns.facebook.uid");
		this.appName = properties.getProperty(SNSProperties.APPLICATION_NAME);
		
		if (sessionKey != null && 
			secretGenerated != null && 
			userID != null &&
			appName != null	) {
				FPM = new FacebookPostMethod(sessionKey, secretGenerated);
		}
		else {
			throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
		}
		

	}

	public List<String> getAllFriends(){
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
			Params.add(new BasicNameValuePair("uids1", userID));
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
		Params.add(new BasicNameValuePair("uid", userID));
		Params.add(new BasicNameValuePair("title", appName));
		Params.add(new BasicNameValuePair("content", key));
		
		try {
			obj = FPM.JSONcall("Notes.create", Params);
			System.out.println(obj.toString());
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

        List <NameValuePair> Params = new ArrayList <NameValuePair>();
		Params.add(new BasicNameValuePair("uid", uid));
		
		try {
			obj = FPM.JSONcall("Notes.get", Params);
			System.out.println(obj.toString());
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
		
		//Get the only the notes for that has the same appname and the last timestamps
        
		return null;
	}

	@Override
	public String snsName() {
		return "facebook";
	}

	@Override
	public String userID() {
		return userID;
	}
}

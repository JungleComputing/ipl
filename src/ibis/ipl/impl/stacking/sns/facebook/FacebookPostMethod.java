package ibis.ipl.impl.stacking.sns.facebook;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class FacebookPostMethod{
	static final String VERSION = "1.0";
	static final String restServer = "http://api.facebook.com/restserver.php";
	final String SECRET;
	final String API_KEY;
	
	private HttpClient httpClient = new DefaultHttpClient();
	private JSONObject JSONResponse;
	private String responseBody;
	private String signature;
	private String sessionKey;
	private String secretGenerated;
	
	public FacebookPostMethod(String secret, String apiKey){
		SECRET = secret;
		API_KEY = apiKey;
	}

	public JSONObject JSONcall(String method) throws JSONException, IOException {		
//	public String JSONcall(String method) {		
		return JSONcall(method, new ArrayList<NameValuePair> (0) );		
	}

	public JSONObject JSONcall(String method, List <NameValuePair> params) throws JSONException, IOException {
//	public String JSONcall(String method, List <NameValuePair> params) {
		HttpPost post = new HttpPost(restServer);
		post.setHeader(HTTP.CONTENT_TYPE, "application/x-www-form-urlencoded");
		
        List <NameValuePair> nvp = new ArrayList <NameValuePair>();
        nvp.add(new BasicNameValuePair("method", method));
        nvp.add(new BasicNameValuePair("api_key", API_KEY));
        nvp.add(new BasicNameValuePair("v", VERSION));
        nvp.add(new BasicNameValuePair("format", "JSON"));
        
	    if (sessionKey != null) {
	        nvp.add(new BasicNameValuePair("call_id", String.valueOf(System.currentTimeMillis())));
	        nvp.add(new BasicNameValuePair("session_key", sessionKey));
		}
	    
	    if (!params.isEmpty())
	    	nvp.addAll(params);
	    
	    if (secretGenerated != null) {
	    	signature = sign(secretGenerated, nvp);
	    }
	    else{
		    signature = sign(SECRET, nvp);
	    }
	    nvp.add(new BasicNameValuePair("sig", signature));
	    
		post.setEntity(new UrlEncodedFormEntity(nvp, HTTP.UTF_8));
		
    	HttpResponse response = httpClient.execute(post);

    	int rc = response.getStatusLine().getStatusCode();
		if (rc == 200){ 
			String responseBody = EntityUtils.toString(response.getEntity());
//			JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity()));
			
			if (responseBody.equals("\"\"")) {
				responseBody = "null";
			}

			//if (!responseBody.startsWith("[") && !responseBody.startsWith("{")) {
			if (!responseBody.startsWith("{")) {
				responseBody = "{\"returnValue\":" + responseBody + "}";
			}
						      
			try {
				JSONResponse = new JSONObject(responseBody);
			}catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
		
		}

	    
	    return JSONResponse;
//	    return responseBody;
	}	
    private String sign(String secretKey, List <NameValuePair> params){
	    List<String> sigParams = new ArrayList<String>(params.size());
	    for (NameValuePair nvp : params) {
	      sigParams.add(nvp.getName() + "=" + nvp.getValue());
	    }
	    
	    Collections.sort(sigParams);
	    
	    StringBuilder buffer = new StringBuilder();
	    for (String param: sigParams) {
	        buffer.append(param);
	    }

	    buffer.append(secretKey);
	
	    StringBuilder result = new StringBuilder();	
	   
    	try {
    		MessageDigest MD =  MessageDigest.getInstance("MD5");
    	    for (byte b : MD.digest(buffer.toString().getBytes())) {
    	    	result.append(Integer.toHexString((b & 0xf0) >>> 4));
    	    	result.append(Integer.toHexString(b & 0x0f));
      		}
    	} catch (NoSuchAlgorithmException e) {
    		throw new RuntimeException("MD5 MessageDigest is missing");
    	} 
		      
	    return result.toString();
    	
    }
    
    //GETTER AND SETTER METHODS
    public void setSessionKey(String sk){
    	sessionKey = sk;
    }
    
    public void setSecretGenerated(String sg){
    	secretGenerated = sg;
    }    
}


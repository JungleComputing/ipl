package ibis.ipl.impl.stacking.sns;

import java.util.Properties;

import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.impl.stacking.sns.facebook.Facebook;
import ibis.ipl.impl.stacking.sns.util.SNS;

public final class SNSFactory {
	public static SNS createSNS(String name, Properties properties) throws IbisCreationFailedException{
    	
		if(name.equals("facebook")) {
    	    		
            //String sessionKey = properties.getProperty("sns.facebook.sessionkey");
    		//String secretGenerated = properties.getProperty("sns.facebook.secretGenerated");
    		//String uid = properties.getProperty("sns.facebook.uid");
    		//String appName = properties.getProperty(SNSProperties.APPLICATION_NAME);
 		
    		//if (sessionKey != null && secretGenerated != null && uid != null) {    		
    			//return new Facebook(uid, sessionKey, secretGenerated);
			SNS facebook = new Facebook(properties);
			
    		return facebook;
    		//}
    		//else {
    			//throw new IbisCreationFailedException("SNSIbis: SNS implementation cannot be created"); 
    		//}
    	}
		
		return null;
		
	}
	
}

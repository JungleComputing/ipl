package ibis.ipl.impl.stacking.sns;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.impl.stacking.sns.facebook.Facebook;
import ibis.ipl.impl.stacking.sns.util.SNS;

/**
 * A factory to initialize the SNS objects
 */
public final class SNSFactory {
	private static final Logger logger = LoggerFactory.getLogger(SNSFactory.class);
	
	public static SNS createSNS(String name, Properties properties) throws IbisCreationFailedException{
    	
		if(name.equals(SNSProperties.FACEBOOK)) {			
            if (logger.isDebugEnabled()) {
                logger.debug("SNSFactory: SNS " + SNSProperties.FACEBOOK + " is selected ");
            }
            
			SNS facebook = new Facebook(properties);
			
    		return facebook;
    	}
		
		if(name.equals(SNSProperties.HYVES)) {
			//TO DO
		}
		
		return null;
		
	}
	
}

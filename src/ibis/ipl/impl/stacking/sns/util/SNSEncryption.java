package ibis.ipl.impl.stacking.sns.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SNSEncryption {
	
    //PrivateKey privateKey;
    //PublicKey publicKey;
	SecretKey key;

	public void initialize(){
		//String params = genDhParams();
		//generateKeyPair(params);	
		
	    try {
	        // Generate a DES key
	        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
	        key = keyGen.generateKey();
	    
	        
	        
	        // Generate a Blowfish key
	        //keyGen = KeyGenerator.getInstance("Blowfish");
	        //key = keyGen.generateKey();
	    
	        // Generate a triple DES key
	        //keyGen = KeyGenerator.getInstance("DESede");
	        //key = keyGen.generateKey();
	    } catch (java.security.NoSuchAlgorithmException e) {}
	}

	public void initialize(byte[] encodedKey) {
	    key = new SecretKeySpec(encodedKey, "DES");	    
	}	
	
	public SecretKey getSecretKey() {
		return key;
	}




/*	
    // Returns a comma-separated string of 3 values.
    // The first number is the prime modulus P.
    // The second number is the base generator G.
    // The third number is bit size of the random exponent L.
    public static String genDhParams() {
        try {
            // Create the parameter generator for a 1024-bit DH key pair
            AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
            paramGen.init(1024);
    
            // Generate the parameters
            AlgorithmParameters params = paramGen.generateParameters();
            DHParameterSpec dhSpec
                = (DHParameterSpec)params.getParameterSpec(DHParameterSpec.class);
    
            // Return the three values in a string
            return ""+dhSpec.getP()+","+dhSpec.getG()+","+dhSpec.getL();
        } catch (NoSuchAlgorithmException e) {
        } catch (InvalidParameterSpecException e) {
        }
        return null;
    }

	
	public void generateKeyPair(String params){
	    String[] values = params.split(",");
	    BigInteger p = new BigInteger(values[0]);
	    BigInteger g = new BigInteger(values[1]);
	    int l = Integer.parseInt(values[2]);
	    
	    try {
	        // Use the values to generate a key pair
	        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
	        DHParameterSpec dhSpec = new DHParameterSpec(p, g, l);
	        keyGen.initialize(dhSpec);
	        KeyPair keypair = keyGen.generateKeyPair();
	    
	        // Get the generated public and private keys
	        privateKey = keypair.getPrivate();
	        publicKey = keypair.getPublic();

	    } 
	    catch (java.security.InvalidAlgorithmParameterException e) {}
	    catch (java.security.NoSuchAlgorithmException e) {}   
	}

	public SecretKey generateSessionKey(PublicKey publicKey){
        // Send the public key bytes to the other party...
        //byte[] publicKeyBytes = publicKey.getEncoded();
    
        // Retrieve the public key bytes of the other party
        //publicKeyBytes = ...;
		byte[] publicKeyBytes = publicKey.getEncoded();
		
        try {
	        // Convert the public key bytes into a PublicKey object
	        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
	        KeyFactory keyFact = KeyFactory.getInstance("DH");
	        publicKey = keyFact.generatePublic(x509KeySpec);
	    
	        // Prepare to generate the secret key with the private key and public key of the other party
	        KeyAgreement ka = KeyAgreement.getInstance("DH");
	        ka.init(privateKey);
	        ka.doPhase(publicKey, true);
	    
	        // Specify the type of key to generate;
	        // see e458 Listing All Available Symmetric Key Generators
	        String algorithm = "DES";
	    
	        // Generate the secret key
	        SecretKey secretKey = ka.generateSecret(algorithm);
	    
	        return secretKey;
	        // Use the secret key to encrypt/decrypt data;
	        // see e462 Encrypting a String with DES
        }
        catch (java.security.InvalidKeyException e) {}
        catch (java.security.spec.InvalidKeySpecException e) {}
        catch (NoSuchAlgorithmException e) {}
        
        return null;
	}
	
	*/
}




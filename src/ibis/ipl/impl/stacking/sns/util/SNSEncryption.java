package ibis.ipl.impl.stacking.sns.util;

import java.security.InvalidKeyException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SNSEncryption {
	
	SecretKey key;
	Cipher eCipher;
	Cipher dCipher;

	public void initialize(){
		
	    try {
	        // Generate a DES key
	        KeyGenerator keyGen = KeyGenerator.getInstance("DES");
	        key = keyGen.generateKey();
	        
	    	eCipher = Cipher.getInstance("DES");
	    	dCipher = Cipher.getInstance("DES");
	        eCipher.init(Cipher.ENCRYPT_MODE, key);
	        dCipher.init(Cipher.DECRYPT_MODE, key);

	    } catch (java.security.NoSuchAlgorithmException e) {
	    	e.printStackTrace();
	    } catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}
	}

	public void initialize(byte[] encodedKey) {
	    key = new SecretKeySpec(encodedKey, "DES");	    
	}	
	
	public SecretKey getSecretKey() {
		return key;
	}
	
	public Cipher getECipher() {
		return eCipher;
	}

	public Cipher getDCipher() {
		return dCipher;
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




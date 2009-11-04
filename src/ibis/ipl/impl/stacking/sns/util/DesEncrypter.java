package ibis.ipl.impl.stacking.sns.util;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

public class DesEncrypter {
    Cipher ecipher;
    Cipher dcipher;

    DesEncrypter(SecretKey key) {
        try {
            ecipher = Cipher.getInstance("DES");
            dcipher = Cipher.getInstance("DES");
            ecipher.init(Cipher.ENCRYPT_MODE, key);
            dcipher.init(Cipher.DECRYPT_MODE, key);

        } catch (javax.crypto.NoSuchPaddingException e) {
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch (java.security.InvalidKeyException e) {
        }
    }

    public byte[] encrypt(byte[] data) {
        try {
            // Encode the string into bytes using utf-8
            //byte[] utf8 = str.getBytes("UTF8");

            // Encrypt
            byte[] enc = ecipher.doFinal(data);

            // Encode bytes to base64 to get a string
            //return new sun.misc.BASE64Encoder().encode(enc);
            return enc;
        } 
        catch (javax.crypto.BadPaddingException e) {} 
        catch (IllegalBlockSizeException e) {} 
        //catch (UnsupportedEncodingException e) {} 
        //catch (java.io.IOException e) {}
        return null;
    }

    public byte[] decrypt(byte[] data) {
        try {
            // Decode base64 to get bytes
            //byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);

            // Decrypt
            byte[] utf8 = dcipher.doFinal(data);

            // Decode using utf-8
            //return new String(utf8, "UTF8");
        } 
        catch (javax.crypto.BadPaddingException e) {} 
        catch (IllegalBlockSizeException e) {} 
        //catch (UnsupportedEncodingException e) {} 
        //catch (java.io.IOException e) {}
        return null;
    }
}
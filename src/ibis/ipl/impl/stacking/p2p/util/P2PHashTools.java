package ibis.ipl.impl.stacking.p2p.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * implements various hash functions used to assign random identifiers to nodes
 * in the overlay network
 * 
 * @author Delia
 * 
 */
public class P2PHashTools {

	/**
	 * compute MD5 hash value of text
	 * 
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static String MD5(String text) {
		// select algorithm and update digest
		MessageDigest algorithm;
		try {
			algorithm = MessageDigest.getInstance("MD5");

			algorithm.reset();
			byte[] data = text.getBytes();
			algorithm.update(data, 0, data.length);
			BigInteger i = new BigInteger(1, algorithm.digest());

			return String.format("%1$032X", i);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * compute MD5 of text and convert the result in base radix
	 * 
	 * @param text
	 * @param radix
	 * @return
	 */
	static String MD5(String text, int radix) {
		// compute MD5 and convert from base 16 to base 10
		BigInteger bigNumber = new BigInteger(MD5(text), 16);

		// convert from base 10 to specified base
		String md5Text = bigNumber.toString(radix);

		return md5Text;
	}

	/**
	 * convert array of bytes returned by MessageDigest.digest to hex
	 * 
	 * @param data
	 * @return
	 */
	private static String convertToHex(byte[] data) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buffer.append((char) ('0' + halfbyte));
				else
					buffer.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}

		return buffer.toString();
	}

	/**
	 * compute SHA1 value of text and return hex representation
	 * 
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static String SHA1(String text) {
		byte[] sha1hash = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			sha1hash = new byte[40];
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			sha1hash = md.digest();
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return convertToHex(sha1hash);
	}

	/**
	 * compute SHA1 of text and convert the result in base radix
	 * 
	 * @param text
	 * @param radix
	 * @return
	 */
	public static String SHA1(String text, int radix) {
		// compute SHA1 of text
		BigInteger bigNumber = new BigInteger(SHA1(text), 16);

		// convert to specified radix
		String sha1Text = bigNumber.toString(radix);

		return sha1Text;
	}
}

package crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {
	public static SecureRandom sr;
	public static MessageDigest sha1;
	public static int secParam;
	public static int secParamBytes;

	static {
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
			sha1 = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		secParam = 80;
		secParamBytes = (secParam + 7) / 8;
	}
}

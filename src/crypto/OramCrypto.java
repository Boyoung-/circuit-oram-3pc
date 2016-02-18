package crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class OramCrypto {
	public static SecureRandom sr;
	
	static {
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}

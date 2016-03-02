package crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {
	public static SecureRandom sr;

	static {
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}

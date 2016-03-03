package crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {
	public static SecureRandom sr;
	public static int secParam;
	public static int secParamBytes;

	static {
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		secParam = 80;
		secParamBytes = (secParam + 7) / 8;
	}
}

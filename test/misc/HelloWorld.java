package misc;

import java.math.BigInteger;
import java.util.Arrays;

public class HelloWorld {

	public static void main(String[] args) {
		System.out.println("HelloWorld!");

		byte[] tmp = new byte[3];
		BigInteger bi = new BigInteger(1, tmp);
		System.out.println(bi.toByteArray().length);

		// System.out.println(tmp[3]);

		System.out.println(Arrays.copyOfRange(tmp, 2, 1).length);
		// throw new ArrayIndexOutOfBoundsException("" + 11);

		long a = 1L << -3;
	}

}

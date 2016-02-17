package misc;

import java.math.BigInteger;

public class HelloWorld {

	public static void main(String[] args) {
		System.out.println("HelloWorld!");

		byte[] tmp = new byte[0];
		BigInteger bi = new BigInteger(1, tmp);
		System.out.println(bi.toByteArray().length);
	}

}

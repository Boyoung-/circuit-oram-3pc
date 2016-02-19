package misc;

import java.math.BigInteger;

public class HelloWorld {

	public static void main(String[] args) {
		System.out.println("HelloWorld!");

		byte[] tmp = new byte[3];
		BigInteger bi = new BigInteger(1, tmp);
		System.out.println(bi.toByteArray().length);

		// System.out.println(tmp[3]);

		// System.out.println(Arrays.copyOfRange(tmp, 2, 1).length);

		byte[] a = new byte[] { 0 };
		byte[] b = a.clone();
		a[0] = 1;
		System.out.println(a[0] + " " + b[0]);
		// throw new ArrayIndexOutOfBoundsException("" + 11);

		System.out.println((new long[3])[0]);
	}

}

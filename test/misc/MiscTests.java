package misc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crypto.Crypto;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import util.StopWatch;
import util.Util;

public class MiscTests {

	public static void main(String[] args) {
		/*
		 * System.out.println("HelloWorld!");
		 * 
		 * byte[] tmp = new byte[3]; BigInteger bi = new BigInteger(1, tmp);
		 * System.out.println(bi.toByteArray().length);
		 * 
		 * // System.out.println(tmp[3]);
		 * 
		 * // System.out.println(Arrays.copyOfRange(tmp, 2, 1).length);
		 * 
		 * byte[] a = new byte[] { 0 }; byte[] b = a.clone(); a[0] = 1;
		 * System.out.println(a[0] + " " + b[0]); // throw new
		 * ArrayIndexOutOfBoundsException("" + 11);
		 * 
		 * System.out.println((new long[3])[0]);
		 * 
		 * byte[] negInt = Util.intToBytes(-3); System.out.println(new
		 * BigInteger(negInt).intValue());
		 * 
		 * byte aa = 1; aa ^= 1; System.out.println(aa);
		 */

		/*
		 * Metadata md = new Metadata(); Forest forest =
		 * Forest.readFromFile(md.getDefaultForestFileName()); forest.print();
		 */

		/*
		 * StopWatch sw1 = new StopWatch(); StopWatch sw2 = new StopWatch();
		 * byte[] arr1 = Util.nextBytes((int) Math.pow(2, 20), Crypto.sr);
		 * byte[] arr2 = Util.nextBytes((int) Math.pow(2, 20), Crypto.sr);
		 * 
		 * sw1.start(); Util.xor(arr1, arr2); sw1.stop();
		 * 
		 * sw2.start(); new BigInteger(1, arr1).xor(new BigInteger(1,
		 * arr2)).toByteArray(); sw2.stop();
		 * 
		 * System.out.println(sw1.toMS()); System.out.println(sw2.toMS());
		 */

		int n = 20;
		Integer[] oldArr = new Integer[n];
		for (int i = 0; i < n; i++)
			oldArr[i] = Crypto.sr.nextInt(50);
		int[] pi = Util.randomPermutation(n, Crypto.sr);
		int[] pi_ivs = Util.inversePermutation(pi);
		Integer[] newArr = Util.permute(oldArr, pi);
		newArr = Util.permute(newArr, pi_ivs);

		for (int i = 0; i < n; i++) {
			System.out.println(oldArr[i] + " " + newArr[i]);
		}
	}

}

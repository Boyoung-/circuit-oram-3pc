package misc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import oram.Forest;
import oram.Metadata;
import util.Util;

public class HelloWorld {

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

		Metadata md = new Metadata();
		Forest forest = Forest.readFromFile(md.getDefaultForestFileName());
		forest.print();
	}

}

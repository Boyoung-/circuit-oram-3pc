package oram;

import java.math.BigInteger;

public class TestTuple {

	public static void main(String[] args) {
		byte[] F = new byte[] { 1 };
		byte[] N = new byte[] { 2 };
		byte[] L = new byte[] { 3 };
		byte[] A = new byte[] { 4 };
		Tuple tuple = new Tuple(F, N, L, A);
		System.out.println(tuple);
		System.out.println(new BigInteger(1, tuple.toByteArray()).toString(2));

		F = new byte[0];
		N = new byte[0];
		L = new byte[0];
		tuple = new Tuple(F, N, L, A);
		System.out.println(tuple);
		System.out.println(new BigInteger(1, tuple.toByteArray()).toString(2));
	}

}

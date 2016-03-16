package util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import exceptions.LengthNotMatchException;

public class Util {
	public static boolean equal(byte[] a, byte[] b) {
		if (a.length == 0 && b.length == 0)
			return true;
		if (a.length != b.length)
			return false;
		return new BigInteger(a).compareTo(new BigInteger(b)) == 0;
	}

	public static byte[] nextBytes(int len, Random r) {
		byte[] data = new byte[len];
		r.nextBytes(data);
		return data;
	}

	public static long nextLong(long range, Random r) {
		long bits, val;
		do {
			bits = (r.nextLong() << 1) >>> 1;
			val = bits % range;
		} while (bits - val + (range - 1) < 0L);
		return val;
	}

	public static long getSubBits(long l, int end, int start) {
		if (start < 0)
			throw new IllegalArgumentException(start + " < 0");
		if (start > end)
			throw new IllegalArgumentException(start + " > " + end);
		long mask = (1L << (end - start)) - 1L;
		return (l >>> start) & mask;
	}

	public static BigInteger getSubBits(BigInteger bi, int end, int start) {
		if (start < 0)
			throw new IllegalArgumentException(start + " < 0");
		if (start > end)
			throw new IllegalArgumentException(start + " > " + end);
		BigInteger mask = BigInteger.ONE.shiftLeft(end - start).subtract(BigInteger.ONE);
		return bi.shiftRight(start).and(mask);
	}

	public static long setSubBits(long target, long input, int end, int start) {
		input = getSubBits(input, end - start, 0);
		long trash = getSubBits(target, end, start);
		return ((trash ^ input) << start) ^ target;
	}

	public static BigInteger setSubBits(BigInteger target, BigInteger input, int end, int start) {
		if (input.bitLength() > end - start)
			input = getSubBits(input, end - start, 0);
		BigInteger trash = getSubBits(target, end, start);
		return trash.xor(input).shiftLeft(start).xor(target);
	}

	public static byte[] rmSignBit(byte[] arr) {
		if (arr[0] == 0)
			return Arrays.copyOfRange(arr, 1, arr.length);
		return arr;
	}

	// c = a ^ b
	public static byte[] xor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		byte[] c = new byte[a.length];
		for (int i = 0; i < a.length; i++)
			c[i] = (byte) (a[i] ^ b[i]);
		return c;
	}

	// a = a ^ b to save memory
	public static void setXor(byte[] a, byte[] b) {
		if (a.length != b.length)
			throw new LengthNotMatchException(a.length + " != " + b.length);
		for (int i = 0; i < a.length; i++)
			a[i] = (byte) (a[i] ^ b[i]);
	}

	public static byte[] intToBytes(int i) {
		ByteBuffer bb = ByteBuffer.allocate(4);
		bb.putInt(i);
		return bb.array();
	}

	public static int bytesToInt(byte[] b) {
		return new BigInteger(b).intValue();
	}

	public static int[] randomPermutation(int len, Random rand) {
		List<Integer> list = new ArrayList<Integer>(len);
		for (int i = 0; i < len; i++)
			list.add(i);
		Collections.shuffle(list, rand);
		int[] array = new int[len];
		for (int i = 0; i < len; i++)
			array[i] = list.get(i);
		return array;
	}

	public static int[] inversePermutation(int[] p) {
		int[] ip = new int[p.length];
		for (int i = 0; i < p.length; i++)
			ip[p[i]] = i;
		return ip;
	}

	public static <T> T[] permute(T[] original, int[] p) {
		@SuppressWarnings("unchecked")
		T[] permuted = (T[]) new Object[original.length];
		for (int i = 0; i < original.length; i++)
			permuted[p[i]] = original[i];
		return permuted;
	}

	public static byte[] longToBytes(long l, int numBytes) {
		byte[] bytes = BigInteger.valueOf(l).toByteArray();
		if (bytes.length == numBytes)
			return bytes;
		else if (bytes.length > numBytes)
			return Arrays.copyOfRange(bytes, bytes.length - numBytes, bytes.length);
		else {
			byte[] out = new byte[numBytes];
			System.arraycopy(bytes, 0, out, numBytes - bytes.length, bytes.length);
			return out;
		}
	}

	public static void debug(String s) {
		// only to make Communication.java compile
	}

	public static void disp(String s) {
		// only to make Communication.java compile
	}

	public static void error(String s) {
		// only to make Communication.java compile
	}

	public static void error(String s, Exception e) {
		// only to make Communication.java compile
	}
}

package util;

import java.util.Arrays;
import java.util.Random;

public class Util {
	public static long nextLong(Random r, long range) {
		long bits, val;
		do {
			bits = (r.nextLong() << 1) >>> 1;
			val = bits % range;
		} while (bits - val + (range - 1) < 0L);
		return val;
	}

	public static long getSubBits(long l, long end, long start) {
		if (start < 0)
			throw new IllegalArgumentException(start + " < 0");
		if (start > end)
			throw new IllegalArgumentException(start + " > " + end);
		long mask = (1L << (end - start)) - 1L;
		return (l >>> start) & mask;
	}

	public static long setSubBits(long target, long input, long end, long start) {
		long len = end - start;
		input = getSubBits(input, len, 0);
		long trash = getSubBits(target, end, start);
		return ((trash ^ input) << start) ^ target;
	}

	public static byte[] rmSignBit(byte[] arr) {
		if (arr[0] == 0)
			return Arrays.copyOfRange(arr, 1, arr.length);
		return arr;
	}
}

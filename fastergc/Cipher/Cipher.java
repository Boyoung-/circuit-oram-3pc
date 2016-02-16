// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package Cipher;

import java.security.*;
import java.math.*;

import sprout.crypto.SR;
import YaoGC.Wire;

public final class Cipher {
	private static final int unitLength = 160; // SHA-1 has 160-bit output.

	public static final BigInteger mask = BigInteger.ONE.shiftLeft(
			Wire.labelBitLength).subtract(BigInteger.ONE);

	private static final BigInteger mask128 = BigInteger.ONE.shiftLeft(128)
			.subtract(BigInteger.ONE);
	
	private static final BigInteger mask160 = BigInteger.ONE.shiftLeft(160)
			.subtract(BigInteger.ONE);

	private static MessageDigest sha1 = SR.digest;
	
	public static synchronized BigInteger encrypt(BigInteger lp0,
			BigInteger lp1, int k0, int k1, BigInteger m) {
		BigInteger ret = getPadding(lp0, lp1, k0, k1);
		ret = ret.xor(m);

		return ret;
	}

	public static synchronized BigInteger encrypt(BigInteger lp0,
			BigInteger lp1, int k, BigInteger m) {
		BigInteger ret = getPadding(lp0, lp1, k);
		ret = ret.xor(m);

		return ret;
	}
	
	public static synchronized BigInteger decrypt(BigInteger lp0,
			BigInteger lp1, int k0, int k1, BigInteger c) {
		BigInteger ret = getPadding(lp0, lp1, k0, k1);
		ret = ret.xor(c);

		return ret;
	}

	public static synchronized BigInteger decrypt(BigInteger lp0,
			BigInteger lp1, int k, BigInteger c) {
		BigInteger ret = getPadding(lp0, lp1, k);
		ret = ret.xor(c);

		return ret;
	}

	public static synchronized BigInteger encrypt(int w, BigInteger key,
			int outBit) {
		sha1.update(BigInteger.valueOf(w).toByteArray());
		sha1.update(key.toByteArray());
		return new BigInteger(sha1.digest()).and(mask128).xor(
				BigInteger.valueOf(outBit));
	}

	public static synchronized int decrypt(int w, BigInteger key, BigInteger c) {
		sha1.update(BigInteger.valueOf(w).toByteArray());
		sha1.update(key.toByteArray());
		return new BigInteger(sha1.digest()).and(mask128).xor(c).intValue();
	}

	// this padding generation function is dedicated for encrypting garbled
	// tables.
	private static synchronized BigInteger getPadding(BigInteger lp0,
			BigInteger lp1, int k) {
		sha1.update(lp0.toByteArray());
		sha1.update(lp1.toByteArray());
		sha1.update(BigInteger.valueOf(k).toByteArray());
		return (new BigInteger(sha1.digest())).and(mask);
	}
	
	private static synchronized BigInteger getPadding(BigInteger lp0,
			BigInteger lp1, int k0, int k1) {
		sha1.update(lp0.toByteArray());
		sha1.update(lp1.toByteArray());
		sha1.update(BigInteger.valueOf(k0).toByteArray());
		sha1.update(BigInteger.valueOf(k1).toByteArray());
		return (new BigInteger(sha1.digest())).and(mask160);
	}

	public static synchronized BigInteger encrypt(BigInteger key,
			BigInteger msg, int msgLength) {
		return msg.xor(getPaddingOfLength(key, msgLength));
	}

	public static synchronized BigInteger decrypt(BigInteger key,
			BigInteger cph, int cphLength) {
		return cph.xor(getPaddingOfLength(key, cphLength));
	}

	private static synchronized BigInteger getPaddingOfLength(BigInteger key,
			int padLength) {
		sha1.update(key.toByteArray());
		BigInteger pad = BigInteger.ZERO;
		byte[] tmp = new byte[unitLength / 8];
		for (int i = 0; i < padLength / unitLength; i++) {
			System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
			pad = pad.shiftLeft(unitLength).xor(new BigInteger(1, tmp));
			sha1.update(tmp);
		}
		System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
		pad = pad.shiftLeft(padLength % unitLength).xor(
				(new BigInteger(1, tmp)).shiftRight(unitLength
						- (padLength % unitLength)));
		return pad;
	}

	public static synchronized BigInteger encrypt(int j, BigInteger key,
			BigInteger msg, int msgLength) {
		return msg.xor(getPaddingOfLength(j, key, msgLength));
	}

	public static synchronized BigInteger decrypt(int j, BigInteger key,
			BigInteger cph, int cphLength) {
		return cph.xor(getPaddingOfLength(j, key, cphLength));
	}

	private static synchronized BigInteger getPaddingOfLength(int j,
			BigInteger key, int padLength) {
		sha1.update(BigInteger.valueOf(j).toByteArray());
		sha1.update(key.toByteArray());
		BigInteger pad = BigInteger.ZERO;
		byte[] tmp = new byte[unitLength / 8];
		for (int i = 0; i < padLength / unitLength; i++) {
			System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
			pad = pad.shiftLeft(unitLength).xor(new BigInteger(1, tmp));
			sha1.update(tmp);
		}
		System.arraycopy(sha1.digest(), 0, tmp, 0, unitLength / 8);
		pad = pad.shiftLeft(padLength % unitLength).xor(
				(new BigInteger(1, tmp)).shiftRight(unitLength
						- (padLength % unitLength)));
		return pad;
	}
}
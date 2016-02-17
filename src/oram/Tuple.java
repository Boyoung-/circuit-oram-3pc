package oram;

import java.math.BigInteger;

public class Tuple {
	private byte[] F;
	private byte[] N;
	private byte[] L;
	private byte[] A;

	public Tuple(byte[] f, byte[] n, byte[] l, byte[] a) {
		F = f.clone();
		N = n.clone();
		L = l.clone();
		A = a.clone();
	}

	public byte[] getF() {
		return F;
	}

	public byte[] getN() {
		return N;
	}

	public byte[] getL() {
		return L;
	}

	public byte[] getA() {
		return A;
	}

	public byte[] toByteArray() {
		byte[] tuple = new byte[F.length + N.length + L.length + A.length];
		int offset = 0;
		System.arraycopy(F, 0, tuple, offset, F.length);
		offset += F.length;
		System.arraycopy(N, 0, tuple, offset, N.length);
		offset += N.length;
		System.arraycopy(L, 0, tuple, offset, L.length);
		offset += L.length;
		System.arraycopy(A, 0, tuple, offset, A.length);
		return tuple;
	}

	@Override
	public String toString() {
		String str = "Tuple: ";
		str += ("F=" + new BigInteger(1, getF()).toString(2) + ", ");
		str += ("N=" + new BigInteger(1, getN()).toString(2) + ", ");
		str += ("L=" + new BigInteger(1, getL()).toString(2) + ", ");
		str += ("A=" + new BigInteger(1, getA()).toString(2));
		return str;
	}

}

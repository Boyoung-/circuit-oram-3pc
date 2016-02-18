package oram;

import java.math.BigInteger;

public class Tuple {
	private int numBytes;
	private byte[] F;
	private byte[] N;
	private byte[] L;
	private byte[] A;

	public Tuple(int fs, int ns, int ls, int as) {
		numBytes = fs + ns + ls + as;
		F = new byte[fs];
		N = new byte[ns];
		L = new byte[ls];
		A = new byte[as];
	}

	public Tuple(byte[] f, byte[] n, byte[] l, byte[] a) {
		numBytes = f.length + n.length + l.length + a.length;
		F = f.clone();
		N = n.clone();
		L = l.clone();
		A = a.clone();
	}

	public int getNumBytes() {
		return numBytes;
	}

	public byte[] getF() {
		return F;
	}

	public void setF(byte[] f) {
		F = f.clone();
	}

	public byte[] getN() {
		return N;
	}

	public void setN(byte[] n) {
		N = n.clone();
	}

	public byte[] getL() {
		return L;
	}

	public void setL(byte[] l) {
		L = l.clone();
	}

	public byte[] getA() {
		return A;
	}

	public void setA(byte[] a) {
		A = a.clone();
	}

	public byte[] toByteArray() {
		byte[] tuple = new byte[numBytes];
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

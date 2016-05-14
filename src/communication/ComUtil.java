package communication;

import java.util.Arrays;

import com.oblivm.backend.gc.GCSignal;

import oram.Tuple;
import util.Util;

public class ComUtil {
	public static byte[] toByteArray(byte[][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		byte[] out = new byte[len1 * len2];
		for (int i = 0; i < len1; i++)
			System.arraycopy(in[i], 0, out, i * len2, len2);
		return out;
	}

	public static byte[][] toDoubleByteArray(byte[] in, int len1) {
		int len2 = in.length / len1;
		byte[][] out = new byte[len1][];
		for (int i = 0; i < len1; i++) {
			out[i] = Arrays.copyOfRange(in, i * len2, (i + 1) * len2);
		}
		return out;
	}

	public static byte[] toByteArray(byte[][][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		int len3 = in[0][0].length;
		byte[] out = new byte[len1 * len2 * len3];
		for (int i = 0; i < len1; i++)
			for (int j = 0; j < len2; j++)
				System.arraycopy(in[i][j], 0, out, i * len2 * len3 + j * len3, len3);
		return out;
	}

	public static byte[][][] toTripleByteArray(byte[] in, int len1, int len2) {
		int len3 = in.length / len1 / len2;
		byte[][][] out = new byte[len1][len2][];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				out[i][j] = Arrays.copyOfRange(in, i * len2 * len3 + j * len3, i * len2 * len3 + j * len3 + len3);
			}
		}
		return out;
	}

	public static byte[] toByteArray(int[] in) {
		byte[] out = new byte[in.length * 4];
		for (int i = 0; i < in.length; i++) {
			byte[] n = Util.intToBytes(in[i]);
			System.arraycopy(n, 0, out, i * 4, 4);
		}
		return out;
	}

	public static int[] toIntArray(byte[] in) {
		int len1 = in.length / 4;
		int[] out = new int[len1];
		for (int i = 0; i < len1; i++) {
			byte[] b = Arrays.copyOfRange(in, i * 4, (i + 1) * 4);
			out[i] = Util.bytesToInt(b);
		}
		return out;
	}

	public static byte[] toByteArray(int[][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		byte[] out = new byte[len1 * len2 * 4];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				byte[] n = Util.intToBytes(in[i][j]);
				System.arraycopy(n, 0, out, (i * len2 + j) * 4, 4);
			}
		}
		return out;
	}

	public static int[][] toDoubleIntArray(byte[] in, int len1) {
		int len2 = in.length / len1 / 4;
		int[][] out = new int[len1][len2];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				byte[] b = Arrays.copyOfRange(in, (i * len2 + j) * 4, (i * len2 + j + 1) * 4);
				out[i][j] = Util.bytesToInt(b);
			}
		}
		return out;
	}

	public static byte[] toByteArray(Tuple[] in) {
		int len1 = in.length;
		int f = in[0].getF().length;
		int n = in[0].getN().length;
		int l = in[0].getL().length;
		int a = in[0].getA().length;
		int len2 = f + n + l + a;
		byte[] out = new byte[len1 * len2];
		for (int i = 0; i < len1; i++) {
			System.arraycopy(in[i].getF(), 0, out, i * len2, f);
			System.arraycopy(in[i].getN(), 0, out, i * len2 + f, n);
			System.arraycopy(in[i].getL(), 0, out, i * len2 + f + n, l);
			System.arraycopy(in[i].getA(), 0, out, i * len2 + f + n + l, a);
		}
		return out;
	}

	public static Tuple[] toTupleArray(byte[] in, int len1, int f, int n, int l) {
		int len2 = in.length / len1;
		Tuple[] out = new Tuple[len1];
		for (int i = 0; i < len1; i++) {
			byte[] F = Arrays.copyOfRange(in, i * len2, i * len2 + f);
			byte[] N = Arrays.copyOfRange(in, i * len2 + f, i * len2 + f + n);
			byte[] L = Arrays.copyOfRange(in, i * len2 + f + n, i * len2 + f + n + l);
			byte[] A = Arrays.copyOfRange(in, i * len2 + f + n + l, (i + 1) * len2);
			out[i] = new Tuple(F, N, L, A);
		}
		return out;
	}

	public static byte[] toByteArray(GCSignal[] in) {
		byte[] out = new byte[in.length * GCSignal.len];
		for (int i = 0; i < in.length; i++) {
			System.arraycopy(in[i].bytes, 0, out, i * GCSignal.len, GCSignal.len);
		}
		return out;
	}

	public static GCSignal[] toGCSignalArray(byte[] in) {
		int len1 = in.length / GCSignal.len;
		GCSignal[] out = new GCSignal[len1];
		for (int i = 0; i < len1; i++) {
			byte[] b = Arrays.copyOfRange(in, i * GCSignal.len, (i + 1) * GCSignal.len);
			out[i] = new GCSignal(b);
		}
		return out;
	}

	public static byte[] toByteArray(GCSignal[][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		byte[] out = new byte[len1 * len2 * GCSignal.len];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				System.arraycopy(in[i][j].bytes, 0, out, (i * len2 + j) * GCSignal.len, GCSignal.len);
			}
		}
		return out;
	}

	public static GCSignal[][] toDoubleGCSignalArray(byte[] in, int len1) {
		int len2 = in.length / len1 / GCSignal.len;
		GCSignal[][] out = new GCSignal[len1][len2];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				byte[] b = Arrays.copyOfRange(in, (i * len2 + j) * GCSignal.len, (i * len2 + j + 1) * GCSignal.len);
				out[i][j] = new GCSignal(b);
			}
		}
		return out;
	}

	public static byte[] toByteArray(GCSignal[][][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		int len3 = in[0][0].length;
		byte[] out = new byte[len1 * len2 * len3 * GCSignal.len];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				for (int k = 0; k < len3; k++) {
					System.arraycopy(in[i][j][k].bytes, 0, out, (i * len2 * len3 + j * len3 + k) * GCSignal.len,
							GCSignal.len);
				}
			}
		}
		return out;
	}

	public static GCSignal[][][] toTripleGCSignalArray(byte[] in, int len1, int len2) {
		int len3 = in.length / len1 / len2 / GCSignal.len;
		GCSignal[][][] out = new GCSignal[len1][len2][len3];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				for (int k = 0; k < len3; k++) {
					byte[] b = Arrays.copyOfRange(in, (i * len2 * len3 + j * len3 + k) * GCSignal.len,
							(i * len2 * len3 + j * len3 + k + 1) * GCSignal.len);
					out[i][j][k] = new GCSignal(b);
				}
			}
		}
		return out;
	}

	public static byte[] toByteArray(GCSignal[][][][] in) {
		int len1 = in.length;
		int len2 = in[0].length;
		int len3 = in[0][0].length;
		int len4 = in[0][0][0].length;
		byte[] out = new byte[len1 * len2 * len3 * len4 * GCSignal.len];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				for (int k = 0; k < len3; k++) {
					for (int l = 0; l < len4; l++) {
						System.arraycopy(in[i][j][k][l].bytes, 0, out,
								(i * len2 * len3 * len4 + j * len3 * len4 + k * len4 + l) * GCSignal.len, GCSignal.len);
					}
				}
			}
		}
		return out;
	}

	public static GCSignal[][][][] toQuadGCSignalArray(byte[] in, int len1, int len2, int len3) {
		int len4 = in.length / len1 / len2 / len3 / GCSignal.len;
		GCSignal[][][][] out = new GCSignal[len1][len2][len3][len4];
		for (int i = 0; i < len1; i++) {
			for (int j = 0; j < len2; j++) {
				for (int k = 0; k < len3; k++) {
					for (int l = 0; l < len4; l++) {
						byte[] b = Arrays.copyOfRange(in,
								(i * len2 * len3 * len4 + j * len3 * len4 + k * len4 + l) * GCSignal.len,
								(i * len2 * len3 * len4 + j * len3 * len4 + k * len4 + l + 1) * GCSignal.len);
						out[i][j][k][l] = new GCSignal(b);
					}
				}
			}
		}
		return out;
	}
}

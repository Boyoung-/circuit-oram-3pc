package protocols;

import java.math.BigInteger;

import org.apache.commons.lang3.tuple.Pair;

import communication.Communication;
import crypto.PRF;
import crypto.PRG;
import oram.Forest;
import oram.Metadata;

public class PreSSCOT extends Protocol {
	public PreSSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Pair<Integer, BigInteger> executeCharlie(Communication D,
			Communication E, int i, int N, int l, int l_p) {
		// protocol
		// step 1
		byte[] msg_ev = E.read();

		// step 2
		byte[] msg_pw = D.read();

		// step 3
		byte[][] e = new byte[N][];
		byte[][] v = new byte[N][];
		byte[][] p = new byte[N][];
		byte[][] w = new byte[N][];
		PRG G = new PRG(l);
		int gBytes = (l + 7) / 8;

		for (int t = 0; t < N; t++) {
			e[t] = Arrays.copyOfRange(msg_ev, t * gBytes, (t + 1) * gBytes);
			v[t] = Arrays.copyOfRange(msg_ev, N * gBytes + t * SR.kBytes, N
					* gBytes + (t + 1) * SR.kBytes);
			p[t] = Arrays.copyOfRange(msg_pw, t * SR.kBytes, (t + 1)
					* SR.kBytes);
			w[t] = Arrays.copyOfRange(msg_pw, (N + t) * SR.kBytes, (N + t + 1)
					* SR.kBytes);

			if (new BigInteger(1, v[t]).compareTo(new BigInteger(1, w[t])) == 0) {
				//BigInteger m_t = new BigInteger(1, e[t]).xor(new BigInteger(1, G.compute(p[t])));
				byte[] tmp = G.compute(p[t]);
				BigInteger m_t = new BigInteger(1, e[t]).xor(new BigInteger(1, tmp));
				return Pair.of(t, m_t);
			}
		}

		// error
		return null;
	}

	public void executeDebbie(Communication C, Communication E, int i, int N,
			int l, int l_p, BigInteger[] b) {
		// protocol
		// step 2
		int diffBits = SR.kBits - l_p;
		BigInteger[] y = new BigInteger[N];
		byte[][][] pw = new byte[2][N][];
		byte[] msg_pw = new byte[SR.kBytes * N * 2];
		PRF F_k = new PRF(SR.kBits);
		PRF F_k_p = new PRF(SR.kBits);
		F_k.init(PreData.sscot_k[i]);
		F_k_p.init(PreData.sscot_k_p[i]);

		for (int t = 0; t < N; t++) {
			y[t] = PreData.sscot_r[i][t].xor(b[t].shiftLeft(diffBits));
			pw[0][t] = F_k.compute(y[t].toByteArray());
			pw[1][t] = F_k_p.compute(y[t].toByteArray());
			System.arraycopy(pw[0][t], 0, msg_pw, t * SR.kBytes, SR.kBytes);
			System.arraycopy(pw[1][t], 0, msg_pw, (N + t) * SR.kBytes,
					SR.kBytes);
		}

		C.write(msg_pw, PID.sscot);
	}

	public void executeEddie(Communication C, Communication D, int i, int N,
			int l, int l_p, BigInteger[] m, BigInteger[] a) {
		// protocol
		// step 1
		int gBytes = (l + 7) / 8;
		int diffBits = SR.kBits - l_p;
		BigInteger[] x = new BigInteger[N];
		byte[][][] ev = new byte[2][N][];
		byte[] msg_ev = new byte[(SR.kBytes + gBytes) * N];
		PRF F_k = new PRF(SR.kBits);
		PRF F_k_p = new PRF(SR.kBits);
		PRG G = new PRG(l);
		F_k.init(PreData.sscot_k[i]);
		F_k_p.init(PreData.sscot_k_p[i]);

		for (int t = 0; t < N; t++) {
			x[t] = PreData.sscot_r[i][t].xor(a[t].shiftLeft(diffBits));
			//ev[0][t] = new BigInteger(1, G.compute(F_k.compute(x[t].toByteArray()))).xor(m[t]).toByteArray();
			ev[1][t] = F_k_p.compute(x[t].toByteArray());
			byte[] tmp = F_k.compute(x[t].toByteArray());
			tmp = G.compute(tmp);
			ev[0][t] = new BigInteger(1, tmp).xor(m[t]).toByteArray();
			if (ev[0][t].length < gBytes)
				System.arraycopy(ev[0][t], 0, msg_ev, (t + 1) * gBytes
						- ev[0][t].length, ev[0][t].length);
			else
				System.arraycopy(ev[0][t], ev[0][t].length - gBytes, msg_ev, t
						* gBytes, gBytes);
			System.arraycopy(ev[1][t], 0, msg_ev, N * gBytes + t * SR.kBytes,
					SR.kBytes);
		}

		C.write(msg_ev, PID.sscot);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		// TODO Auto-generated method stub
		
	}
}

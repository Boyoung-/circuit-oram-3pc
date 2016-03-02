package protocols;

import java.math.BigInteger;

import org.apache.commons.lang3.tuple.Pair;

import communication.Communication;
import oram.Forest;
import oram.Metadata;

public class SSCOT extends Protocol {
	public SSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Pair<Integer, BigInteger> executeCharlie(Communication D,
			Communication E, int i, int N, int l, int l_p) {
		// protocol
		// step 1
		timing.stopwatch[PID.sscot][TID.online_read].start();
		byte[] msg_ev = E.read();

		// step 2
		byte[] msg_pw = D.read();
		timing.stopwatch[PID.sscot][TID.online_read].stop();

		// step 3
		timing.stopwatch[PID.sscot][TID.online].start();
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
				timing.stopwatch[PID.aes_prg][TID.online].start();
				byte[] tmp = G.compute(p[t]);
				timing.stopwatch[PID.aes_prg][TID.online].stop();
				BigInteger m_t = new BigInteger(1, e[t]).xor(new BigInteger(1, tmp));
				timing.stopwatch[PID.sscot][TID.online].stop();
				return Pair.of(t, m_t);
			}
		}
		timing.stopwatch[PID.sscot][TID.online].stop();

		// error
		return null;
	}

	public void executeDebbie(Communication C, Communication E, int i, int N,
			int l, int l_p, BigInteger[] b) {
		// protocol
		// step 2
		timing.stopwatch[PID.sscot][TID.online].start();
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
			timing.stopwatch[PID.aes_prf][TID.online].start();
			pw[0][t] = F_k.compute(y[t].toByteArray());
			pw[1][t] = F_k_p.compute(y[t].toByteArray());
			timing.stopwatch[PID.aes_prf][TID.online].stop();
			System.arraycopy(pw[0][t], 0, msg_pw, t * SR.kBytes, SR.kBytes);
			System.arraycopy(pw[1][t], 0, msg_pw, (N + t) * SR.kBytes,
					SR.kBytes);
		}
		timing.stopwatch[PID.sscot][TID.online].stop();

		timing.stopwatch[PID.sscot][TID.online_write].start();
		C.write(msg_pw, PID.sscot);
		timing.stopwatch[PID.sscot][TID.online_write].stop();
	}

	public void executeEddie(Communication C, Communication D, int i, int N,
			int l, int l_p, BigInteger[] m, BigInteger[] a) {
		// protocol
		// step 1
		timing.stopwatch[PID.sscot][TID.online].start();
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
			timing.stopwatch[PID.aes_prf][TID.online].start();
			ev[1][t] = F_k_p.compute(x[t].toByteArray());
			byte[] tmp = F_k.compute(x[t].toByteArray());
			timing.stopwatch[PID.aes_prf][TID.online].stop();
			timing.stopwatch[PID.aes_prg][TID.online].start();
			tmp = G.compute(tmp);
			timing.stopwatch[PID.aes_prg][TID.online].stop();
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
		timing.stopwatch[PID.sscot][TID.online].stop();

		timing.stopwatch[PID.sscot][TID.online_write].start();
		C.write(msg_ev, PID.sscot);
		timing.stopwatch[PID.sscot][TID.online_write].stop();
	}

	// for testing correctness
	@Override
	public void run(Party party, Forest forest) throws ForestException {
		System.out.println("#####  Testing SSCOT  #####");

		timing = new Timing();

		for (int ii = 0; ii < 20; ii++) {

			if (party == Party.Eddie) {
				int levels = ForestMetadata.getLevels();
				int i = SR.rand.nextInt(levels - 1) + 1;
				int N = SR.rand.nextInt(50) + 150; // 150-199
				int l = ForestMetadata.getTupleBits(i);
				int l_p = 1 + ForestMetadata.getNBits(i);
				int t = SR.rand.nextInt(N);

				PreData.sscot_k = new byte[levels][16];
				PreData.sscot_k_p = new byte[levels][16];
				PreData.sscot_r = new BigInteger[levels][N];
				BigInteger[] a = new BigInteger[N];
				BigInteger[] b = new BigInteger[N];
				BigInteger[] m = new BigInteger[N];

				SR.rand.nextBytes(PreData.sscot_k[i]);
				SR.rand.nextBytes(PreData.sscot_k_p[i]);
				for (int o = 0; o < N; o++) {
					PreData.sscot_r[i][o] = new BigInteger(SR.kBits, SR.rand);
					a[o] = new BigInteger(l_p, SR.rand);
					b[o] = new BigInteger(l_p, SR.rand);
					while (a[o].compareTo(b[o]) == 0)
						b[o] = new BigInteger(l_p, SR.rand);
					m[o] = new BigInteger(l, SR.rand);
				}
				a[t] = b[t];

				con1.write(i);
				con1.write(N);
				con1.write(l);
				con1.write(l_p);

				con2.write(i);
				con2.write(N);
				con2.write(l);
				con2.write(l_p);
				con2.write(PreData.sscot_k[i]);
				con2.write(PreData.sscot_k_p[i]);
				con2.write(PreData.sscot_r[i]);
				con2.write(b);

				executeEddie(con1, con2, i, N, l, l_p, m, a);

				int output_t = con1.readInt();
				BigInteger m_t = con1.readBigInteger();

				System.out.println("i = " + i);
				if (t == output_t && m[t].compareTo(m_t) == 0) {
					System.out.println("SSCOT test passed:");
				} else {
					System.out.println("SSCOT test failed:");
				}
				System.out.println("t=" + t + ", output_t=" + output_t);
				System.out.println("m[t]=" + m[t] + ", m_t=" + m_t);
			} else if (party == Party.Debbie) {
				int i = con2.readInt();
				int N = con2.readInt();
				int l = con2.readInt();
				int l_p = con2.readInt();

				int levels = ForestMetadata.getLevels();
				PreData.sscot_k = new byte[levels][];
				PreData.sscot_k_p = new byte[levels][];
				PreData.sscot_r = new BigInteger[levels][];

				PreData.sscot_k[i] = con2.read();
				PreData.sscot_k_p[i] = con2.read();
				PreData.sscot_r[i] = con2.readBigIntegerArray();
				BigInteger[] b = con2.readBigIntegerArray();

				executeDebbie(con1, con2, i, N, l, l_p, b);
			} else if (party == Party.Charlie) {
				int i = con2.readInt();
				int N = con2.readInt();
				int l = con2.readInt();
				int l_p = con2.readInt();

				Pair<Integer, BigInteger> output = executeCharlie(con1, con2,
						i, N, l, l_p);
				int t = output.getLeft();
				BigInteger m_t = output.getRight();

				con2.write(t);
				con2.write(m_t);
			}

		}

		System.out.println("#####  Testing SSCOT Finished  #####");
	}

	@Override
	public void run(protocols.Party party, Metadata md, oram.Forest forest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		// TODO Auto-generated method stub
		
	}
}

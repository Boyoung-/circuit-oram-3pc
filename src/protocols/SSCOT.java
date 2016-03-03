package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import crypto.PRG;
import exceptions.NoSuchPartyException;
import exceptions.SSCOTException;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class SSCOT extends Protocol {
	public SSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, byte[][] m, byte[][] a) {
		// step 1
		int n = m.length;
		int l = m[0].length * 8;
		byte[][] x = predata.sscot_r;
		byte[][] e = new byte[n][];
		byte[][] v = new byte[n][];
		PRF F_k = new PRF(Crypto.secParam);
		F_k.init(predata.sscot_k);
		PRF F_kprime = new PRF(Crypto.secParam);
		F_kprime.init(predata.sscot_kprime);
		PRG G = new PRG(l);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < a[i].length; j++)
				x[i][j] = (byte) (predata.sscot_r[i][j] ^ a[i][j]);

			e[i] = Util.xor(G.compute(F_k.compute(x[i])), m[i]);
			v[i] = F_kprime.compute(x[i]);
		}

		con2.write(e);
		con2.write(v);
	}

	public void runD(PreData predata, byte[][] b) {
		// step 2
		int n = b.length;
		byte[][] y = predata.sscot_r;
		byte[][] p = new byte[n][];
		byte[][] w = new byte[n][];
		PRF F_k = new PRF(Crypto.secParam);
		F_k.init(predata.sscot_k);
		PRF F_kprime = new PRF(Crypto.secParam);
		F_kprime.init(predata.sscot_kprime);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < b[i].length; j++)
				y[i][j] = (byte) (predata.sscot_r[i][j] ^ b[i][j]);

			p[i] = F_k.compute(y[i]);
			w[i] = F_kprime.compute(y[i]);
		}

		con2.write(p);
		con2.write(w);
	}

	public OutSSCOT runC() {
		// step 1
		byte[][] e = con1.readDoubleByteArray();
		byte[][] v = con1.readDoubleByteArray();

		// step 2
		byte[][] p = con2.readDoubleByteArray();
		byte[][] w = con2.readDoubleByteArray();

		// step 3
		int n = e.length;
		int l = e[0].length * 8;
		PRG G = new PRG(l);
		OutSSCOT output = null;
		int invariant = 0;

		for (int i = 0; i < n; i++) {
			if (Util.equal(v[i], w[i])) {
				byte[] m = Util.xor(e[i], G.compute(p[i]));
				output = new OutSSCOT(i, m);
				invariant++;
			}
		}

		if (invariant != 1)
			throw new SSCOTException("Invariant error: " + invariant);
		return output;
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		for (int j = 0; j < 100; j++) {
			int n = 100;
			int A = 32;
			int FN = 5;
			byte[][] m = new byte[n][A];
			byte[][] a = new byte[n][FN];
			byte[][] b = new byte[n][FN];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(m[i]);
				Crypto.sr.nextBytes(a[i]);
				Crypto.sr.nextBytes(b[i]);
				while (Util.equal(a[i], b[i]))
					Crypto.sr.nextBytes(b[i]);
			}
			int index = Crypto.sr.nextInt(n);
			b[index] = a[index].clone();

			PreData predata = new PreData();
			PreSSCOT presscot = new PreSSCOT(con1, con2);
			if (party == Party.Eddie) {
				con1.write(b);
				con2.write(m);
				con2.write(index);
				presscot.runE(predata, n);
				runE(predata, m, a);

			} else if (party == Party.Debbie) {
				b = con1.readDoubleByteArray();
				presscot.runD(predata);
				runD(predata, b);

			} else if (party == Party.Charlie) {
				m = con1.readDoubleByteArray();
				index = con1.readInt();
				presscot.runC();
				OutSSCOT output = runC();
				if (output.t == index && Util.equal(output.m_t, m[index]))
					System.out.println("SSCOT test passed");
				else
					System.err.println("SSCOT test failed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}
}

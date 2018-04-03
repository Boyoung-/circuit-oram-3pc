package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRG;
import exceptions.NoSuchPartyException;
import exceptions.SSCOTException;
import oram.Forest;
import oram.Metadata;
import protocols.precomputation.PreSSCOT;
import protocols.struct.OutSSCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class SSCOT extends Protocol {

	private int pid = P.COT;

	public SSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, byte[][] m, byte[][] a, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		int n = m.length;
		int l = m[0].length * 8;
		byte[][] x = predata.sscot_r;
		byte[][] e = new byte[n][];
		byte[][] v = new byte[n][];
		PRG G = new PRG(l);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < a[i].length; j++)
				x[i][j] = (byte) (predata.sscot_r[i][j] ^ a[i][j]);

			e[i] = Util.xor(G.compute(predata.sscot_F_k.compute(x[i])), m[i]);
			v[i] = predata.sscot_F_kprime.compute(x[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, e);
		con2.write(pid, v);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public void runD(PreData predata, byte[][] b, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		int n = b.length;
		byte[][] y = predata.sscot_r;
		byte[][] p = new byte[n][];
		byte[][] w = new byte[n][];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < b[i].length; j++)
				y[i][j] = (byte) (predata.sscot_r[i][j] ^ b[i][j]);

			p[i] = predata.sscot_F_k.compute(y[i]);
			w[i] = predata.sscot_F_kprime.compute(y[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, p);
		con2.write(pid, w);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public OutSSCOT runC(Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		byte[][] e = con1.readDoubleByteArray(pid);
		byte[][] v = con1.readDoubleByteArray(pid);

		// step 2
		byte[][] p = con2.readDoubleByteArray(pid);
		byte[][] w = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

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

		timer.stop(pid, M.online_comp);
		return output;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

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
				presscot.runE(predata, n, timer);
				runE(predata, m, a, timer);

			} else if (party == Party.Debbie) {
				b = con1.readDoubleByteArray();
				presscot.runD(predata, timer);
				runD(predata, b, timer);

			} else if (party == Party.Charlie) {
				m = con1.readDoubleByteArray();
				index = con1.readInt();
				presscot.runC();
				OutSSCOT output = runC(timer);
				if (output.t == index && Util.equal(output.m_t, m[index]))
					System.out.println("SSCOT test passed");
				else
					System.err.println("SSCOT test failed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {
		// TODO Auto-generated method stub
		
	}
}

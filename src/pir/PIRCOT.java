package pir;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import exceptions.SSCOTException;
import oram.Forest;
import oram.Metadata;
import pir.precomputation.PrePIRCOT;
import protocols.Protocol;
import protocols.struct.OutPIRCOT;
import protocols.struct.OutSSCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PIRCOT extends Protocol {

	private int pid = P.COT;

	public PIRCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutPIRCOT runE(PreData predata, byte[][] u, byte[] v, Timer timer) {
		int l = u.length;
		byte[][] a = new byte[l][];
		for (int j = 0; j < l; j++) {
			a[j] = Util.xor(u[(j + predata.sscot_s_DE) % l], v);
			a[j] = Util.padArray(a[j], predata.sscot_r[j].length);
			Util.setXor(a[j], predata.sscot_r[j]);
			a[j] = predata.sscot_F_k.compute(a[j]);
		}

		con2.write(pid, a);

		int delta = con2.readInt(pid);
		int t_E = (predata.sscot_s_DE + delta) % l;

		OutPIRCOT out = new OutPIRCOT();
		out.t_E = t_E;
		out.s_DE = predata.sscot_s_DE;
		out.s_CE = predata.sscot_s_CE;
		return out;
	}

	public OutPIRCOT runD(PreData predata, byte[][] u, byte[] v, Timer timer) {
		int l = u.length;
		byte[][] a = new byte[l][];
		for (int j = 0; j < l; j++) {
			a[j] = Util.xor(u[(j + l - predata.sscot_s_DE) % l], v);
			a[j] = Util.padArray(a[j], predata.sscot_r[j].length);
			Util.setXor(a[j], predata.sscot_r[j]);
			a[j] = predata.sscot_F_k.compute(a[j]);
		}

		con2.write(pid, a);

		int delta = con2.readInt(pid);
		int t_D = (predata.sscot_s_DE + delta) % l;

		OutPIRCOT out = new OutPIRCOT();
		out.t_D = t_D;
		out.s_DE = predata.sscot_s_DE;
		out.s_CD = predata.sscot_s_CD;
		return out;
	}

	public OutPIRCOT runC(PreData predata, Timer timer) {
		byte[][] x = con1.readDoubleByteArray(pid);
		byte[][] y = con2.readDoubleByteArray(pid);
		int l = x.length;

		int count = 0;
		int t_C = 0;
		for (int i = 0; i < l; i++) {
			if (Util.equal(x[i], y[i])) {
				t_C = i;
				count++;
			}
		}

		if (count != 1) {
			throw new SSCOTException("Invariant error: " + count);
		}

		int delta_D = (t_C - predata.sscot_s_CE + l) % l;
		con2.write(pid, delta_D);
		int delta_E = (t_C - predata.sscot_s_CD + l) % l;
		con1.write(pid, delta_E);

		OutPIRCOT out = new OutPIRCOT();
		out.t_C = t_C;
		out.s_CE = predata.sscot_s_CE;
		out.s_CD = predata.sscot_s_CD;
		return out;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();

		for (int j = 0; j < 100; j++) {
			int n = 100;
			int FN = 5;
			byte[][] a = new byte[n][FN];
			byte[][] b = new byte[n][FN];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(a[i]);
			}
			int index = Crypto.sr.nextInt(n);
			byte[] v = a[index].clone();

			PreData predata = new PreData();
			PrePIRCOT presscot = new PrePIRCOT(con1, con2);
			OutPIRCOT output;

			if (party == Party.Eddie) {
				con2.write(index);
				presscot.runE(predata, n, timer);
				output = runE(predata, a, v, timer);

				con2.write(output.t_E);
				con2.write(output.s_CE);
				con2.write(output.s_DE);

			} else if (party == Party.Debbie) {
				presscot.runD(predata, n, timer);
				output = runD(predata, b, new byte[FN], timer);

				con2.write(output.t_D);
				con2.write(output.s_DE);
				con2.write(output.s_CD);

			} else if (party == Party.Charlie) {
				index = con1.readInt();
				presscot.runC(predata, timer);
				output = runC(predata, timer);

				int t_E = con1.readInt();
				int s_CE = con1.readInt();
				int s_DE = con1.readInt();
				if ((t_E + output.s_CD) % n != index)
					System.err.println(j + ": PIRCOT test failed 1");
				else if (s_CE != output.s_CE)
					System.err.println(j + ": PIRCOT test failed 2");
				else if ((s_DE + output.t_C) % n != index)
					System.err.println(j + ": PIRCOT test failed 3");
				else
					System.out.println(j + ": PIRCOT first half test passed");

				int t_D = con2.readInt();
				s_DE = con2.readInt();
				int s_CD = con2.readInt();
				if ((t_D + output.s_CE) % n != index)
					System.err.println(j + ": PIRCOT test failed 4");
				else if (s_CD != output.s_CD)
					System.err.println(j + ": PIRCOT test failed 5");
				else if ((s_DE + output.t_C) % n != index)
					System.err.println(j + ": PIRCOT test failed 6");
				else
					System.out.println(j + ": PIRCOT all test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	public void runE(PreData predata, byte[][] a, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		int n = a.length;
		byte[][] x = predata.sscot_r;
		byte[][] v = new byte[n][];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < a[i].length; j++)
				x[i][j] = (byte) (predata.sscot_r[i][j] ^ a[i][j]);

			v[i] = predata.sscot_F_kprime.compute(x[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, v);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public void runD(PreData predata, byte[][] b, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		int n = b.length;
		byte[][] y = predata.sscot_r;
		byte[][] w = new byte[n][];

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < b[i].length; j++)
				y[i][j] = (byte) (predata.sscot_r[i][j] ^ b[i][j]);

			w[i] = predata.sscot_F_kprime.compute(y[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, w);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public OutSSCOT runC(Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		byte[][] v = con1.readDoubleByteArray(pid);

		// step 2
		byte[][] w = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		// step 3
		int n = v.length;
		OutSSCOT output = null;
		int invariant = 0;

		for (int i = 0; i < n; i++) {
			if (Util.equal(v[i], w[i])) {
				output = new OutSSCOT(i, null);
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
	}
}

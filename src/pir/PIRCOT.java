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
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.Util;

public class PIRCOT extends Protocol {

	public PIRCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutPIRCOT runE(PreData predata, byte[][] u, byte[] v) {
		timer.start(M.online_comp);

		int l = u.length;
		byte[][] a = new byte[l][];
		for (int j = 0; j < l; j++) {
			a[j] = Util.xor(u[(j + predata.sscot_s_DE) % l], v);
			a[j] = Util.padArray(a[j], predata.sscot_r[j].length);
			Util.setXor(a[j], predata.sscot_r[j]);
			a[j] = predata.sscot_F_k.compute(a[j]);
		}

		timer.start(M.online_write);
		con2.write(online_band, a);
		timer.stop(M.online_write);

		timer.start(M.online_read);
		int delta = con2.readIntAndDec();
		timer.stop(M.online_read);

		int t_E = (predata.sscot_s_DE + delta) % l;

		OutPIRCOT out = new OutPIRCOT();
		out.t_E = t_E;
		out.s_DE = predata.sscot_s_DE;
		out.s_CE = predata.sscot_s_CE;

		timer.stop(M.online_comp);
		return out;
	}

	public OutPIRCOT runD(PreData predata, byte[][] u, byte[] v) {
		timer.start(M.online_comp);

		int l = u.length;
		byte[][] a = new byte[l][];
		for (int j = 0; j < l; j++) {
			a[j] = Util.xor(u[(j + predata.sscot_s_DE) % l], v);
			a[j] = Util.padArray(a[j], predata.sscot_r[j].length);
			Util.setXor(a[j], predata.sscot_r[j]);
			a[j] = predata.sscot_F_k.compute(a[j]);
		}

		timer.start(M.online_write);
		con2.write(online_band, a);
		timer.stop(M.online_write);

		timer.start(M.online_read);
		int delta = con2.readIntAndDec();
		timer.stop(M.online_read);

		int t_D = (predata.sscot_s_DE + delta) % l;

		OutPIRCOT out = new OutPIRCOT();
		out.t_D = t_D;
		out.s_DE = predata.sscot_s_DE;
		out.s_CD = predata.sscot_s_CD;

		timer.stop(M.online_comp);
		return out;
	}

	public OutPIRCOT runC(PreData predata) {
		timer.start(M.online_comp);

		timer.start(M.online_read);
		byte[][] x = con1.readDoubleByteArrayAndDec();
		byte[][] y = con2.readDoubleByteArrayAndDec();
		timer.stop(M.online_read);

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
		int delta_E = (t_C - predata.sscot_s_CD + l) % l;

		timer.start(M.online_write);
		con2.write(online_band, delta_D);
		con1.write(online_band, delta_E);
		timer.stop(M.online_write);

		OutPIRCOT out = new OutPIRCOT();
		out.t_C = t_C;
		out.s_CE = predata.sscot_s_CE;
		out.s_CD = predata.sscot_s_CD;

		timer.stop(M.online_comp);
		return out;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

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
				presscot.runE(predata, n);
				output = runE(predata, a, v);

				con2.write(output.t_E);
				con2.write(output.s_CE);
				con2.write(output.s_DE);

			} else if (party == Party.Debbie) {
				presscot.runD(predata, n);
				output = runD(predata, b, new byte[FN]);

				con2.write(output.t_D);
				con2.write(output.s_DE);
				con2.write(output.s_CD);

			} else if (party == Party.Charlie) {
				index = con1.readInt();
				presscot.runC(predata);
				output = runC(predata);

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

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

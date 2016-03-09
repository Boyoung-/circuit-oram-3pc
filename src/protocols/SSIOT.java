package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRG;
import exceptions.NoSuchPartyException;
import exceptions.SSIOTException;
import measure.M;
import measure.P;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class SSIOT extends Protocol {
	public SSIOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, byte[][] y, byte[] Nip1_pr, Timer timer) {
		timer.start(P.IOT, M.online_comp);

		// step 1
		int n = y.length;
		int l = y[0].length * 8;
		byte[][] x = new byte[n][];
		byte[][] e = new byte[n][];
		byte[][] v = new byte[n][];
		PRG G = new PRG(l);

		for (int i = 0; i < n; i++) {
			byte[] i_bytes = Util.intToBytes(i);
			x[i] = predata.ssiot_r.clone();
			for (int j = 0; j < Nip1_pr.length; j++)
				x[i][x[i].length - 1 - j] ^= Nip1_pr[Nip1_pr.length - 1 - j] ^ i_bytes[i_bytes.length - 1 - j];

			e[i] = Util.xor(G.compute(predata.ssiot_F_k.compute(x[i])), y[i]);
			v[i] = predata.ssiot_F_kprime.compute(x[i]);
		}

		timer.start(P.IOT, M.online_write);
		con2.write(e);
		con2.write(v);
		timer.stop(P.IOT, M.online_write);

		timer.stop(P.IOT, M.online_comp);
	}

	public void runD(PreData predata, byte[] Nip1_pr, Timer timer) {
		timer.start(P.IOT, M.online_comp);

		// step 2
		byte[] y = predata.ssiot_r;
		for (int i = 0; i < Nip1_pr.length; i++)
			y[y.length - 1 - i] ^= Nip1_pr[Nip1_pr.length - 1 - i];
		byte[] p = predata.ssiot_F_k.compute(y);
		byte[] w = predata.ssiot_F_kprime.compute(y);

		timer.start(P.IOT, M.online_write);
		con2.write(p);
		con2.write(w);
		timer.stop(P.IOT, M.online_write);

		timer.stop(P.IOT, M.online_comp);
	}

	public OutSSIOT runC(Timer timer) {
		timer.start(P.IOT, M.online_comp);

		// step 1
		timer.start(P.IOT, M.online_read);
		byte[][] e = con1.readObject();
		byte[][] v = con1.readObject();

		// step 2
		byte[] p = con2.read();
		byte[] w = con2.read();
		timer.stop(P.IOT, M.online_read);

		// step 3
		int n = e.length;
		int l = e[0].length * 8;
		PRG G = new PRG(l);
		OutSSIOT output = null;
		int invariant = 0;

		for (int i = 0; i < n; i++) {
			if (Util.equal(v[i], w)) {
				byte[] y = Util.xor(e[i], G.compute(p));
				output = new OutSSIOT(i, y);
				invariant++;
			}
		}

		if (invariant != 1)
			throw new SSIOTException("Invariant error: " + invariant);

		timer.stop(P.IOT, M.online_comp);
		return output;
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int j = 0; j < 100; j++) {
			int twoTauPow = 64;
			int label = 4;
			byte[][] y = new byte[twoTauPow][label];
			byte[] sE_Nip1_pr = new byte[1];
			byte[] sD_Nip1_pr = new byte[1];
			for (int i = 0; i < twoTauPow; i++)
				Crypto.sr.nextBytes(y[i]);
			int index = Crypto.sr.nextInt(twoTauPow);
			Crypto.sr.nextBytes(sE_Nip1_pr);
			sD_Nip1_pr[0] = (byte) (Util.intToBytes(index)[3] ^ sE_Nip1_pr[0]);

			PreData predata = new PreData();
			PreSSIOT pressiot = new PreSSIOT(con1, con2);

			if (party == Party.Eddie) {
				con1.write(sD_Nip1_pr);
				con2.write(y);
				con2.write(index);
				pressiot.runE(predata, twoTauPow, timer);
				runE(predata, y, sE_Nip1_pr, timer);

			} else if (party == Party.Debbie) {
				sD_Nip1_pr = con1.read();
				pressiot.runD(predata, timer);
				runD(predata, sD_Nip1_pr, timer);

			} else if (party == Party.Charlie) {
				y = con1.readObject();
				index = con1.readObject();
				pressiot.runC();
				OutSSIOT output = runC(timer);
				if (output.t == index && Util.equal(output.m_t, y[index]))
					System.out.println("SSIOT test passed");
				else
					System.err.println("SSIOT test failed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}
}

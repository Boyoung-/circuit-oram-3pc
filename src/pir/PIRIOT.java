package pir;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import exceptions.SSIOTException;
import oram.Forest;
import oram.Metadata;
import pir.precomputation.PrePIRIOT;
import protocols.Protocol;
import protocols.struct.OutSSIOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PIRIOT extends Protocol {

	private int pid = P.IOT;

	public PIRIOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int n, byte[] Nip1_pr, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		byte[][] x = new byte[n][];
		byte[][] v = new byte[n][];

		for (int i = 0; i < n; i++) {
			byte[] i_bytes = Util.intToBytes(i);
			x[i] = predata.ssiot_r.clone();
			for (int j = 0; j < Nip1_pr.length; j++)
				x[i][x[i].length - 1 - j] ^= Nip1_pr[Nip1_pr.length - 1 - j] ^ i_bytes[i_bytes.length - 1 - j];

			v[i] = predata.ssiot_F_kprime.compute(x[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, v);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public void runD(PreData predata, byte[] Nip1_pr, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		byte[] y = predata.ssiot_r;
		for (int i = 0; i < Nip1_pr.length; i++)
			y[y.length - 1 - i] ^= Nip1_pr[Nip1_pr.length - 1 - i];
		byte[] w = predata.ssiot_F_kprime.compute(y);

		timer.start(pid, M.online_write);
		con2.write(pid, w);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public OutSSIOT runC(Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		byte[][] v = con1.readDoubleByteArray(pid);

		// step 2
		byte[] w = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 3
		int n = v.length;
		OutSSIOT output = null;
		int invariant = 0;

		for (int i = 0; i < n; i++) {
			if (Util.equal(v[i], w)) {
				output = new OutSSIOT(i, null);
				invariant++;
			}
		}

		if (invariant != 1)
			throw new SSIOTException("Invariant error: " + invariant);

		timer.stop(pid, M.online_comp);
		return output;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int j = 0; j < 100; j++) {
			int twoTauPow = 64;
			byte[] sE_Nip1_pr = new byte[1];
			byte[] sD_Nip1_pr = new byte[1];
			int index = Crypto.sr.nextInt(twoTauPow);
			Crypto.sr.nextBytes(sE_Nip1_pr);
			sD_Nip1_pr[0] = (byte) (Util.intToBytes(index)[3] ^ sE_Nip1_pr[0]);

			PreData predata = new PreData();
			PrePIRIOT pressiot = new PrePIRIOT(con1, con2);

			if (party == Party.Eddie) {
				con1.write(sD_Nip1_pr);
				con2.write(index);
				pressiot.runE(predata, twoTauPow, timer);
				runE(predata, twoTauPow, sE_Nip1_pr, timer);

			} else if (party == Party.Debbie) {
				sD_Nip1_pr = con1.read();
				pressiot.runD(predata, timer);
				runD(predata, sD_Nip1_pr, timer);

			} else if (party == Party.Charlie) {
				index = con1.readInt();
				pressiot.runC();
				OutSSIOT output = runC(timer);
				if (output.t == index)
					System.out.println("PIRIOT test passed");
				else
					System.err.println("PIRIOT test failed");

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

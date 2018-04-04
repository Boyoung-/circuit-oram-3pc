package pir;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class ShiftPIR extends Protocol {

	private int pid = P.SftPIR;

	public ShiftPIR(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[] runP1(PreData predata, byte[][] x, int s, Timer timer) {
		timer.start(pid, M.online_comp);

		// TODO: do in place shift
		byte[][] xp = new byte[x.length][];
		for (int i = 0; i < x.length; i++) {
			xp[i] = x[(i + s) % x.length];
		}

		SSPIR sspir = new SSPIR(con1, con2);
		byte[] z = sspir.runP1(predata, xp, timer);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public byte[] runP2(PreData predata, byte[][] x, int s, Timer timer) {
		timer.start(pid, M.online_comp);

		byte[][] xp = new byte[x.length][];
		for (int i = 0; i < x.length; i++) {
			xp[i] = x[(i + s) % x.length];
		}

		SSPIR sspir = new SSPIR(con1, con2);
		byte[] z = sspir.runP2(predata, xp, timer);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public void runP3(PreData predata, int t, Timer timer) {
		timer.start(pid, M.online_comp);

		SSPIR sspir = new SSPIR(con1, con2);
		sspir.runP3(predata, t, timer);

		timer.stop(pid, M.online_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int l = 100;
			int m = 50;
			byte[][] x = new byte[l][m];
			for (int i = 0; i < l; i++) {
				Crypto.sr.nextBytes(x[i]);
			}
			int s = Crypto.sr.nextInt(l);
			int t = Crypto.sr.nextInt(l);

			if (party == Party.Eddie) {
				con1.write(x);
				con1.write(s);
				byte[] out = this.runP1(predata, x, s, timer);
				con2.write(out);
				con2.write(x);
				con2.write(s);

			} else if (party == Party.Debbie) {
				x = con1.readDoubleByteArray();
				s = con1.readInt();
				byte[] out = this.runP2(predata, x, s, timer);
				con2.write(out);

			} else if (party == Party.Charlie) {
				this.runP3(predata, t, timer);
				byte[] out1 = con1.read();
				x = con1.readDoubleByteArray();
				s = con1.readInt();
				byte[] out2 = con2.read();
				Util.setXor(out1, out2);
				int index = (s + t) % l;

				if (!Util.equal(out1, x[index]))
					System.err.println(j + ": ShiftPIR test failed");
				else
					System.out.println(j + ": ShiftPIR test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

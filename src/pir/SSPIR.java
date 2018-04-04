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

public class SSPIR extends Protocol {

	private int pid = P.SSPIR;

	public SSPIR(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[] runP1(PreData predata, byte[][] x, Timer timer) {
		timer.start(pid, M.offline_comp);

		int l = x.length;
		int m = x[0].length;
		byte[] a1 = new byte[l];
		byte[] r = new byte[m];
		Crypto.sr.nextBytes(a1);
		Crypto.sr.nextBytes(r);

		timer.start(pid, M.offline_write);
		con2.write(a1);
		con1.write(r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		byte[] z = Util.xorSelect(x, a1);
		Util.setXor(z, r);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public byte[] runP2(PreData predata, byte[][] x, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		byte[] r = con1.read();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		timer.start(pid, M.online_read);
		byte[] a2 = con2.read(pid);
		timer.stop(pid, M.online_read);

		byte[] z = Util.xorSelect(x, a2);
		Util.setXor(z, r);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public void runP3(PreData predata, int t, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		byte[] a = con1.read();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		a[t] = (byte) (a[t] ^ 1);

		timer.start(pid, M.online_write);
		con2.write(pid, a);
		timer.stop(pid, M.online_write);

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

			if (party == Party.Eddie) {
				con1.write(x);
				byte[] out = this.runP1(predata, x, timer);
				con2.write(out);
				con2.write(x);

			} else if (party == Party.Debbie) {
				x = con1.readDoubleByteArray();
				byte[] out = this.runP2(predata, x, timer);
				con2.write(out);

			} else if (party == Party.Charlie) {
				int index = Crypto.sr.nextInt(l);
				this.runP3(predata, index, timer);
				byte[] out1 = con1.read();
				x = con1.readDoubleByteArray();
				byte[] out2 = con2.read();
				Util.setXor(out1, out2);

				if (!Util.equal(out1, x[index]))
					System.err.println(j + ": SSPIR test failed");
				else
					System.out.println(j + ": SSPIR test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

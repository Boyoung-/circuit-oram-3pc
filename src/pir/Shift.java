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

public class Shift extends Protocol {

	private int pid = P.Shift;

	public Shift(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[][] runE(PreData predata, byte[][] x, int s, Timer timer) {
		timer.start(pid, M.offline_comp);

		int n = x.length;
		int l = x[0].length;
		byte[][] q = new byte[n][];
		for (int i = 0; i < n; i++)
			q[i] = Util.nextBytes(l, Crypto.sr);

		timer.start(pid, M.offline_write);
		con2.write(q);
		timer.stop(pid, M.offline_write);

		timer.start(pid, M.offline_read);
		byte[][] r = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		timer.start(pid, M.online_read);
		byte[][] z = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		byte[][] b = new byte[n][];
		for (int i = 0; i < n; i++) {
			Util.setXor(z[i], r[i]);
			Util.setXor(z[i], x[i]);
		}
		for (int i = 0; i < n; i++) {
			b[i] = z[(i + s) % n];
			Util.setXor(b[i], q[i]);
		}

		timer.stop(pid, M.online_comp);
		return b;
	}

	public void runD(PreData predata, int s, int n, int l, Timer timer) {
		timer.start(pid, M.offline_comp);

		byte[][] r = new byte[n][];
		for (int i = 0; i < n; i++)
			r[i] = Util.nextBytes(l, Crypto.sr);

		timer.start(pid, M.offline_write);
		con1.write(r);
		timer.stop(pid, M.offline_write);

		timer.start(pid, M.offline_read);
		byte[][] p = con2.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		for (int i = 0; i < n; i++)
			Util.setXor(p[i], r[i]);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		byte[][] a = new byte[n][];
		for (int i = 0; i < n; i++)
			a[i] = p[(i + s) % n];

		timer.start(pid, M.online_write);
		con2.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return;
	}

	public byte[][] runC(PreData predata, byte[][] x, Timer timer) {
		timer.start(pid, M.offline_comp);

		int n = x.length;
		int l = x[0].length;
		byte[][] p = new byte[n][];
		for (int i = 0; i < n; i++)
			p[i] = Util.nextBytes(l, Crypto.sr);

		timer.start(pid, M.offline_write);
		con2.write(p);
		timer.stop(pid, M.offline_write);

		timer.start(pid, M.offline_read);
		byte[][] q = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		for (int i = 0; i < n; i++)
			Util.setXor(p[i], x[i]);

		timer.start(pid, M.online_write);
		con1.write(pid, p);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[][] a = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		for (int i = 0; i < n; i++)
			Util.setXor(a[i], q[i]);

		timer.stop(pid, M.online_comp);
		return a;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int n = 500;
			int l = 50;
			byte[][] x = new byte[n][l];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(x[i]);
			}
			int s = Crypto.sr.nextInt(n);

			if (party == Party.Eddie) {
				con1.write(s);
				byte[][] y1 = this.runE(predata, x, s, timer);

				byte[][] x2 = con2.readDoubleByteArray();
				byte[][] y2 = con2.readDoubleByteArray();
				for (int i = 0; i < n; i++) {
					Util.setXor(x2[i], x[i]);
					Util.setXor(y2[i], y1[i]);
				}
				boolean fail = false;
				for (int i = 0; i < n; i++) {
					if (!Util.equal(y2[i], x2[(i + s) % n])) {
						System.err.println(j + ": Shift test failed");
						fail = true;
						break;
					}
				}
				if (!fail)
					System.out.println(j + ": Shift test passed");

			} else if (party == Party.Debbie) {
				s = con1.readInt();
				this.runD(predata, s, n, l, timer);

			} else if (party == Party.Charlie) {
				byte[][] y2 = this.runC(predata, x, timer);

				con1.write(x);
				con1.write(y2);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

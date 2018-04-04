package pir;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.OutFF;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class FlipFlag extends Protocol {

	private int pid = P.FF;

	public FlipFlag(Communication con1, Communication con2) {
		super(con1, con2);
	}

	// TODO: remove loop around setXor: use Util.setXor(byte[][], byte[][])

	public OutFF runE(PreData predata, byte[][] fb_DE, byte[][] fb_CE, int i2, Timer timer) {
		timer.start(pid, M.offline_comp);

		OutFF outff = new OutFF();

		timer.start(pid, M.offline_read);
		outff.fb_DE = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int n = fb_DE.length;
		byte[][] a2 = new byte[n][1];

		Shift shift = new Shift(con1, con2);
		byte[][] m2 = shift.runE(predata, a2, n - i2, timer);

		for (int i = 0; i < n; i++)
			Util.setXor(m2[i], outff.fb_DE[i]);

		timer.start(pid, M.online_write);
		con2.write(pid, m2);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[][] m1 = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		outff.fb_CE = Util.xor(m1, m2);
		Util.setXor(outff.fb_CE, fb_CE);
		Util.setXor(outff.fb_DE, fb_DE);

		timer.stop(pid, M.online_comp);
		return outff;
	}

	public OutFF runD(PreData predata, byte[][] fb_DE, byte[][] fb_CD, int i2, Timer timer) {
		timer.start(pid, M.offline_comp);

		int n = fb_DE.length;
		OutFF outff = new OutFF();
		outff.fb_CD = new byte[n][1];
		outff.fb_DE = new byte[n][1];
		for (int i = 0; i < n; i++) {
			Crypto.sr.nextBytes(outff.fb_CD[i]);
			Crypto.sr.nextBytes(outff.fb_DE[i]);
		}

		timer.start(pid, M.offline_write);
		con1.write(outff.fb_DE);
		con2.write(outff.fb_CD);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		Shift shift = new Shift(con1, con2);
		shift.runD(predata, n - i2, n, 1, timer);

		Util.setXor(outff.fb_CD, fb_CD);
		Util.setXor(outff.fb_DE, fb_DE);

		timer.stop(pid, M.online_comp);
		return outff;
	}

	public OutFF runC(PreData predata, byte[][] fb_CD, byte[][] fb_CE, int i1, Timer timer) {
		timer.start(pid, M.offline_comp);

		OutFF outff = new OutFF();

		timer.start(pid, M.offline_read);
		outff.fb_CD = con2.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int n = fb_CD.length;
		byte[][] a1 = new byte[n][1];
		a1[i1][0] = 1;

		Shift shift = new Shift(con1, con2);
		byte[][] m1 = shift.runC(predata, a1, timer);

		for (int i = 0; i < n; i++)
			Util.setXor(m1[i], outff.fb_CD[i]);

		timer.start(pid, M.online_write);
		con1.write(pid, m1);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[][] m2 = con1.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		outff.fb_CE = Util.xor(m1, m2);
		Util.setXor(outff.fb_CE, fb_CE);
		Util.setXor(outff.fb_CD, fb_CD);

		timer.stop(pid, M.online_comp);
		return outff;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int n = 100;
			int l = 1;
			byte[][] fb_CD = new byte[n][l];
			byte[][] fb_CE = new byte[n][l];
			byte[][] fb_DE = new byte[n][l];
			byte[][] fb = new byte[n][1];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(fb_CD[i]);
				Crypto.sr.nextBytes(fb_CE[i]);
				Crypto.sr.nextBytes(fb_DE[i]);
				fb[i][0] = (byte) (fb_CD[i][0] ^ fb_DE[i][0] ^ fb_CE[i][0]);
			}
			int i1 = Crypto.sr.nextInt(n);
			int i2 = Crypto.sr.nextInt(n);
			int i = (i1 + i2) % n;
			OutFF outff = new OutFF();

			if (party == Party.Eddie) {
				con1.write(fb_CD);
				con1.write(fb_DE);
				con1.write(i2);
				con2.write(fb_CE);
				con2.write(fb_CD);
				con2.write(i1);

				outff = this.runE(predata, fb_DE, fb_CE, i2, timer);
				outff.fb_CD = con1.readDoubleByteArray();

				byte[][] fbp = Util.xor(Util.xor(outff.fb_CD, outff.fb_CE), outff.fb_DE);
				fbp[i][0] = (byte) (fbp[i][0] ^ 1);

				boolean fail = false;
				for (int k = 0; k < n; k++) {
					if ((fb[k][0] & 1) != (fbp[k][0] & 1)) {
						System.err.println(j + ": FlipFlag test failed");
						fail = true;
						break;
					}
				}
				if (!fail)
					System.out.println(j + ": FlipFlag test passed");

			} else if (party == Party.Debbie) {
				fb_CD = con1.readDoubleByteArray();
				fb_DE = con1.readDoubleByteArray();
				i2 = con1.readInt();

				outff = this.runD(predata, fb_DE, fb_CD, i2, timer);
				con1.write(outff.fb_CD);

			} else if (party == Party.Charlie) {
				fb_CE = con1.readDoubleByteArray();
				fb_CD = con1.readDoubleByteArray();
				i1 = con1.readInt();

				outff = this.runC(predata, fb_CD, fb_CE, i1, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

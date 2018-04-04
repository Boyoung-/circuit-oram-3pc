package pir;

import java.util.Arrays;

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

public class ShiftXorPIR extends Protocol {

	private int pid = P.SftXorPIR;

	public ShiftXorPIR(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[] runP1(PreData predata, byte[][] x, int s1, int s2, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		int n = x.length;
		int l = x[0].length / m;

		byte[][] xp = new byte[n * m][];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				xp[i * m + j] = Arrays.copyOfRange(x[(i + s1) % n], (j ^ s2) * l, ((j ^ s2) + 1) * l);
			}
		}

		SSPIR sspir = new SSPIR(con1, con2);
		byte[] z = sspir.runP1(predata, xp, timer);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public byte[] runP2(PreData predata, byte[][] x, int s1, int s2, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		int n = x.length;
		int l = x[0].length / m;

		byte[][] xp = new byte[n * m][];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				xp[i * m + j] = Arrays.copyOfRange(x[(i + s1) % n], (j ^ s2) * l, ((j ^ s2) + 1) * l);
			}
		}

		SSPIR sspir = new SSPIR(con1, con2);
		byte[] z = sspir.runP2(predata, xp, timer);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public void runP3(PreData predata, int t1, int t2, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		int t = t1 * m + t2;

		SSPIR sspir = new SSPIR(con1, con2);
		sspir.runP3(predata, t, timer);

		timer.stop(pid, M.online_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int n = 500;
			int m = 16;
			int l = 4;
			byte[][] x = new byte[n][m * l];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(x[i]);
			}
			int s1 = Crypto.sr.nextInt(n);
			int t1 = Crypto.sr.nextInt(n);
			int s2 = Crypto.sr.nextInt(m);
			int t2 = Crypto.sr.nextInt(m);

			if (party == Party.Eddie) {
				con1.write(x);
				con1.write(s1);
				con1.write(s2);
				con2.write(t1);
				con2.write(t2);
				byte[] e = this.runP1(predata, x, s1, s2, m, timer);

				byte[] d = con1.read();
				Util.setXor(e, d);

				int i1 = (s1 + t1) % n;
				int i2 = s2 ^ t2;
				byte[] expect = Arrays.copyOfRange(x[i1], i2 * l, (i2 + 1) * l);

				if (!Util.equal(e, expect))
					System.err.println(j + ": ShiftXorPIR test failed");
				else
					System.out.println(j + ": ShiftXorPIR test passed");

			} else if (party == Party.Debbie) {
				x = con1.readDoubleByteArray();
				s1 = con1.readInt();
				s2 = con1.readInt();
				byte[] d = this.runP2(predata, x, s1, s2, m, timer);

				con1.write(d);

			} else if (party == Party.Charlie) {
				t1 = con1.readInt();
				t2 = con1.readInt();
				this.runP3(predata, t1, t2, m, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

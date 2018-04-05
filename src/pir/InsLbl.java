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

public class InsLbl extends Protocol {

	private int pid = P.InsLbl;

	public InsLbl(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runP1(PreData predata, int dN1, byte[] L1, int ttp, Timer timer) {
		timer.start(pid, M.offline_comp);

		int l = L1.length;

		byte[] p = Util.nextBytes(ttp * l, Crypto.sr);
		byte[] a = Util.nextBytes(ttp * l, Crypto.sr);
		byte[] b = Util.nextBytes(ttp * l, Crypto.sr);
		int v = Crypto.sr.nextInt(ttp);
		int w = Crypto.sr.nextInt(ttp);

		int alpha1 = Crypto.sr.nextInt(ttp);
		int u1 = alpha1 ^ v;
		byte[] pstar = Util.xor(p, Util.xorRotate(a, u1, ttp, l));

		timer.start(pid, M.offline_write);
		con1.write(p);
		con1.write(a);
		con1.write(b);
		con1.write(v);
		con1.write(w);
		con2.write(u1);
		con2.write(pstar);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int m = dN1 ^ alpha1;

		timer.start(pid, M.online_write);
		con1.write(pid, m);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		m = con1.readInt(pid);
		timer.stop(pid, M.online_read);

		int beta1 = m ^ dN1;

		int index = beta1 ^ w;
		for (int i = 0; i < l; i++) {
			b[index * l + i] = (byte) (b[index * l + i] ^ L1[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, b);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return;
	}

	public byte[] runP2(PreData predata, int dN2, byte[] L2, int ttp, Timer timer) {
		timer.start(pid, M.offline_comp);

		int l = L2.length;

		timer.start(pid, M.offline_read);
		byte[] p = con1.read();
		byte[] a = con1.read();
		byte[] b = con1.read();
		int v = con1.readInt();
		int w = con1.readInt();
		timer.stop(pid, M.offline_read);

		int beta2 = Crypto.sr.nextInt(ttp);
		int u2 = beta2 ^ w;
		byte[] z2 = Util.xor(p, Util.xorRotate(b, u2, ttp, l));

		timer.start(pid, M.offline_write);
		con2.write(u2);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int m = beta2 ^ dN2;

		timer.start(pid, M.online_write);
		con1.write(pid, m);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		m = con1.readInt(pid);
		timer.stop(pid, M.online_read);

		int alpha2 = m ^ dN2;

		int index = alpha2 ^ v;
		for (int i = 0; i < l; i++) {
			a[index * l + i] = (byte) (a[index * l + i] ^ L2[i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return z2;
	}

	public byte[] runP3(PreData predata, int ttp, int l, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		int u1 = con1.readInt();
		byte[] pstar = con1.read();
		int u2 = con2.readInt();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		timer.start(pid, M.online_read);
		byte[] s1 = con1.read(pid);
		byte[] s2 = con2.read(pid);
		timer.stop(pid, M.online_read);

		s2 = Util.xorRotate(s2, u1, ttp, l);
		s1 = Util.xorRotate(s1, u2, ttp, l);
		Util.setXor(pstar, s1);
		Util.setXor(pstar, s2);

		timer.stop(pid, M.online_comp);
		return pstar;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int ttp = (int) Math.pow(2, 8);
			int l = 10;
			int dN1 = Crypto.sr.nextInt(ttp);
			int dN2 = Crypto.sr.nextInt(ttp);
			byte[] L1 = Util.nextBytes(l, Crypto.sr);
			byte[] L2 = Util.nextBytes(l, Crypto.sr);

			if (party == Party.Eddie) {
				this.runP1(predata, dN1, L1, ttp, timer);
				con1.write(dN1);
				con1.write(L1);

			} else if (party == Party.Debbie) {
				byte[] m1 = this.runP2(predata, dN2, L2, ttp, timer);
				byte[] m2 = con2.read();
				dN1 = con1.readInt();
				L1 = con1.read();
				int dN = dN1 ^ dN2;
				byte[] L = Util.xor(L1, L2);
				byte[] M = Util.xor(m1, m2);
				byte[] expectL = Arrays.copyOfRange(M, dN * l, dN * l + l);

				boolean fail = false;
				if (!Util.equal(L, expectL)) {
					System.err.println(j + ": InsLbl test failed on L");
					fail = true;
				}
				for (int i = 0; i < dN * l; i++) {
					if (M[i] != 0) {
						System.err.println(j + ": InsLbl test failed 1");
						fail = true;
						break;
					}
				}
				for (int i = dN * l + l; i < M.length; i++) {
					if (M[i] != 0) {
						System.err.println(j + ": InsLbl test failed 2");
						fail = true;
						break;
					}
				}
				if (!fail)
					System.out.println(j + ": InsLbl test passed");

			} else if (party == Party.Charlie) {
				byte[] m2 = this.runP3(predata, ttp, l, timer);
				con2.write(m2);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

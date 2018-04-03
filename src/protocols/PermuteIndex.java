package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.precomputation.PrePermuteIndex;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PermuteIndex extends Protocol {

	private int pid = P.PI;

	public PermuteIndex(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE() {
	}

	public int[] runD(PreData predata, boolean firstTree, byte[][] ti, int w, Timer timer) {
		if (firstTree)
			return null;

		timer.start(pid, M.online_comp);

		byte[][] z = Util.xor(ti, predata.pi_p);

		timer.start(pid, M.online_write);
		con2.write(pid, z);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[][] g = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		ti = Util.xor(predata.pi_a, g);

		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));
		int[] ti_pp = new int[ti.length];
		for (int i = 0; i < ti.length; i++)
			ti_pp[i] = Util.getSubBits(new BigInteger(ti[i]), logW, 0).intValue();

		timer.stop(pid, M.online_comp);
		return ti_pp;
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.online_comp);

		timer.start(pid, M.online_read);
		byte[][] z = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		z = Util.xor(z, predata.pi_r);
		z = Util.permute(z, predata.evict_pi);
		byte[][] g = Util.xor(predata.evict_rho, z);

		timer.start(pid, M.online_write);
		con2.write(pid, g);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 100; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PrePermuteIndex prepermuteindex = new PrePermuteIndex(con1, con2);

			if (party == Party.Eddie) {
				int d = Crypto.sr.nextInt(15) + 5;
				int w = Crypto.sr.nextInt(15) + 5;
				int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));

				byte[][] ti = new byte[d][];
				predata.evict_pi = Util.randomPermutation(d, Crypto.sr);
				predata.evict_rho = new byte[d][];
				for (int j = 0; j < d; j++) {
					ti[j] = Util.nextBytes((logW + 7) / 8, Crypto.sr);
					predata.evict_rho[j] = Util.nextBytes((logW + 7) / 8, Crypto.sr);
				}

				con1.write(predata.evict_pi);
				con1.write(predata.evict_rho);
				con1.write(ti);
				con1.write(w);

				con2.write(predata.evict_pi);
				con2.write(predata.evict_rho);

				prepermuteindex.runE(predata, d, w, timer);

				runE();

				int[] ti_pp = con1.readIntArray();
				ti = Util.permute(ti, predata.evict_pi);
				int j = 0;
				for (; j < d; j++) {
					int tmp = Util.getSubBits(new BigInteger(Util.xor(predata.evict_rho[j], ti[j])), logW, 0)
							.intValue();
					if (tmp != ti_pp[j]) {
						System.err.println("PermuteIndex test failed");
						break;
					}
				}
				if (j == d)
					System.out.println("PermuteIndex test passed");

			} else if (party == Party.Debbie) {
				predata.evict_pi = con1.readIntArray();
				predata.evict_rho = con1.readDoubleByteArray();
				byte[][] ti = con1.readDoubleByteArray();
				int w = con1.readInt();

				prepermuteindex.runD(predata, timer);

				int[] ti_pp = runD(predata, false, ti, w, timer);
				con1.write(ti_pp);

			} else if (party == Party.Charlie) {
				predata.evict_pi = con1.readIntArray();
				predata.evict_rho = con1.readDoubleByteArray();

				prepermuteindex.runC(predata, timer);

				runC(predata, false, timer);

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

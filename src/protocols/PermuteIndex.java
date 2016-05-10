package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class PermuteIndex extends Protocol {
	public PermuteIndex(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE() {
	}

	public int[] runD(PreData predata, boolean firstTree, int[] ti, Timer timer) {
		if (firstTree)
			return null;
		
		BigInteger[] ti_p = new BigInteger[ti.length];
		for (int i=0; i<ti.length; i++)
			ti_p[i] = BigInteger.valueOf(ti[i]);

		BigInteger[] z = Util.xor(ti_p, predata.pi_p);

		con2.write(z);

		BigInteger[] g = con2.readObject();

		ti_p = Util.xor(predata.pi_a, g);

		int[] ti_pp = new int[ti.length];
		for (int i=0; i<ti.length; i++)
			ti_pp[i] = ti_p[i].intValue();
		
		return ti_pp;
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

		BigInteger[] z = con2.readObject();

		z = Util.xor(z, predata.pi_r);
		z = Util.permute(z, predata.evict_pi);
		BigInteger[] g = Util.xor(predata.evict_rho, z);

		con2.write(g);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 10; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PrePermuteIndex prepermuteindex = new PrePermuteIndex(con1, con2);

			if (party == Party.Eddie) {
				int d = Crypto.sr.nextInt(5) + 5;
				int w = Crypto.sr.nextInt(5) + 5;
				int logW = (int) Math.ceil(Math.log(w+1) / Math.log(2));
				
				int[] ti = new int[d];
				predata.evict_pi = Util.randomPermutation(d, Crypto.sr);
				predata.evict_rho = new BigInteger[d];
				for (int j=0; j<d; j++) {
					ti[j] = Crypto.sr.nextInt(w+1);
					predata.evict_rho[j] = new BigInteger(logW, Crypto.sr);
				}

				con1.write(predata.evict_pi);
				con1.write(predata.evict_rho);
				con1.write(ti);

				con2.write(predata.evict_pi);
				con2.write(predata.evict_rho);

				prepermuteindex.runE(predata, d, w, timer);

				runE();

				ti = Util.permute(ti, predata.evict_pi);
				for (int j = 0; j < d; j++) {
					ti[j] = predata.evict_rho[j].intValue() ^ ti[j];
					System.out.print(ti[j] + " ");
				}
				System.out.println();

			} else if (party == Party.Debbie) {
				predata.evict_pi = con1.readObject();
				predata.evict_rho = con1.readObject();
				int[] ti = con1.readObject();

				prepermuteindex.runD(predata, timer);

				int[] ti_pp = runD(predata, false, ti, timer);
				for (int j = 0; j < ti.length; j++) {
					System.out.print(ti_pp[j] + " ");
				}
				System.out.println();

			} else if (party == Party.Charlie) {
				predata.evict_pi = con1.readObject();
				predata.evict_rho = con1.readObject();

				prepermuteindex.runC(predata, timer);

				runC(predata, false, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

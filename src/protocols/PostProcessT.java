package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Global;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.precomputation.PreAccess;
import protocols.precomputation.PrePostProcessT;
import protocols.struct.OutAccess;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PostProcessT extends Protocol {

	private int pid = P.PPT;

	public PostProcessT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Tuple runE(PreData predata, Tuple Ti, boolean lastTree, Timer timer) {
		timer.start(pid, M.online_comp);

		if (lastTree) {
			Tuple out = new Tuple(Ti);
			Util.setXor(out.getL(), predata.ppt_Li);

			timer.stop(pid, M.online_comp);
			return out;
		}

		// step 1
		timer.start(pid, M.online_read);
		int delta = con2.readInt();
		timer.stop(pid, M.online_read);

		// step 3
		int twoTauPow = predata.ppt_s.length;
		byte[][] e = new byte[twoTauPow][];
		for (int i = 0; i < twoTauPow; i++)
			e[i] = predata.ppt_s[(i + delta) % twoTauPow];
		byte[] e_all = new byte[twoTauPow * e[0].length];
		for (int i = 0; i < twoTauPow; i++)
			System.arraycopy(e[i], 0, e_all, i * e[0].length, e[0].length);

		Tuple out = new Tuple(Ti);
		Util.setXor(out.getL(), predata.ppt_Li);
		Util.setXor(out.getA(), e_all);

		timer.stop(pid, M.online_comp);
		return out;
	}

	public void runD() {
	}

	public Tuple runC(PreData predata, Tuple Ti, byte[] Li, byte[] Lip1, int j2, boolean lastTree, Timer timer) {
		timer.start(pid, M.online_comp);

		if (lastTree) {
			Tuple out = new Tuple(Ti);
			Util.setXor(out.getL(), Util.xor(Li, predata.ppt_Li));

			timer.stop(pid, M.online_comp);
			return out;
		}

		// step 1
		int twoTauPow = predata.ppt_r.length;
		int delta = (predata.ppt_alpha - j2 + twoTauPow) % twoTauPow;

		timer.start(pid, M.online_write);
		con1.write(pid, delta);
		timer.stop(pid, M.online_write);

		// step 2
		byte[][] c = new byte[twoTauPow][];
		for (int i = 0; i < twoTauPow; i++)
			c[i] = predata.ppt_r[(i + delta) % twoTauPow];
		c[j2] = Util.xor(Util.xor(c[j2], Lip1), predata.ppt_Lip1);
		byte[] c_all = new byte[twoTauPow * Lip1.length];
		for (int i = 0; i < twoTauPow; i++)
			System.arraycopy(c[i], 0, c_all, i * Lip1.length, Lip1.length);

		Tuple out = new Tuple(Ti);
		Util.setXor(out.getL(), Util.xor(Li, predata.ppt_Li));
		Util.setXor(out.getA(), c_all);

		timer.stop(pid, M.online_comp);
		return out;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		int records = 5;
		int repeat = 5;

		int tau = md.getTau();
		int numTrees = md.getNumTrees();
		long numInsert = md.getNumInsertRecords();
		int addrBits = md.getAddrBits();

		Timer timer = new Timer();

		sanityCheck();

		System.out.println();

		for (int i = 0; i < records; i++) {
			long N = Global.cheat ? 0 : Util.nextLong(numInsert, Crypto.sr);

			for (int j = 0; j < repeat; j++) {
				System.out.println("Test: " + i + " " + j);
				System.out.println("N=" + BigInteger.valueOf(N).toString(2));

				byte[] Li = new byte[0];

				PreData prev = null;

				for (int ti = 0; ti < numTrees; ti++) {
					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);

					PreData predata = new PreData();
					PreAccess preaccess = new PreAccess(con1, con2);
					Access access = new Access(con1, con2);
					PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);

					if (party == Party.Eddie) {
						Tree OTi = forest.getTree(ti);
						int numTuples = (OTi.getD() - 1) * OTi.getW() + OTi.getStashSize();
						int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
								md.getABytesOfTree(ti) };
						preaccess.runE(predata, md.getTwoTauPow(), numTuples, tupleParam, timer);

						byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
						byte[] sD_Ni = Util.xor(Ni, sE_Ni);
						con1.write(sD_Ni);

						byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
						byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
						con1.write(sD_Nip1_pr);

						OutAccess outaccess = access.runE(predata, OTi, sE_Ni, sE_Nip1_pr, timer);

						if (ti == numTrees - 1)
							con2.write(N);

						prepostprocesst.runE(predata, timer);
						Tuple Ti_prime = runE(predata, outaccess.E_Ti, ti == numTrees - 1, timer);

						Ti_prime.setXor(con2.readTuple());
						byte[] Li_prime = Util.xor(predata.ppt_Li, con2.read());
						byte[] Lip1_prime = Util.xor(predata.ppt_Lip1, con2.read());
						int j2 = con2.readInt();
						Tuple Ti = outaccess.E_Ti.xor(con2.readTuple());

						if (!Util.equal(Ti.getF(), Ti_prime.getF()))
							System.err.println("PPT test failed");
						else if (!Util.equal(Ti.getN(), Ti_prime.getN()))
							System.err.println("PPT test failed");
						else if (!Util.equal(Li_prime, Ti_prime.getL()))
							System.err.println("PPT test failed");
						else if (!Util.equal(Lip1_prime,
								Ti_prime.getSubA(j2 * Lip1_prime.length, (j2 + 1) * Lip1_prime.length)))
							System.err.println("PPT test failed");
						else
							System.out.println("PPT test passed");

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runD(predata, timer);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						access.runD(predata, OTi, sD_Ni, sD_Nip1_pr, timer);

						prepostprocesst.runD(predata, prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), tau,
								timer);
						runD();

					} else if (party == Party.Charlie) {
						preaccess.runC(timer);

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						OutAccess outaccess = access.runC(md, ti, Li, timer);

						prepostprocesst.runC(predata, prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), timer);
						Tuple Ti_prime = runC(predata, outaccess.C_Ti, Li, outaccess.C_Lip1, outaccess.C_j2,
								ti == numTrees - 1, timer);

						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = con1.readLong();
							long data = new BigInteger(1, outaccess.C_Ti.getA()).longValue();
							if (N == data) {
								System.out.println("Access passed");
								System.out.println();
							} else {
								throw new AccessException("Access failed");
							}
						}

						con1.write(Ti_prime);
						con1.write(predata.ppt_Li);
						con1.write(predata.ppt_Lip1);
						con1.write(outaccess.C_j2);
						con1.write(outaccess.C_Ti);

					} else {
						throw new NoSuchPartyException(party + "");
					}

					prev = predata;
				}
			}
		}

		// timer.print();
	}
}

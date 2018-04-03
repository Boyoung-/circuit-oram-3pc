package pir;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Global;
import oram.Metadata;
import oram.Tree;
import pir.precomputation.PrePIRReshuffle;
import protocols.Protocol;
import protocols.precomputation.PreAccess;
import protocols.struct.OutAccess;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PIRReshuffle extends Protocol {

	private int pid = P.RSF;

	public PIRReshuffle(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[][] runE(PreData predata, byte[][] path, boolean firstTree, Timer timer) {
		if (firstTree)
			return path;

		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		byte[][] z = con2.readDoubleByteArray(pid);
		timer.stop(pid, M.online_read);

		// step 2
		byte[][] b = new byte[z.length][];
		for (int i = 0; i < b.length; i++)
			b[i] = Util.xor(Util.xor(path[i], z[i]), predata.pir_reshuffle_r[i]);
		byte[][] b_prime = Util.permute(b, predata.reshuffle_pi);

		timer.stop(pid, M.online_comp);
		return b_prime;
	}

	public void runD() {
	}

	public byte[][] runC(PreData predata, byte[][] path, boolean firstTree, Timer timer) {
		if (firstTree)
			return path;

		timer.start(pid, M.online_comp);

		// step 1
		byte[][] z = new byte[path.length][];
		for (int i = 0; i < z.length; i++)
			z[i] = Util.xor(path[i], predata.pir_reshuffle_p[i]);

		timer.start(pid, M.online_write);
		con1.write(pid, z);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return predata.pir_reshuffle_a_prime;
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

				for (int ti = 0; ti < numTrees; ti++) {
					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);

					PreData predata = new PreData();
					PreAccess preaccess = new PreAccess(con1, con2);
					PIRAccess access = new PIRAccess(con1, con2);
					PrePIRReshuffle prereshuffle = new PrePIRReshuffle(con1, con2);

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

						prereshuffle.runE(predata, timer);
						byte[][] fbArray = new byte[outaccess.E_P.length][];
						for (int i1 = 0; i1 < fbArray.length; i1++)
							fbArray[i1] = outaccess.E_P[i1].getF().clone();
						byte[][] E_P_prime = runE(predata, fbArray, ti == 0, timer);

						byte[][] C_P = con2.readDoubleByteArray();
						byte[][] C_P_prime = con2.readDoubleByteArray();
						byte[][] oldPath = new byte[C_P.length][];
						byte[][] newPath = new byte[C_P.length][];

						for (int k = 0; k < C_P.length; k++) {
							oldPath[k] = Util.xor(outaccess.E_P[k].getF(), C_P[k]);
							newPath[k] = Util.xor(E_P_prime[k], C_P_prime[k]);
						}
						oldPath = Util.permute(oldPath, predata.reshuffle_pi);

						boolean pass = true;
						for (int k = 0; k < newPath.length; k++) {
							if (!Util.equal(oldPath[k], newPath[k])) {
								System.err.println("PIR Reshuffle test failed");
								pass = false;
								break;
							}
						}
						if (pass)
							System.out.println("PIR Reshuffle test passed");

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runD(predata, timer);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						access.runD(predata, OTi, sD_Ni, sD_Nip1_pr, timer);

						int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
								md.getABytesOfTree(ti) };
						prereshuffle.runD(predata, tupleParam, timer);
						runD();

					} else if (party == Party.Charlie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runC(timer);

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						OutAccess outaccess = access.runC(md, OTi, ti, Li, timer);

						prereshuffle.runC(predata, timer);
						byte[][] fbArray = new byte[outaccess.C_P.length][];
						for (int i1 = 0; i1 < fbArray.length; i1++)
							fbArray[i1] = outaccess.C_P[i1].getF().clone();
						byte[][] C_P_prime = runC(predata, fbArray, ti == 0, timer);

						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = con1.readLong();
							// long data = new BigInteger(1,
							// outaccess.C_Ti.getA()).longValue();
							// if (N == data) {
							// System.out.println("Access passed");
							// System.out.println();
							// } else {
							// throw new AccessException("Access failed");
							// }
						}

						con1.write(fbArray);
						con1.write(C_P_prime);

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
			}
		}

		// timer.print();
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {
		// TODO Auto-generated method stub

	}
}

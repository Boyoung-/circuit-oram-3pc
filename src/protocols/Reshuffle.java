package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.precomputation.PreAccess;
import protocols.precomputation.PreReshuffle;
import protocols.struct.OutAccess;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class Reshuffle extends Protocol {

	private int pid = P.RSF;

	public Reshuffle(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Tuple[] runE(PreData predata, Tuple[] path, boolean firstTree, Timer timer) {
		if (firstTree)
			return path;

		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		Tuple[] z = con2.readObject();
		timer.stop(pid, M.online_read);

		// step 2
		Tuple[] b = new Tuple[z.length];
		for (int i = 0; i < b.length; i++)
			b[i] = path[i].xor(z[i]).xor(predata.reshuffle_r[i]);
		Tuple[] b_prime = Util.permute(b, predata.reshuffle_pi);

		timer.stop(pid, M.online_comp);
		return b_prime;
	}

	public void runD() {
	}

	public Tuple[] runC(PreData predata, Tuple[] path, boolean firstTree, Timer timer) {
		if (firstTree)
			return path;

		timer.start(pid, M.online_comp);

		// step 1
		Tuple[] z = new Tuple[path.length];
		for (int i = 0; i < z.length; i++)
			z[i] = path[i].xor(predata.reshuffle_p[i]);

		timer.start(pid, M.online_write);
		con1.write(z);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return predata.reshuffle_a_prime;
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
			long N = Util.nextLong(numInsert, Crypto.sr);

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
					Access access = new Access(con1, con2);
					PreReshuffle prereshuffle = new PreReshuffle(con1, con2);

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
						Tuple[] E_P_prime = runE(predata, outaccess.E_P, ti == 0, timer);

						Tuple[] C_P = con2.readObject();
						Tuple[] C_P_prime = con2.readObject();
						Tuple[] oldPath = new Tuple[C_P.length];
						Tuple[] newPath = new Tuple[C_P.length];

						for (int k = 0; k < C_P.length; k++) {
							oldPath[k] = outaccess.E_P[k].xor(C_P[k]);
							newPath[k] = E_P_prime[k].xor(C_P_prime[k]);
						}
						oldPath = Util.permute(oldPath, predata.reshuffle_pi);

						boolean pass = true;
						for (int k = 0; k < newPath.length; k++) {
							if (!oldPath[k].equals(newPath[k])) {
								System.err.println("Reshuffle test failed");
								pass = false;
								break;
							}
						}
						if (pass)
							System.out.println("Reshuffle test passed");

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
						preaccess.runC(timer);

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						OutAccess outaccess = access.runC(md, ti, Li, timer);

						prereshuffle.runC(predata, timer);
						Tuple[] C_P_prime = runC(predata, outaccess.C_P, ti == 0, timer);

						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = con1.readObject();
							long data = new BigInteger(1, outaccess.C_Ti.getA()).longValue();
							if (N == data) {
								System.out.println("Access passed");
								System.out.println();
							} else {
								throw new AccessException("Access failed");
							}
						}

						con1.write(outaccess.C_P);
						con1.write(C_P_prime);

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
			}
		}

		// timer.print();
	}
}

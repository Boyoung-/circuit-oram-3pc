package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.precomputation.PreRetrieve;
import protocols.struct.OutAccess;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.StopWatch;
import util.Timer;
import util.Util;

public class Retrieve extends Protocol {

	public Retrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutAccess runE(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, int h, Timer timer) {
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		OutAccess outaccess = access.runE(predata, OTi, Ni, Nip1_pr, timer);
		Tuple[] path = reshuffle.runE(predata, outaccess.E_P, OTi.getTreeIndex() == 0, timer);
		Tuple Ti = postprocesst.runE(predata, outaccess.E_Ti, OTi.getTreeIndex() == h - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, OTi.getStashSize());
		root = updateroot.runE(predata, OTi.getTreeIndex() == 0, outaccess.Li, root, Ti, timer);
		System.arraycopy(root, 0, path, 0, root.length);
		eviction.runE(predata, OTi.getTreeIndex() == 0, outaccess.Li,
				OTi.getTreeIndex() == 0 ? new Tuple[] { Ti } : path, OTi.getD(), OTi.getW(), OTi, timer);

		return outaccess;
	}

	public void runD(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer timer) {
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		byte[] Li = access.runD(predata, OTi, Ni, Nip1_pr, timer);
		reshuffle.runD();
		postprocesst.runD();
		updateroot.runD(predata, OTi.getTreeIndex() == 0, Li, OTi.getW(), timer);
		eviction.runD(predata, OTi.getTreeIndex() == 0, Li, OTi.getW(), OTi, timer);
	}

	public OutAccess runC(PreData predata, Metadata md, int ti, byte[] Li, int h, Timer timer) {
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		OutAccess outaccess = access.runC(md, ti, Li, timer);
		Tuple[] path = reshuffle.runC(predata, outaccess.C_P, ti == 0, timer);
		Tuple Ti = postprocesst.runC(predata, outaccess.C_Ti, Li, outaccess.C_Lip1, outaccess.C_j2, ti == h - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, md.getStashSizeOfTree(ti));
		root = updateroot.runC(predata, ti == 0, root, Ti, timer);
		System.arraycopy(root, 0, path, 0, root.length);
		eviction.runC(predata, ti == 0, ti == 0 ? new Tuple[] { Ti } : path, md.getLBitsOfTree(ti) + 1, md.getW(),
				md.getStashSizeOfTree(ti), timer);

		return outaccess;
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
		StopWatch sw = new StopWatch();

		sanityCheck();

		System.out.println();

		for (int i = 0; i < records; i++) {
			long N = Util.nextLong(numInsert, Crypto.sr);

			for (int j = 0; j < repeat; j++) {
				System.out.println("Test: " + i + " " + j);
				System.out.println("N=" + BigInteger.valueOf(N).toString(2));

				System.out.print("Precomputation... ");
				PreData[] predata = new PreData[numTrees];
				PreRetrieve preretrieve = new PreRetrieve(con1, con2);
				for (int ti = 0; ti < numTrees; ti++) {
					predata[ti] = new PreData();

					if (party == Party.Eddie) {
						preretrieve.runE(predata[ti], md, ti, timer);

					} else if (party == Party.Debbie) {
						preretrieve.runD(predata[ti], md, ti, ti == 0 ? null : predata[ti - 1], timer);

					} else if (party == Party.Charlie) {
						preretrieve.runC(predata[ti], md, ti, ti == 0 ? null : predata[ti - 1], timer);

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
				System.out.println("done!");

				byte[] Li = new byte[0];
				for (int ti = 0; ti < numTrees; ti++) {
					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);

					if (party == Party.Eddie) {
						Tree OTi = forest.getTree(ti);

						byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
						byte[] sD_Ni = Util.xor(Ni, sE_Ni);
						con1.write(sD_Ni);

						byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
						byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
						con1.write(sD_Nip1_pr);

						sw.start();
						runE(predata[ti], OTi, sE_Ni, sE_Nip1_pr, numTrees, timer);
						sw.stop();

						if (ti == numTrees - 1)
							con2.write(N);

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);

						byte[] sD_Ni = con1.read();
						byte[] sD_Nip1_pr = con1.read();

						sw.start();
						runD(predata[ti], OTi, sD_Ni, sD_Nip1_pr, timer);
						sw.stop();

					} else if (party == Party.Charlie) {
						int lBits = md.getLBitsOfTree(ti);
						System.out.println("L" + ti + "="
								+ Util.addZeros(Util.getSubBits(new BigInteger(1, Li), lBits, 0).toString(2), lBits));

						sw.start();
						OutAccess outaccess = runC(predata[ti], md, ti, Li, numTrees, timer);
						sw.stop();

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

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
			}
		}

		// timer.print();

		// System.out.println();
		// System.out.println(sw.toMS());
	}
}

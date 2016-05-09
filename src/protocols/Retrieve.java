package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.StopWatch;
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

		OutAccess outaccess = access.runE(predata, OTi, Ni, Nip1_pr, timer);
		Tuple[] path = reshuffle.runE(predata, outaccess.E_P, OTi.getTreeIndex() == 0, timer);
		Tuple Ti = postprocesst.runE(predata, outaccess.E_Ti, OTi.getTreeIndex() == h - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, OTi.getStashSize());
		root = updateroot.runE(predata, OTi.getTreeIndex() == 0, outaccess.Li, root, Ti, timer);

		return outaccess;
	}

	public void runD(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer timer) {
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);

		byte[] Li = access.runD(predata, OTi, Ni, Nip1_pr, timer);
		reshuffle.runD();
		postprocesst.runD();
		updateroot.runD(predata, OTi.getTreeIndex() == 0, Li, OTi.getW(), timer);
	}

	public OutAccess runC(PreData predata, Metadata md, int ti, byte[] Li, int h, Timer timer) {
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);

		OutAccess outaccess = access.runC(md, ti, Li, timer);
		Tuple[] path = reshuffle.runC(predata, outaccess.C_P, ti == 0, timer);
		Tuple Ti = postprocesst.runC(predata, outaccess.C_Ti, Li, outaccess.C_Lip1, outaccess.C_j2, ti == h - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, md.getStashSizeOfTree(ti));
		root = updateroot.runC(predata, ti == 0, root, Ti, timer);

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

				byte[] Li = new byte[0];

				PreData prev = null;

				for (int ti = 0; ti < numTrees; ti++) {
					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);

					PreData predata = new PreData();
					PreRetrieve preretrieve = new PreRetrieve(con1, con2);

					if (party == Party.Eddie) {
						Tree OTi = forest.getTree(ti);
						preretrieve.runE(predata, md, ti, timer);

						byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
						byte[] sD_Ni = Util.xor(Ni, sE_Ni);
						con1.write(sD_Ni);

						byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
						byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
						con1.write(sD_Nip1_pr);

						sw.start();
						runE(predata, OTi, sE_Ni, sE_Nip1_pr, numTrees, timer);
						sw.stop();

						if (ti == numTrees - 1)
							con2.write(N);

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preretrieve.runD(predata, md, ti, prev, timer);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						sw.start();
						runD(predata, OTi, sD_Ni, sD_Nip1_pr, timer);
						sw.stop();

					} else if (party == Party.Charlie) {
						preretrieve.runC(predata, md, ti, prev, timer);

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						sw.start();
						OutAccess outaccess = runC(predata, md, ti, Li, numTrees, timer);
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

					prev = predata;
				}
			}
		}

		// timer.print();

		// System.out.println();
		// System.out.println(sw.toMS());
	}
}

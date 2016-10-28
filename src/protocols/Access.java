package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import oram.Bucket;
import oram.Forest;
import oram.Global;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.precomputation.PreAccess;
import protocols.struct.OutAccess;
import protocols.struct.OutSSCOT;
import protocols.struct.OutSSIOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class Access extends Protocol {

	private int pid = P.ACC;

	public Access(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutAccess runE(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		pathTuples = Util.permute(pathTuples, predata.access_sigma);

		// step 3
		byte[] y = null;
		if (OTi.getTreeIndex() == 0)
			y = pathTuples[0].getA();
		else if (OTi.getTreeIndex() < OTi.getH() - 1)
			y = Util.nextBytes(OTi.getABytes(), Crypto.sr);
		else
			y = new byte[OTi.getABytes()];

		if (OTi.getTreeIndex() > 0) {
			byte[][] a = new byte[pathTuples.length][];
			byte[][] m = new byte[pathTuples.length][];
			for (int i = 0; i < pathTuples.length; i++) {
				m[i] = Util.xor(pathTuples[i].getA(), y);
				a[i] = ArrayUtils.addAll(pathTuples[i].getF(), pathTuples[i].getN());
				for (int j = 0; j < Ni.length; j++)
					a[i][a[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
			}

			SSCOT sscot = new SSCOT(con1, con2);
			sscot.runE(predata, m, a, timer);
		}

		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			int ySegBytes = y.length / OTi.getTwoTauPow();
			byte[][] y_array = new byte[OTi.getTwoTauPow()][];
			for (int i = 0; i < OTi.getTwoTauPow(); i++)
				y_array[i] = Arrays.copyOfRange(y, i * ySegBytes, (i + 1) * ySegBytes);

			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runE(predata, y_array, Nip1_pr, timer);
		}

		// step 5
		Tuple Ti = null;
		if (OTi.getTreeIndex() == 0)
			Ti = pathTuples[0];
		else
			Ti = new Tuple(new byte[1], Ni, Li, y);

		OutAccess outaccess = new OutAccess(Li, null, null, null, null, Ti, pathTuples);

		timer.stop(pid, M.online_comp);
		return outaccess;
	}

	public byte[] runD(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		pathTuples = Util.permute(pathTuples, predata.access_sigma);

		// step 2
		timer.start(pid, M.online_write);
		con2.write(pid, pathTuples);
		con2.write(pid, Ni);
		timer.stop(pid, M.online_write);

		// step 3
		if (OTi.getTreeIndex() > 0) {
			byte[][] b = new byte[pathTuples.length][];
			for (int i = 0; i < pathTuples.length; i++) {
				b[i] = ArrayUtils.addAll(pathTuples[i].getF(), pathTuples[i].getN());
				b[i][0] ^= 1;
				for (int j = 0; j < Ni.length; j++)
					b[i][b[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
			}

			SSCOT sscot = new SSCOT(con1, con2);
			sscot.runD(predata, b, timer);
		}

		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runD(predata, Nip1_pr, timer);
		}

		timer.stop(pid, M.online_comp);

		return Li;
	}

	public OutAccess runC(Metadata md, int treeIndex, byte[] Li, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: send Li to E and D
		timer.start(pid, M.online_write);
		if (treeIndex > 0) {
			con1.write(pid, Li);
			con2.write(pid, Li);
		}
		timer.stop(pid, M.online_write);

		// step 2
		timer.start(pid, M.online_read);
		Tuple[] pathTuples = con2.readTupleArray(pid);
		byte[] Ni = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 3
		int j1 = 0;
		byte[] z = null;
		if (treeIndex == 0) {
			z = pathTuples[0].getA();
		} else {
			SSCOT sscot = new SSCOT(con1, con2);
			OutSSCOT je = sscot.runC(timer);
			j1 = je.t;
			byte[] d = pathTuples[j1].getA();
			z = Util.xor(je.m_t, d);
		}

		// step 4
		int j2 = 0;
		byte[] Lip1 = null;
		if (treeIndex < md.getNumTrees() - 1) {
			SSIOT ssiot = new SSIOT(con1, con2);
			OutSSIOT jy = ssiot.runC(timer);

			// step 5
			j2 = jy.t;
			int lSegBytes = md.getABytesOfTree(treeIndex) / md.getTwoTauPow();
			byte[] z_j2 = Arrays.copyOfRange(z, j2 * lSegBytes, (j2 + 1) * lSegBytes);
			Lip1 = Util.xor(jy.m_t, z_j2);
		}

		Tuple Ti = null;
		if (treeIndex == 0) {
			Ti = pathTuples[0];
		} else {
			Ti = new Tuple(new byte[] { 1 }, Ni, new byte[md.getLBytesOfTree(treeIndex)], z);

			pathTuples[j1].getF()[0] = (byte) (1 - pathTuples[j1].getF()[0]);
			Crypto.sr.nextBytes(pathTuples[j1].getN());
			Crypto.sr.nextBytes(pathTuples[j1].getL());
			Crypto.sr.nextBytes(pathTuples[j1].getA());
		}

		OutAccess outaccess = new OutAccess(Li, Lip1, Ti, pathTuples, j2, null, null);

		timer.stop(pid, M.online_comp);
		return outaccess;
	}

	public OutAccess runE2(Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);

		// step 5
		Tuple Ti = null;
		if (OTi.getTreeIndex() == 0)
			Ti = pathTuples[0];
		else {
			Ti = new Tuple(1, OTi.getNBytes(), OTi.getLBytes(), OTi.getABytes(), Crypto.sr);
			Ti.setF(new byte[1]);
		}

		OutAccess outaccess = new OutAccess(Li, null, null, null, null, Ti, pathTuples);

		timer.stop(pid, M.online_comp);
		return outaccess;
	}

	public byte[] runD2(Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read(pid);
		timer.stop(pid, M.online_read);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);

		// step 2
		timer.start(pid, M.online_write);
		con2.write(pid, pathTuples);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return Li;
	}

	public OutAccess runC2(Metadata md, int treeIndex, byte[] Li, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: send Li to E and D
		timer.start(pid, M.online_write);
		if (treeIndex > 0) {
			con1.write(pid, Li);
			con2.write(pid, Li);
		}
		timer.stop(pid, M.online_write);

		// step 2
		timer.start(pid, M.online_read);
		Tuple[] pathTuples = con2.readTupleArray(pid);
		timer.stop(pid, M.online_read);

		// step 5
		Tuple Ti = null;
		if (treeIndex == 0) {
			Ti = pathTuples[0];
		} else {
			Ti = new Tuple(1, md.getNBytesOfTree(treeIndex), md.getLBytesOfTree(treeIndex),
					md.getABytesOfTree(treeIndex), Crypto.sr);
			Ti.setF(new byte[1]);
		}

		OutAccess outaccess = new OutAccess(Li, null, Ti, pathTuples, null, null, null);

		timer.stop(pid, M.online_comp);
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

						runE(predata, OTi, sE_Ni, sE_Nip1_pr, timer);

						if (ti == numTrees - 1)
							con2.write(N);

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runD(predata, timer);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						runD(predata, OTi, sD_Ni, sD_Nip1_pr, timer);

					} else if (party == Party.Charlie) {
						preaccess.runC(timer);

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						OutAccess outaccess = runC(md, ti, Li, timer);

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

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
			}
		}

		// timer.print();
	}
}

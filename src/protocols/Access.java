package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.Util;

public class Access extends Protocol {

	public Access(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutAccess runE(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr) {
		// step 0: get Li from C
		byte[] Li = new byte[0];
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);

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
			sscot.runE(predata, m, a);
		}

		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			int ySegBytes = y.length / OTi.getTwoTauPow();
			byte[][] y_array = new byte[OTi.getTwoTauPow()][];
			for (int i = 0; i < OTi.getTwoTauPow(); i++)
				y_array[i] = Arrays.copyOfRange(y, i * ySegBytes, (i + 1) * ySegBytes);

			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runE(predata, y_array, Nip1_pr);
		}

		// step 5
		Tuple Ti = null;
		if (OTi.getTreeIndex() == 0)
			Ti = pathTuples[0];
		else
			Ti = new Tuple(new byte[0], Ni, Li, y);

		OutAccess outaccess = new OutAccess(null, null, null, Ti, pathTuples);
		return outaccess;
	}

	public void runD(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr) {
		// step 0: get Li from C
		byte[] Li = new byte[0];
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);

		// step 2
		con2.write(pathTuples);
		con2.write(Ni);

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
			sscot.runD(predata, b);
		}

		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runD(predata, Nip1_pr);
		}
	}

	public OutAccess runC(Metadata md, int treeIndex, byte[] Li) {
		// step 0: send Li to E and D
		if (treeIndex > 0) {
			con1.write(Li);
			con2.write(Li);
		}

		// step 2
		Object[] objArray = con2.readObjectArray();
		Tuple[] pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);
		byte[] Ni = con2.read();

		// step 3
		int j1 = 0;
		byte[] z = null;
		if (treeIndex == 0) {
			z = pathTuples[0].getA();
		} else {
			SSCOT sscot = new SSCOT(con1, con2);
			OutSSCOT je = sscot.runC();
			j1 = je.t;
			byte[] d = pathTuples[j1].getA();
			z = Util.xor(je.m_t, d);
		}

		// step 4
		int j2 = 0;
		byte[] Lip1 = null;
		if (treeIndex < md.getNumTrees() - 1) {
			SSIOT ssiot = new SSIOT(con1, con2);
			OutSSIOT jy = ssiot.runC();

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

		OutAccess outaccess = new OutAccess(Lip1, Ti, pathTuples, null, null);
		return outaccess;
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		/*
		 * PreData predata = new PreData(); PreAccess preaccess = new
		 * PreAccess(con1, con2); int treeIndex = 1; Tree tree = null; int
		 * numTuples = 0; if (forest != null) { tree =
		 * forest.getTree(treeIndex); numTuples = (tree.getD() - 1) *
		 * tree.getW() + tree.getStashSize(); } byte[] Li = new BigInteger("11",
		 * 2).toByteArray(); byte[] Ni = new byte[] { 0 }; byte[] Nip1_pr = new
		 * byte[] { 0 }; if (party == Party.Eddie) { preaccess.runE(predata,
		 * tree, numTuples); runE(predata, tree, Ni, Nip1_pr);
		 * 
		 * } else if (party == Party.Debbie) { preaccess.runD(predata);
		 * runD(predata, tree, Ni, Nip1_pr);
		 * 
		 * } else if (party == Party.Charlie) { preaccess.runC(); runC(md,
		 * treeIndex, Li);
		 * 
		 * } else { throw new NoSuchPartyException(party + ""); }
		 */

		int records = 10;
		int repeart = 5;

		int tau = md.getTau();
		int numTrees = md.getNumTrees();
		long numInsert = md.getNumInsertRecords();
		int addrBits = md.getAddrBits();

		for (int i = 0; i < records; i++) {
			long N = Util.nextLong(numInsert, Crypto.sr);
			// System.out.println("N=" + BigInteger.valueOf(N).toString(2));
			for (int j = 0; j < repeart; j++) {
				byte[] Li = new byte[0];
				for (int ti = 0; ti < numTrees; ti++) {
					// System.out.println(i + " " + j + " " + ti);

					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);
					// System.out.println("Ni=" +
					// BigInteger.valueOf(Ni_value).toString(2));
					// System.out.println("Nip1_pr=" +
					// BigInteger.valueOf(Nip1_pr_value).toString(2));

					PreData predata = new PreData();
					PreAccess preaccess = new PreAccess(con1, con2);
					Access access = new Access(con1, con2);

					if (party == Party.Eddie) {
						Tree OTi = forest.getTree(ti);
						int numTuples = (OTi.getD() - 1) * OTi.getW() + OTi.getStashSize();
						preaccess.runE(predata, OTi, numTuples);

						byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
						byte[] sD_Ni = Util.xor(Ni, sE_Ni);
						con1.write(sD_Ni);

						byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
						byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
						con1.write(sD_Nip1_pr);

						access.runE(predata, OTi, sE_Ni, sE_Nip1_pr);

						if (ti == numTrees - 1)
							con2.write(N);

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runD(predata);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						access.runD(predata, OTi, sD_Ni, sD_Nip1_pr);

					} else if (party == Party.Charlie) {
						preaccess.runC();

						OutAccess outaccess = access.runC(md, ti, Li);
						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = con1.readObject();
							long data = new BigInteger(1, outaccess.C_Ti.getA()).longValue();
							System.out.println(N);
							System.out.println(data);
						}

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}
			}
		}
	}
}

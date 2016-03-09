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
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.StopWatch;
import util.Util;

public class Access extends Protocol {
	
	private StopWatch step0;
	private StopWatch step1;
	private StopWatch step2;
	private StopWatch step3;
	private StopWatch step4;
	private StopWatch step5;

	public Access(Communication con1, Communication con2) {
		super(con1, con2);
		
		step0 = new StopWatch();
		step1 = new StopWatch();
		step2 = new StopWatch();
		step3 = new StopWatch();
		step4 = new StopWatch();
		step5 = new StopWatch();
	}

	public OutAccess runE(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr) {
		step0.start();
		// step 0: get Li from C
		byte[] Li = new byte[0];
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();
		step0.stop();

		step1.start();
		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);
		step1.stop();

		step3.start();
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
		step3.stop();

		step4.start();
		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			int ySegBytes = y.length / OTi.getTwoTauPow();
			byte[][] y_array = new byte[OTi.getTwoTauPow()][];
			for (int i = 0; i < OTi.getTwoTauPow(); i++)
				y_array[i] = Arrays.copyOfRange(y, i * ySegBytes, (i + 1) * ySegBytes);

			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runE(predata, y_array, Nip1_pr);
		}
		step4.stop();

		step5.start();
		// step 5
		Tuple Ti = null;
		if (OTi.getTreeIndex() == 0)
			Ti = pathTuples[0];
		else
			Ti = new Tuple(new byte[0], Ni, Li, y);
		step5.stop();

		OutAccess outaccess = new OutAccess(null, null, null, Ti, pathTuples);
		return outaccess;
	}

	public void runD(PreData predata, Tree OTi, byte[] Ni, byte[] Nip1_pr) {
		step0.start();
		// step 0: get Li from C
		byte[] Li = new byte[0];
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();
		step0.stop();

		step1.start();
		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);
		step1.stop();

		byte[] test = Util.nextBytes(pathTuples.length*pathTuples[0].getNumBytes(), Crypto.sr);
		
		step2.start();
		// step 2
		//con2.write(pathTuples);
		con2.write(pathTuples.length);
		for (int i=0; i<pathTuples.length; i++)
			con2.write(pathTuples[i].toByteArray());
		con2.write(Ni);
		
		//con2.write(test);
		step2.stop();

		step3.start();
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
		step3.stop();

		step4.start();
		// step 4
		if (OTi.getTreeIndex() < OTi.getH() - 1) {
			SSIOT ssiot = new SSIOT(con1, con2);
			ssiot.runD(predata, Nip1_pr);
		}
		step4.stop();
	}

	public OutAccess runC(Metadata md, int treeIndex, byte[] Li) {
		step0.start();
		// step 0: send Li to E and D
		if (treeIndex > 0) {
			con1.write(Li);
			con2.write(Li);
		}
		step0.stop();

		step2.start();
		// step 2
		//Object[] objArray = con2.readObjectArray();
		//Tuple[] pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);
		//Tuple[] pathTuples = con2.readTupleArray();
		int numTuples = con2.readInt();
		Tuple[] pathTuples = new Tuple[numTuples];
		for (int i=0; i<numTuples; i++) {
			byte[] data = con2.read();
			int f = treeIndex==0?0:1;
			int n = md.getNBytesOfTree(treeIndex);
			int l = md.getLBytesOfTree(treeIndex);
			int a = md.getABytesOfTree(treeIndex);
			pathTuples[i] = new Tuple(new byte[f], new byte[n], new byte[l], new byte[a]);
			pathTuples[i].setF(Arrays.copyOfRange(data, 0, f));
			pathTuples[i].setN(Arrays.copyOfRange(data, f, f+n));
			pathTuples[i].setL(Arrays.copyOfRange(data, f+n, f+n+l));
			pathTuples[i].setA(Arrays.copyOfRange(data, f+n+l, data.length));
		}
		byte[] Ni = con2.read();
		
		//byte[] test = con2.read();
		step2.stop();

		step3.start();
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
		step3.stop();
		
		step4.start();
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
		step4.stop();

		step5.start();
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
		step5.stop();

		OutAccess outaccess = new OutAccess(Lip1, Ti, pathTuples, null, null);
		return outaccess;
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		int records = 5;
		int repeat = 5;

		int tau = md.getTau();
		int numTrees = md.getNumTrees();
		long numInsert = md.getNumInsertRecords();
		int addrBits = md.getAddrBits();

		StopWatch stopwatch = new StopWatch();

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

						stopwatch.start();
						runE(predata, OTi, sE_Ni, sE_Nip1_pr);
						stopwatch.stop();

						if (ti == numTrees - 1)
							con2.write(BigInteger.valueOf(N).toByteArray());

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);
						preaccess.runD(predata);

						byte[] sD_Ni = con1.read();

						byte[] sD_Nip1_pr = con1.read();

						stopwatch.start();
						runD(predata, OTi, sD_Ni, sD_Nip1_pr);
						stopwatch.stop();

					} else if (party == Party.Charlie) {
						preaccess.runC();

						System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));

						stopwatch.start();
						OutAccess outaccess = runC(md, ti, Li);
						stopwatch.stop();

						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = new BigInteger(con1.read()).longValue();
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

		System.out.println(stopwatch.toMS());
		
		System.out.println("step0\n" + step0.toMS());
		System.out.println("step1\n" + step1.toMS());
		System.out.println("step2\n" + step2.toMS());
		System.out.println("step3\n" + step3.toMS());
		System.out.println("step4\n" + step4.toMS());
		System.out.println("step5\n" + step5.toMS());
	}
}

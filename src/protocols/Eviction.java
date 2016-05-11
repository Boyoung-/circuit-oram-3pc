package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import measure.Timer;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.Util;

public class Eviction extends Protocol {
	public Eviction(Communication con1, Communication con2) {
		super(con1, con2);
	}

	private int[] prepareEviction(int target[], int[] ti, int W) {
		int d = ti.length;
		int[] evict = new int[W * d];
		for (int r = 0; r < d; r++) {
			int tupleIndex = r * W + ti[r];
			for (int c = 0; c < W; c++) {
				int currIndex = r * W + c;
				if (currIndex == tupleIndex) {
					int targetIndex = target[r] * W + ti[target[r]];
					evict[targetIndex] = currIndex;
				} else
					evict[currIndex] = currIndex;
			}
		}
		return evict;
	}

	public void runE(PreData predata, boolean firstTree, byte[] Li, Tuple[] originalPath, int d, int w, Tree OTi,
			Timer timer) {
		if (firstTree) {
			OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), new Bucket[] { new Bucket(originalPath) });
			return;
		}

		int sw = OTi.getStashSize();
		Tuple[] pathTuples = new Tuple[d * w];
		System.arraycopy(originalPath, 0, pathTuples, 0, w);
		System.arraycopy(originalPath, sw, pathTuples, w, (d - 1) * w);

		Bucket[] pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, w, w);

		GCSignal[] LiInputKeys = GCUtil.revSelectKeys(predata.evict_LiKeyPairs, Li);
		GCSignal[][] E_feInputKeys = new GCSignal[d][];
		GCSignal[][][] E_labelInputKeys = new GCSignal[d][][];
		GCSignal[][] deltaInputKeys = new GCSignal[d][];
		for (int i = 0; i < d; i++) {
			E_feInputKeys[i] = GCUtil.selectFeKeys(predata.evict_E_feKeyPairs[i], pathBuckets[i].getTuples());
			E_labelInputKeys[i] = GCUtil.selectLabelKeys(predata.evict_E_labelKeyPairs[i], pathBuckets[i].getTuples());
			deltaInputKeys[i] = GCUtil.revSelectKeys(predata.evict_deltaKeyPairs[i],
					predata.evict_delta[i].toByteArray());
		}

		con1.write(LiInputKeys);
		con1.write(E_feInputKeys);
		con1.write(E_labelInputKeys);
		con1.write(deltaInputKeys);

		PermuteTarget permutetarget = new PermuteTarget(con1, con2);
		permutetarget.runE();

		PermuteIndex permuteindex = new PermuteIndex(con1, con2);
		permuteindex.runE();

		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));
		int W = (int) Math.pow(2, logW);
		for (int i = 0; i < d; i++) {
			pathBuckets[i].expand(W);
			pathBuckets[i].permute(predata.evict_delta_p[i]);
		}
		pathBuckets = Util.permute(pathBuckets, predata.evict_pi);
		for (int i = 0; i < d; i++) {
			pathBuckets[i].permute(predata.evict_rho_p[i]);
		}
		pathTuples = Bucket.bucketsToTuples(pathBuckets);

		SSXOT ssxot = new SSXOT(con1, con2, 1);
		pathTuples = ssxot.runE(predata, pathTuples, timer);

		pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, W, W);
		for (int i = 0; i < d; i++) {
			int[] rho_ivs = Util.inversePermutation(predata.evict_rho_p[i]);
			pathBuckets[i].permute(rho_ivs);
		}
		int[] pi_ivs = Util.inversePermutation(predata.evict_pi);
		pathBuckets = Util.permute(pathBuckets, pi_ivs);
		for (int i = 0; i < d; i++) {
			int[] delta_ivs = Util.inversePermutation(predata.evict_delta_p[i]);
			pathBuckets[i].permute(delta_ivs);
			pathBuckets[i].shrink(w);
		}

		pathBuckets[0].expand(Arrays.copyOfRange(originalPath, w, sw));
		OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), pathBuckets);
	}

	public void runD(PreData predata, boolean firstTree, byte[] Li, int w, Tree OTi, Timer timer) {
		if (firstTree) {
			Tuple[] originalPath = con2.readObject();
			OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), new Bucket[] { new Bucket(originalPath) });
			return;
		}

		GCSignal[] LiInputKeys = con1.readObject();
		GCSignal[][] E_feInputKeys = con1.readObject();
		GCSignal[][][] E_labelInputKeys = con1.readObject();
		GCSignal[][] deltaInputKeys = con1.readObject();

		GCSignal[][] C_feInputKeys = con2.readObject();
		GCSignal[][][] C_labelInputKeys = con2.readObject();

		GCSignal[][][] outKeys = predata.evict_gcroute.routing(LiInputKeys, E_feInputKeys, C_feInputKeys,
				E_labelInputKeys, C_labelInputKeys, deltaInputKeys);

		int[] ti_p = new int[deltaInputKeys.length];
		for (int i = 0; i < ti_p.length; i++) {
			ti_p[i] = GCUtil.evaOutKeys(outKeys[1][i], predata.evict_tiOutKeyHashes[i]).intValue();
		}

		/*
		 * System.out.println("ti:"); for (int i = 0; i < ti_p.length; i++)
		 * System.out.print(ti_p[i] + " "); System.out.println();
		 */

		PermuteTarget permutetarget = new PermuteTarget(con1, con2);
		int[] target_pp = permutetarget.runD(predata, firstTree, outKeys[0], timer);

		PermuteIndex permuteindex = new PermuteIndex(con1, con2);
		int[] ti_pp = permuteindex.runD(predata, firstTree, ti_p, timer);

		/*
		 * System.out.println("ti_pp:"); for (int i = 0; i < ti_p.length; i++)
		 * System.out.print(ti_pp[i] + " "); System.out.println();
		 * 
		 * System.out.println("target_pp:"); for (int i = 0; i < ti_p.length;
		 * i++) System.out.print(target_pp[i] + " "); System.out.println();
		 */

		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));
		int W = (int) Math.pow(2, logW);
		int[] evict = prepareEviction(target_pp, ti_pp, W);
		/*
		 * for (int i = 0; i < evict.length; i++) System.out.print(evict[i] +
		 * " "); System.out.println();
		 */

		SSXOT ssxot = new SSXOT(con1, con2, 1);
		ssxot.runD(predata, evict, timer);

		Bucket[] pathBuckets = con2.readObject();
		OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), pathBuckets);
	}

	public void runC(PreData predata, boolean firstTree, Tuple[] originalPath, int d, int w, int sw, Timer timer) {
		if (firstTree) {
			con2.write(originalPath);
			return;
		}

		Tuple[] pathTuples = new Tuple[d * w];
		System.arraycopy(originalPath, 0, pathTuples, 0, w);
		System.arraycopy(originalPath, sw, pathTuples, w, (d - 1) * w);

		Bucket[] pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, w, w);

		GCSignal[][] C_feInputKeys = new GCSignal[d][];
		GCSignal[][][] C_labelInputKeys = new GCSignal[d][][];
		for (int i = 0; i < d; i++) {
			C_feInputKeys[i] = GCUtil.selectFeKeys(predata.evict_C_feKeyPairs[i], pathBuckets[i].getTuples());
			C_labelInputKeys[i] = GCUtil.selectLabelKeys(predata.evict_C_labelKeyPairs[i], pathBuckets[i].getTuples());
		}

		con2.write(C_feInputKeys);
		con2.write(C_labelInputKeys);

		PermuteTarget permutetarget = new PermuteTarget(con1, con2);
		permutetarget.runC(predata, firstTree, timer);

		PermuteIndex permuteindex = new PermuteIndex(con1, con2);
		permuteindex.runC(predata, firstTree, timer);

		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));
		int W = (int) Math.pow(2, logW);
		for (int i = 0; i < d; i++) {
			pathBuckets[i].expand(W);
			pathBuckets[i].permute(predata.evict_delta_p[i]);
		}
		pathBuckets = Util.permute(pathBuckets, predata.evict_pi);
		for (int i = 0; i < d; i++) {
			pathBuckets[i].permute(predata.evict_rho_p[i]);
		}
		pathTuples = Bucket.bucketsToTuples(pathBuckets);

		SSXOT ssxot = new SSXOT(con1, con2, 1);
		pathTuples = ssxot.runC(predata, pathTuples, timer);

		pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, W, W);
		for (int i = 0; i < d; i++) {
			int[] rho_ivs = Util.inversePermutation(predata.evict_rho_p[i]);
			pathBuckets[i].permute(rho_ivs);
		}
		int[] pi_ivs = Util.inversePermutation(predata.evict_pi);
		pathBuckets = Util.permute(pathBuckets, pi_ivs);
		for (int i = 0; i < d; i++) {
			int[] delta_ivs = Util.inversePermutation(predata.evict_delta_p[i]);
			pathBuckets[i].permute(delta_ivs);
			pathBuckets[i].shrink(w);
		}

		pathBuckets[0].expand(Arrays.copyOfRange(originalPath, w, sw));
		con2.write(pathBuckets);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 2; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PreEviction preeviction = new PreEviction(con1, con2);

			if (party == Party.Eddie) {
				int w = Crypto.sr.nextInt(1) + 4;
				int d = Crypto.sr.nextInt(1) + 5;
				int lBits = d - 1;
				int lBytes = (lBits + 7) / 8;
				byte[] Li = Util.nextBytes(lBytes, Crypto.sr);
				Tuple[] path = new Tuple[d * w];
				for (int j = 0; j < d * w; j++)
					path[j] = new Tuple(1, 2, lBytes, 3, Crypto.sr);

				System.out.println("d, w: " + d + " " + w);

				con1.write(d);
				con1.write(w);
				con1.write(Li);
				con2.write(d);
				con2.write(w);

				preeviction.runE(predata, false, d, w, timer);
				// runE(predata, false, Li, path, d, w, null, timer);

				int emptyIndex = 0;
				for (int j = 0; j < d * w; j++) {
					if (new BigInteger(path[j].getF()).testBit(0)) {
						String l = Util.addZeros(
								Util.getSubBits(new BigInteger(1, Util.xor(path[j].getL(), Li)), lBits, 0).toString(2),
								lBits);
						System.out.println(j + ":\t" + l);
					} else {
						emptyIndex = j;
					}
				}
				System.out.println("last empty: " + emptyIndex);

				System.out.println("pi:");
				for (int j = 0; j < d; j++)
					System.out.print(predata.evict_pi[j] + " ");
				System.out.println();

				System.out.println("delta:");
				for (int j = 0; j < d; j++)
					System.out.print(predata.evict_delta[j].intValue() + " ");
				System.out.println();

				System.out.println("rho:");
				for (int j = 0; j < d; j++)
					System.out.print(predata.evict_rho[j].intValue() + " ");
				System.out.println();

			} else if (party == Party.Debbie) {
				int d = con1.readObject();
				int w = con1.readObject();
				byte[] Li = con1.read();
				int[] tupleParam = new int[] { 1, 2, (d - 1 + 7) / 8, 3 };

				preeviction.runD(predata, false, d, w, tupleParam, timer);
				// runD(predata, false, Li, w, null, timer);

			} else if (party == Party.Charlie) {
				int d = con1.readObject();
				int w = con1.readObject();
				int lBytes = (d - 1 + 7) / 8;
				Tuple[] path = new Tuple[d * w];
				for (int j = 0; j < d * w; j++)
					path[j] = new Tuple(1, 2, lBytes, 3, null);

				preeviction.runC(predata, false, timer);
				// runC(predata, false, path, d, w, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

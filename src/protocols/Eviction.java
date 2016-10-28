package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import gc.GCUtil;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class Eviction extends Protocol {

	private int pid = P.EVI;

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

	public void runE(PreData predata, boolean firstTree, byte[] Li, Tuple[] originalPath, Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		if (firstTree) {
			OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), new Bucket[] { new Bucket(originalPath) });
			timer.stop(pid, M.online_comp);
			return;
		}

		int d = OTi.getD();
		int sw = OTi.getStashSize();
		int w = OTi.getW();
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
			deltaInputKeys[i] = GCUtil.revSelectKeys(predata.evict_deltaKeyPairs[i], predata.evict_delta[i]);
		}

		timer.start(pid, M.online_write);
		con1.write(pid, LiInputKeys);
		con1.write(pid, E_feInputKeys);
		con1.write(pid, E_labelInputKeys);
		con1.write(pid, deltaInputKeys);
		timer.stop(pid, M.online_write);

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

		timer.stop(pid, M.online_comp);
	}

	public void runD(PreData predata, boolean firstTree, byte[] Li, Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		if (firstTree) {
			timer.start(pid, M.online_read);
			Tuple[] originalPath = con2.readTupleArray(pid);
			timer.stop(pid, M.online_read);

			OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), new Bucket[] { new Bucket(originalPath) });

			timer.stop(pid, M.online_comp);
			return;
		}

		timer.start(pid, M.online_read);
		GCSignal[] LiInputKeys = con1.readGCSignalArray(pid);
		GCSignal[][] E_feInputKeys = con1.readDoubleGCSignalArray(pid);
		GCSignal[][][] E_labelInputKeys = con1.readTripleGCSignalArray(pid);
		GCSignal[][] deltaInputKeys = con1.readDoubleGCSignalArray(pid);

		GCSignal[][] C_feInputKeys = con2.readDoubleGCSignalArray(pid);
		GCSignal[][][] C_labelInputKeys = con2.readTripleGCSignalArray(pid);
		timer.stop(pid, M.online_read);

		int w = OTi.getW();
		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));

		GCSignal[][][] outKeys = predata.evict_gcroute.routing(LiInputKeys, E_feInputKeys, C_feInputKeys,
				E_labelInputKeys, C_labelInputKeys, deltaInputKeys);

		byte[][] ti_p = new byte[deltaInputKeys.length][];
		for (int i = 0; i < ti_p.length; i++) {
			ti_p[i] = Util.padArray(GCUtil.evaOutKeys(outKeys[1][i], predata.evict_tiOutKeyHashes[i]).toByteArray(),
					(logW + 7) / 8);
		}

		PermuteTarget permutetarget = new PermuteTarget(con1, con2);
		int[] target_pp = permutetarget.runD(predata, firstTree, outKeys[0], timer);

		PermuteIndex permuteindex = new PermuteIndex(con1, con2);
		int[] ti_pp = permuteindex.runD(predata, firstTree, ti_p, w, timer);

		int W = (int) Math.pow(2, logW);
		int[] evict = prepareEviction(target_pp, ti_pp, W);

		SSXOT ssxot = new SSXOT(con1, con2, 1);
		ssxot.runD(predata, evict, timer);

		timer.start(pid, M.online_read);
		Bucket[] pathBuckets = con2.readBucketArray(pid);
		timer.stop(pid, M.online_read);

		OTi.setBucketsOnPath(new BigInteger(1, Li).longValue(), pathBuckets);

		timer.stop(pid, M.online_comp);
	}

	public void runC(PreData predata, boolean firstTree, Tuple[] originalPath, int d, int sw, int w, Timer timer) {
		if (firstTree) {
			timer.start(pid, M.online_write);
			con2.write(pid, originalPath);
			timer.stop(pid, M.online_write);
			return;
		}

		timer.start(pid, M.online_comp);

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

		timer.start(pid, M.online_write);
		con2.write(pid, C_feInputKeys);
		con2.write(pid, C_labelInputKeys);
		timer.stop(pid, M.online_write);

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

		timer.start(pid, M.online_write);
		con2.write(pid, pathBuckets);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		System.out.println("Use Retrieve to test Eviction");
	}
}

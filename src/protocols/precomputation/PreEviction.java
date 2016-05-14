package protocols.precomputation;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import crypto.Crypto;
import gc.GCRoute;
import gc.GCUtil;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreEviction extends Protocol {

	private int pid = P.EVI;

	public PreEviction(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, boolean firstTree, int d, int w, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.offline_comp);

		// GC
		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));

		predata.evict_LiKeyPairs = GCUtil.genKeyPairs(d - 1);
		GCSignal[] LiZeroKeys = GCUtil.getZeroKeys(predata.evict_LiKeyPairs);

		predata.evict_E_feKeyPairs = new GCSignal[d][][];
		predata.evict_C_feKeyPairs = new GCSignal[d][][];
		GCSignal[][] E_feZeroKeys = new GCSignal[d][];
		GCSignal[][] C_feZeroKeys = new GCSignal[d][];

		predata.evict_E_labelKeyPairs = new GCSignal[d][w][][];
		predata.evict_C_labelKeyPairs = new GCSignal[d][w][][];
		GCSignal[][][] E_labelZeroKeys = new GCSignal[d][w][];
		GCSignal[][][] C_labelZeroKeys = new GCSignal[d][w][];

		predata.evict_deltaKeyPairs = new GCSignal[d][][];
		GCSignal[][] deltaZeroKeys = new GCSignal[d][];

		for (int i = 0; i < d; i++) {
			predata.evict_E_feKeyPairs[i] = GCUtil.genKeyPairs(w);
			predata.evict_C_feKeyPairs[i] = GCUtil.genKeyPairs(w);
			E_feZeroKeys[i] = GCUtil.getZeroKeys(predata.evict_E_feKeyPairs[i]);
			C_feZeroKeys[i] = GCUtil.getZeroKeys(predata.evict_C_feKeyPairs[i]);

			predata.evict_deltaKeyPairs[i] = GCUtil.genKeyPairs(logW);
			deltaZeroKeys[i] = GCUtil.getZeroKeys(predata.evict_deltaKeyPairs[i]);

			for (int j = 0; j < w; j++) {
				predata.evict_E_labelKeyPairs[i][j] = GCUtil.genKeyPairs(d - 1);
				predata.evict_C_labelKeyPairs[i][j] = GCUtil.genKeyPairs(d - 1);
				E_labelZeroKeys[i][j] = GCUtil.getZeroKeys(predata.evict_E_labelKeyPairs[i][j]);
				C_labelZeroKeys[i][j] = GCUtil.getZeroKeys(predata.evict_C_labelKeyPairs[i][j]);
			}
		}

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel, timer, pid, M.offline_write);
		GCSignal[][][] outZeroKeys = new GCRoute<GCSignal>(gen, d, w).routing(LiZeroKeys, E_feZeroKeys, C_feZeroKeys,
				E_labelZeroKeys, C_labelZeroKeys, deltaZeroKeys);

		predata.evict_tiOutKeyHashes = new byte[d][][];
		predata.evict_targetOutKeyPairs = new GCSignal[d][][];
		for (int i = 0; i < d; i++) {
			predata.evict_tiOutKeyHashes[i] = GCUtil.genOutKeyHashes(outZeroKeys[1][i]);
			predata.evict_targetOutKeyPairs[i] = GCUtil.recoverOutKeyPairs(outZeroKeys[0][i]);
		}

		timer.start(pid, M.offline_write);
		con2.write(predata.evict_C_feKeyPairs);
		con2.write(predata.evict_C_labelKeyPairs);

		con1.write(predata.evict_tiOutKeyHashes);
		con1.write(predata.evict_targetOutKeyPairs);
		timer.stop(pid, M.offline_write);

		// Permutation
		predata.evict_pi = Util.randomPermutation(d, Crypto.sr);
		predata.evict_delta = new byte[d][];
		predata.evict_rho = new byte[d][];
		predata.evict_delta_p = new int[d][];
		predata.evict_rho_p = new int[d][];

		for (int i = 0; i < d; i++) {
			predata.evict_delta[i] = Util.nextBytes((logW + 7) / 8, Crypto.sr);
			predata.evict_rho[i] = Util.nextBytes((logW + 7) / 8, Crypto.sr);
			predata.evict_delta_p[i] = Util.getXorPermutation(predata.evict_delta[i], logW);
			predata.evict_rho_p[i] = Util.getXorPermutation(predata.evict_rho[i], logW);
		}

		timer.start(pid, M.offline_write);
		con2.write(predata.evict_pi);
		con2.write(predata.evict_delta);
		con2.write(predata.evict_rho);
		con2.write(predata.evict_delta_p);
		con2.write(predata.evict_rho_p);
		timer.stop(pid, M.offline_write);

		// PermuteTarget
		PrePermuteTarget prepermutetarget = new PrePermuteTarget(con1, con2);
		prepermutetarget.runE(predata, d, timer);

		// PermuteIndex
		PrePermuteIndex prepermuteindex = new PrePermuteIndex(con1, con2);
		prepermuteindex.runE(predata, d, w, timer);

		// SSXOT
		PreSSXOT pressxot = new PreSSXOT(con1, con2, 1);
		pressxot.runE(predata, timer);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, boolean firstTree, int d, int w, int[] tupleParam, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.offline_comp);

		// GC
		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));

		GCSignal[] LiZeroKeys = GCUtil.genEmptyKeys(d - 1);
		GCSignal[][] E_feZeroKeys = new GCSignal[d][];
		GCSignal[][] C_feZeroKeys = new GCSignal[d][];
		GCSignal[][][] E_labelZeroKeys = new GCSignal[d][w][];
		GCSignal[][][] C_labelZeroKeys = new GCSignal[d][w][];
		GCSignal[][] deltaZeroKeys = new GCSignal[d][];

		for (int i = 0; i < d; i++) {
			E_feZeroKeys[i] = GCUtil.genEmptyKeys(w);
			C_feZeroKeys[i] = GCUtil.genEmptyKeys(w);
			deltaZeroKeys[i] = GCUtil.genEmptyKeys(logW);

			for (int j = 0; j < w; j++) {
				E_labelZeroKeys[i][j] = GCUtil.genEmptyKeys(d - 1);
				C_labelZeroKeys[i][j] = GCUtil.genEmptyKeys(d - 1);
			}
		}

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> eva = new GCEva(channel, timer, pid, M.offline_read);
		predata.evict_gcroute = new GCRoute<GCSignal>(eva, d, w);
		predata.evict_gcroute.routing(LiZeroKeys, E_feZeroKeys, C_feZeroKeys, E_labelZeroKeys, C_labelZeroKeys,
				deltaZeroKeys);
		eva.setEvaluate();

		timer.start(pid, M.offline_read);
		predata.evict_tiOutKeyHashes = con1.readTripleByteArray();
		predata.evict_targetOutKeyPairs = con1.readTripleGCSignalArray();
		timer.stop(pid, M.offline_read);

		// PermuteTarget
		PrePermuteTarget prepermutetarget = new PrePermuteTarget(con1, con2);
		prepermutetarget.runD(predata, d, timer);

		// PermuteIndex
		PrePermuteIndex prepermuteindex = new PrePermuteIndex(con1, con2);
		prepermuteindex.runD(predata, timer);

		// SSXOT
		int W = (int) Math.pow(2, logW);
		PreSSXOT pressxot = new PreSSXOT(con1, con2, 1);
		pressxot.runD(predata, d * W, d * W, tupleParam, timer);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.offline_comp);

		// GC
		timer.start(pid, M.offline_read);
		predata.evict_C_feKeyPairs = con1.readTripleGCSignalArray();
		predata.evict_C_labelKeyPairs = con1.readQuadGCSignalArray();

		// Permutation
		predata.evict_pi = con1.readIntArray();
		predata.evict_delta = con1.readDoubleByteArray();
		predata.evict_rho = con1.readDoubleByteArray();
		predata.evict_delta_p = con1.readDoubleIntArray();
		predata.evict_rho_p = con1.readDoubleIntArray();
		timer.stop(pid, M.offline_read);

		// PermuteTarget
		PrePermuteTarget prepermutetarget = new PrePermuteTarget(con1, con2);
		prepermutetarget.runC(predata, timer);

		// PermuteIndex
		PrePermuteIndex prepermuteindex = new PrePermuteIndex(con1, con2);
		prepermuteindex.runC(predata, timer);

		// SSXOT
		PreSSXOT pressxot = new PreSSXOT(con1, con2, 1);
		pressxot.runC(predata, timer);

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

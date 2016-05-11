package protocols.precomputation;

import java.math.BigInteger;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import gc.GCUpdateRoot;
import gc.GCUtil;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;

public class PreUpdateRoot extends Protocol {
	public PreUpdateRoot(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int sw, int lBits, Timer timer) {
		timer.start(P.UR, M.offline_comp);

		int sLogW = (int) Math.ceil(Math.log(sw) / Math.log(2));
		predata.ur_j1KeyPairs = GCUtil.genKeyPairs(sLogW);
		predata.ur_LiKeyPairs = GCUtil.genKeyPairs(lBits);
		predata.ur_E_feKeyPairs = GCUtil.genKeyPairs(sw);
		predata.ur_C_feKeyPairs = GCUtil.genKeyPairs(sw);
		GCSignal[] j1ZeroKeys = GCUtil.getZeroKeys(predata.ur_j1KeyPairs);
		GCSignal[] LiZeroKeys = GCUtil.getZeroKeys(predata.ur_LiKeyPairs);
		GCSignal[] E_feZeroKeys = GCUtil.getZeroKeys(predata.ur_E_feKeyPairs);
		GCSignal[] C_feZeroKeys = GCUtil.getZeroKeys(predata.ur_C_feKeyPairs);
		predata.ur_E_labelKeyPairs = new GCSignal[sw][][];
		predata.ur_C_labelKeyPairs = new GCSignal[sw][][];
		GCSignal[][] E_labelZeroKeys = new GCSignal[sw][];
		GCSignal[][] C_labelZeroKeys = new GCSignal[sw][];
		for (int i = 0; i < sw; i++) {
			predata.ur_E_labelKeyPairs[i] = GCUtil.genKeyPairs(lBits);
			predata.ur_C_labelKeyPairs[i] = GCUtil.genKeyPairs(lBits);
			E_labelZeroKeys[i] = GCUtil.getZeroKeys(predata.ur_E_labelKeyPairs[i]);
			C_labelZeroKeys[i] = GCUtil.getZeroKeys(predata.ur_C_labelKeyPairs[i]);
		}

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel);
		GCSignal[][] outZeroKeys = new GCUpdateRoot<GCSignal>(gen, lBits + 1, sw).rootFindDeepestAndEmpty(j1ZeroKeys,
				LiZeroKeys, E_feZeroKeys, C_feZeroKeys, E_labelZeroKeys, C_labelZeroKeys);

		predata.ur_outKeyHashes = new BigInteger[outZeroKeys.length][];
		for (int i = 0; i < outZeroKeys.length; i++)
			predata.ur_outKeyHashes[i] = GCUtil.genOutKeyHashes(outZeroKeys[i]);

		timer.start(P.UR, M.offline_write);
		con2.write(predata.ur_C_feKeyPairs);
		con2.write(predata.ur_C_labelKeyPairs);
		con1.write(predata.ur_outKeyHashes);
		timer.stop(P.UR, M.offline_write);

		PreSSXOT pressxot = new PreSSXOT(con1, con2, 0);
		pressxot.runE(predata, timer);

		timer.stop(P.UR, M.offline_comp);
	}

	public void runD(PreData predata, int sw, int lBits, int[] tupleParam, Timer timer) {
		timer.start(P.UR, M.offline_comp);

		int logSW = (int) Math.ceil(Math.log(sw) / Math.log(2));
		GCSignal[] j1ZeroKeys = GCUtil.genEmptyKeys(logSW);
		GCSignal[] LiZeroKeys = GCUtil.genEmptyKeys(lBits);
		GCSignal[] E_feZeroKeys = GCUtil.genEmptyKeys(sw);
		GCSignal[] C_feZeroKeys = GCUtil.genEmptyKeys(sw);
		GCSignal[][] E_labelZeroKeys = new GCSignal[sw][];
		GCSignal[][] C_labelZeroKeys = new GCSignal[sw][];
		for (int i = 0; i < sw; i++) {
			E_labelZeroKeys[i] = GCUtil.genEmptyKeys(lBits);
			C_labelZeroKeys[i] = GCUtil.genEmptyKeys(lBits);
		}

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> eva = new GCEva(channel);
		predata.ur_gcur = new GCUpdateRoot<GCSignal>(eva, lBits + 1, sw);
		predata.ur_gcur.rootFindDeepestAndEmpty(j1ZeroKeys, LiZeroKeys, E_feZeroKeys, C_feZeroKeys, E_labelZeroKeys,
				C_labelZeroKeys);
		eva.setEvaluate();

		timer.start(P.UR, M.offline_read);
		predata.ur_outKeyHashes = con1.readObject();
		timer.stop(P.UR, M.offline_read);

		PreSSXOT pressxot = new PreSSXOT(con1, con2, 0);
		pressxot.runD(predata, sw + 1, sw, tupleParam, timer);

		timer.stop(P.UR, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(P.UR, M.offline_comp);

		timer.start(P.UR, M.offline_read);
		predata.ur_C_feKeyPairs = con1.readObject();
		predata.ur_C_labelKeyPairs = con1.readObject();
		timer.stop(P.UR, M.offline_read);

		PreSSXOT pressxot = new PreSSXOT(con1, con2, 0);
		pressxot.runC(predata, timer);

		timer.stop(P.UR, M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

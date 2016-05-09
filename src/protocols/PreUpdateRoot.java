package protocols;

import java.math.BigInteger;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import gc.GCLib;
import gc.GCUtil;
import measure.Timer;
import oram.Forest;
import oram.Metadata;

public class PreUpdateRoot extends Protocol {
	public PreUpdateRoot(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int sw, int lBits, byte[] Li, Timer timer) {
		int sLogW = (int) Math.ceil(Math.log(sw) / Math.log(2));
		predata.ur_j1KeyPairs = GCUtil.genKeyPairs(sLogW);
		predata.ur_E_feKeyPairs = GCUtil.genKeyPairs(sw);
		predata.ur_C_feKeyPairs = GCUtil.genKeyPairs(sw);
		GCSignal[] j1ZeroKeys = GCUtil.getZeroKeys(predata.ur_j1KeyPairs);
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
		GCSignal[][] outZeroKeys = new GCLib<GCSignal>(gen, lBits + 1, sw).rootFindDeepestAndEmpty(Li, j1ZeroKeys,
				E_feZeroKeys, C_feZeroKeys, E_labelZeroKeys, C_labelZeroKeys);

		predata.ur_outKeyHashes = new BigInteger[outZeroKeys.length][];
		for (int i = 0; i < outZeroKeys.length; i++)
			predata.ur_outKeyHashes[i] = GCUtil.genOutKeyHashes(outZeroKeys[i]);

		con2.write(predata.ur_C_feKeyPairs);
		con2.write(predata.ur_C_labelKeyPairs);
		con1.write(predata.ur_outKeyHashes);
	}

	public void runD(PreData predata, int sw, int lBits, byte[] Li, Timer timer) {
		int sLogW = (int) Math.ceil(Math.log(sw) / Math.log(2));
		GCSignal[] j1ZeroKeys = GCUtil.genEmptyKeys(sLogW);
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
		predata.ur_gc = new GCLib<GCSignal>(eva, lBits + 1, sw);
		predata.ur_gc.rootFindDeepestAndEmpty(Li, j1ZeroKeys, E_feZeroKeys, C_feZeroKeys, E_labelZeroKeys,
				C_labelZeroKeys);
		eva.setEvaluate();

		predata.ur_outKeyHashes = con1.readObject();
	}

	public void runC(PreData predata, Timer timer) {
		predata.ur_C_feKeyPairs = con1.readObject();
		predata.ur_C_labelKeyPairs = con1.readObject();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

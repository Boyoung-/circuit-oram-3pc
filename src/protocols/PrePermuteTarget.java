package protocols;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import gc.GCUtil;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class PrePermuteTarget extends Protocol {
	public PrePermuteTarget(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int d, Timer timer) {
		int logD = (int) Math.ceil(Math.log(d) / Math.log(2));

		predata.pt_keyT = new BigInteger[d][d];
		predata.pt_targetT = new BigInteger[d][d];
		predata.pt_maskT = new BigInteger[d][d];

		for (int i = 0; i < d; i++) {
			for (int j = 0; j < d; j++) {
				GCSignal[] keys = GCUtil.revSelectKeys(predata.evict_targetOutKeyPairs[i],
						BigInteger.valueOf(j).toByteArray());
				predata.pt_keyT[i][j] = new BigInteger(GCUtil.hashAll(keys));

				predata.pt_maskT[i][j] = new BigInteger(logD, Crypto.sr);

				predata.pt_targetT[i][j] = BigInteger.valueOf(predata.evict_pi[j]).xor(predata.pt_maskT[i][j]);
			}

			int[] randPerm = Util.randomPermutation(d, Crypto.sr);
			predata.pt_keyT[i] = Util.permute(predata.pt_keyT[i], randPerm);
			predata.pt_maskT[i] = Util.permute(predata.pt_maskT[i], randPerm);
			predata.pt_targetT[i] = Util.permute(predata.pt_targetT[i], randPerm);
		}

		con1.write(predata.pt_keyT);
		con1.write(predata.pt_targetT);

		con2.write(predata.pt_maskT);

		predata.pt_p = new BigInteger[d];
		predata.pt_r = new BigInteger[d];
		predata.pt_a = new BigInteger[d];
		for (int i = 0; i < d; i++) {
			predata.pt_p[i] = new BigInteger(logD, Crypto.sr);
			predata.pt_r[i] = new BigInteger(logD, Crypto.sr);
			predata.pt_a[i] = predata.pt_p[i].xor(predata.pt_r[i]);
		}
		predata.pt_a = Util.permute(predata.pt_a, predata.evict_pi);

		con1.write(predata.pt_p);
		con1.write(predata.pt_a);

		con2.write(predata.pt_r);
	}

	public void runD(PreData predata, int d, Timer timer) {
		// PermuteTargetI
		predata.pt_keyT = con1.readObject();
		predata.pt_targetT = con1.readObject();

		// PermuteTargetII
		predata.pt_p = con1.readObject();
		predata.pt_a = con1.readObject();
	}

	public void runC(PreData predata, Timer timer) {
		// PermuteTargetI
		predata.pt_maskT = con1.readObject();

		// PermuteTargetII
		predata.pt_r = con1.readObject();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

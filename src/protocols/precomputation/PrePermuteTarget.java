package protocols.precomputation;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
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

public class PrePermuteTarget extends Protocol {

	private int pid = P.PT;

	public PrePermuteTarget(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int d, Timer timer) {
		timer.start(pid, M.offline_comp);

		// PermuteTargetI
		int logD = (int) Math.ceil(Math.log(d) / Math.log(2));

		predata.pt_keyT = new byte[d][d][];
		predata.pt_targetT = new byte[d][d][];
		predata.pt_maskT = new byte[d][d][];

		for (int i = 0; i < d; i++) {
			for (int j = 0; j < d; j++) {
				GCSignal[] keys = GCUtil.revSelectKeys(predata.evict_targetOutKeyPairs[i],
						BigInteger.valueOf(j).toByteArray());
				predata.pt_keyT[i][j] = GCUtil.hashAll(keys);

				predata.pt_maskT[i][j] = Util.nextBytes((logD + 7) / 8, Crypto.sr);

				predata.pt_targetT[i][j] = Util.xor(
						Util.padArray(BigInteger.valueOf(predata.evict_pi[j]).toByteArray(), (logD + 7) / 8),
						predata.pt_maskT[i][j]);
			}

			int[] randPerm = Util.randomPermutation(d, Crypto.sr);
			predata.pt_keyT[i] = Util.permute(predata.pt_keyT[i], randPerm);
			predata.pt_maskT[i] = Util.permute(predata.pt_maskT[i], randPerm);
			predata.pt_targetT[i] = Util.permute(predata.pt_targetT[i], randPerm);
		}

		timer.start(pid, M.offline_write);
		con1.write(predata.pt_keyT);
		con1.write(predata.pt_targetT);
		con2.write(predata.pt_maskT);
		timer.stop(pid, M.offline_write);

		// PermuteTargetII
		predata.pt_p = new byte[d][];
		predata.pt_r = new byte[d][];
		predata.pt_a = new byte[d][];
		for (int i = 0; i < d; i++) {
			predata.pt_p[i] = Util.nextBytes((logD + 7) / 8, Crypto.sr);
			predata.pt_r[i] = Util.nextBytes((logD + 7) / 8, Crypto.sr);
			predata.pt_a[i] = Util.xor(predata.pt_p[i], predata.pt_r[i]);
		}
		predata.pt_a = Util.permute(predata.pt_a, predata.evict_pi);

		timer.start(pid, M.offline_write);
		con1.write(predata.pt_p);
		con1.write(predata.pt_a);
		con2.write(predata.pt_r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, int d, Timer timer) {
		timer.start(pid, M.offline_read);
		// PermuteTargetI
		predata.pt_keyT = con1.readTripleByteArray();
		predata.pt_targetT = con1.readTripleByteArray();

		// PermuteTargetII
		predata.pt_p = con1.readDoubleByteArray();
		predata.pt_a = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		// PermuteTargetI
		predata.pt_maskT = con1.readTripleByteArray();

		// PermuteTargetII
		predata.pt_r = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

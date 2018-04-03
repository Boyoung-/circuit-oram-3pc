package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreSSXOT extends Protocol {

	private int id;
	private int pid;

	public PreSSXOT(Communication con1, Communication con2, int id) {
		super(con1, con2);
		this.id = id;
		pid = id == 0 ? P.URXOT : P.XOT;
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.ssxot_E_pi[id] = con1.readIntArray();
		predata.ssxot_E_r[id] = con1.readTupleArray();
		timer.stop(pid, M.offline_read);
	}

	public void runD(PreData predata, int n, int k, int[] tupleParam, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.ssxot_delta[id] = new Tuple[k];
		for (int i = 0; i < k; i++)
			predata.ssxot_delta[id][i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3],
					Crypto.sr);

		predata.ssxot_E_pi[id] = Util.randomPermutation(n, Crypto.sr);
		predata.ssxot_C_pi[id] = Util.randomPermutation(n, Crypto.sr);
		predata.ssxot_E_pi_ivs[id] = Util.inversePermutation(predata.ssxot_E_pi[id]);
		predata.ssxot_C_pi_ivs[id] = Util.inversePermutation(predata.ssxot_C_pi[id]);

		predata.ssxot_E_r[id] = new Tuple[n];
		predata.ssxot_C_r[id] = new Tuple[n];
		for (int i = 0; i < n; i++) {
			predata.ssxot_E_r[id][i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
			predata.ssxot_C_r[id][i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
		}

		timer.start(pid, M.offline_write);
		con1.write(predata.ssxot_E_pi[id]);
		con1.write(predata.ssxot_E_r[id]);
		con2.write(predata.ssxot_C_pi[id]);
		con2.write(predata.ssxot_C_r[id]);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.ssxot_C_pi[id] = con2.readIntArray();
		predata.ssxot_C_r[id] = con2.readTupleArray();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {
		// TODO Auto-generated method stub

	}
}

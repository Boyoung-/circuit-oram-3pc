package protocols;

import communication.Communication;
import crypto.Crypto;
import measure.M;
import measure.P;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import util.Util;

public class PreSSXOT extends Protocol {
	public PreSSXOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(P.XOT, M.offline_read);
		predata.ssxot_E_pi = con1.readObject();
		predata.ssxot_E_r = con1.readObject();
		timer.stop(P.XOT, M.offline_read);
	}

	public void runD(PreData predata, int n, int k, int[] tupleParam, Timer timer) {
		timer.start(P.XOT, M.offline_comp);

		predata.ssxot_delta = new Tuple[k];
		for (int i = 0; i < k; i++)
			predata.ssxot_delta[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);

		predata.ssxot_E_pi = Util.randomPermutation(n, Crypto.sr);
		predata.ssxot_C_pi = Util.randomPermutation(n, Crypto.sr);
		predata.ssxot_E_pi_ivs = Util.inversePermutation(predata.ssxot_E_pi);
		predata.ssxot_C_pi_ivs = Util.inversePermutation(predata.ssxot_C_pi);

		predata.ssxot_E_r = new Tuple[n];
		predata.ssxot_C_r = new Tuple[n];
		for (int i = 0; i < n; i++) {
			predata.ssxot_E_r[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
			predata.ssxot_C_r[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
		}

		timer.start(P.XOT, M.offline_write);
		con1.write(predata.ssxot_E_pi);
		con1.write(predata.ssxot_E_r);
		con2.write(predata.ssxot_C_pi);
		con2.write(predata.ssxot_C_r);
		timer.stop(P.XOT, M.offline_write);

		timer.stop(P.XOT, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(P.XOT, M.offline_read);
		predata.ssxot_C_pi = con2.readObject();
		predata.ssxot_C_r = con2.readObject();
		timer.stop(P.XOT, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

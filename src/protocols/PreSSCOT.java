package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import measure.M;
import measure.P;
import measure.Timer;
import oram.Forest;
import oram.Metadata;

public class PreSSCOT extends Protocol {
	public PreSSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int n, Timer timer) {
		timer.start(P.COT, M.offline_comp);

		predata.sscot_k = PRF.generateKey(Crypto.sr);
		predata.sscot_kprime = PRF.generateKey(Crypto.sr);
		predata.sscot_r = new byte[n][];
		for (int i = 0; i < n; i++) {
			predata.sscot_r[i] = new byte[Crypto.secParamBytes];
			Crypto.sr.nextBytes(predata.sscot_r[i]);
		}

		timer.start(P.COT, M.offline_write);
		con1.write(predata.sscot_k);
		con1.write(predata.sscot_kprime);
		con1.write(predata.sscot_r);
		timer.stop(P.COT, M.offline_write);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);
		predata.sscot_F_kprime = new PRF(Crypto.secParam);
		predata.sscot_F_kprime.init(predata.sscot_kprime);

		timer.stop(P.COT, M.offline_comp);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(P.COT, M.offline_comp);

		timer.start(P.COT, M.offline_read);
		predata.sscot_k = con1.read();
		predata.sscot_kprime = con1.read();
		predata.sscot_r = con1.readObject();
		timer.stop(P.COT, M.offline_read);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);
		predata.sscot_F_kprime = new PRF(Crypto.secParam);
		predata.sscot_F_kprime.init(predata.sscot_kprime);

		timer.stop(P.COT, M.offline_comp);
	}

	public void runC() {
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

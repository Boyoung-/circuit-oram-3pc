package protocols.precomputation;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PrePermuteIndex extends Protocol {

	private int pid = P.PI;

	public PrePermuteIndex(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int d, int w, Timer timer) {
		timer.start(pid, M.offline_comp);

		int logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2));

		predata.pi_p = new BigInteger[d];
		predata.pi_r = new BigInteger[d];
		predata.pi_a = new BigInteger[d];
		for (int i = 0; i < d; i++) {
			predata.pi_p[i] = new BigInteger(logW, Crypto.sr);
			predata.pi_r[i] = new BigInteger(logW, Crypto.sr);
			predata.pi_a[i] = predata.pi_p[i].xor(predata.pi_r[i]);
		}
		predata.pi_a = Util.permute(predata.pi_a, predata.evict_pi);

		timer.start(pid, M.offline_write);
		con1.write(predata.pi_p);
		con1.write(predata.pi_a);
		con2.write(predata.pi_r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.pi_p = con1.readObject();
		predata.pi_a = con1.readObject();
		timer.stop(pid, M.offline_read);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.pi_r = con1.readObject();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

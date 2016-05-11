package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class PrePermuteIndex extends Protocol {
	public PrePermuteIndex(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int d, int w, Timer timer) {
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

		con1.write(predata.pi_p);
		con1.write(predata.pi_a);

		con2.write(predata.pi_r);
	}

	public void runD(PreData predata, Timer timer) {
		predata.pi_p = con1.readObject();
		predata.pi_a = con1.readObject();
	}

	public void runC(PreData predata, Timer timer) {
		predata.pi_r = con1.readObject();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

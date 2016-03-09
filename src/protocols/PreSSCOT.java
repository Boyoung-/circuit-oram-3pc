package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import oram.Forest;
import oram.Metadata;

public class PreSSCOT extends Protocol {
	public PreSSCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int n) {
		predata.sscot_k = PRF.generateKey(Crypto.sr);
		predata.sscot_kprime = PRF.generateKey(Crypto.sr);
		predata.sscot_r = new byte[n][];
		for (int i = 0; i < n; i++) {
			predata.sscot_r[i] = new byte[Crypto.secParamBytes];
			Crypto.sr.nextBytes(predata.sscot_r[i]);
		}
		con1.write(predata.sscot_k);
		con1.write(predata.sscot_kprime);
		con1.write(predata.sscot_r);
	}

	public void runD(PreData predata) {
		predata.sscot_k = con1.read();
		predata.sscot_kprime = con1.read();
		predata.sscot_r = con1.readObject();
	}

	public void runC() {
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

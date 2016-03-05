package protocols;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class PreSSIOT extends Protocol {
	public PreSSIOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int n) {
		predata.ssiot_k = PRF.generateKey(Crypto.sr);
		predata.ssiot_kprime = PRF.generateKey(Crypto.sr);
		predata.ssiot_r = Util.nextBytes(Crypto.secParamBytes, Crypto.sr);

		con1.write(predata.ssiot_k);
		con1.write(predata.ssiot_kprime);
		con1.write(predata.ssiot_r);
	}

	public void runD(PreData predata) {
		predata.ssiot_k = con1.read();
		predata.ssiot_kprime = con1.read();
		predata.ssiot_r = con1.read();
	}

	public void runC() {
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

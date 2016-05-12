package protocols.precomputation;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PreSSIOT extends Protocol {

	private int pid = P.IOT;

	public PreSSIOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int n, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.ssiot_k = PRF.generateKey(Crypto.sr);
		predata.ssiot_kprime = PRF.generateKey(Crypto.sr);
		predata.ssiot_r = Util.nextBytes(Crypto.secParamBytes, Crypto.sr);

		timer.start(pid, M.offline_write);
		con1.write(predata.ssiot_k);
		con1.write(predata.ssiot_kprime);
		con1.write(predata.ssiot_r);
		timer.stop(pid, M.offline_write);

		predata.ssiot_F_k = new PRF(Crypto.secParam);
		predata.ssiot_F_k.init(predata.ssiot_k);
		predata.ssiot_F_kprime = new PRF(Crypto.secParam);
		predata.ssiot_F_kprime.init(predata.ssiot_kprime);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		timer.start(pid, M.offline_read);
		predata.ssiot_k = con1.read();
		predata.ssiot_kprime = con1.read();
		predata.ssiot_r = con1.read();
		timer.stop(pid, M.offline_read);

		predata.ssiot_F_k = new PRF(Crypto.secParam);
		predata.ssiot_F_k.init(predata.ssiot_k);
		predata.ssiot_F_kprime = new PRF(Crypto.secParam);
		predata.ssiot_F_kprime.init(predata.ssiot_kprime);

		timer.stop(pid, M.offline_comp);
	}

	public void runC() {
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

package protocols.precomputation;

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

public class PrePostProcessT extends Protocol {

	private int pid = P.PPT;

	public PrePostProcessT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.ppt_Li = con1.read();
		predata.ppt_Lip1 = con1.read();

		predata.ppt_s = con1.readObject();
		timer.stop(pid, M.offline_read);
	}

	public void runD(PreData predata, PreData prev, int LiBytes, int Lip1Bytes, int tau, Timer timer) {
		timer.start(pid, M.offline_comp);

		if (prev != null)
			predata.ppt_Li = prev.ppt_Lip1;
		else
			predata.ppt_Li = Util.nextBytes(LiBytes, Crypto.sr);
		predata.ppt_Lip1 = Util.nextBytes(Lip1Bytes, Crypto.sr);

		int twoTauPow = (int) Math.pow(2, tau);
		predata.ppt_alpha = Crypto.sr.nextInt(tau);
		predata.ppt_r = new byte[twoTauPow][];
		predata.ppt_s = new byte[twoTauPow][];
		for (int i = 0; i < twoTauPow; i++) {
			predata.ppt_r[i] = Util.nextBytes(Lip1Bytes, Crypto.sr);
			predata.ppt_s[i] = predata.ppt_r[i];
		}
		predata.ppt_s[predata.ppt_alpha] = Util.xor(predata.ppt_r[predata.ppt_alpha], predata.ppt_Lip1);

		timer.start(pid, M.offline_write);
		con1.write(predata.ppt_Li);
		con1.write(predata.ppt_Lip1);

		con2.write(predata.ppt_alpha);
		con2.write(predata.ppt_r);
		con1.write(predata.ppt_s);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, PreData prev, int LiBytes, int Lip1Bytes, Timer timer) {
		timer.start(pid, M.offline_comp);

		if (prev != null)
			predata.ppt_Li = prev.ppt_Lip1;
		else
			predata.ppt_Li = Util.nextBytes(LiBytes, Crypto.sr);
		predata.ppt_Lip1 = Util.nextBytes(Lip1Bytes, Crypto.sr);

		timer.start(pid, M.offline_read);
		predata.ppt_alpha = con2.readInt();
		predata.ppt_r = con2.readObject();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

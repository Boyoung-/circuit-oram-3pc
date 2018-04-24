package pir.precomputation;

import communication.Communication;
import crypto.Crypto;
import crypto.PRF;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;

public class PrePIRCOT extends Protocol {

	public PrePIRCOT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	// TODO: change PRF output bits to max(32, N)

	public void runE(PreData predata, int l) {
		timer.start(M.offline_comp);

		predata.sscot_k = PRF.generateKey(Crypto.sr);
		predata.sscot_r = new byte[l][];
		for (int i = 0; i < l; i++) {
			predata.sscot_r[i] = new byte[Crypto.secParamBytes];
			Crypto.sr.nextBytes(predata.sscot_r[i]);
		}
		predata.sscot_s_DE = Crypto.sr.nextInt(l);
		predata.sscot_s_CE = Crypto.sr.nextInt(l);

		timer.start(M.offline_write);
		con1.write(offline_band, predata.sscot_k);
		con1.write(offline_band, predata.sscot_r);
		con1.write(offline_band, predata.sscot_s_DE);
		con2.write(offline_band, predata.sscot_s_CE);
		timer.stop(M.offline_write);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);

		timer.stop(M.offline_comp);
	}

	public void runD(PreData predata, int l) {
		timer.start(M.offline_comp);

		predata.sscot_s_CD = Crypto.sr.nextInt(l);

		timer.start(M.offline_write);
		con2.write(offline_band, predata.sscot_s_CD);
		timer.stop(M.offline_write);

		timer.start(M.offline_read);
		predata.sscot_k = con1.readAndDec();
		predata.sscot_r = con1.readDoubleByteArrayAndDec();
		predata.sscot_s_DE = con1.readIntAndDec();
		timer.stop(M.offline_read);

		predata.sscot_F_k = new PRF(Crypto.secParam);
		predata.sscot_F_k.init(predata.sscot_k);

		timer.stop(M.offline_comp);
	}

	public void runC(PreData predata) {
		timer.start(M.offline_comp);

		timer.start(M.offline_read);
		predata.sscot_s_CE = con1.readIntAndDec();
		predata.sscot_s_CD = con2.readIntAndDec();
		timer.stop(M.offline_read);

		timer.stop(M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {
	}
}

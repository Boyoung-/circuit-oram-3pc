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

public class PreAccess extends Protocol {
	public PreAccess(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int twotaupow, int numTuples, int[] tupleParam, Timer timer) {
		timer.start(P.ACC, M.offline_comp);

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runE(predata, numTuples, timer);

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runE(predata, twotaupow, timer);

		// Access
		predata.access_sigma = Util.randomPermutation(numTuples, Crypto.sr);
		predata.access_p = new Tuple[numTuples];
		for (int i = 0; i < numTuples; i++)
			predata.access_p[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);

		timer.start(P.ACC, M.offline_write);
		con1.write(predata.access_sigma);
		con1.write(predata.access_p);
		timer.stop(P.ACC, M.offline_write);

		timer.stop(P.ACC, M.offline_comp);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(P.ACC, M.offline_comp);

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runD(predata, timer);

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runD(predata, timer);

		// Access
		timer.start(P.ACC, M.offline_read);
		predata.access_sigma = con1.readObject();
		predata.access_p = con1.readObject();
		timer.stop(P.ACC, M.offline_read);

		timer.stop(P.ACC, M.offline_comp);
	}

	public void runC(Timer timer) {
		timer.start(P.ACC, M.offline_comp);

		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runC();

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runC();

		timer.stop(P.ACC, M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

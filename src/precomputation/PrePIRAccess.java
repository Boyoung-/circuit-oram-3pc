package precomputation;

import communication.Communication;
//import crypto.Crypto;
import oram.Forest;
import oram.Metadata;
//import oram.Tuple;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PrePIRAccess extends Protocol {

	private int pid = P.ACC;

	public PrePIRAccess(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, int twotaupow, int numTuples, int[] tupleParam, Timer timer) {
		timer.start(pid, M.offline_comp);

		// SSCOT
		PrePIRCOT presscot = new PrePIRCOT(con1, con2);
		presscot.runE(predata, numTuples, timer);

		// SSIOT
		PrePIRIOT pressiot = new PrePIRIOT(con1, con2);
		pressiot.runE(predata, twotaupow, timer);

		// Access
		// predata.access_sigma = Util.randomPermutation(numTuples, Crypto.sr);
		predata.access_sigma = Util.identityPermutation(numTuples);
		// predata.access_p = new Tuple[numTuples];
		// for (int i = 0; i < numTuples; i++)
		// predata.access_p[i] = new Tuple(tupleParam[0], tupleParam[1],
		// tupleParam[2], tupleParam[3], Crypto.sr);

		timer.start(pid, M.offline_write);
		con1.write(predata.access_sigma);
		// con1.write(predata.access_p);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		// SSCOT
		PrePIRCOT presscot = new PrePIRCOT(con1, con2);
		presscot.runD(predata, timer);

		// SSIOT
		PrePIRIOT pressiot = new PrePIRIOT(con1, con2);
		pressiot.runD(predata, timer);

		// Access
		timer.start(pid, M.offline_read);
		predata.access_sigma = con1.readIntArray();
		// predata.access_p = con1.readTupleArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(Timer timer) {
		timer.start(pid, M.offline_comp);

		// SSCOT
		PrePIRCOT presscot = new PrePIRCOT(con1, con2);
		presscot.runC();

		// SSIOT
		PrePIRIOT pressiot = new PrePIRIOT(con1, con2);
		pressiot.runC();

		timer.stop(pid, M.offline_comp);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

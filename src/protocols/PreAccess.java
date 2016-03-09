package protocols;

import communication.Communication;
import crypto.Crypto;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.Util;

public class PreAccess extends Protocol {
	public PreAccess(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Tree OT, int numTuples) {
		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runE(predata, numTuples);

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runE(predata, OT.getTwoTauPow());

		// Access
		predata.access_sigma = Util.randomPermutation(numTuples, Crypto.sr);
		predata.access_p = new Tuple[numTuples];
		for (int i = 0; i < numTuples; i++)
			predata.access_p[i] = new Tuple(OT.getFBytes(), OT.getNBytes(), OT.getLBytes(), OT.getABytes(), Crypto.sr);

		con1.write(predata.access_sigma);
		con1.write(predata.access_p);
	}

	public void runD(PreData predata) {
		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runD(predata);

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runD(predata);

		// Access
		predata.access_sigma = con1.readObject();
		predata.access_p = con1.readObject();
	}

	public void runC() {
		// SSCOT
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runC();

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runC();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

package protocols;

import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import util.Util;

public class PreAccess extends Protocol {
	public PreAccess(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Tree OT, int numBuckets) {
		// SSCOT
		int numTuples = OT.getStashSize() + (numBuckets - 1) * OT.getW();
		PreSSCOT presscot = new PreSSCOT(con1, con2);
		presscot.runE(predata, numTuples);

		// SSIOT
		PreSSIOT pressiot = new PreSSIOT(con1, con2);
		pressiot.runE(predata, OT.getTwoTauPow());

		// Access
		predata.access_sigma = Util.randomPermutation(numBuckets, Crypto.sr);

		int[] tupleParam = new int[] { OT.getFBytes(), OT.getNBytes(), OT.getLBytes(), OT.getABytes() };
		predata.access_p = new Bucket[numBuckets];
		predata.access_p[0] = new Bucket(OT.getStashSize(), tupleParam, Crypto.sr);
		for (int i = 1; i < numBuckets; i++)
			predata.access_p[i] = new Bucket(OT.getW(), tupleParam, Crypto.sr);

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
		predata.access_sigma = con1.readIntArray();
		Object[] objArray = con1.readObjectArray();
		predata.access_p = Arrays.copyOf(objArray, objArray.length, Bucket[].class);
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

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

public class PreReshuffle extends Protocol {
	public PreReshuffle(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(P.RSF, M.offline_comp);

		predata.reshuffle_pi = Util.inversePermutation(predata.access_sigma);

		timer.start(P.RSF, M.offline_read);
		predata.reshuffle_r = con1.readObject();
		timer.stop(P.RSF, M.offline_read);

		timer.stop(P.RSF, M.offline_comp);
	}

	public void runD(PreData predata, int[] tupleParam, Timer timer) {
		timer.start(P.RSF, M.offline_comp);

		predata.reshuffle_pi = Util.inversePermutation(predata.access_sigma);
		int numTuples = predata.reshuffle_pi.length;
		predata.reshuffle_p = new Tuple[numTuples];
		predata.reshuffle_r = new Tuple[numTuples];
		Tuple[] a = new Tuple[numTuples];
		for (int i = 0; i < numTuples; i++) {
			predata.reshuffle_p[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
			predata.reshuffle_r[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
			a[i] = predata.reshuffle_p[i].xor(predata.reshuffle_r[i]);
		}
		predata.reshuffle_a_prime = Util.permute(a, predata.reshuffle_pi);

		timer.start(P.RSF, M.offline_write);
		con2.write(predata.reshuffle_p);
		con2.write(predata.reshuffle_a_prime);
		con1.write(predata.reshuffle_r);
		timer.stop(P.RSF, M.offline_write);

		timer.stop(P.RSF, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(P.RSF, M.offline_read);
		predata.reshuffle_p = con2.readObject();
		predata.reshuffle_a_prime = con2.readObject();
		timer.stop(P.RSF, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

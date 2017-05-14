package pir.precomputation;

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

public class PrePIRReshuffle extends Protocol {

	private int pid = P.RSF;

	public PrePIRReshuffle(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.reshuffle_pi = Util.inversePermutation(predata.access_sigma);

		timer.start(pid, M.offline_read);
		predata.pir_reshuffle_r = con1.readDoubleByteArray();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);
	}

	public void runD(PreData predata, int[] tupleParam, Timer timer) {
		timer.start(pid, M.offline_comp);

		predata.reshuffle_pi = Util.inversePermutation(predata.access_sigma);
		int numTuples = predata.reshuffle_pi.length;
		predata.pir_reshuffle_p = new byte[numTuples][];
		predata.pir_reshuffle_r = new byte[numTuples][];
		byte[][] a = new byte[numTuples][];
		for (int i = 0; i < numTuples; i++) {
			predata.pir_reshuffle_p[i] = Util.nextBytes(1, Crypto.sr);
			predata.pir_reshuffle_r[i] = Util.nextBytes(1, Crypto.sr);
			a[i] = Util.xor(predata.pir_reshuffle_p[i], predata.pir_reshuffle_r[i]);
		}
		predata.pir_reshuffle_a_prime = Util.permute(a, predata.reshuffle_pi);

		timer.start(pid, M.offline_write);
		con2.write(predata.pir_reshuffle_p);
		con2.write(predata.pir_reshuffle_a_prime);
		con1.write(predata.pir_reshuffle_r);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);
	}

	public void runC(PreData predata, Timer timer) {
		timer.start(pid, M.offline_read);
		predata.pir_reshuffle_p = con2.readDoubleByteArray();
		predata.pir_reshuffle_a_prime = con2.readDoubleByteArray();
		timer.stop(pid, M.offline_read);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

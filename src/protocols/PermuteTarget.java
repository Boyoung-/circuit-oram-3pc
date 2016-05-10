package protocols;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class PermuteTarget extends Protocol {
	public PermuteTarget(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE() {
	}

	public int[] runD(PreData predata, boolean firstTree, GCSignal[][] targetOutKeys, Timer timer) {
		if (firstTree)
			return null;

		// PermuteTargetI
		int d = targetOutKeys.length;
		int I[] = new int[d];
		BigInteger[] target = new BigInteger[d];

		for (int i = 0; i < d; i++) {
			BigInteger hashKeys = new BigInteger(GCUtil.hashAll(targetOutKeys[i]));
			for (int j = 0; j < d; j++) {
				if (hashKeys.compareTo(predata.pt_keyT[i][j]) == 0) {
					I[i] = j;
					target[i] = predata.pt_targetT[i][j];
					break;
				}
			}
		}

		// PermuteTargetII
		BigInteger[] z = Util.xor(target, predata.pt_p);

		con2.write(z);
		con2.write(I);

		BigInteger[] g = con2.readObject();

		target = Util.xor(predata.pt_a, g);

		int[] target_pp = new int[d];
		for (int i=0; i<d; i++)
			target_pp[i] = target[i].intValue();
		
		return target_pp;
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

		// PermuteTargetII
		BigInteger[] z = con2.readObject();
		int[] I = con2.readObject();

		BigInteger[] mk = new BigInteger[z.length];
		for (int i = 0; i < mk.length; i++) {
			mk[i] = predata.pt_maskT[i][I[i]].xor(z[i]);
			mk[i] = predata.pt_r[i].xor(mk[i]);
		}
		BigInteger[] g = Util.permute(mk, predata.evict_pi);

		con2.write(g);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 10; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PrePermuteTarget prepermutetarget = new PrePermuteTarget(con1, con2);

			if (party == Party.Eddie) {
				int d = Crypto.sr.nextInt(1) + 5;
				int logD = (int) Math.ceil(Math.log(d) / Math.log(2));
				int[] target = Util.randomPermutation(d, Crypto.sr);

				predata.evict_pi = Util.randomPermutation(d, Crypto.sr);
				predata.evict_targetOutKeyPairs = new GCSignal[d][][];
				GCSignal[][] targetOutKeys = new GCSignal[d][];
				for (int j = 0; j < d; j++) {
					predata.evict_targetOutKeyPairs[j] = GCUtil.genKeyPairs(logD);
					targetOutKeys[j] = GCUtil.revSelectKeys(predata.evict_targetOutKeyPairs[j],
							BigInteger.valueOf(target[j]).toByteArray());
				}

				con1.write(d);
				con1.write(predata.evict_pi);
				con1.write(predata.evict_targetOutKeyPairs);
				con1.write(targetOutKeys);

				con2.write(predata.evict_pi);

				prepermutetarget.runE(predata, d, timer);

				runE();

				int[] pi_ivs = Util.inversePermutation(predata.evict_pi);

				int[] piTargetPiIvs = new int[d];
				for (int j = 0; j < d; j++) {
					piTargetPiIvs[j] = predata.evict_pi[target[pi_ivs[j]]];
					System.out.print(piTargetPiIvs[j] + " ");
				}
				System.out.println();

			} else if (party == Party.Debbie) {
				int d = con1.readObject();
				predata.evict_pi = con1.readObject();
				predata.evict_targetOutKeyPairs = con1.readObject();
				GCSignal[][] targetOutKeys = con1.readObject();

				prepermutetarget.runD(predata, d, timer);

				int[] target_pp = runD(predata, false, targetOutKeys, timer);
				for (int j = 0; j < d; j++) {
					System.out.print(target_pp[j] + " ");
				}
				System.out.println();

			} else if (party == Party.Charlie) {
				predata.evict_pi = con1.readObject();

				prepermutetarget.runC(predata, timer);

				runC(predata, false, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

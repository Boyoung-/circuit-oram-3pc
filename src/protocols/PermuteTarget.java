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

	public void runE(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

	}

	public BigInteger[] runD(PreData predata, boolean firstTree, GCSignal[][] targetOutKeys, Timer timer) {
		if (firstTree)
			return null;

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
		
		for (int j=0; j<d; j++) {
			System.out.print(target[j].intValue() + " ");
		}
		System.out.println();

		return target;
	}

	public void runC(PreData predata, boolean firstTree, Timer timer) {
		if (firstTree)
			return;

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

				prepermutetarget.runE(predata, d, timer);
				
				runE(predata, false, timer);
				
				int[] piTarget = new int[d];
				for (int j=0; j<d; j++) {
					piTarget[j] = predata.evict_pi[target[j]];
					System.out.print(piTarget[j] + " ");
				}
				System.out.println();

			} else if (party == Party.Debbie) {
				int d = con1.readObject();
				predata.evict_pi = con1.readObject();
				predata.evict_targetOutKeyPairs = con1.readObject();
				GCSignal[][] targetOutKeys = con1.readObject();

				prepermutetarget.runD(predata, d, timer);
				
				runD(predata, false, targetOutKeys, timer);

			} else if (party == Party.Charlie) {
				prepermutetarget.runC(predata, timer);
				
				runC(predata, false, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

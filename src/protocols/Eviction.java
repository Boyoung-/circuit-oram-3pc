package protocols;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import measure.Timer;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import util.Util;

public class Eviction extends Protocol {
	public Eviction(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Tuple[] runE(PreData predata, boolean firstTree, byte[] Li, Tuple[] pathTuples, int d, int w, Timer timer) {
		if (firstTree)
			return pathTuples;

		Bucket[] pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, w, w);

		GCSignal[] LiInputKeys = GCUtil.revSelectKeys(predata.evict_LiKeyPairs, Li);
		GCSignal[][] E_feInputKeys = new GCSignal[d][];
		GCSignal[][][] E_labelInputKeys = new GCSignal[d][][];
		GCSignal[][] deltaInputKeys = new GCSignal[d][];
		for (int i = 0; i < d; i++) {
			E_feInputKeys[i] = GCUtil.selectFeKeys(predata.evict_E_feKeyPairs[i], pathBuckets[i].getTuples());
			E_labelInputKeys[i] = GCUtil.selectLabelKeys(predata.evict_E_labelKeyPairs[i], pathBuckets[i].getTuples());
			deltaInputKeys[i] = GCUtil.revSelectKeys(predata.evict_deltaKeyPairs[i],
					predata.evict_delta[i].toByteArray());
		}

		con1.write(LiInputKeys);
		con1.write(E_feInputKeys);
		con1.write(E_labelInputKeys);
		con1.write(deltaInputKeys);

		return pathTuples;
	}

	public void runD(PreData predata, boolean firstTree, byte[] Li, int w, Timer timer) {
		if (firstTree)
			return;

		GCSignal[] LiInputKeys = con1.readObject();
		GCSignal[][] E_feInputKeys = con1.readObject();
		GCSignal[][][] E_labelInputKeys = con1.readObject();
		GCSignal[][] deltaInputKeys = con1.readObject();

		GCSignal[][] C_feInputKeys = con2.readObject();
		GCSignal[][][] C_labelInputKeys = con2.readObject();

		GCSignal[][][] outKeys = predata.evict_gc.routing(LiInputKeys, E_feInputKeys, C_feInputKeys, E_labelInputKeys,
				C_labelInputKeys, deltaInputKeys);

		int[] ti_p = new int[deltaInputKeys.length];
		// int[] target = new int[deltaInputKeys.length];
		for (int i = 0; i < ti_p.length; i++) {
			ti_p[i] = GCUtil.evaOutKeys(outKeys[1][i], predata.evict_tiOutKeyHashes[i]).intValue();
			// target[i] = GCUtil.evaOutKeys(outKeys[0][i],
			// predata.tmpKeyHashes[i]).intValue();
		}

		System.out.println("ti:");
		for (int i = 0; i < ti_p.length; i++)
			System.out.print(ti_p[i] + " ");
		System.out.println();
		// System.out.println("target:");
		// for (int i=0; i<ti_p.length; i++)
		// System.out.print(target[i] + " ");
		// System.out.println();
	}

	public Tuple[] runC(PreData predata, boolean firstTree, Tuple[] pathTuples, int d, int w, Timer timer) {
		if (firstTree)
			return pathTuples;

		Bucket[] pathBuckets = Bucket.tuplesToBuckets(pathTuples, d, w, w);

		GCSignal[][] C_feInputKeys = new GCSignal[d][];
		GCSignal[][][] C_labelInputKeys = new GCSignal[d][][];
		for (int i = 0; i < d; i++) {
			C_feInputKeys[i] = GCUtil.selectFeKeys(predata.evict_C_feKeyPairs[i], pathBuckets[i].getTuples());
			C_labelInputKeys[i] = GCUtil.selectLabelKeys(predata.evict_C_labelKeyPairs[i], pathBuckets[i].getTuples());
		}

		con2.write(C_feInputKeys);
		con2.write(C_labelInputKeys);

		return pathTuples;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 2; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PreEviction preeviction = new PreEviction(con1, con2);

			if (party == Party.Eddie) {
				int w = Crypto.sr.nextInt(1) + 4;
				int d = Crypto.sr.nextInt(1) + 5;
				int lBits = d - 1;
				int lBytes = (lBits + 7) / 8;
				byte[] Li = Util.nextBytes(lBytes, Crypto.sr);
				Tuple[] path = new Tuple[d * w];
				for (int j = 0; j < d * w; j++)
					path[j] = new Tuple(1, 2, lBytes, 3, Crypto.sr);

				System.out.println("d, w: " + d + " " + w);

				con1.write(d);
				con1.write(w);
				con1.write(Li);
				con2.write(d);
				con2.write(w);

				preeviction.runE(predata, d, w, timer);
				runE(predata, false, Li, path, d, w, timer);

				int emptyIndex = 0;
				for (int j = 0; j < d * w; j++) {
					if (new BigInteger(path[j].getF()).testBit(0)) {
						String l = Util.addZeros(
								Util.getSubBits(new BigInteger(1, Util.xor(path[j].getL(), Li)), lBits, 0).toString(2),
								lBits);
						System.out.println(j + ":\t" + l);
					} else {
						emptyIndex = j;
					}
				}
				System.out.println("last empty: " + emptyIndex);

			} else if (party == Party.Debbie) {
				int d = con1.readObject();
				int w = con1.readObject();
				byte[] Li = con1.read();
				int[] tupleParam = new int[] { 1, 2, (d - 1 + 7) / 8, 3 };

				preeviction.runD(predata, d, w, tupleParam, timer);
				runD(predata, false, Li, w, timer);

			} else if (party == Party.Charlie) {
				int d = con1.readObject();
				int w = con1.readObject();
				int lBytes = (d - 1 + 7) / 8;
				Tuple[] path = new Tuple[d * w];
				for (int j = 0; j < d * w; j++)
					path[j] = new Tuple(1, 2, lBytes, 3, null);

				preeviction.runC(predata, timer);
				runC(predata, false, path, d, w, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

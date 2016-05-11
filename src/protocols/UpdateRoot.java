package protocols;

import java.math.BigInteger;

import org.apache.commons.lang3.ArrayUtils;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import util.Util;

public class UpdateRoot extends Protocol {
	public UpdateRoot(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Tuple[] runE(PreData predata, boolean firstTree, byte[] Li, Tuple[] R, Tuple Ti, Timer timer) {
		if (firstTree)
			return R;

		// step 1
		int j1 = Crypto.sr.nextInt(R.length);
		GCSignal[] j1InputKeys = GCUtil.revSelectKeys(predata.ur_j1KeyPairs, BigInteger.valueOf(j1).toByteArray());
		GCSignal[] LiInputKeys = GCUtil.revSelectKeys(predata.ur_LiKeyPairs, Li);
		GCSignal[] E_feInputKeys = GCUtil.selectFeKeys(predata.ur_E_feKeyPairs, R);
		GCSignal[][] E_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_E_labelKeyPairs, R);

		con1.write(j1InputKeys);
		con1.write(LiInputKeys);
		con1.write(E_feInputKeys);
		con1.write(E_labelInputKeys);

		// step 4
		R = ArrayUtils.addAll(R, new Tuple[] { Ti });
		SSXOT ssxot = new SSXOT(con1, con2, 0);
		R = ssxot.runE(predata, R, timer);

		return R;
	}

	public void runD(PreData predata, boolean firstTree, byte[] Li, int w, Timer timer) {
		if (firstTree)
			return;

		// step 1
		GCSignal[] j1InputKeys = con1.readObject();
		GCSignal[] LiInputKeys = con1.readObject();
		GCSignal[] E_feInputKeys = con1.readObject();
		GCSignal[][] E_labelInputKeys = con1.readObject();
		GCSignal[] C_feInputKeys = con2.readObject();
		GCSignal[][] C_labelInputKeys = con2.readObject();

		// step 2
		GCSignal[][] outKeys = predata.ur_gcur.rootFindDeepestAndEmpty(j1InputKeys, LiInputKeys, E_feInputKeys,
				C_feInputKeys, E_labelInputKeys, C_labelInputKeys);
		int j1 = GCUtil.evaOutKeys(outKeys[0], predata.ur_outKeyHashes[0]).intValue();
		int j2 = GCUtil.evaOutKeys(outKeys[1], predata.ur_outKeyHashes[1]).intValue();

		// System.out.println(j1 + " " + j2);

		// step 3
		int r = Crypto.sr.nextInt(w);
		int[] I = new int[E_feInputKeys.length];
		for (int i = 0; i < I.length; i++)
			I[i] = i;
		I[j2] = I.length;
		int tmp = I[r];
		I[r] = I[j1];
		I[j1] = tmp;

		// step 4
		SSXOT ssxot = new SSXOT(con1, con2, 0);
		ssxot.runD(predata, I, timer);

		// for (int i=0; i<I.length; i++)
		// System.out.print(I[i] + " ");
		// System.out.println();
	}

	public Tuple[] runC(PreData predata, boolean firstTree, Tuple[] R, Tuple Ti, Timer timer) {
		if (firstTree)
			return R;

		// step 1
		GCSignal[] C_feInputKeys = GCUtil.selectFeKeys(predata.ur_C_feKeyPairs, R);
		GCSignal[][] C_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_C_labelKeyPairs, R);

		con2.write(C_feInputKeys);
		con2.write(C_labelInputKeys);

		// step 4
		R = ArrayUtils.addAll(R, new Tuple[] { Ti });
		SSXOT ssxot = new SSXOT(con1, con2, 0);
		R = ssxot.runC(predata, R, timer);

		return R;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 10; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);

			if (party == Party.Eddie) {
				int sw = Crypto.sr.nextInt(25) + 10;
				int lBits = Crypto.sr.nextInt(30) + 5;
				byte[] Li = Util.nextBytes((lBits + 7) / 8, Crypto.sr);
				Tuple[] R = new Tuple[sw];
				for (int j = 0; j < sw; j++)
					R[j] = new Tuple(1, 2, (lBits + 7) / 8, 3, Crypto.sr);
				Tuple Ti = new Tuple(1, 2, (lBits + 7) / 8, 3, Crypto.sr);

				System.out.println("sw,lBits: " + sw + " " + lBits);

				con1.write(sw);
				con1.write(lBits);
				con1.write(Li);
				con2.write(sw);
				con2.write(lBits);

				preupdateroot.runE(predata, sw, lBits, timer);
				runE(predata, false, Li, R, Ti, timer);

				int emptyIndex = 0;
				for (int j = 0; j < sw; j++) {
					if (new BigInteger(R[j].getF()).testBit(0)) {
						String l = Util.addZeros(
								Util.getSubBits(new BigInteger(1, Util.xor(R[j].getL(), Li)), lBits, 0).toString(2),
								lBits);
						System.out.println(j + ":\t" + l);
					} else {
						emptyIndex = j;
					}
				}
				System.out.println("last empty: " + emptyIndex);

			} else if (party == Party.Debbie) {
				int sw = con1.readObject();
				int lBits = con1.readObject();
				byte[] Li = con1.read();
				int[] tupleParam = new int[] { 1, 2, (lBits + 7) / 8, 3 };

				preupdateroot.runD(predata, sw, lBits, tupleParam, timer);
				runD(predata, false, Li, md.getW(), timer);

			} else if (party == Party.Charlie) {
				int sw = con1.readObject();
				int lBits = con1.readObject();
				Tuple[] R = new Tuple[sw];
				for (int j = 0; j < sw; j++)
					R[j] = new Tuple(1, 2, (lBits + 7) / 8, 3, null);
				Tuple Ti = new Tuple(1, 2, (lBits + 7) / 8, 3, null);

				preupdateroot.runC(predata, timer);
				runC(predata, false, R, Ti, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

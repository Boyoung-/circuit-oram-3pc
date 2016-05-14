package protocols;

import java.math.BigInteger;

import org.apache.commons.lang3.ArrayUtils;

import com.oblivm.backend.gc.GCSignal;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCUtil;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import protocols.precomputation.PreUpdateRoot;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class UpdateRoot extends Protocol {

	private int pid = P.UR;

	public UpdateRoot(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public Tuple[] runE(PreData predata, boolean firstTree, byte[] Li, Tuple[] R, Tuple Ti, Timer timer) {
		if (firstTree)
			return R;

		timer.start(pid, M.online_comp);

		// step 1
		int j1 = Crypto.sr.nextInt(R.length);
		GCSignal[] j1InputKeys = GCUtil.revSelectKeys(predata.ur_j1KeyPairs, BigInteger.valueOf(j1).toByteArray());
		GCSignal[] LiInputKeys = GCUtil.revSelectKeys(predata.ur_LiKeyPairs, Li);
		GCSignal[] E_feInputKeys = GCUtil.selectFeKeys(predata.ur_E_feKeyPairs, R);
		GCSignal[][] E_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_E_labelKeyPairs, R);

		timer.start(pid, M.online_write);
		con1.write(pid, j1InputKeys);
		con1.write(pid, LiInputKeys);
		con1.write(pid, E_feInputKeys);
		con1.write(pid, E_labelInputKeys);
		timer.stop(pid, M.online_write);

		// step 4
		R = ArrayUtils.addAll(R, new Tuple[] { Ti });
		SSXOT ssxot = new SSXOT(con1, con2, 0);
		R = ssxot.runE(predata, R, timer);

		timer.stop(pid, M.online_comp);
		return R;
	}

	public void runD(PreData predata, boolean firstTree, byte[] Li, int w, Timer timer) {
		if (firstTree)
			return;

		timer.start(pid, M.online_comp);

		// step 1
		timer.start(pid, M.online_read);
		GCSignal[] j1InputKeys = con1.readGCSignalArray();
		GCSignal[] LiInputKeys = con1.readGCSignalArray();
		GCSignal[] E_feInputKeys = con1.readGCSignalArray();
		GCSignal[][] E_labelInputKeys = con1.readDoubleGCSignalArray();
		GCSignal[] C_feInputKeys = con2.readGCSignalArray();
		GCSignal[][] C_labelInputKeys = con2.readDoubleGCSignalArray();
		timer.stop(pid, M.online_read);

		// step 2
		GCSignal[][] outKeys = predata.ur_gcur.rootFindDeepestAndEmpty(j1InputKeys, LiInputKeys, E_feInputKeys,
				C_feInputKeys, E_labelInputKeys, C_labelInputKeys);
		int j1 = GCUtil.evaOutKeys(outKeys[0], predata.ur_outKeyHashes[0]).intValue();
		int j2 = GCUtil.evaOutKeys(outKeys[1], predata.ur_outKeyHashes[1]).intValue();

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

		timer.stop(pid, M.online_comp);
	}

	public Tuple[] runC(PreData predata, boolean firstTree, Tuple[] R, Tuple Ti, Timer timer) {
		if (firstTree)
			return R;

		timer.start(pid, M.online_comp);

		// step 1
		GCSignal[] C_feInputKeys = GCUtil.selectFeKeys(predata.ur_C_feKeyPairs, R);
		GCSignal[][] C_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_C_labelKeyPairs, R);

		timer.start(pid, M.online_write);
		con2.write(pid, C_feInputKeys);
		con2.write(pid, C_labelInputKeys);
		timer.stop(pid, M.online_write);

		// step 4
		R = ArrayUtils.addAll(R, new Tuple[] { Ti });
		SSXOT ssxot = new SSXOT(con1, con2, 0);
		R = ssxot.runC(predata, R, timer);

		timer.stop(pid, M.online_comp);
		return R;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		Timer timer = new Timer();

		for (int i = 0; i < 20; i++) {

			System.out.println("i=" + i);

			PreData predata = new PreData();
			PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);

			if (party == Party.Eddie) {
				int sw = Crypto.sr.nextInt(15) + 10;
				int lBits = Crypto.sr.nextInt(20) + 5;
				byte[] Li = Util.nextBytes((lBits + 7) / 8, Crypto.sr);
				Tuple[] R = new Tuple[sw];
				for (int j = 0; j < sw; j++)
					R[j] = new Tuple(1, 2, (lBits + 7) / 8, 3, Crypto.sr);
				Tuple Ti = new Tuple(1, 2, (lBits + 7) / 8, 3, Crypto.sr);

				con1.write(sw);
				con1.write(lBits);
				con1.write(Li);
				con2.write(sw);
				con2.write(lBits);

				preupdateroot.runE(predata, sw, lBits, timer);
				Tuple[] newR = runE(predata, false, Li, R, Ti, timer);

				Tuple[] R_C = con2.readTupleArray();
				int cnt = 0;
				int[] index = new int[3];
				for (int j = 0; j < sw; j++) {
					newR[j].setXor(R_C[j]);
					if (!R[j].equals(newR[j])) {
						index[cnt] = j;
						cnt++;
					}
				}

				if (cnt == 1) {
					if (newR[index[0]].equals(Ti) && (R[index[0]].getF()[0] & 1) == 0)
						System.out.println("UpdateRoot test passed");
					else
						System.err.println("UpdateRoot test failed 1");
				} else if (cnt == 2) {
					int u = -1;
					for (int k = 0; k < cnt; k++) {
						if (newR[index[k]].equals(Ti)) {
							u = k;
							break;
						}
					}
					if (u == -1)
						System.err.println("UpdateRoot test failed 2");
					else {
						int a1 = index[u];
						int a2 = index[1 - u];
						if (!R[a1].equals(newR[a2]) || (R[u].getF()[0] & 1) == 1)
							System.err.println("UpdateRoot test failed 3");
						else
							System.out.println("UpdateRoot test passed");
					}
				} else if (cnt == 3) {
					int u = -1;
					for (int k = 0; k < cnt; k++) {
						if (newR[index[k]].equals(Ti)) {
							u = k;
							break;
						}
					}
					if (u == -1)
						System.err.println("UpdateRoot test failed 4");
					else {
						int a1, a2;
						if (u == 0) {
							a1 = 1;
							a2 = 2;
						} else if (u == 1) {
							a1 = 0;
							a2 = 2;
						} else {
							a1 = 0;
							a2 = 1;
						}
						u = index[u];
						a1 = index[a1];
						a2 = index[a2];
						if ((R[u].getF()[0] & 1) == 1)
							System.err.println("UpdateRoot test failed 5");
						else if (!R[a1].equals(newR[a2]))
							System.err.println("UpdateRoot test failed 6");
						else if (!R[a1].equals(newR[a2]) || !R[a2].equals(newR[a1]))
							System.err.println("UpdateRoot test failed 7");
						else
							System.out.println("UpdateRoot test passed");
					}
				} else {
					System.err.println("UpdateRoot test failed 8");
				}
				System.out.println();

			} else if (party == Party.Debbie) {
				int sw = con1.readInt();
				int lBits = con1.readInt();
				byte[] Li = con1.read();
				int[] tupleParam = new int[] { 1, 2, (lBits + 7) / 8, 3 };

				preupdateroot.runD(predata, sw, lBits, tupleParam, timer);
				runD(predata, false, Li, md.getW(), timer);

			} else if (party == Party.Charlie) {
				int sw = con1.readInt();
				int lBits = con1.readInt();
				Tuple[] R = new Tuple[sw];
				for (int j = 0; j < sw; j++)
					R[j] = new Tuple(1, 2, (lBits + 7) / 8, 3, null);
				Tuple Ti = new Tuple(1, 2, (lBits + 7) / 8, 3, null);

				preupdateroot.runC(predata, timer);
				R = runC(predata, false, R, Ti, timer);

				con1.write(R);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

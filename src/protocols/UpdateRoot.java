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
import oram.Tuple;
import util.Util;

public class UpdateRoot extends Protocol {
	public UpdateRoot(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, byte[] Li, Tuple[] R, Timer timer) {
		// step 1
		int j1 = Crypto.sr.nextInt(R.length);
		GCSignal[] j1InputKeys = GCUtil.revSelectKeys(predata.ur_j1KeyPairs, BigInteger.valueOf(j1).toByteArray());
		GCSignal[] E_feInputKeys = GCUtil.selectFeKeys(predata.ur_E_feKeyPairs, R);
		GCSignal[][] E_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_E_labelKeyPairs, R);

		con1.write(j1InputKeys);
		con1.write(E_feInputKeys);
		con1.write(E_labelInputKeys);
	}

	public void runD(PreData predata, byte[] Li, Timer timer) {
		GCSignal[] j1InputKeys = con1.readObject();
		GCSignal[] E_feInputKeys = con1.readObject();
		GCSignal[][] E_labelInputKeys = con1.readObject();
		GCSignal[] C_feInputKeys = con2.readObject();
		GCSignal[][] C_labelInputKeys = con2.readObject();

		GCSignal[][] outKeys = predata.ur_gc.rootFindDeepestAndEmpty(Li, j1InputKeys, E_feInputKeys, C_feInputKeys,
				E_labelInputKeys, C_labelInputKeys);
		int j1 = GCUtil.evaOutKeys(outKeys[0], predata.ur_outKeyHashes[0]).intValue();
		int j2 = GCUtil.evaOutKeys(outKeys[1], predata.ur_outKeyHashes[1]).intValue();

		System.out.println(j1 + " " + j2);
	}

	public void runC(PreData predata, Tuple[] R, Timer timer) {
		// step 1
		GCSignal[] C_feInputKeys = GCUtil.selectFeKeys(predata.ur_C_feKeyPairs, R);
		GCSignal[][] C_labelInputKeys = GCUtil.selectLabelKeys(predata.ur_C_labelKeyPairs, R);

		con2.write(C_feInputKeys);
		con2.write(C_labelInputKeys);
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
				byte[] Li = Util.nextBytes((lBits+7)/8, Crypto.sr);
				Tuple[] R = new Tuple[sw];
				for (int j=0; j<sw; j++)
					R[j] = new Tuple(1, 2, (lBits+7)/8, 3, Crypto.sr);
				
				System.out.println("sw,lBits: " + sw + " " + lBits);
				
				con1.write(sw);
				con1.write(lBits);
				con1.write(Li);
				con2.write(sw);
				con2.write(lBits);
				
				preupdateroot.runE(predata, sw, lBits, Li, timer);
				runE(predata, Li, R, timer);
				
				int emptyIndex = 0;
				for (int j=0; j<sw; j++) {
					if (new BigInteger(R[j].getF()).testBit(0)) {
						String l = Util.addZeros(Util.getSubBits(new BigInteger(1, Util.xor(R[j].getL(), Li)), lBits, 0).toString(2), lBits);
						System.out.println(j + ":\t" + l);
					}
					else {
						emptyIndex = j;
					}
				}
				System.out.println("last empty: " + emptyIndex);

			} else if (party == Party.Debbie) {
				int sw = con1.readObject();
				int lBits = con1.readObject();
				byte[] Li = con1.read();
				
				preupdateroot.runD(predata, sw, lBits, Li, timer);
				runD(predata, Li, timer);

			} else if (party == Party.Charlie) {
				int sw = con1.readObject();
				int lBits = con1.readObject();
				Tuple[] R = new Tuple[sw];
				for (int j=0; j<sw; j++)
					R[j] = new Tuple(1, 2, lBits, 3, null);
				
				preupdateroot.runC(predata, timer);
				runC(predata, R, timer);
				
				/*
				if (output.t == index && Util.equal(output.m_t, m[index]))
					System.out.println("SSCOT test passed");
				else
					System.err.println("SSCOT test failed");
					*/

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

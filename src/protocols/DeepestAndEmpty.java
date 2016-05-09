package protocols;

import java.math.BigInteger;

import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCGenComp;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import gc.GCLib;
import oram.Forest;
import oram.Metadata;
import util.Util;

public class DeepestAndEmpty extends Protocol {

	private int d = 10;
	private int w = 8;

	public DeepestAndEmpty(Communication con1, Communication con2) {
		super(con1, con2);
	}

	private GCSignal[] revSelectKeys(GCSignal[][] pairs, byte[] input) {
		BigInteger in = new BigInteger(1, input);
		GCSignal[] out = new GCSignal[pairs.length];
		for (int i = 0; i < pairs.length; i++)
			out[i] = pairs[i][in.testBit(i) ? 1 : 0];
		return out;
	}

	private GCSignal[] selectKeys(GCSignal[][] pairs, byte[] input) {
		BigInteger in = new BigInteger(1, input);
		GCSignal[] out = new GCSignal[pairs.length];
		for (int i = 0; i < pairs.length; i++)
			out[i] = pairs[i][in.testBit(pairs.length - 1 - i) ? 1 : 0];
		return out;
	}

	private GCSignal[][] genKeyPairs(int n) {
		GCSignal[][] pairs = new GCSignal[n][];
		for (int i = 0; i < n; i++)
			pairs[i] = GCGenComp.genPair();
		return pairs;
	}

	private GCSignal[] getZeroKeys(GCSignal[][] pairs) {
		GCSignal[] keys = new GCSignal[pairs.length];
		for (int i = 0; i < keys.length; i++)
			keys[i] = pairs[i][0];
		return keys;
	}

	private BigInteger decodeOutput(GCSignal[] out1, GCSignal[] out2) {
		BigInteger output = BigInteger.ZERO;
		for (int i = 0; i < out1.length; i++) {
			if (out2[i].isPublic()) {
				if (out2[i].v)
					output = output.setBit(i);
			} else if (out1[i].equals(out2[i])) {
				;
			} else if (out1[i].equals(GCGenComp.R.xor(out2[i]))) {
				output = output.setBit(i);
			} else {
				System.err.println("ERROR on GC output!");
				return null;
			}
		}
		return output;
	}

	private String addZeros(String a, int n) {
		String out = a;
		for (int i = 0; i < n - a.length(); i++)
			out = "0" + out;
		return out;
	}

	public void runE() {
		int i = Crypto.sr.nextInt(d);
		byte[] Li = Util.nextBytes((d - 1 + 7) / 8, Crypto.sr);
		byte[] feBits = Util.nextBytes((w + 7) / 8, Crypto.sr);
		byte[][] tupleLabels = new byte[w][];
		for (int j = 0; j < w; j++)
			tupleLabels[j] = Util.nextBytes((d - 1 + 7) / 8, Crypto.sr);

		GCSignal[][] E_feKeyPairs = genKeyPairs(w);
		GCSignal[][] C_feKeyPairs = genKeyPairs(w);
		GCSignal[] E_feZeroKeys = getZeroKeys(E_feKeyPairs);
		GCSignal[] C_feZeroKeys = getZeroKeys(C_feKeyPairs);
		GCSignal[] E_feKeyInput = selectKeys(E_feKeyPairs, feBits);

		GCSignal[][][] E_labelKeyPairs = new GCSignal[w][][];
		GCSignal[][][] C_labelKeyPairs = new GCSignal[w][][];
		GCSignal[][] E_labelZeroKeys = new GCSignal[w][];
		GCSignal[][] C_labelZeroKeys = new GCSignal[w][];
		GCSignal[][] E_labelKeyInput = new GCSignal[w][];
		for (int j = 0; j < w; j++) {
			E_labelKeyPairs[j] = genKeyPairs(d - 1);
			C_labelKeyPairs[j] = genKeyPairs(d - 1);
			E_labelZeroKeys[j] = getZeroKeys(E_labelKeyPairs[j]);
			C_labelZeroKeys[j] = getZeroKeys(C_labelKeyPairs[j]);
			E_labelKeyInput[j] = revSelectKeys(E_labelKeyPairs[j], tupleLabels[j]);
		}

		con1.write(i);
		con1.write(Li);
		con1.write(E_feKeyInput);
		con1.write(C_feZeroKeys);
		con1.write(E_labelKeyInput);
		con1.write(C_labelZeroKeys);

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel);
		GCSignal[][] E_out = new GCLib<GCSignal>(gen, d, w).findDeepestAndEmpty(i, Li, E_feZeroKeys, C_feZeroKeys,
				E_labelZeroKeys, C_labelZeroKeys);

		GCSignal[][] D_out = con1.readObject();

		int l = decodeOutput(E_out[0], D_out[0]).intValue();
		int j1 = decodeOutput(E_out[1], D_out[1]).intValue();
		int j2 = decodeOutput(E_out[2], D_out[2]).intValue();
		int et = decodeOutput(E_out[3], D_out[3]).intValue();

		System.out.println("i=" + i);

		System.out.print("full tuples: ");
		BigInteger fe = new BigInteger(1, feBits);
		for (int j = 0; j < w; j++)
			if (fe.testBit(w - 1 - j))
				System.out.print(j + " ");
		System.out.println();
		System.out.print("empty tuples: ");
		for (int j = 0; j < w; j++)
			if (!fe.testBit(w - 1 - j))
				System.out.print(j + " ");
		System.out.println();

		System.out.println("tuple labels xor path label:");
		BigInteger pathLabel = new BigInteger(1, Li);
		for (int j = 0; j < w; j++) {
			BigInteger xor = Util.getSubBits(new BigInteger(1, tupleLabels[j]).xor(pathLabel), d - 1, 0);
			System.out.println(j + ": " + addZeros(xor.toString(2), d - 1));
		}

		System.out.println();
		System.out.println("deepest level: " + l);
		System.out.println("deepest tuple index: " + j1);
		System.out.println("empty tuple index: " + j2);
		System.out.println("empty tuple exists: " + et);
	}

	public void runD() {
		int i = con1.readObject();
		byte[] Li = con1.read();
		GCSignal[] E_feKeyInput = con1.readObject();
		GCSignal[] C_feZeroKeys = con1.readObject();
		GCSignal[][] E_labelKeyInput = con1.readObject();
		GCSignal[][] C_labelZeroKeys = con1.readObject();

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> gen = new GCEva(channel);
		GCLib<GCSignal> dae = new GCLib<GCSignal>(gen, d, w);
		dae.findDeepestAndEmpty(i, Li, E_feKeyInput, C_feZeroKeys, E_labelKeyInput, C_labelZeroKeys);

		gen.setEvaluate();
		GCSignal[][] D_out = dae.findDeepestAndEmpty(i, Li, E_feKeyInput, C_feZeroKeys, E_labelKeyInput,
				C_labelZeroKeys);

		con1.write(D_out);
	}

	public void runC() {

	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {

		if (party == Party.Eddie) {
			runE();

		} else if (party == Party.Debbie) {
			runD();

		} else if (party == Party.Charlie) {
			runC();

		} else {
			throw new NoSuchPartyException(party + "");
		}
	}
}

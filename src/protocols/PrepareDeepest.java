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

public class PrepareDeepest extends Protocol {

	private int d = 10;
	private int w = 8;
	private int logD = (int) Math.ceil(Math.log(d) / Math.log(2));

	public PrepareDeepest(Communication con1, Communication con2) {
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
		byte[] Li = Util.nextBytes((d - 1 + 7) / 8, Crypto.sr);

		byte[][] feBits = new byte[d][];
		byte[][][] tupleLabels = new byte[d][w][];

		GCSignal[][] LiKeyPairs = genKeyPairs(d - 1);
		GCSignal[] LiZeroKeys = getZeroKeys(LiKeyPairs);
		GCSignal[] LiKeyInput = revSelectKeys(LiKeyPairs, Li);

		GCSignal[][][] E_feKeyPairs = new GCSignal[d][][];
		GCSignal[][][] C_feKeyPairs = new GCSignal[d][][];
		GCSignal[][] E_feZeroKeys = new GCSignal[d][];
		GCSignal[][] C_feZeroKeys = new GCSignal[d][];
		GCSignal[][] E_feKeyInput = new GCSignal[d][];

		GCSignal[][][][] E_labelKeyPairs = new GCSignal[d][w][][];
		GCSignal[][][][] C_labelKeyPairs = new GCSignal[d][w][][];
		GCSignal[][][] E_labelZeroKeys = new GCSignal[d][w][];
		GCSignal[][][] C_labelZeroKeys = new GCSignal[d][w][];
		GCSignal[][][] E_labelKeyInput = new GCSignal[d][w][];

		for (int i = 0; i < d; i++) {
			feBits[i] = Util.nextBytes((w + 7) / 8, Crypto.sr);
			for (int j = 0; j < w; j++)
				tupleLabels[i][j] = Util.nextBytes((d - 1 + 7) / 8, Crypto.sr);

			E_feKeyPairs[i] = genKeyPairs(w);
			C_feKeyPairs[i] = genKeyPairs(w);
			E_feZeroKeys[i] = getZeroKeys(E_feKeyPairs[i]);
			C_feZeroKeys[i] = getZeroKeys(C_feKeyPairs[i]);
			E_feKeyInput[i] = selectKeys(E_feKeyPairs[i], feBits[i]);

			for (int j = 0; j < w; j++) {
				E_labelKeyPairs[i][j] = genKeyPairs(d - 1);
				C_labelKeyPairs[i][j] = genKeyPairs(d - 1);
				E_labelZeroKeys[i][j] = getZeroKeys(E_labelKeyPairs[i][j]);
				C_labelZeroKeys[i][j] = getZeroKeys(C_labelKeyPairs[i][j]);
				E_labelKeyInput[i][j] = revSelectKeys(E_labelKeyPairs[i][j], tupleLabels[i][j]);
			}
		}

		con1.write(LiKeyInput);
		con1.write(E_feKeyInput);
		con1.write(C_feZeroKeys);
		con1.write(E_labelKeyInput);
		con1.write(C_labelZeroKeys);

		Network channel = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(channel);
		GCSignal[][][] E_out = new GCLib<GCSignal>(gen, d, w).prepareDeepest(LiZeroKeys, E_feZeroKeys, C_feZeroKeys,
				E_labelZeroKeys, C_labelZeroKeys);

		GCSignal[][][] D_out = con1.readObject();

		int[] deepest = new int[d];
		int[] j1 = new int[d];
		int[] j2 = new int[d];
		int[] et = new int[d];

		for (int i = 0; i < d; i++) {
			deepest[i] = decodeOutput(E_out[0][i], D_out[0][i]).intValue();
			j1[i] = decodeOutput(E_out[1][i], D_out[1][i]).intValue();
			j2[i] = decodeOutput(E_out[2][i], D_out[2][i]).intValue();
			et[i] = decodeOutput(E_out[3][i], D_out[3][i]).intValue();

			System.out.println("i=" + i);

			System.out.print("full tuples: ");
			BigInteger fe = new BigInteger(1, feBits[i]);
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
				BigInteger xor = Util.getSubBits(new BigInteger(1, tupleLabels[i][j]).xor(pathLabel), d - 1, 0);
				System.out.println(j + ": " + addZeros(xor.toString(2), d - 1));
			}

			int perp = (int) (Math.pow(2, logD + 1)) - 1;

			System.out.println();
			System.out.println("deepest[i]: " + ((deepest[i] == perp) ? "\\perp" : "" + deepest[i]));
			System.out.println("j1[i]: " + j1[i]);
			System.out.println("j2[i]: " + j2[i]);
			System.out.println("et[i]: " + et[i]);

			System.out.println();
		}
	}

	public void runD() {
		GCSignal[] LiKeyInput = con1.readObject();
		GCSignal[][] E_feKeyInput = con1.readObject();
		GCSignal[][] C_feZeroKeys = con1.readObject();
		GCSignal[][][] E_labelKeyInput = con1.readObject();
		GCSignal[][][] C_labelZeroKeys = con1.readObject();

		Network channel = new Network(con1, null);
		CompEnv<GCSignal> gen = new GCEva(channel);
		GCLib<GCSignal> dae = new GCLib<GCSignal>(gen, d, w);
		dae.prepareDeepest(LiKeyInput, E_feKeyInput, C_feZeroKeys, E_labelKeyInput, C_labelZeroKeys);

		gen.setEvaluate();
		GCSignal[][][] D_out = dae.prepareDeepest(LiKeyInput, E_feKeyInput, C_feZeroKeys, E_labelKeyInput,
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

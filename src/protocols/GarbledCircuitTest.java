package protocols;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;
import com.oblivm.backend.gc.GCGenComp;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.gc.regular.GCEva;
import com.oblivm.backend.gc.regular.GCGen;
import com.oblivm.backend.network.Network;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;

public class GarbledCircuitTest extends Protocol {

	private int totalLength = 2;

	public GarbledCircuitTest(Communication con1, Communication con2) {
		super(con1, con2);
	}

	private int countOnes(boolean[] in) {
		int cnt = 0;
		for (int i = 0; i < in.length; i++)
			if (in[i])
				cnt++;
		return cnt;
	}

	private int booleansToInt(boolean[] arr) {
		int n = 0;
		for (int i = arr.length - 1; i >= 0; i--)
			n = (n << 1) | (arr[i] ? 1 : 0);
		return n;
	}

	public void runE() {
		Network w = new Network(null, con1);
		CompEnv<GCSignal> gen = new GCGen(w);

		boolean[] input1 = new boolean[totalLength];
		boolean[] input2 = new boolean[totalLength];
		boolean[] input = new boolean[totalLength];
		for (int i = 0; i < input1.length; ++i) {
			input1[i] = false;
			input2[i] = true;
			input[i] = input1[i] ^ input2[i];
		}

		GCSignal[][] inputKeyPairs1 = new GCSignal[input1.length][];
		GCSignal[] localInputKeys1 = new GCSignal[input1.length];
		GCSignal[][] inputKeyPairs2 = new GCSignal[input1.length][];
		GCSignal[] localInputKeys2 = new GCSignal[input1.length];
		GCSignal[] inputE = new GCSignal[input1.length];
		GCSignal[] inputD = new GCSignal[input1.length];
		for (int i = 0; i < input1.length; i++) {
			inputKeyPairs1[i] = GCGenComp.genPair();
			localInputKeys1[i] = inputKeyPairs1[i][0];
			inputKeyPairs2[i] = GCGenComp.genPair();
			localInputKeys2[i] = inputKeyPairs2[i][0];

			inputE[i] = input1[i] ? inputKeyPairs1[i][1] : inputKeyPairs1[i][0];
			inputD[i] = input2[i] ? inputKeyPairs2[i][1] : inputKeyPairs2[i][0];
		}

		GCSignal[] outputE = new GCSignal[] { new IntegerLib<GCSignal>(gen).less(localInputKeys1, localInputKeys2) };

		con1.write(inputE);
		con1.write(inputD);

		GCSignal[] outputD = con1.readObject();

		boolean[] output = new boolean[totalLength];
		for (int i = 0; i < outputE.length; i++) {
			if (outputE[i].isPublic())
				output[i] = outputE[i].v;
			else if (outputE[i].equals(outputD[i]))
				output[i] = false;
			else if (outputD[i].equals(GCGenComp.R.xor(outputE[i])))
				output[i] = true;
			else
				System.err.println("ERROR on GC output!");
		}

		// int inCnt = countOnes(input);
		// int outCnt = booleansToInt(output);
		// System.out.println((inCnt == outCnt) + " " + inCnt + " " + outCnt);
		System.out.println(output[0]);
	}

	public void runD() {
		Network w = new Network(con1, null);
		CompEnv<GCSignal> gen = new GCEva(w);

		GCSignal[] randomInput = new GCSignal[totalLength];
		for (int i = 0; i < randomInput.length; i++)
			randomInput[i] = GCSignal.freshLabel(Crypto.sr);
		IntegerLib<GCSignal> il = new IntegerLib<GCSignal>(gen);
		il.less(randomInput, randomInput);

		GCSignal[] inputE = con1.readObject();
		GCSignal[] inputD = con1.readObject();

		gen.setEvaluate();
		GCSignal[] outputD = new GCSignal[] { il.less(inputE, inputD) };

		con1.write(outputD);
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

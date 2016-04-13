package gc;

import java.math.BigInteger;
import java.util.Arrays;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;

public class GCLib<T> extends IntegerLib<T> {

	private int d;
	private int w;
	private int logD;
	private int logW;

	public GCLib(CompEnv<T> e, int d, int w) {
		super(e);
		this.d = d;
		this.w = w;
		logD = (int) Math.ceil(Math.log(d) / Math.log(2));
		logW = (int) Math.ceil(Math.log(w) / Math.log(2));
	}

	private void zerosFollowedByOnes(T[] input) {
		for (int i = input.length - 2; i >= 0; i--) {
			input[i] = or(input[i], input[i + 1]);
		}
	}

	public T[][] deepestAndEmptyTuples(int i, byte[] Li, T[] E_feBits, T[] C_feBits, T[][] E_tupleLabels,
			T[][] C_tupleLabels) {
		T[] pathLabel = toSignals(new BigInteger(1, Li).longValue(), d - 1); // no
																				// sign
																				// bit
		T[] feBits = xor(E_feBits, C_feBits);
		T[][] tupleLabels = env.newTArray(E_tupleLabels.length, 0);
		for (int j = 0; j < tupleLabels.length; j++)
			tupleLabels[j] = xor(E_tupleLabels[j], C_tupleLabels[j]);

		T[] l = padSignal(ones(d - 1 - i), d); // has sign bit
		T[] j1 = zeros(logW); // no sign bit
		T[] j2 = zeros(logW);
		T[] et = zeros(1);

		for (int j = 0; j < w; j++) {
			T[] tupleIndex = toSignals(j, logW);
			T[] lz = xor(pathLabel, tupleLabels[j]);
			zerosFollowedByOnes(lz);
			lz = padSignal(lz, d); // add sign bit
			T firstIf = and(feBits[j], less(lz, l));
			l = mux(l, lz, firstIf);
			j1 = mux(j1, tupleIndex, firstIf);
			et = mux(ones(1), et, feBits[j]);
			j2 = mux(tupleIndex, j2, feBits[j]);
		}

		T[] l_p = numberOfOnes(not(Arrays.copyOfRange(l, 0, d - 1))); // has
																		// sign
																		// bit

		T[][] output = env.newTArray(4, 0);
		output[0] = l_p;
		output[1] = padSignal(j1, logW + 1); // add sign bit
		output[2] = padSignal(j2, logW + 1);
		output[3] = et;
		return output;
	}

}

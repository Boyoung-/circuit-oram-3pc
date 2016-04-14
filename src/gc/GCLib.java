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

	public T[][][] prepareDeepest(byte[] Li, T[][] E_feBits, T[][] C_feBits, T[][][] E_tupleLabels,
			T[][][] C_tupleLabels) {
		T[][][] output = env.newTArray(4, d, 0);

		T[][] deepest = env.newTArray(d, 0);
		for (int j = 0; j < d; j++)
			deepest[j] = ones(logD + 1);
		T[] src = ones(logD + 1);
		T[] goal = ones(logD + 1); // -1 in 2's complement form

		for (int i = 0; i < d; i++) {
			T[] index = toSignals(i, logD + 1);
			deepest[i] = mux(deepest[i], src, geq(goal, index));

			T[][] dae = deepestAndEmptyTuples(i, Li, E_feBits[i], C_feBits[i], E_tupleLabels[i], C_tupleLabels[i]);
			T[] l = dae[0];
			output[1][i] = dae[1];
			output[2][i] = dae[2];
			output[3][i] = dae[3];

			T secondIf = greater(l, goal);
			goal = mux(goal, l, secondIf);
			src = mux(src, index, secondIf);
		}

		output[0] = deepest;
		return output;
	}

}

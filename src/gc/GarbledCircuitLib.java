package gc;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;

import crypto.Crypto;
import util.Util;

public class GarbledCircuitLib<T> extends IntegerLib<T> {

	private int d;
	private int w;
	private int logD;
	private int logW;

	public GarbledCircuitLib(CompEnv<T> e, int d, int w) {
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

	public T[][] deepestAndEmptyTuples(long i, T[] pathLabel, T[] feBits, T[][] tupleLabels) {
		T[] l = toSignals(1L << (d - 1 - i) - 1L, d - 1);
		T[] j1 = toSignals(Util.nextLong(w, Crypto.sr), logW);
		T[] j2 = toSignals(Util.nextLong(w, Crypto.sr), logW);
		T[] et = zeros(1);
		for (int j = 0; j < w; j++) {
			T[] tupleIndex = toSignals(j, logW);
			T[] lz = xor(pathLabel, tupleLabels[j]);
			zerosFollowedByOnes(lz);
			T firstIf = and(feBits[j], less(lz, l));
			l = mux(l, lz, firstIf);
			j1 = mux(j1, tupleIndex, firstIf);
			et = mux(ones(1), et, feBits[j]);
			j2 = mux(tupleIndex, j2, feBits[j]);
		}
		T[] l_p = numberOfOnes(not(l)); // TODO: set length to logD?

		T[][] output = env.newTArray(4, 0);
		output[0] = l_p;
		output[1] = j1;
		output[2] = j2;
		output[3] = et;
		return output;
	}

}

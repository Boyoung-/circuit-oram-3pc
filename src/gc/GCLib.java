package gc;

import java.math.BigInteger;
import java.util.Arrays;

import com.oblivm.backend.circuits.arithmetic.IntegerLib;
import com.oblivm.backend.flexsc.CompEnv;

public class GCLib<T> extends IntegerLib<T> {

	// TODO: take root bucket width into account

	private int d;
	private int w;
	private int logD;
	private int logW;

	public GCLib(CompEnv<T> e, int d, int w) {
		super(e);
		this.d = d;
		this.w = w;
		logD = (int) Math.ceil(Math.log(d) / Math.log(2));
		logW = (int) Math.ceil(Math.log(w + 1) / Math.log(2)); // includes fake
																// empty tuple
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
		T[] perpD = ones(logD + 1);
		T[][][] output = env.newTArray(4, d, 0);

		T[][] deepest = env.newTArray(d, 0);
		for (int j = 0; j < d; j++)
			deepest[j] = perpD;
		T[] src = perpD;
		T[] goal = perpD; // \perp = -1 in 2's complement form

		for (int i = 0; i < d; i++) {
			T[] index = toSignals(i, logD + 1);
			deepest[i] = mux(deepest[i], src, geq(goal, index));

			T[][] dae = deepestAndEmptyTuples(i, Li, E_feBits[i], C_feBits[i], E_tupleLabels[i], C_tupleLabels[i]);
			T[] l = dae[0];
			output[1][i] = dae[1];
			output[2][i] = dae[2];
			output[3][i] = dae[3];

			T lGreaterGoal = greater(l, goal);
			goal = mux(goal, l, lGreaterGoal);
			src = mux(src, index, lGreaterGoal);
		}

		output[0] = deepest;
		return output;
	}

	public T[][][] prepareTarget(T[][] deepest, T[][] j1, T[][] j2, T[][] et) {
		// TODO: root bucket ft?
		T[] ft = toSignals(w, logW + 1);
		T[] perpD = ones(logD + 1);
		T[] perpW = ones(logW + 1);
		T[][][] output = env.newTArray(3, 4, 0);

		T[] nTop = perpD;
		T[] nBot = perpD;
		T[] eTop = perpD;
		T[] eBot = perpD;
		T[] src = perpD;
		T[] dest = perpD;
		T[][] target = env.newTArray(d, 0);
		T[][] f = env.newTArray(d, 0);
		for (int i = 0; i < d; i++) {
			target[i] = perpD;
			f[i] = perpW;
		}

		for (int i = d - 1; i >= 0; i--) {
			T[] index = toSignals(i, logD + 1);

			T iEqSrc = eq(index, src);
			target[i] = mux(target[i], dest, iEqSrc);
			f[i] = mux(f[i], j1[i], iEqSrc);
			src = mux(src, perpD, iEqSrc);

			T deepestEqPerp = eq(deepest[i], perpD);
			dest = mux(dest, index, and(iEqSrc, deepestEqPerp));
			dest = mux(dest, perpD, and(iEqSrc, not(deepestEqPerp)));

			T destEqPerp = eq(dest, perpD);
			T srcEqPerp = eq(src, perpD);
			T fourthIf = and(not(deepestEqPerp), and(not(destEqPerp), srcEqPerp));
			target[i] = mux(target[i], dest, fourthIf);
			f[i] = mux(f[i], j2[i], fourthIf);

			T targetEqPerp = eq(target[i], perpD);
			T fifthIf = and(not(deepestEqPerp), or(and(destEqPerp, et[i][0]), not(targetEqPerp)));
			src = mux(src, deepest[i], fifthIf);
			dest = mux(dest, index, fifthIf);
			eTop = mux(eTop, src, fifthIf);

			T eBotEqPerp = eq(eBot, perpD);
			T sixthIf = and(fifthIf, eBotEqPerp);
			eBot = mux(eBot, dest, sixthIf);
			f[i] = mux(f[i], j2[i], sixthIf);

			T fEqPerp = eq(f[i], perpW);
			f[i] = mux(f[i], ft, fEqPerp);
			nTop = mux(nTop, index, fEqPerp);

			T nBotEqPerp = eq(nBot, perpD);
			nBot = mux(nBot, index, and(fEqPerp, nBotEqPerp));
		}

		output[0] = target;
		output[1] = f;
		output[2][0] = nTop;
		output[2][1] = nBot;
		output[2][2] = eTop;
		output[2][3] = eBot;
		return output;
	}

	public T[][][] combineDeepestAndTarget(byte[] Li, T[][] E_feBits, T[][] C_feBits, T[][][] E_tupleLabels,
			T[][][] C_tupleLabels) {
		T[][][] out = prepareDeepest(Li, E_feBits, C_feBits, E_tupleLabels, C_tupleLabels);
		return prepareTarget(out[0], out[1], out[2], out[3]);
	}

}

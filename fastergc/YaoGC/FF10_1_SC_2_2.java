// by Boyang Wei

package YaoGC;

import java.math.*;

import Cipher.*;
import sprout.oram.PID;
import sprout.oram.TID;

public abstract class FF10_1_SC_2_2 extends SimpleCircuit_2_2 {
	public FF10_1_SC_2_2() {
		super("FF10_1_SC_2_2");
	}

	public static FF10_1_SC_2_2 newInstance() {
		if (Circuit.isForGarbling)
			return new G_FF10_1_SC_2_2();
		else
			return new E_FF10_1_SC_2_2();
	}

	protected void compute() {}
	
	private int outS(int l, int r) {
		int o;
		if (l == 0) {
			if (r == 0)
				o = 0;
			else
				o = 1;
		} else {
			if (r == 0)
				o = 1;
			else
				o = 1;
		}
		return o;
	}
	
	private int outO(int l, int r) {
		int o;
		if (l == 0) {
			if (r == 0)
				o = 0;
			else
				o = 1;
		} else {
			if (r == 0)
				o = 0;
			else
				o = 0;
		}
		return o;
	}

	protected void fillTruthTable() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire0 = outputWires[0];
		Wire outWire1 = outputWires[1];

		BigInteger[] labelL = { inWireL.lbl, Wire.conjugate(inWireL.lbl) };
		if (inWireL.invd == true) {
			BigInteger tmp = labelL[0];
			labelL[0] = labelL[1];
			labelL[1] = tmp;
		}

		BigInteger[] labelR = { inWireR.lbl, Wire.conjugate(inWireR.lbl) };
		if (inWireR.invd == true) {
			BigInteger tmp = labelR[0];
			labelR[0] = labelR[1];
			labelR[1] = tmp;
		}

		int k0 = outWire0.serialNum;
		int k1 = outWire1.serialNum;

		gtt = new BigInteger[2][2];

		int cL = inWireL.lbl.testBit(0) ? 1 : 0;
		int cR = inWireR.lbl.testBit(0) ? 1 : 0;

		timing.stopwatch[PID.sha1][TID.offline].start();
		BigInteger lb = Cipher.encrypt(labelL[cL], labelR[cR], k0, k1, BigInteger.ZERO);
		timing.stopwatch[PID.sha1][TID.offline].stop();
		BigInteger[] lbS = new BigInteger[2];
		BigInteger[] lbO = new BigInteger[2];
		
		lbS[outS(cL, cR)] = lb.shiftRight(Wire.labelBitLength);
		lbO[outO(cL, cR)] = lb.and(Cipher.mask);
		lbS[1 - outS(cL, cR)] = Wire.conjugate(lbS[outS(cL, cR)]);
		lbO[1 - outO(cL, cR)] = Wire.conjugate(lbO[outO(cL, cR)]);
		
		outWire0.lbl = lbS[0];
		outWire1.lbl = lbO[0];

		gtt[0 ^ cL][0 ^ cR] = lbS[outS(0, 0)].shiftLeft(Wire.labelBitLength).xor(lbO[outO(0, 0)]);
		gtt[0 ^ cL][1 ^ cR] = lbS[outS(0, 1)].shiftLeft(Wire.labelBitLength).xor(lbO[outO(0, 1)]);
		gtt[1 ^ cL][0 ^ cR] = lbS[outS(1, 0)].shiftLeft(Wire.labelBitLength).xor(lbO[outO(1, 0)]);
		gtt[1 ^ cL][1 ^ cR] = lbS[outS(1, 1)].shiftLeft(Wire.labelBitLength).xor(lbO[outO(1, 1)]);

		int lsb = lbS[0].testBit(0) ? 1 : 0;
		if (outputWires[0].outBitEncPair != null) {
			timing.stopwatch[PID.sha1][TID.offline].start();
			outputWires[0].outBitEncPair[lsb] = Cipher.encrypt(k0, lbS[0], 0);
			outputWires[0].outBitEncPair[1 - lsb] = Cipher.encrypt(k0, lbS[1], 1);
			timing.stopwatch[PID.sha1][TID.offline].stop();
		}
		
		lsb = lbO[0].testBit(0) ? 1 : 0;
		if (outputWires[1].outBitEncPair != null) {
			timing.stopwatch[PID.sha1][TID.offline].start();
			outputWires[1].outBitEncPair[lsb] = Cipher.encrypt(k1, lbO[0], 0);
			outputWires[1].outBitEncPair[1 - lsb] = Cipher.encrypt(k1, lbO[1], 1);
			timing.stopwatch[PID.sha1][TID.offline].stop();
		}
	}

	protected boolean shortCut() {
		return false;
	}

	protected boolean collapse() {
		return false;
	}
}

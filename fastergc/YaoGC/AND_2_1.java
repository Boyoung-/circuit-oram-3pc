// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

import java.math.*;

import Cipher.*;
import sprout.oram.PID;
import sprout.oram.TID;

public abstract class AND_2_1 extends SimpleCircuit_2_1 {
	public AND_2_1() {
		super("AND_2_1");
	}

	public static AND_2_1 newInstance() {
		if (Circuit.isForGarbling)
			return new G_AND_2_1();
		else
			return new E_AND_2_1();
	}

	protected void compute() {
		int left = inputWires[0].value;
		int right = inputWires[1].value;

		outputWires[0].value = left & right;
	}

	protected void fillTruthTable() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

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

		int k = outWire.serialNum;

		gtt = new BigInteger[2][2];

		int cL = inWireL.lbl.testBit(0) ? 1 : 0;
		int cR = inWireR.lbl.testBit(0) ? 1 : 0;

		BigInteger[] lb = new BigInteger[2];
		timing.stopwatch[PID.sha1][TID.offline].start();
		lb[cL & cR] = Cipher.encrypt(labelL[cL], labelR[cR], k, BigInteger.ZERO);
		timing.stopwatch[PID.sha1][TID.offline].stop();
		lb[1 - (cL & cR)] = Wire.conjugate(lb[cL & cR]);
		outWire.lbl = lb[0];

		gtt[0 ^ cL][0 ^ cR] = lb[0];
		gtt[0 ^ cL][1 ^ cR] = lb[0];
		gtt[1 ^ cL][0 ^ cR] = lb[0];
		gtt[1 ^ cL][1 ^ cR] = lb[1];

		int lsb = lb[0].testBit(0) ? 1 : 0;
		if (outputWires[0].outBitEncPair != null) {
			timing.stopwatch[PID.sha1][TID.offline].start();
			outputWires[0].outBitEncPair[lsb] = Cipher.encrypt(k, lb[0], 0);
			outputWires[0].outBitEncPair[1 - lsb] = Cipher.encrypt(k, lb[1], 1);
			timing.stopwatch[PID.sha1][TID.offline].stop();
		}
	}

	protected boolean shortCut() {
		if (inputWires[0].value == 0) {
			outputWires[0].value = 0;
			return true;
		}

		if (inputWires[1].value == 0) {
			outputWires[0].value = 0;
			return true;
		}

		return false;
	}

	protected boolean collapse() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (inWireL.lbl.equals(inWireR.lbl)) {
			if (inWireL.invd == inWireR.invd) {
				outWire.invd = inWireL.invd;
				outWire.setLabel(inWireL.lbl);
			} else {
				outWire.invd = false;
				outWire.value = 0;
			}

			return true;
		}

		return false;
	}
}

// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

import java.math.*;

import sprout.oram.PID;
import sprout.oram.TID;
import Cipher.Cipher;

public abstract class SimpleCircuit_2_1 extends Circuit {

	protected BigInteger[][] gtt;

	public SimpleCircuit_2_1(String name) {
		super(2, 1, name);
	}

	public void build() throws Exception {
		createInputWires();
		createOutputWires();
	}

	protected void createInputWires() {
		super.createInputWires();

		for (int i = 0; i < inDegree; i++)
			inputWires[i].addObserver(this, new TransitiveObservable.Socket(
					inputWires, i));
	}

	protected void createOutputWires() {
		outputWires[0] = new Wire();
	}

	protected void execute(boolean evaluate) {

		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		if (evaluate) {
			if (inWireL.value != Wire.UNKNOWN_SIG
					&& inWireR.value != Wire.UNKNOWN_SIG) {
				compute();
			} else if (inWireL.value != Wire.UNKNOWN_SIG) {
				if (shortCut())
					outWire.invd = false;
				else {
					outWire.value = Wire.UNKNOWN_SIG;
					outWire.invd = inWireR.invd;
					outWire.setLabel(inWireR.lbl);
				}
			} else if (inWireR.value != Wire.UNKNOWN_SIG) {
				if (shortCut())
					outWire.invd = false;
				else {
					outWire.value = Wire.UNKNOWN_SIG;
					outWire.invd = inWireL.invd;
					outWire.setLabel(inWireL.lbl);
				}
			} else {
				outWire.value = Wire.UNKNOWN_SIG;
				outWire.invd = false;

				if (collapse()) {
					System.err
							.println("Same labels detected! Please check label generation.");
				} else {
					execYao();
				}
			}
		} else
			passTruthTable();

		outWire.setReady(evaluate);
	}

	protected abstract void execYao();

	protected abstract void passTruthTable();

	protected abstract boolean shortCut();

	protected abstract boolean collapse();

	protected void sendGTT() {
		timing.stopwatch[PID.gcf][TID.offline].stop();

		timing.stopwatch[PID.gcf][TID.offline_write].start();
		receiver.write(gtt[0][1]);
		receiver.write(gtt[1][0]);
		receiver.write(gtt[1][1]);

		if (outputWires[0].outBitEncPair != null)
			receiver.write(outputWires[0].outBitEncPair);
		timing.stopwatch[PID.gcf][TID.offline_write].stop();

		timing.stopwatch[PID.gcf][TID.offline].start();
	}

	protected void receiveGTT() {
		try {
			gtt = new BigInteger[2][2];
			gtt[0][0] = BigInteger.ZERO;
			timing.stopwatch[PID.gcf][TID.offline].stop();

			timing.stopwatch[PID.gcf][TID.offline_read].start();
			gtt[0][1] = sender.readBigInteger();
			gtt[1][0] = sender.readBigInteger();
			gtt[1][1] = sender.readBigInteger();

			if (outputWires[0].outBitEncPair != null)
				outputWires[0].outBitEncPair = sender.readBigIntegerArray();
			timing.stopwatch[PID.gcf][TID.offline_read].stop();

			timing.stopwatch[PID.gcf][TID.offline].start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected void encryptTruthTable() {
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

		int cL = inWireL.lbl.testBit(0) ? 1 : 0;
		int cR = inWireR.lbl.testBit(0) ? 1 : 0;

		timing.stopwatch[PID.sha1][TID.offline].start();
		if (cL != 0 || cR != 0)
			gtt[0 ^ cL][0 ^ cR] = Cipher.encrypt(labelL[0], labelR[0], k,
					gtt[0 ^ cL][0 ^ cR]);
		if (cL != 0 || cR != 1)
			gtt[0 ^ cL][1 ^ cR] = Cipher.encrypt(labelL[0], labelR[1], k,
					gtt[0 ^ cL][1 ^ cR]);
		if (cL != 1 || cR != 0)
			gtt[1 ^ cL][0 ^ cR] = Cipher.encrypt(labelL[1], labelR[0], k,
					gtt[1 ^ cL][0 ^ cR]);
		if (cL != 1 || cR != 1)
			gtt[1 ^ cL][1 ^ cR] = Cipher.encrypt(labelL[1], labelR[1], k,
					gtt[1 ^ cL][1 ^ cR]);
		timing.stopwatch[PID.sha1][TID.offline].stop();
	}
}

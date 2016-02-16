// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

import java.math.BigInteger;

import Cipher.Cipher;
import sprout.oram.PID;
import sprout.oram.TID;

class E_FF10_1_SC_2_2 extends FF10_1_SC_2_2 {
	public E_FF10_1_SC_2_2() {
		super();
	}

	protected void execYao() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire0 = outputWires[0];
		Wire outWire1 = outputWires[1];

		// receiveGTT();

		int i0 = Wire.getLSB(inWireL.lbl);
		i0 = inWireL.invd ? (1 - i0) : i0;
		int i1 = Wire.getLSB(inWireR.lbl);
		i1 = inWireR.invd ? (1 - i1) : i1;
		
		int k0 = outWire0.serialNum;
		int k1 = outWire1.serialNum;

		timing.stopwatch[PID.sha1][TID.online].start();
		BigInteger out = Cipher.decrypt(inWireL.lbl, inWireR.lbl,
				k0, k1, gtt[i0][i1]);
		timing.stopwatch[PID.sha1][TID.online].stop();

		outWire0.setLabel(out.shiftRight(Wire.labelBitLength));
		outWire1.setLabel(out.and(Cipher.mask));
	}

	@Override
	protected void passTruthTable() {
		receiveGTT();
	}
}

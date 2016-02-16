// by Boyang Wei

package YaoGC;

import java.math.*;

import Cipher.*;
import sprout.oram.PID;
import sprout.oram.TID;

class E_T1_FF0_S_2_1 extends T1_FF0_S_2_1 {
	public E_T1_FF0_S_2_1() {
		super();
	}

	protected void execYao() {
		Wire inWireL = inputWires[0];
		Wire inWireR = inputWires[1];
		Wire outWire = outputWires[0];

		// receiveGTT();

		int i0 = Wire.getLSB(inWireL.lbl);
		i0 = inWireL.invd ? (1 - i0) : i0;
		int i1 = Wire.getLSB(inWireR.lbl);
		i1 = inWireR.invd ? (1 - i1) : i1;

		timing.stopwatch[PID.sha1][TID.online].start();
		BigInteger out = Cipher.decrypt(inWireL.lbl, inWireR.lbl,
				outWire.serialNum, gtt[i0][i1]);
		timing.stopwatch[PID.sha1][TID.online].stop();

		outWire.setLabel(out);
	}

	@Override
	protected void passTruthTable() {
		receiveGTT();
	}
}

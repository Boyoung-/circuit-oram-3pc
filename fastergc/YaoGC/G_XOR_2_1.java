// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

import java.math.BigInteger;

import sprout.oram.PID;
import sprout.oram.TID;
import Cipher.Cipher;

class G_XOR_2_1 extends XOR_2_1 {
	public G_XOR_2_1() {
		super();
	}

	protected void sendOutBitEncPair() {
		if (outputWires[0].outBitEncPair != null) {
			BigInteger[] lb = new BigInteger[2];
			lb[0] = outputWires[0].lbl;
			lb[1] = Wire.conjugate(lb[0]);
			int lsb = lb[0].testBit(0) ? 1 : 0;
			int k = outputWires[0].serialNum;
			timing.stopwatch[PID.sha1][TID.offline].start();
			outputWires[0].outBitEncPair[lsb] = Cipher.encrypt(k, lb[0], 0);
			outputWires[0].outBitEncPair[1 - lsb] = Cipher.encrypt(k, lb[1], 1);
			timing.stopwatch[PID.sha1][TID.offline].stop();
			timing.stopwatch[PID.gcf][TID.offline].stop();

			timing.stopwatch[PID.gcf][TID.offline_write].start();
			receiver.write(outputWires[0].outBitEncPair);
			timing.stopwatch[PID.gcf][TID.offline_write].stop();

			timing.stopwatch[PID.gcf][TID.offline].start();
		}
	}
}

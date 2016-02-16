// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

import sprout.oram.PID;
import sprout.oram.TID;

class E_XOR_2_1 extends XOR_2_1 {
	public E_XOR_2_1() {
		super();
	}

	protected void sendOutBitEncPair() {
		if (outputWires[0].outBitEncPair != null) {
			timing.stopwatch[PID.gcf][TID.offline].stop();

			timing.stopwatch[PID.gcf][TID.offline_read].start();
			outputWires[0].outBitEncPair = sender.readBigIntegerArray();
			timing.stopwatch[PID.gcf][TID.offline_read].stop();

			timing.stopwatch[PID.gcf][TID.offline].start();
		}
	}
}

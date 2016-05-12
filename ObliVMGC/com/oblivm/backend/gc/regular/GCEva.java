package com.oblivm.backend.gc.regular;

import com.oblivm.backend.flexsc.Flag;
import com.oblivm.backend.flexsc.Mode;
import com.oblivm.backend.gc.GCEvaComp;
import com.oblivm.backend.gc.GCSignal;
import com.oblivm.backend.network.Network;

import util.Timer;

public class GCEva extends GCEvaComp {
	Garbler gb;
	GCSignal[][] gtt = new GCSignal[2][2];

	boolean evaluate = false;
	GCEva next = null;
	GCEva curr = null;

	Timer timer = null;
	int p;
	int m;

	public GCEva(Network channel) {
		super(channel, Mode.REAL);
		gb = new Garbler();
		gtt[0][0] = GCSignal.ZERO;
		gtt[0][1] = GCSignal.newInstance(new byte[10]);
		gtt[1][0] = GCSignal.newInstance(new byte[10]);
		gtt[1][1] = GCSignal.newInstance(new byte[10]);
	}

	public GCEva(Network channel, Timer timer, int p, int m) {
		super(channel, Mode.REAL);
		gb = new Garbler();
		gtt[0][0] = GCSignal.ZERO;
		gtt[0][1] = GCSignal.newInstance(new byte[10]);
		gtt[1][0] = GCSignal.newInstance(new byte[10]);
		gtt[1][1] = GCSignal.newInstance(new byte[10]);

		this.timer = timer;
		this.p = p;
		this.m = m;
	}

	public void setEvaluate() {
		evaluate = true;
		curr = this;
	}

	private void receiveGTT() {
		try {
			if (timer != null)
				timer.start(p, m);

			Flag.sw.startGCIO();
			GCSignal.receive(channel, gtt[0][1]);
			GCSignal.receive(channel, gtt[1][0]);
			GCSignal.receive(channel, gtt[1][1]);
			// gtt[1][0] = GCSignal.receive(channel);
			// gtt[1][1] = GCSignal.receive(channel);
			Flag.sw.stopGCIO();

			if (timer != null)
				timer.stop(p, m);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public GCSignal and(GCSignal a, GCSignal b) {
		Flag.sw.startGC();

		GCSignal res;
		if (a.isPublic() && b.isPublic())
			res = new GCSignal(a.v && b.v);
		else if (a.isPublic())
			res = a.v ? b : new GCSignal(false);
		else if (b.isPublic())
			res = b.v ? a : new GCSignal(false);
		else {
			res = new GCSignal(new byte[10]);
			if (!evaluate) {
				++numOfAnds;

				if (curr == null) {
					curr = this;
				} else {
					curr.next = new GCEva(channel);
					curr = curr.next;
				}
				curr.receiveGTT();
			} else {
				int i0 = a.getLSB();
				int i1 = b.getLSB();
				gb.dec(a, b, gid, curr.gtt[i0][i1], res);
				curr = curr.next;
				gid++;
			}
		}
		Flag.sw.stopGC();
		return res;
	}
}
package util;

import exceptions.BandwidthException;

public class Bandwidth {

	public String task;
	public int bandwidth;

	public Bandwidth(String t) {
		task = t;
		bandwidth = 0;
	}

	public Bandwidth(Bandwidth b) {
		task = b.task;
		bandwidth = b.bandwidth;
	}

	public void reset() {
		bandwidth = 0;
	}

	public void add(int n) {
		bandwidth += n;
	}

	public void add(Bandwidth b) {
		if (!task.equals(b.task))
			throw new BandwidthException("Task: " + task + " != " + b.task);

		bandwidth = bandwidth + b.bandwidth;
	}

	public String noPreToString() {
		return "" + bandwidth;
	}

	@Override
	public String toString() {
		return task + "(bytes): " + bandwidth;
	}
}

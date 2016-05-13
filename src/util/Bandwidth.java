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

	public Bandwidth add(Bandwidth b) {
		if (!task.equals(b.task))
			throw new BandwidthException("Task: " + task + " != " + b.task);
		Bandwidth total = new Bandwidth(task);
		total.bandwidth = bandwidth + b.bandwidth;
		return total;
	}

	public String noPreToString() {
		return "" + bandwidth;
	}

	@Override
	public String toString() {
		return task + "(bytes): " + bandwidth;
	}
}

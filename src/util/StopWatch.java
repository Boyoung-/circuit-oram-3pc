package util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import exceptions.StopWatchException;

public class StopWatch {

	private long startWC;
	private long startCPU;
	public long elapsedWC;
	public long elapsedCPU;
	private boolean isOn;

	public StopWatch() {
		startWC = 0;
		startCPU = 0;
		elapsedWC = 0;
		elapsedCPU = 0;
		isOn = false;
	}

	public void reset() {
		if (isOn) {
			try {
				throw new StopWatchException("StopWatch is still running");
			} catch (StopWatchException e) {
				e.printStackTrace();
			}
		}

		startWC = 0;
		startCPU = 0;
		elapsedWC = 0;
		elapsedCPU = 0;
	}

	public void start() {
		if (isOn) {
			try {
				throw new StopWatchException("StopWatch is already running");
			} catch (StopWatchException e) {
				e.printStackTrace();
			}
		}

		isOn = true;
		startWC = System.nanoTime();
		startCPU = getCPUTime();
	}

	public void stop() {
		if (!isOn) {
			try {
				throw new StopWatchException("StopWatch is not running");
			} catch (StopWatchException e) {
				e.printStackTrace();
			}
		}

		isOn = false;
		elapsedCPU += getCPUTime() - startCPU;
		elapsedWC += System.nanoTime() - startWC;
	}

	private long getCPUTime() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
	}

	public String toMS() {
		String out = "WallClock(ms): " + elapsedWC / 1000000;
		out += "\nCPUClock(ms): " + elapsedCPU / 1000000;
		return out;
	}
}

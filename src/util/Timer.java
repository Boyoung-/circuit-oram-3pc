package util;

import java.util.Stack;

import exceptions.TimerException;

public class Timer {
	StopWatch[] watches;
	Stack<StopWatch> stack;

	public Timer() {
		watches = new StopWatch[M.size];
		for (int j = 0; j < M.size; j++)
			watches[j] = new StopWatch(M.names[j]);
		stack = new Stack<StopWatch>();
	}

	public Timer(StopWatch[] sws) {
		watches = sws;
		stack = new Stack<StopWatch>();
	}

	public void start(int m) {
		if (!stack.empty()) {
			if (stack.peek() == watches[m])
				throw new TimerException("Stopwatch already added to stack");
			stack.peek().stop();
		}
		stack.push(watches[m]).start();

	}

	public void stop(int m) {
		if (stack.empty())
			throw new TimerException("No stopwatch running");
		if (stack.peek() != watches[m])
			throw new TimerException("Wrong Stopwatch to stop");
		stack.pop().stop();
		if (!stack.empty())
			stack.peek().start();
	}

	public void reset() {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			watches[i].reset();
	}

	public void print() {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			System.out.println(watches[i].toMS());
	}

	public void noPrePrint() {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			System.out.println(watches[i].noPreToMS());
	}

	public Timer divideBy(int n) {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		StopWatch[] sws = new StopWatch[M.size];
		for (int i = 0; i < watches.length; i++)
			sws[i] = watches[i].divideBy(n);
		return new Timer(sws);
	}

	public Timer add(Timer t) {
		if (!stack.empty() || !t.stack.empty())
			throw new TimerException("Stack not empty");

		StopWatch[] sws = new StopWatch[M.size];
		for (int i = 0; i < watches.length; i++)
			sws[i] = watches[i].add(t.watches[i]);
		return new Timer(sws);
	}
}

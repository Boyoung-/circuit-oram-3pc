package util;

import java.util.Stack;

import exceptions.TimerException;

public class Timer {
	StopWatch[][] watches;
	Stack<StopWatch> stack;

	public Timer() {
		watches = new StopWatch[P.size][M.size];
		for (int i = 0; i < P.size; i++)
			for (int j = 0; j < M.size; j++)
				watches[i][j] = new StopWatch(P.names[i] + "_" + M.names[j]);
		stack = new Stack<StopWatch>();
	}

	public Timer(StopWatch[][] sws) {
		watches = sws;
		stack = new Stack<StopWatch>();
	}

	public void start(int p, int m) {
		if (!stack.empty()) {
			if (stack.peek() == watches[p][m])
				throw new TimerException("Stopwatch already added to stack");
			stack.peek().stop();
		}
		stack.push(watches[p][m]).start();
	}

	public void stop(int p, int m) {
		if (stack.empty())
			throw new TimerException("No stopwatch found");
		stack.pop().stop();
		if (!stack.empty())
			stack.peek().start();
	}

	public void reset() {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			for (int j = 0; j < watches[i].length; j++)
				watches[i][j].reset();
	}

	public void print() {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			for (int j = 0; j < watches[i].length; j++)
				System.out.println(watches[i][j].toMS());
	}

	public Timer divideBy(int n) {
		if (!stack.empty())
			throw new TimerException("Stack not empty");
		StopWatch[][] sws = new StopWatch[P.size][M.size];
		for (int i = 0; i < watches.length; i++)
			for (int j = 0; j < watches[i].length; j++)
				sws[i][j] = watches[i][j].divideBy(n);
		return new Timer(sws);
	}
}

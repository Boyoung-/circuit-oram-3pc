package util;

import java.util.Stack;

import exceptions.TimingException;

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

	public void start(int p, int m) {
		if (!stack.empty()) {
			if (stack.peek() == watches[p][m])
				throw new TimingException("Stopwatch already added to stack");
			stack.peek().stop();
		}
		stack.push(watches[p][m]).start();
	}

	public void stop(int p, int m) {
		if (stack.empty())
			throw new TimingException("No stopwatch found");
		stack.pop().stop();
		if (!stack.empty())
			stack.peek().start();
	}

	public void print() {
		if (!stack.empty())
			throw new TimingException("Stack not empty");
		for (int i = 0; i < watches.length; i++)
			for (int j = 0; j < watches[i].length; j++)
				System.out.println(watches[i][j].toMS());
	}
}

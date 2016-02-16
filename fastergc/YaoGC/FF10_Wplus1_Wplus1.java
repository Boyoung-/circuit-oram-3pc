// by Boyang Wei

package YaoGC;

public class FF10_Wplus1_Wplus1 extends CompositeCircuit {
	private final int w;
	private final boolean b; // find 1 or find 0
	private int s; // sigma

	public FF10_Wplus1_Wplus1(int w, boolean b, int s) {
		super(w + 1, w + 1, w * 3, "FindFirstZeroOrOne_" + (w + 1) + "_" + w
				+ 1);

		this.w = w;
		this.b = b;
		this.s = s;
	}

	protected void createSubCircuits() throws Exception {
		for (int i = 0; i < w * 2; i++) {
			if (s <= i && i <= w + s - 1) {
				if (b)
					subCircuits[i] = new FF10_2_2(1);
				else
					subCircuits[i] = new FF10_2_2(2);
			} else { // i < s || w+s-1 < i
				subCircuits[i] = new FF10_2_2(3);
			}
		}

		for (int i = 2 * w; i < w * 3; i++)
			subCircuits[i] = XOR_2_1.newInstance();

		s = 0;

		super.createSubCircuits();
	}

	protected void connectWires() throws Exception {
		inputWires[0].connectTo(subCircuits[0].inputWires, 0); // Enable wire

		for (int i = 0; i < w; i++) {
			inputWires[i + 1].connectTo(subCircuits[i].inputWires, 1); // green
																		// wire
			inputWires[i + 1].connectTo(subCircuits[i + w].inputWires, 1); // green
																			// wire
			subCircuits[i].outputWires[1].connectTo(
					subCircuits[i + 2 * w].inputWires, 0); // orange wire
			subCircuits[i + w].outputWires[1].connectTo(
					subCircuits[i + 2 * w].inputWires, 1); // orange wire
		}

		for (int i = 0; i < 2 * w - 1; i++)
			subCircuits[i].outputWires[0].connectTo(
					subCircuits[i + 1].inputWires, 0); // blue wire
	}

	protected void defineOutputWires() {
		for (int i = 0; i < w; i++)
			outputWires[i] = subCircuits[i + 2 * w].outputWires[0]; // orange
																	// wire

		outputWires[w] = subCircuits[2 * w - 1].outputWires[0]; // Success wire
	}
}
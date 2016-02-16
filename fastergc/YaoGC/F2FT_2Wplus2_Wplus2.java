// by Boyang Wei

package YaoGC;

public class F2FT_2Wplus2_Wplus2 extends CompositeCircuit {
	private final int w;
	private int s1; // sigma
	private int s2;

	public F2FT_2Wplus2_Wplus2(int w, int s1, int s2) {
		super(2 * w + 2, w + 2, 6 * w + 4, "F2FT_" + (2 * w + 2) + "_" + w + 2);

		this.w = w;
		this.s1 = s1;
		this.s2 = s2;
	}

	protected void createSubCircuits() throws Exception {
		for (int i = 0; i < w; i++)
			subCircuits[i] = AND_2_1.newInstance();
		for (int i = w; i < 6 * w; i++)
			subCircuits[i] = XOR_2_1.newInstance();

		subCircuits[6 * w] = new FF10_Wplus1_Wplus1(w, true, s1);
		subCircuits[6 * w + 1] = new FF10_Wplus1_Wplus1(w, false, s1);
		subCircuits[6 * w + 2] = new FF10_Wplus1_Wplus1(w, true, s2);
		subCircuits[6 * w + 3] = new FF10_Wplus1_Wplus1(w, false, s2);

		s1 = s2 = 0;

		super.createSubCircuits();
	}

	protected void connectWires() throws Exception {
		inputWires[0].connectTo(subCircuits[6 * w].inputWires, 0);
		inputWires[1].connectTo(subCircuits[6 * w + 2].inputWires, 0);
		subCircuits[6 * w].outputWires[w].connectTo(
				subCircuits[6 * w + 1].inputWires, 0);
		subCircuits[6 * w + 2].outputWires[w].connectTo(
				subCircuits[6 * w + 3].inputWires, 0);

		for (int i = 0; i < w; i++) {
			inputWires[i + 2].connectTo(subCircuits[i].inputWires, 0);
			inputWires[i + w + 2].connectTo(subCircuits[i].inputWires, 1);
			inputWires[i + w + 2].connectTo(subCircuits[6 * w + 1].inputWires,
					i + 1);
			inputWires[i + w + 2].connectTo(subCircuits[i + 2 * w].inputWires,
					1);
			subCircuits[i].outputWires[0].connectTo(
					subCircuits[6 * w].inputWires, i + 1);
			subCircuits[i].outputWires[0].connectTo(
					subCircuits[i + w].inputWires, 1);
			subCircuits[6 * w].outputWires[i].connectTo(
					subCircuits[i + w].inputWires, 0);
			subCircuits[6 * w].outputWires[i].connectTo(
					subCircuits[i + 3 * w].inputWires, 1);
			subCircuits[6 * w + 1].outputWires[i].connectTo(subCircuits[i + 2
					* w].inputWires, 0);
			subCircuits[6 * w + 1].outputWires[i].connectTo(subCircuits[i + 3
					* w].inputWires, 0);
			subCircuits[i + w].outputWires[0].connectTo(
					subCircuits[6 * w + 2].inputWires, i + 1);
			subCircuits[i + 2 * w].outputWires[0].connectTo(
					subCircuits[6 * w + 3].inputWires, i + 1);
			subCircuits[6 * w + 2].outputWires[i].connectTo(subCircuits[i + 4
					* w].inputWires, 0);
			subCircuits[6 * w + 3].outputWires[i].connectTo(subCircuits[i + 4
					* w].inputWires, 1);
			subCircuits[i + 3 * w].outputWires[0].connectTo(subCircuits[i + 5
					* w].inputWires, 1);
			subCircuits[i + 4 * w].outputWires[0].connectTo(subCircuits[i + 5
					* w].inputWires, 0);
		}
	}

	protected void defineOutputWires() {
		outputWires[0] = subCircuits[6 * w + 1].outputWires[w];
		outputWires[1] = subCircuits[6 * w + 3].outputWires[w];

		for (int i = 0; i < w; i++)
			outputWires[i + 2] = subCircuits[i + 5 * w].outputWires[0];
	}
}
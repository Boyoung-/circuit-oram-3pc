// by Boyang Wei
// FindFirst1 Garbled Gate of Table 1

package YaoGC;

public class FF10_2_2 extends CompositeCircuit {
	private int id; // 1, 2, or 3
	
	public FF10_2_2(int id) {
		super(2, 2, 1, "FF10_2_2");

		this.id = id;
	}

	protected void createSubCircuits() throws Exception {
		if (id == 1) {
			subCircuits[0] = FF10_1_SC_2_2.newInstance();
		} else if (id == 2) {
			subCircuits[0] = FF10_2_SC_2_2.newInstance();
		} else {
			subCircuits[0] = FF10_3_SC_2_2.newInstance();
		}
		
		id = -1;

		super.createSubCircuits();
	}

	protected void connectWires() throws Exception {
		inputWires[0].connectTo(subCircuits[0].inputWires, 0);
		inputWires[1].connectTo(subCircuits[0].inputWires, 1);
	}

	protected void defineOutputWires() {
		outputWires[0] = subCircuits[0].outputWires[0];
		outputWires[1] = subCircuits[0].outputWires[1];
	}

	/*
	public FF10_2_2(int id) {
		super(2, 2, 2, "FF10_2_2");

		this.id = id;
	}

	protected void createSubCircuits() throws Exception {
		if (id == 1) {
			subCircuits[0] = T1_FF1_S_2_1.newInstance();
			subCircuits[1] = T1_FF1_O_2_1.newInstance();
		} else if (id == 2) {
			subCircuits[0] = T1_FF0_S_2_1.newInstance();
			subCircuits[1] = T1_FF0_O_2_1.newInstance();
		} else {
			subCircuits[0] = T2_FF10_S_2_1.newInstance();
			subCircuits[1] = T2_FF10_O_2_1.newInstance();
		}
		
		id = -1;

		super.createSubCircuits();
	}

	protected void connectWires() throws Exception {
		inputWires[0].connectTo(subCircuits[0].inputWires, 0);
		inputWires[0].connectTo(subCircuits[1].inputWires, 0);
		inputWires[1].connectTo(subCircuits[0].inputWires, 1);
		inputWires[1].connectTo(subCircuits[1].inputWires, 1);
	}

	protected void defineOutputWires() {
		outputWires[0] = subCircuits[0].outputWires[0];
		outputWires[1] = subCircuits[1].outputWires[0];
	}
	*/

	protected void fixInternalWires() {
	}
}
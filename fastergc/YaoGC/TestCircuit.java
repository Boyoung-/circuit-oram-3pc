// by Boyang Wei

package YaoGC;

public class TestCircuit extends CompositeCircuit {
	public TestCircuit() {
		super(2, 2, 1, "TestCircuit");
	}

	protected void createSubCircuits() throws Exception {
		subCircuits[0] = new FF10_2_2(1);

		super.createSubCircuits();
	}

	protected void connectWires() throws Exception {
		// Enable wires
		inputWires[0].connectTo(subCircuits[0].inputWires, 0);
		inputWires[1].connectTo(subCircuits[0].inputWires, 1);
	}

	protected void defineOutputWires() {
		outputWires[0] = subCircuits[0].outputWires[0];
		outputWires[1] = subCircuits[0].outputWires[1];
	}

	/*
	 * public void sendOutBitsLookup(boolean send) { //sendOutBitsLookup = send;
	 * if (send) for (int i=0; i<outputWires.length; i++)
	 * outputWires[i].outBitEncPair = new BigInteger[2]; else for (int i=0;
	 * i<outputWires.length; i++) outputWires[i].outBitEncPair = null; }
	 */
}
// by Boyang Wei

package YaoGC;

class G_T2_FF10_O_2_1 extends T2_FF10_O_2_1 {
	public G_T2_FF10_O_2_1() {
		super();
	}

	protected void execYao() {
		// fillTruthTable();
		// encryptTruthTable();
		// sendGTT();
		// gtt = null;
	}

	@Override
	protected void passTruthTable() {
		fillTruthTable();
		encryptTruthTable();
		sendGTT();
		gtt = null;
	}
}

// Copyright (C) 2010 by Yan Huang <yhuang@virginia.edu>

package YaoGC;

class G_FF10_2_SC_2_2 extends FF10_2_SC_2_2 {
	public G_FF10_2_SC_2_2() {
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

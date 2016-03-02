package init;

import crypto.Crypto;
import oram.Forest;
import oram.Metadata;

public class InitForest {

	public static void main(String[] args) {
		Metadata md = new Metadata();

		Forest forest = new Forest(md);
		forest.writeToFile();

		Forest share1 = forest;
		Forest share2 = new Forest(md, Crypto.sr);
		share1.setXor(share2);
		share1.writeToFile(md.getDefaultSharesName1());
		share2.writeToFile(md.getDefaultSharesName2());
	}

}

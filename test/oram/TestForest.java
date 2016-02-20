package oram;

public class TestForest {

	public static void main(String[] args) {
		Forest forest1 = new Forest();
		Forest forest2 = new Forest();
		Forest forest3 = forest1.xor(forest2);
		forest1.print();
		forest2.print();
		forest3.print();
	}

}

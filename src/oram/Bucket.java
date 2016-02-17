package oram;

public class Bucket {
	private Tree tree;
	private Tuple[] tuples;

	public Bucket(Tree tree, Tuple[] tuples) {
		this.tree = tree;
		this.tuples = tuples;
	}

	public Tuple[] getTuples() {
		return tuples;
	}

	public Tuple getTupleAt(int i) {
		return tuples[i];
	}

	public byte[] toByteArray() {
		int tupleBytes = tree.getTupleBytes();
		byte[] bucket = new byte[tupleBytes * tuples.length];
		for (int i = 0; i < tuples.length; i++) {
			byte[] tuple = tuples[i].toByteArray();
			System.arraycopy(tuple, 0, bucket, i * tupleBytes, tupleBytes);
		}
		return bucket;
	}

	@Override
	public String toString() {
		String str = "Bucket:";
		for (int i = 0; i < tuples.length; i++)
			str += ("\n  " + tuples[i]);
		return str;
	}

}

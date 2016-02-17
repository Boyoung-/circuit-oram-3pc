package oram;

public class Bucket {
	private Tuple[] tuples;

	public Bucket(int numTuples) {
		tuples = new Tuple[numTuples];
	}

	public Bucket(Tuple[] tuples) {
		this.tuples = tuples;
	}

	public int getNumTuples() {
		return tuples.length;
	}

	public Tuple[] getTuples() {
		return tuples;
	}

	public Tuple getTuple(int i) {
		return tuples[i];
	}

	public void setTuples(Tuple[] tuples) {
		this.tuples = tuples;
	}

	public void setTuple(int i, Tuple tuple) {
		tuples[i] = tuple;
	}

	public byte[] toByteArray() {
		int tupleBytes = tuples[0].getNumBytes();
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

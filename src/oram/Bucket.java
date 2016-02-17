package oram;

public class Bucket {
	private int tupleBytes;
	private Tuple[] tuples;

	public Bucket(int numTuples, int tb) {
		tupleBytes = tb;
		tuples = new Tuple[numTuples];
	}

	public Bucket(Tuple[] ts) {
		tupleBytes = ts[0].getNumBytes();
		tuples = ts;
	}

	public int getNumTuples() {
		return tuples.length;
	}

	public int getTupleBytes() {
		return tupleBytes;
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

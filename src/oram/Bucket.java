package oram;

public class Bucket {
	private Tuple[] tuples;

	public Tuple[] getTuples() {
		return tuples;
	}

	public Tuple getTupleAt(int i) {
		return tuples[i];
	}

	public byte[] toByteArray() {
		// TODO: remove this firstTuple if has access to tree metadata
		byte[] firstTuple = tuples[0].toByteArray();
		int tupleBytes = firstTuple.length;
		byte[] bucket = new byte[tupleBytes * tuples.length];
		System.arraycopy(firstTuple, 0, bucket, 0, tupleBytes);
		for (int i = 1; i < tuples.length; i++) {
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

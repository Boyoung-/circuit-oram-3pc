package oram;

import java.util.Random;

import exceptions.LengthNotMatchException;

public class Bucket {
	private Tuple[] tuples;

	public Bucket(int numTuples, int[] tupleParams, Random rand) {
		if (tupleParams.length != 4)
			throw new LengthNotMatchException(tupleParams.length + " != 4");
		tuples = new Tuple[numTuples];
		for (int i = 0; i < numTuples; i++)
			tuples[i] = new Tuple(tupleParams[0], tupleParams[1], tupleParams[2], tupleParams[3], rand);
	}

	public Bucket(Tuple[] tuples) {
		this.tuples = tuples;
	}

	// deep copy
	public Bucket(Bucket b) {
		tuples = new Tuple[b.getNumTuples()];
		for (int i = 0; i < tuples.length; i++)
			tuples[i] = new Tuple(b.getTuple(i));
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
		if (this.tuples.length != tuples.length)
			throw new LengthNotMatchException(this.tuples.length + " != " + tuples.length);
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

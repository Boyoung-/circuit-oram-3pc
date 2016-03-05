package oram;

import java.io.Serializable;
import java.util.Random;

import exceptions.LengthNotMatchException;

public class Bucket implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int numBytes;
	private Tuple[] tuples;

	public Bucket(int numTuples, int[] tupleParams, Random rand) {
		if (tupleParams.length != 4)
			throw new LengthNotMatchException(tupleParams.length + " != 4");
		tuples = new Tuple[numTuples];
		for (int i = 0; i < numTuples; i++)
			tuples[i] = new Tuple(tupleParams[0], tupleParams[1], tupleParams[2], tupleParams[3], rand);
		numBytes = numTuples * tuples[0].getNumBytes();
	}

	public Bucket(Tuple[] tuples) {
		this.tuples = tuples;
		numBytes = tuples.length * tuples[0].getNumBytes();
	}

	// deep copy
	public Bucket(Bucket b) {
		numBytes = b.getNumBytes();
		tuples = new Tuple[b.getNumTuples()];
		for (int i = 0; i < tuples.length; i++)
			tuples[i] = new Tuple(b.getTuple(i));
	}

	public int getNumBytes() {
		return numBytes;
	}

	public int getNumTuples() {
		return tuples.length;
	}

	public Tuple getTuple(int i) {
		return tuples[i];
	}

	public void setTuple(int i, Tuple tuple) {
		if (!tuples[i].sameLength(tuple))
			throw new LengthNotMatchException(tuples[i].getNumBytes() + " != " + tuple.getNumBytes());
		tuples[i] = tuple;
	}

	public Bucket xor(Bucket b) {
		if (!this.sameLength(b))
			throw new LengthNotMatchException(numBytes + " != " + b.getNumBytes());
		Tuple[] newTuples = new Tuple[tuples.length];
		for (int i = 0; i < tuples.length; i++)
			newTuples[i] = tuples[i].xor(b.getTuple(i));
		return new Bucket(newTuples);
	}

	public void setXor(Bucket b) {
		if (!this.sameLength(b))
			throw new LengthNotMatchException(numBytes + " != " + b.getNumBytes());
		for (int i = 0; i < tuples.length; i++)
			tuples[i].setXor(b.getTuple(i));
	}

	public boolean sameLength(Bucket b) {
		return numBytes == b.getNumBytes();
	}

	public byte[] toByteArray() {
		int tupleBytes = tuples[0].getNumBytes();
		byte[] bucket = new byte[numBytes];
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

	public static Tuple[] bucketsToTuples(Bucket[] buckets) {
		int numTuples = 0;
		for (int i = 0; i < buckets.length; i++)
			numTuples += buckets[i].getNumTuples();

		Tuple[] tuples = new Tuple[numTuples];
		int tupleCnt = 0;
		for (int i = 0; i < buckets.length; i++)
			for (int j = 0; j < buckets[i].getNumTuples(); j++) {
				tuples[tupleCnt] = buckets[i].getTuple(j);
				tupleCnt++;
			}

		return tuples;
	}
}

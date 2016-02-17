package oram;

import java.math.BigInteger;

public class TestTree {

	public static void main(String[] args) {
		Metadata md = new Metadata();
		Tree tree = new Tree(2, md);
		long tupleCounter = 0;
		for (int i = 0; i < tree.getD(); i++) {
			long numBuckets = (long) Math.pow(2, i);
			for (int j = 0; j < numBuckets; j++) {
				long bucketIndex = j + numBuckets - 1;
				Bucket bucket = tree.getBucket(bucketIndex);
				for (int k = 0; k < bucket.getNumTuples(); k++) {
					byte[] fnl = new byte[0];
					byte[] a = BigInteger.valueOf(tupleCounter).toByteArray();
					Tuple tuple = new Tuple(fnl, fnl, fnl, a);
					tupleCounter++;
					bucket.setTuple(k, tuple);
				}
			}
		}

		BigInteger L = new BigInteger("0110011", 2);
		System.out.println(L.toString(2));
		Bucket[] pathBuckets = tree.getBucketsOnPath(L.longValue());
		for (int i = 0; i < pathBuckets.length; i++)
			System.out.println(pathBuckets[i]);
	}

}

package oram;

import java.math.BigInteger;

import Util.Array64;
import exceptions.InvalidPathLabelException;
import exceptions.LengthNotMatchException;

public class Tree {
	private int treeIndex;
	private int w;
	private int stashSize;
	private int nBits;
	private int lBits;
	private int aBits;
	private int nBytes;
	private int lBytes;
	private int aBytes;
	private int tupleBytes;
	private long numBuckets;
	private int d;

	private Array64<Bucket> buckets;

	public Tree(int i, Metadata md) {
		treeIndex = i;
		w = md.getW();
		stashSize = md.getStashSizeOfTree(i);
		nBits = md.getNBitsOfTree(i);
		lBits = md.getLBitsOfTree(i);
		aBits = md.getABitsOfTree(i);
		nBytes = md.getNBytesOfTree(i);
		lBytes = md.getLBytesOfTree(i);
		aBytes = md.getABytesOfTree(i);
		tupleBytes = md.getTupleBytesOfTree(i);
		numBuckets = md.getNumBucketsOfTree(i);
		d = lBits + 1;

		buckets = new Array64<Bucket>(numBuckets);
	}

	public int getTreeIndex() {
		return treeIndex;
	}

	public int getW() {
		return w;
	}

	public int getStashSize() {
		return stashSize;
	}

	public int getNBits() {
		return nBits;
	}

	public int getLBits() {
		return lBits;
	}

	public int getABits() {
		return aBits;
	}

	public int getNBytes() {
		return nBytes;
	}

	public int getLBytes() {
		return lBytes;
	}

	public int getABytes() {
		return aBytes;
	}

	public int getTupleBytes() {
		return tupleBytes;
	}

	public long getNumBucket() {
		return numBuckets;
	}

	public int getD() {
		return d;
	}

	public Array64<Bucket> getBuckets() {
		return buckets;
	}

	public Bucket getBucket(long bucketIndex) {
		return buckets.get(bucketIndex);
	}

	public void setBucket(long bucketIndex, Bucket bucket) {
		buckets.set(bucketIndex, bucket);
	}

	private long[] getBucketIndicesOnPath(long L) {
		if (treeIndex == 0)
			return new long[] { 0 };
		if (L < 0 || L > numBuckets / 2)
			throw new InvalidPathLabelException(BigInteger.valueOf(L).toString(2));
		long[] indices = new long[d];
		for (int i = 0; i < d; i++)
			indices[i] = (L >> (d - i)) + (long) Math.pow(2, i) - 1;
		return indices;
	}

	public Bucket[] getBucketsOnPath(long L) {
		long[] indices = getBucketIndicesOnPath(L);
		Bucket[] buckets = new Bucket[indices.length];
		for (int i = 0; i < indices.length; i++)
			buckets[i] = getBucket(indices[i]);
		return buckets;
	}

	public void setBucketsOnPath(long L, Bucket[] buckets) {
		long[] indices = getBucketIndicesOnPath(L);
		if (indices.length != buckets.length)
			throw new LengthNotMatchException(indices.length + " != " + buckets.length);
		for (int i = 0; i < indices.length; i++)
			setBucket(indices[i], buckets[i]);
	}
}

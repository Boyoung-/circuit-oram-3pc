package oram;

import Util.Array64;

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

	/*
	private byte[] getByteBucket(long bucketNum) {
		if (bucketNum < 0 || bucketNum >= ForestMetadata.getNumBuckets(index))
			try {
				throw new TreeException("Bucket number error");
			} catch (TreeException e) {
				e.printStackTrace();
			}

		int bucketBytes = ForestMetadata.getBucketBytes(index);
		long start = ForestMetadata.getTreeOffset(index) + bucketNum * bucketBytes;
		return Forest.getForestData(start, bucketBytes);
	}

	public Bucket getBucket(long bucketNum) {
		return new Bucket(index, getByteBucket(bucketNum));
	}

	private void setByteBucket(byte[] bucket, long bucketNum) {
		if (bucketNum < 0 || bucketNum >= ForestMetadata.getNumBuckets(index))
			try {
				throw new TreeException("Bucket number error");
			} catch (TreeException e) {
				e.printStackTrace();
			}

		int bucketBytes = ForestMetadata.getBucketBytes(index);
		if (bucket.length != bucketBytes)
			try {
				throw new TreeException("Bucket length error");
			} catch (TreeException e) {
				e.printStackTrace();
			}

		long start = ForestMetadata.getTreeOffset(index) + bucketNum * bucketBytes;
		Forest.setForestData(start, bucket);
	}

	public void setBucket(Bucket bucket, long bucketNum) {
		setByteBucket(bucket.toByteArray(), bucketNum);
	}

	private List<Long> getBucketIndicesOnPath(long L) {
		List<Long> indices = new ArrayList<Long>();
		if (index == 0) {
			indices.add(0L);
			return indices;
		}

		if (L < 0 || L >= ForestMetadata.getNumLeaves(index))
			try {
				throw new TreeException("L=" + L + ": Invalid path");
			} catch (TreeException e1) {
				e1.printStackTrace();
			}

		int e = ForestMetadata.getLeafExpansion();
		int lBits = ForestMetadata.getLBits(index);

		for (int i = 0; i < lBits; i++) {
			long bucketIndex = (L >> (lBits - i)) + (long) Math.pow(2, i) - 1;
			indices.add(bucketIndex);
		}

		long bucketIndex = ForestMetadata.getNumLeaves(index) - 1 + L * e;
		for (int i = 0; i < e; i++)
			indices.add(bucketIndex + i);

		return indices;
	}

	public Bucket[] getBucketsOnPath(BigInteger L) {
		if (L == null && index != 0) {
			try {
				throw new TreeException("L is null");
			} catch (TreeException e) {
				e.printStackTrace();
			}
			return null;
		} else if (L == null && index == 0)
			return getBucketsOnPath(0);
		else
			return getBucketsOnPath(L.longValue());
	}

	public Bucket[] getBucketsOnPath(long L) {
		List<Long> indices = getBucketIndicesOnPath(L);
		Bucket[] buckets = new Bucket[indices.size()];
		for (int i = 0; i < indices.size(); i++)
			buckets[i] = getBucket(indices.get(i));
		return buckets;
	}

	public Bucket[] getBucketsOnPath(String L) throws TreeException, BucketException {
		if (L == null)
			throw new TreeException("L is null");
		if (L.equals("") && index != 0)
			throw new TreeException("Invalid L");

		if (L.equals(""))
			return getBucketsOnPath(0);
		return getBucketsOnPath(new BigInteger(L, 2).longValue());
	}

	public void setBucketsOnPath(Bucket[] buckets, BigInteger L) {
		if (L == null && index != 0)
			try {
				throw new TreeException("L is null");
			} catch (TreeException e) {
				e.printStackTrace();
			}
		else if (L == null && index == 0) {
			setBucketsOnPath(buckets, 0);
			return;
		} else
			setBucketsOnPath(buckets, L.longValue());
	}

	public void setBucketsOnPath(Bucket[] buckets, long L) {
		List<Long> indices = getBucketIndicesOnPath(L);
		if (indices.size() != buckets.length)
			try {
				throw new TreeException("Number of buckets is not correct");
			} catch (TreeException e) {
				e.printStackTrace();
			}

		for (int i = 0; i < indices.size(); i++)
			setBucket(buckets[i], indices.get(i));
	}

	public void setBucketsOnPath(Bucket[] buckets, String L) throws TreeException {
		if (L == null)
			throw new TreeException("L is null");
		if (L.equals("") && index != 0)
			throw new TreeException("Invalid L");

		if (L.equals("")) {
			setBucketsOnPath(buckets, 0);
			return;
		}
		setBucketsOnPath(buckets, new BigInteger(L, 2).longValue());
	}
*/
}

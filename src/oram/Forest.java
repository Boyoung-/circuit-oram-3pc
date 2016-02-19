package oram;

import java.math.BigInteger;
import java.util.HashMap;

import crypto.OramCrypto;
import util.Util;

public class Forest {
	private Tree[] trees;

	public Forest() {
		Metadata md = new Metadata();
		init(md);
		insertTuples(md);
	}

	public Forest(Metadata md) {
		init(md);
		insertTuples(md);
	}

	// init an empty forest
	private void init(Metadata md) {
		trees = new Tree[md.getNumTrees()];
		// for each tree
		for (int treeIndex = 0; treeIndex < md.getNumTrees(); treeIndex++) {
			// init the tree
			trees[treeIndex] = new Tree(treeIndex, md);
			// get bytes of tuple in this tree
			int fBytes = treeIndex == 0 ? 0 : 1;
			int nBytes = trees[treeIndex].getNBytes();
			int lBytes = trees[treeIndex].getLBytes();
			int aBytes = trees[treeIndex].getABytes();
			// for each level of the tree
			long numBuckets = 1;
			for (int i = 0; i < trees[treeIndex].getD(); i++) {
				// for each bucket
				for (int j = 0; j < numBuckets; j++) {
					// calculate bucket index
					long bucketIndex = j + numBuckets - 1;
					// get the bucket
					Bucket bucket = trees[treeIndex].getBucket(bucketIndex);
					// for each tuple within the bucket
					for (int k = 0; k < bucket.getNumTuples(); k++) {
						// create a empty tuple
						Tuple tuple = new Tuple(fBytes, nBytes, lBytes, aBytes);
						// add to the bucket
						bucket.setTuple(k, tuple);
					}
				}
				// numBuckets doubled for next level
				numBuckets *= 2;
			}
		}
	}

	// insert records into ORAM forest
	private void insertTuples(Metadata md) {
		int numTrees = trees.length;
		int tau = md.getTau();
		int w = md.getW();
		int lastNBits = md.getAddrBits() - tau * (numTrees - 2);
		// mapping between address N and leaf tuple
		@SuppressWarnings("unchecked")
		HashMap<Long, Long>[] addrToTuple = new HashMap[numTrees];
		for (int i = 1; i < numTrees; i++)
			addrToTuple[i] = new HashMap<Long, Long>();
		// keep track of each(current) inserted tuple's N and L for each tree
		long[] N = new long[numTrees];
		long[] L = new long[numTrees];

		// start inserting records with address addr
		for (long addr = 0; addr < md.getNumInsertRecords(); addr++) {
			// for each tree (from last to first)
			for (int i = numTrees - 1; i >= 0; i--) {
				long numBuckets = trees[i].getNumBucket();
				// index of bucket that contains the tuple we will insert/update
				long bucketIndex = 0;
				// index of tuple within the bucket
				int tupleIndex = 0;
				// set correct bucket/tuple index on trees after the first one
				if (i > 0) {
					// set N of the tuple
					if (i == numTrees - 1)
						N[i] = addr;
					else if (i == numTrees - 2)
						N[i] = N[i + 1] >> lastNBits;
					else
						N[i] = N[i + 1] >> tau;
					// find the corresponding leaf tuple index using N
					Long leafTupleIndex = addrToTuple[i].get(N[i]);
					// if N is a new address, then find an unused leaf tuple
					if (leafTupleIndex == null) {
						do {
							leafTupleIndex = Util.nextLong(OramCrypto.sr, (numBuckets / 2 + 1) * w);
						} while (addrToTuple[i].containsValue(leafTupleIndex));
						addrToTuple[i].put(N[i], leafTupleIndex);
					}
					// get leaf tuple label and set bucket/tuple index
					L[i] = leafTupleIndex / (long) w;
					bucketIndex = L[i] + numBuckets / 2;
					tupleIndex = (int) (leafTupleIndex % w);
				}
				// retrieve the tuple that needs to be updated
				Tuple targetTuple = trees[i].getBucket(bucketIndex).getTuple(tupleIndex);

				// for all trees except the last one,
				// update only one label bits in the A field of the target tuple
				if (i < numTrees - 1) {
					int indexN;
					if (i == numTrees - 2)
						indexN = (int) Util.getSubBits(N[i + 1], lastNBits, 0);
					else
						indexN = (int) Util.getSubBits(N[i + 1], tau, 0);
					int start = indexN * trees[i].getAlBytes();
					int end = start + trees[i].getAlBytes();
					targetTuple.setALabel(start, end, Util.rmSignBit(BigInteger.valueOf(L[i + 1]).toByteArray()));
				}
				// for the last tree, update the whole A field of the target
				// tuple
				else
					targetTuple.setA(Util.rmSignBit(BigInteger.valueOf(addr).toByteArray()));

				// for all trees except the first one,
				// also update F, N, L fields
				// no need to update F, N, L for the first tree
				if (i > 0) {
					targetTuple.setF(new byte[] { 1 });
					targetTuple.setN(Util.rmSignBit(BigInteger.valueOf(N[i]).toByteArray()));
					targetTuple.setL(Util.rmSignBit(BigInteger.valueOf(L[i]).toByteArray()));
				}
			}
		}
	}

	public void print() {
		System.out.println("===== ORAM Forest =====");
		System.out.println();

		for (int i = 0; i < trees.length; i++) {
			System.out.println("***** Tree " + i + " *****");
			for (int j = 0; j < trees[i].getNumBucket(); j++)
				System.out.println(trees[i].getBucket(j));
			System.out.println();
		}

		System.out.println("===== End of Forest =====");
		System.out.println();
	}
}

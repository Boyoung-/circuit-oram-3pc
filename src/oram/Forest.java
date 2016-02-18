package oram;

import java.math.BigInteger;
import java.util.HashMap;

import crypto.OramCrypto;
import util.Util;

public class Forest {
	//private String defaultFileName;

	private Tree[] trees;

	public Forest(Metadata md) {
		// init an empty forest
		init(md);

		int numTrees = trees.length;
		@SuppressWarnings("unchecked")
		HashMap<Long, Long>[] addrToTuple = new HashMap[numTrees];
		for (int i=1; i<numTrees; i++)
			addrToTuple[i] = new HashMap<Long, Long>();
		
		int tau = md.getTau();
		int w = md.getW();
		int lastNBits = md.getAddrBits() - tau * (numTrees-2);
		
		long[] N = new long[numTrees];
		long[] L = new long[numTrees];
		
		for (long addr=0; addr<md.getNumInsertRecords(); addr++) {
			for (int i=numTrees-1; i>=0; i--) {
				long numBuckets = trees[i].getNumBucket();
				long bucketIndex = 0;
				int tupleIndex = 0;
				if (i > 0) {
					if (i == numTrees-1)
						N[i] = addr;
					else if (i == numTrees-2)
						N[i] = N[i + 1] >> lastNBits;
					else
						N[i] = N[i + 1] >> tau;
					Long tuple = addrToTuple[i].get(N[i]);
					if (tuple == null) {
						do {
							tuple = Util.nextLong(OramCrypto.sr, (numBuckets/2+1)*w);
						} while (addrToTuple[i].containsValue(tuple));
						addrToTuple[i].put(N[i], tuple);
					}
					L[i] = tuple / (long) w;
					bucketIndex = L[i] + numBuckets/2;
					tupleIndex = (int) (tuple % w);
				}
				Tuple targetTuple = trees[i].getBucket(bucketIndex).getTuple(tupleIndex);

				
				if (i < numTrees-1) {
					int indexN;
					if (i == numTrees - 2)
						indexN = (int) Util.getSubBits(N[i + 1], lastNBits, 0);
					else
						indexN = (int) Util.getSubBits(N[i + 1], tau, 0);
					int start = (md.getTwoTauPow() - indexN - 1) * md.getLBitsOfTree(i + 1);
					A = Util.setSubBits(new BigInteger(1, old.getA()), L[i + 1], start,
							start + ForestMetadata.getLBits(i + 1));
				}
				else
					// A = new BigInteger(ForestMetadata.getABits(i), rnd); //
					// generate random record content
					A = BigInteger.valueOf(address); // for testing: record
														// content is the same
														// as its N
				
				if (i == 0)
					tuple = A;
				else {
					tuple = FB.shiftLeft(ForestMetadata.getTupleBits(i) - 1)
							.or(N[i].shiftLeft(ForestMetadata.getLBits(i) + ForestMetadata.getABits(i)))
							.or(L[i].shiftLeft(ForestMetadata.getABits(i))).or(A);
				}

				Tuple newTuple = new Tuple(i, Util.rmSignBit(tuple.toByteArray()));
				bucket.setTuple(newTuple, tupleIndex);
				Util.disp("Tree-" + i + " writing " + newTuple);
				trees.get(i).setBucket(bucket, bucketIndex);
			}
		}
	}
	
	private void init(Metadata md) {
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
			for (int i = 0; i < trees[treeIndex].getD(); i++) {
				// get numBuckets on this level
				long numBuckets = (long) Math.pow(2, i);
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
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initForest(String filename1, String filename2) throws Exception {

		// following used to hold new tuple content
		BigInteger FB = null;
		BigInteger[] N = new BigInteger[levels];
		BigInteger[] L = new BigInteger[levels];
		BigInteger A;
		BigInteger tuple; // new tuple to be inserted
		Bucket bucket; // bucket to be updated
		long bucketIndex; // bucket index in the tree
		int tupleIndex; // tuple index in the bucket

		// this one is for loadpathcheat
		BigInteger[] firstL = new BigInteger[levels];

		HashMap<Long, Long>[] nToSlot = new HashMap[levels];
		for (int i = 1; i < levels; i++)
			nToSlot[i] = new HashMap<Long, Long>();

		int shiftN = ForestMetadata.getLastNBits() % tau;
		if (shiftN == 0)
			shiftN = tau;

		System.out.println("===== Forest Generation =====");
		for (long address = 0L; address < numInsert; address++) {
			System.out.println("record: " + address);
			for (int i = h; i >= 0; i--) {
				if (i == 0) {
					// FB = BigInteger.ONE;
					N[i] = BigInteger.ZERO;
					// L[i] = BigInteger.ZERO;
					bucketIndex = 0;
					tupleIndex = 0;

					if (address == 0)
						firstL[i] = null;
				} else {
					FB = BigInteger.ONE;
					if (i == h)
						N[i] = BigInteger.valueOf(address);
					else if (i == h - 1)
						N[i] = N[i + 1].shiftRight(shiftN);
					else
						N[i] = N[i + 1].shiftRight(tau);
					// N[i] = BigInteger.valueOf(address >> ((h-i)*tau));
					Long slot = nToSlot[i].get(N[i].longValue());
					if (slot == null) {
						do {
							slot = Util.nextLong(ForestMetadata.getNumLeafTuples(i));
						} while (nToSlot[i].containsValue(slot));
						nToSlot[i].put(N[i].longValue(), slot);
					}
					L[i] = BigInteger.valueOf(slot / (w * e));
					bucketIndex = slot / w + ForestMetadata.getNumLeaves(i) - 1;
					tupleIndex = (int) (slot % w);

					if (address == 0)
						firstL[i] = L[i];
				}

				bucket = trees.get(i).getBucket(bucketIndex);
				bucket.setIndex(i);

				if (i == h)
					// A = new BigInteger(ForestMetadata.getABits(i), rnd); //
					// generate random record content
					A = BigInteger.valueOf(address); // for testing: record
														// content is the same
														// as its N
				else {
					BigInteger indexN = null;
					if (i == h - 1)
						indexN = Util.getSubBits(N[i + 1], 0, shiftN);
					else
						indexN = Util.getSubBits(N[i + 1], 0, tau);
					int start = (ForestMetadata.getTwoTauPow() - indexN.intValue() - 1)
							* ForestMetadata.getLBits(i + 1);
					Tuple old = bucket.getTuple(tupleIndex);
					A = Util.setSubBits(new BigInteger(1, old.getA()), L[i + 1], start,
							start + ForestMetadata.getLBits(i + 1));
				}

				if (i == 0)
					tuple = A;
				else {
					tuple = FB.shiftLeft(ForestMetadata.getTupleBits(i) - 1)
							.or(N[i].shiftLeft(ForestMetadata.getLBits(i) + ForestMetadata.getABits(i)))
							.or(L[i].shiftLeft(ForestMetadata.getABits(i))).or(A);
				}

				Tuple newTuple = new Tuple(i, Util.rmSignBit(tuple.toByteArray()));
				bucket.setTuple(newTuple, tupleIndex);
				Util.disp("Tree-" + i + " writing " + newTuple);
				trees.get(i).setBucket(bucket, bucketIndex);
			}
			System.out.println("--------------------");
		}

		Util.disp("");

		if (noForest) {
			noForestInitPaths(firstL);
			return;
		}

		// these two lines are real xors
		// data2 = new ByteArray64(ForestMetadata.getForestBytes(), "random");
		// data1.setXOR(data2);

		// this line is for testing
		data2 = new ByteArray64(ForestMetadata.getForestBytes(), "empty");

		writeToFile(filename1, filename2);

		if (loadPathCheat)
			initPaths(firstL);
	}
}

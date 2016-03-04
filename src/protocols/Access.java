package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import util.Util;

public class Access extends Protocol {

	public Access(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Tree OTi, byte[] Li, byte[] Nip1, byte[] Ni, byte[] Nip1_pr) {
		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		// Object[] objArray = Util.permute(pathBuckets, predata.access_sigma);
		// pathBuckets = Arrays.copyOf(objArray, objArray.length,
		// Bucket[].class);
		for (int i = 0; i < pathBuckets.length; i++) {
			pathBuckets[i].setXor(predata.access_p[i]);
		}

		// step 3
		int numTuples = OTi.getStashSize() + (pathBuckets.length - 1) * OTi.getW();
		byte[][] a = new byte[numTuples][];
		byte[][] m = new byte[numTuples][];
		int tupleCnt = 0;
		for (int i = 0; i < pathBuckets.length; i++)
			for (int j = 0; j < pathBuckets[i].getNumTuples(); j++) {
				Tuple tuple = pathBuckets[i].getTuple(j);
				a[tupleCnt] = ArrayUtils.addAll(tuple.getF(), tuple.getN());
				m[tupleCnt] = tuple.getA();
				tupleCnt++;
			}
		for (int i = 0; i < numTuples; i++) {
			for (int j = 0; j < Ni.length; j++)
				a[i][a[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
		}

		SSCOT sscot = new SSCOT(con1, con2);
		sscot.runE(predata, m, a);
	}

	public void runD(PreData predata, Tree OTi, byte[] Li, byte[] Nip1, byte[] Ni, byte[] Nip1_pr) {
		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		// Object[] objArray = Util.permute(pathBuckets, predata.access_sigma);
		// pathBuckets = Arrays.copyOf(objArray, objArray.length,
		// Bucket[].class);
		for (int i = 0; i < pathBuckets.length; i++) {
			pathBuckets[i].setXor(predata.access_p[i]);
		}

		// step 2
		con2.write(pathBuckets);
		con2.write(Nip1);

		// step 3
		int numTuples = OTi.getStashSize() + (pathBuckets.length - 1) * OTi.getW();
		byte[][] b = new byte[numTuples][];
		int tupleCnt = 0;
		for (int i = 0; i < pathBuckets.length; i++)
			for (int j = 0; j < pathBuckets[i].getNumTuples(); j++) {
				Tuple tuple = pathBuckets[i].getTuple(j);
				b[tupleCnt] = ArrayUtils.addAll(tuple.getF(), tuple.getN());
				tupleCnt++;
			}
		for (int i = 0; i < numTuples; i++) {
			b[i][0] ^= 1;
			for (int j = 0; j < Ni.length; j++)
				b[i][b[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
		}

		SSCOT sscot = new SSCOT(con1, con2);
		sscot.runD(predata, b);
	}

	public void runC() {
		// step 2
		Object[] objArray = con2.readObjectArray();
		Bucket[] pathBuckets = Arrays.copyOf(objArray, objArray.length, Bucket[].class);
		byte[] Nip1 = con2.read();

		// step 3
		SSCOT sscot = new SSCOT(con1, con2);
		sscot.runC();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		PreData predata = new PreData();
		PreAccess preaccess = new PreAccess(con1, con2);
		int treeIndex = 1;
		Tree tree = null;
		int numBuckets = 0;
		if (forest != null) {
			tree = forest.getTree(treeIndex);
			numBuckets = tree.getD();
		}
		byte[] Li = new BigInteger("101", 2).toByteArray();
		byte[] Nip1 = new byte[] { 0 };
		byte[] Ni = new byte[] { 0 };
		byte[] Nip1_pr = new byte[] { 0 };
		if (party == Party.Eddie) {
			preaccess.runE(predata, tree, numBuckets);
			runE(predata, tree, Li, Nip1, Ni, Nip1_pr);

		} else if (party == Party.Debbie) {
			preaccess.runD(predata);
			runD(predata, tree, Li, Nip1, Ni, Nip1_pr);

		} else if (party == Party.Charlie) {
			preaccess.runC();
			runC();

		} else {
			throw new NoSuchPartyException(party + "");
		}
	}
}

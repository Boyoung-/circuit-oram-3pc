package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import communication.Communication;
import crypto.Crypto;
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
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);

		// step 3
		byte[][] a = new byte[pathTuples.length][];
		byte[][] m = new byte[pathTuples.length][];
		byte[] y = Util.nextBytes(OTi.getABytes(), Crypto.sr);
		for (int i = 0; i < pathTuples.length; i++) {
			m[i] = Util.xor(pathTuples[i].getA(), y);
			a[i] = ArrayUtils.addAll(pathTuples[i].getF(), pathTuples[i].getN());
			for (int j = 0; j < Ni.length; j++)
				a[i][a[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
		}

		SSCOT sscot = new SSCOT(con1, con2);
		sscot.runE(predata, m, a);

		// step 4
		int ySegBytes = y.length / OTi.getTwoTauPow();
		byte[][] y_array = new byte[OTi.getTwoTauPow()][];
		for (int i = 0; i < OTi.getTwoTauPow(); i++)
			y_array[i] = Arrays.copyOfRange(y, i * ySegBytes, (i + 1) * ySegBytes);

		SSIOT ssiot = new SSIOT(con1, con2);
		ssiot.runE(predata, y_array, Nip1_pr);
	}

	public void runD(PreData predata, Tree OTi, byte[] Li, byte[] Nip1, byte[] Ni, byte[] Nip1_pr) {
		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(new BigInteger(1, Li).longValue());
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);
		for (int i = 0; i < pathTuples.length; i++)
			pathTuples[i].setXor(predata.access_p[i]);
		Object[] objArray = Util.permute(pathTuples, predata.access_sigma);
		pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);

		// step 2
		con2.write(pathTuples);
		// con2.write(Nip1);

		// step 3
		byte[][] b = new byte[pathTuples.length][];
		for (int i = 0; i < pathTuples.length; i++) {
			b[i] = ArrayUtils.addAll(pathTuples[i].getF(), pathTuples[i].getN());
			b[i][0] ^= 1;
			for (int j = 0; j < Ni.length; j++)
				b[i][b[i].length - 1 - j] ^= Ni[Ni.length - 1 - j];
		}

		SSCOT sscot = new SSCOT(con1, con2);
		sscot.runD(predata, b);

		// step 4
		SSIOT ssiot = new SSIOT(con1, con2);
		ssiot.runD(predata, Nip1_pr);
	}

	public void runC() {
		// step 2
		Object[] objArray = con2.readObjectArray();
		Tuple[] pathTuples = Arrays.copyOf(objArray, objArray.length, Tuple[].class);
		// byte[] Nip1 = con2.read();

		// step 3
		SSCOT sscot = new SSCOT(con1, con2);
		OutSSCOT je = sscot.runC();
		byte[] d = pathTuples[je.t].getA();
		byte[] z = Util.xor(je.m_t, d);

		// step 4
		SSIOT ssiot = new SSIOT(con1, con2);
		OutSSIOT jy = ssiot.runC();
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
		PreData predata = new PreData();
		PreAccess preaccess = new PreAccess(con1, con2);
		int treeIndex = 1;
		Tree tree = null;
		int numTuples = 0;
		if (forest != null) {
			tree = forest.getTree(treeIndex);
			numTuples = (tree.getD() - 1) * tree.getW() + tree.getStashSize();
		}
		byte[] Li = new BigInteger("11", 2).toByteArray();
		byte[] Nip1 = new byte[] { 0 };
		byte[] Ni = new byte[] { 0 };
		byte[] Nip1_pr = new byte[] { 0 };
		if (party == Party.Eddie) {
			preaccess.runE(predata, tree, numTuples);
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

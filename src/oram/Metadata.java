package oram;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Metadata {
	private String CONFIG_FILE = "config/config.yaml";

	private String TAU = "tau";
	private String ADDRBITS = "addrBits";
	private String W = "w";
	private String DBYTES = "dBytes";
	private String INSERT = "insert";
	private String STASH = "stash";

	private int tau;
	private int twoTauPow;
	private int addrBits;
	private int w;
	private int dBytes;
	private int tempStashSize;
	private int numTrees;
	private int h;
	private long maxNumRecords;
	private long numInsertRecords;

	private int[] nBits;
	private int[] lBits;
	private int[] aBits;

	private int[] nBytes;
	private int[] lBytes;
	private int[] aBytes;
	private int[] tupleBytes;

	private int[] stashSizes;
	private long[] numBuckets;
	private long[] treeBytes;
	private long[] treeOffsets;

	private long forestBytes;

	public Metadata() {
		setup(CONFIG_FILE);
	}

	public Metadata(String filename) {
		setup(filename);
	}

	private void setup(String filename) {
		Yaml yaml = new Yaml();
		InputStream input = null;
		try {
			input = new FileInputStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> configMap = (Map<String, Object>) yaml.load(input);

		tau = Integer.parseInt(configMap.get(TAU).toString());
		addrBits = Integer.parseInt(configMap.get(ADDRBITS).toString());
		w = Integer.parseInt(configMap.get(W).toString());
		dBytes = Integer.parseInt(configMap.get(DBYTES).toString());
		numInsertRecords = Long.parseLong(configMap.get(INSERT).toString(), 10);
		tempStashSize = Integer.parseInt(configMap.get(STASH).toString());

		init();
	}

	private void init() {
		twoTauPow = (int) Math.pow(2, tau);
		h = (addrBits - 1) / tau + 1;
		numTrees = h + 1;
		maxNumRecords = (long) Math.pow(2, addrBits);
		if (numInsertRecords < 0 || numInsertRecords > maxNumRecords)
			numInsertRecords = maxNumRecords;

		lBits = new int[numTrees];
		nBits = new int[numTrees];
		aBits = new int[numTrees];
		lBytes = new int[numTrees];
		nBytes = new int[numTrees];
		aBytes = new int[numTrees];
		tupleBytes = new int[numTrees];

		stashSizes = new int[numTrees];
		numBuckets = new long[numTrees];
		treeOffsets = new long[numTrees];
		treeBytes = new long[numTrees];

		for (int i = h; i >= 0; i--) {
			if (i == 0) {
				nBits[i] = 0;
				lBits[i] = 0;
				aBits[i] = twoTauPow * lBits[i + 1];
			} else if (i < h) {
				nBits[i] = i * tau;
				lBits[i] = nBits[i] + 1;
				aBits[i] = twoTauPow * lBits[i + 1];
			} else {
				nBits[i] = addrBits;
				lBits[i] = nBits[i] + 1;
				aBits[i] = dBytes * 8;
			}

			nBytes[i] = (nBits[i] + 7) / 8;
			lBytes[i] = (lBits[i] + 7) / 8;
			aBytes[i] = (aBits[i] + 7) / 8;
			numBuckets[i] = (long) Math.pow(2, lBits[i] + 1) - 1;
			if (i == 0) {
				tupleBytes[i] = aBytes[i];
				stashSizes[i] = 1;
				treeBytes[i] = tupleBytes[i];
			} else {
				tupleBytes[i] = 1 + nBytes[i] + lBytes[i] + aBytes[i];
				stashSizes[i] = tempStashSize;
				treeBytes[i] = ((numBuckets[i] - 1) * w + stashSizes[i]) * tupleBytes[i];
			}
		}

		forestBytes = 0L;
		for (int i = 0; i < numTrees; i++) {
			treeOffsets[i] = forestBytes;
			forestBytes += treeBytes[i];
		}

		printInfo();
	}

	public void printInfo() {
		System.out.println("===== ORAM Forest Metadata =====");
		System.out.println();
		System.out.println("tau:				" + tau);
		System.out.println("address bits:		" + addrBits);
		System.out.println("w:					" + w);
		System.out.println("D bytes:			" + dBytes);
		System.out.println();
		System.out.println("max records:		" + maxNumRecords);
		System.out.println("inserted records:	" + numInsertRecords);
		System.out.println("trees:				" + numTrees);
		System.out.println("forest bytes:		" + forestBytes);
		System.out.println();

		for (int i = 0; i < numTrees; i++) {
			System.out.println("[Tree " + i + "]");
			System.out.println("	nBits		-> " + nBits[i]);
			System.out.println("	lBits		-> " + lBits[i]);
			System.out.println("	aBits		-> " + aBits[i]);
			System.out.println("	tupleBytes	-> " + tupleBytes[i]);
			System.out.println("	stashSize	-> " + stashSizes[i]);
			System.out.println("	numBuckets	-> " + numBuckets[i]);
			System.out.println("	treeOffset	-> " + treeOffsets[i]);
			System.out.println("	treeBytes	-> " + treeBytes[i]);
			System.out.println();
		}
		System.out.println("===== End of Metadata =====");
		System.out.println();
	}

	public void write(String filename) {
		Yaml yaml = new Yaml();
		FileWriter writer = null;
		try {
			writer = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, String> configMap = new HashMap<String, String>();
		configMap.put(TAU, "" + tau);
		configMap.put(ADDRBITS, "" + addrBits);
		configMap.put(W, "" + w);
		configMap.put(DBYTES, "" + dBytes);
		configMap.put(INSERT, "" + numInsertRecords);
		configMap.put(STASH, "" + tempStashSize);

		yaml.dump(configMap, writer);
	}

	public void write() {
		write(CONFIG_FILE);
	}

	public int getTau() {
		return tau;
	}

	public int getTwoTauPow() {
		return twoTauPow;
	}

	public int getAddrBits() {
		return addrBits;
	}

	public int getW() {
		return w;
	}

	public int getDBytes() {
		return dBytes;
	}

	public int getTempStashSize() {
		return tempStashSize;
	}

	public int getNumTrees() {
		return numTrees;
	}

	public int getH() {
		return h;
	}

	public long getMaxNumRecords() {
		return maxNumRecords;
	}

	public long getNumInsertRecords() {
		return numInsertRecords;
	}

	public int getNBitsOfTree(int i) {
		return nBits[i];
	}

	public int getLBitsOfTree(int i) {
		return lBits[i];
	}

	public int getABitsOfTree(int i) {
		return aBits[i];
	}

	public int getNBytesOfTree(int i) {
		return nBytes[i];
	}

	public int getLBytesOfTree(int i) {
		return lBytes[i];
	}

	public int getABytesOfTree(int i) {
		return aBytes[i];
	}

	public int getTupleBytesOfTree(int i) {
		return tupleBytes[i];
	}

	public int getStashSizeOfTree(int i) {
		return stashSizes[i];
	}

	public long getNumBucketsOfTree(int i) {
		return numBuckets[i];
	}

	public long getTreeBytesOfTree(int i) {
		return treeBytes[i];
	}

	public long getTreeOffsetOfTree(int i) {
		return treeOffsets[i];
	}

	public long getForestBytes() {
		return forestBytes;
	}
}

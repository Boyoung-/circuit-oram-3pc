package pir;

import java.util.Arrays;

import communication.Communication;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.Eviction;
import protocols.Protocol;
import protocols.UpdateRoot;
import protocols.precomputation.PreEviction;
import protocols.precomputation.PreUpdateRoot;
import protocols.struct.OutFF;
import protocols.struct.OutPIRAccess;
import protocols.struct.OutULiT;
import protocols.struct.Party;
import protocols.struct.PreData;
import protocols.struct.TwoThreeXorByte;
import protocols.struct.TwoThreeXorInt;
import util.Timer;

// TODO: really FlipFlag on path, and update path in Eviction

public class PIRRetrieve extends Protocol {

	Communication[] cons1;
	Communication[] cons2;

	public PIRRetrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void setCons(Communication[] a, Communication[] b) {
		cons1 = a;
		cons2 = b;
	}

	public void runE(Metadata md, PreData predata, Tree tree_DE, Tree tree_CE, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		int treeIndex = tree_DE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		PIRAccess piracc = new PIRAccess(con1, con2);
		OutPIRAccess outpiracc = piracc.runE(md, predata, tree_DE, tree_CE, Li, L, N, dN, timer);

		OutULiT T = new OutULiT();
		if (!isLastTree) {
			TwoThreeXorByte Lp = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex));
			TwoThreeXorByte Lpi = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex + 1));
			ULiT ulit = new ULiT(con1, con2);
			T = ulit.runE(predata, outpiracc.X, N, dN, Lp, Lpi, outpiracc.nextL, md.getTwoTauPow(), timer);
		} else {
			T.DE = outpiracc.pathTuples_DE[0];
			T.CE = outpiracc.pathTuples_CE[0];
		}

		int pathTuples = outpiracc.pathTuples_CE.length;

		if (!isFirstTree) {
			byte[][] fb_DE = new byte[pathTuples][];
			byte[][] fb_CE = new byte[pathTuples][];
			for (int i = 0; i < pathTuples; i++) {
				fb_DE[i] = outpiracc.pathTuples_DE[i].getF();
				fb_CE[i] = outpiracc.pathTuples_CE[i].getF();
			}
			FlipFlag ff = new FlipFlag(con1, con2);
			OutFF outff = ff.runE(predata, fb_DE, fb_CE, outpiracc.j.s_DE, timer);
			for (int i = 0; i < pathTuples; i++) {
				// outpiracc.pathTuples_DE[i].setF(outff.fb_DE[i]);
				// outpiracc.pathTuples_CE[i].setF(outff.fb_CE[i]);
			}
		}

		int stashSize = tree_DE.getStashSize();
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		preupdateroot.runE(predata, isFirstTree, stashSize, md.getLBitsOfTree(treeIndex), timer);

		Tuple[] path = new Tuple[pathTuples];
		for (int i = 0; i < pathTuples; i++) {
			path[i] = outpiracc.pathTuples_DE[i].xor(outpiracc.pathTuples_CE[i]);
		}
		Tuple[] R = Arrays.copyOfRange(path, 0, stashSize);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		R = updateroot.runE(predata, isFirstTree, Li, R, T.DE.xor(T.CE), timer);
		System.arraycopy(R, 0, path, 0, R.length);

		PreEviction preeviction = new PreEviction(con1, con2);
		preeviction.runE(predata, isFirstTree, tree_DE.getD(), tree_DE.getW(), timer);

		Eviction eviction = new Eviction(con1, con2);
		eviction.runE(predata, isFirstTree, Li, path, tree_DE, timer);
	}

	public void runD(Metadata md, PreData predata, Tree tree_DE, Tree tree_CD, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		int treeIndex = tree_DE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		PIRAccess piracc = new PIRAccess(con1, con2);
		OutPIRAccess outpiracc = piracc.runD(md, predata, tree_DE, tree_CD, Li, L, N, dN, timer);

		OutULiT T = new OutULiT();
		if (!isLastTree) {
			TwoThreeXorByte Lp = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex));
			TwoThreeXorByte Lpi = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex + 1));
			ULiT ulit = new ULiT(con1, con2);
			T = ulit.runD(predata, outpiracc.X, N, dN, Lp, Lpi, outpiracc.nextL, md.getTwoTauPow(), timer);
		} else {
			T.CD = outpiracc.pathTuples_CD[0];
			T.DE = outpiracc.pathTuples_DE[0];
		}

		int pathTuples = outpiracc.pathTuples_CD.length;

		if (!isFirstTree) {
			byte[][] fb_DE = new byte[pathTuples][];
			byte[][] fb_CD = new byte[pathTuples][];
			for (int i = 0; i < pathTuples; i++) {
				fb_DE[i] = outpiracc.pathTuples_DE[i].getF();
				fb_CD[i] = outpiracc.pathTuples_CD[i].getF();
			}
			FlipFlag ff = new FlipFlag(con1, con2);
			OutFF outff = ff.runD(predata, fb_DE, fb_CD, outpiracc.j.s_DE, timer);
			for (int i = 0; i < pathTuples; i++) {
				// outpiracc.pathTuples_DE[i].setF(outff.fb_DE[i]);
				// outpiracc.pathTuples_CD[i].setF(outff.fb_CD[i]);
			}
		}

		int stashSize = tree_DE.getStashSize();
		int[] tupleParam = new int[] { treeIndex == 0 ? 0 : 1, md.getNBytesOfTree(treeIndex),
				md.getLBytesOfTree(treeIndex), md.getABytesOfTree(treeIndex) };
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		preupdateroot.runD(predata, isFirstTree, stashSize, md.getLBitsOfTree(treeIndex), tupleParam, timer);

		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		updateroot.runD(predata, isFirstTree, Li, tree_DE.getW(), timer);

		PreEviction preeviction = new PreEviction(con1, con2);
		preeviction.runD(predata, isFirstTree, tree_DE.getD(), tree_DE.getW(), tupleParam, timer);

		Eviction eviction = new Eviction(con1, con2);
		eviction.runD(predata, isFirstTree, Li, tree_DE, timer);
	}

	public void runC(Metadata md, PreData predata, Tree tree_CD, Tree tree_CE, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		int treeIndex = tree_CE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		PIRAccess piracc = new PIRAccess(con1, con2);
		OutPIRAccess outpiracc = piracc.runC(md, predata, tree_CD, tree_CE, Li, L, N, dN, timer);

		OutULiT T = new OutULiT();
		if (!isLastTree) {
			TwoThreeXorByte Lp = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex));
			TwoThreeXorByte Lpi = new TwoThreeXorByte(md.getLBytesOfTree(treeIndex + 1));
			ULiT ulit = new ULiT(con1, con2);
			T = ulit.runC(predata, outpiracc.X, N, dN, Lp, Lpi, outpiracc.nextL, md.getTwoTauPow(), timer);
		} else {
			T.CD = outpiracc.pathTuples_CD[0];
			T.CE = outpiracc.pathTuples_CE[0];
		}

		int pathTuples = outpiracc.pathTuples_CD.length;

		if (!isFirstTree) {
			byte[][] fb_CE = new byte[pathTuples][];
			byte[][] fb_CD = new byte[pathTuples][];
			for (int i = 0; i < pathTuples; i++) {
				fb_CE[i] = outpiracc.pathTuples_CE[i].getF();
				fb_CD[i] = outpiracc.pathTuples_CD[i].getF();
			}
			FlipFlag ff = new FlipFlag(con1, con2);
			OutFF outff = ff.runC(predata, fb_CD, fb_CE, outpiracc.j.t_C, timer);
			for (int i = 0; i < pathTuples; i++) {
				// outpiracc.pathTuples_CD[i].setF(outff.fb_CD[i]);
				// outpiracc.pathTuples_CE[i].setF(outff.fb_CE[i]);
			}
		}

		int stashSize = tree_CE.getStashSize();
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		preupdateroot.runC(predata, isFirstTree, timer);

		Tuple[] path = outpiracc.pathTuples_CD;
		Tuple[] R = Arrays.copyOfRange(path, 0, stashSize);

		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		R = updateroot.runC(predata, isFirstTree, R, T.CD, timer);
		System.arraycopy(R, 0, path, 0, R.length);

		PreEviction preeviction = new PreEviction(con1, con2);
		preeviction.runC(predata, isFirstTree, timer);

		Eviction eviction = new Eviction(con1, con2);
		eviction.runC(predata, isFirstTree, path, tree_CD.getD(), stashSize, tree_CD.getW(), timer);
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		Tree tree_CD = null;
		Tree tree_DE = null;
		Tree tree_CE = null;

		for (int test = 0; test < 100; test++) {

			for (int treeIndex = 0; treeIndex < md.getNumTrees(); treeIndex++) {
				if (party == Party.Eddie) {
					tree_DE = forest[0].getTree(treeIndex);
					tree_CE = forest[1].getTree(treeIndex);
				} else if (party == Party.Debbie) {
					tree_DE = forest[0].getTree(treeIndex);
					tree_CD = forest[1].getTree(treeIndex);
				} else if (party == Party.Charlie) {
					tree_CE = forest[0].getTree(treeIndex);
					tree_CD = forest[1].getTree(treeIndex);
				} else {
					throw new NoSuchPartyException(party + "");
				}

				int Llen = md.getLBytesOfTree(treeIndex);
				int Nlen = md.getNBytesOfTree(treeIndex);

				TwoThreeXorInt dN = new TwoThreeXorInt();

				TwoThreeXorByte N = new TwoThreeXorByte();
				N.CD = new byte[Nlen];
				N.DE = new byte[Nlen];
				N.CE = new byte[Nlen];
				TwoThreeXorByte L = new TwoThreeXorByte();
				L.CD = new byte[Llen];
				L.DE = new byte[Llen];
				L.CE = new byte[Llen];
				byte[] Li = new byte[Llen];

				if (party == Party.Eddie) {
					this.runE(md, predata, tree_DE, tree_CE, Li, L, N, dN, timer);
					// OutPIRAccess out = this.runE(md, predata, tree_DE, tree_CE, Li, L, N, dN,
					// timer);
					// out.j.t_D = con1.readInt();
					// out.j.t_C = con2.readInt();
					// out.X.CD = con1.read();
					// int pathTuples = out.pathTuples_CE.length;
					// int index = (out.j.t_D + out.j.s_CE) % pathTuples;
					// byte[] X = Util.xor(Util.xor(out.X.DE, out.X.CE), out.X.CD);
					//
					boolean fail = false;
					// if (index != 0) {
					// System.err.println(test + " " + treeIndex + ": PIRAcc test failed on KSearch
					// index");
					// fail = true;
					// }
					// if (new BigInteger(1, X).intValue() != 0) {
					// System.err.println(test + " " + treeIndex + ": PIRAcc test failed on
					// 3ShiftPIR X");
					// fail = true;
					// }
					// if (treeIndex < md.getNumTrees() - 1 && new BigInteger(1,
					// out.Lip1).intValue() != 0) {
					// System.err.println(test + " " + treeIndex + ": PIRAcc test failed on
					// 3ShiftXorPIR Lip1");
					// fail = true;
					// }
					if (!fail)
						System.out.println(test + " " + treeIndex + ": PIRAcc test passed");

				} else if (party == Party.Debbie) {
					this.runD(md, predata, tree_DE, tree_CD, Li, L, N, dN, timer);
					// OutPIRAccess out = this.runD(md, predata, tree_DE, tree_CD, Li, L, N, dN,
					// timer);
					// con1.write(out.j.t_D);
					// con1.write(out.X.CD);

				} else if (party == Party.Charlie) {
					this.runC(md, predata, tree_CD, tree_CE, Li, L, N, dN, timer);
					// OutPIRAccess out = this.runC(md, predata, tree_CD, tree_CE, Li, L, N, dN,
					// timer);
					// con1.write(out.j.t_C);

				} else {
					throw new NoSuchPartyException(party + "");
				}
			}

		}
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

package pir;

import java.math.BigInteger;

import org.apache.commons.lang3.ArrayUtils;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Bucket;
import oram.Forest;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import pir.precomputation.PrePIRCOT;
import protocols.Protocol;
import protocols.struct.OutAccess;
import protocols.struct.OutPIRAccess;
import protocols.struct.OutPIRCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import protocols.struct.TwoOneXor;
import protocols.struct.TwoThreeXorByte;
import protocols.struct.TwoThreeXorInt;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class PIRAccess extends Protocol {

	private int pid = P.ACC;

	public PIRAccess(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutPIRAccess runE(Metadata md, PreData predata, Tree tree_DE, Tree tree_CE, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		timer.start(pid, M.online_comp);

		Bucket[] pathBuckets_DE = tree_DE.getBucketsOnPath(Li);
		Tuple[] pathTuples_DE = Bucket.bucketsToTuples(pathBuckets_DE);
		Bucket[] pathBuckets_CE = tree_CE.getBucketsOnPath(Li);
		Tuple[] pathTuples_CE = Bucket.bucketsToTuples(pathBuckets_CE);

		int pathTuples = pathTuples_CE.length;
		int ttp = md.getTwoTauPow();
		int treeIndex = tree_DE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		byte[][] x_DE = new byte[pathTuples][];
		byte[][] x_CE = new byte[pathTuples][];
		for (int i = 0; i < pathTuples; i++) {
			x_DE[i] = pathTuples_DE[i].getA();
			x_CE[i] = pathTuples_CE[i].getA();
		}
		OutPIRCOT j = new OutPIRCOT();
		TwoOneXor dN21 = new TwoOneXor();
		TwoThreeXorByte X = new TwoThreeXorByte();

		if (isFirstTree) {
			X.DE = x_DE[0];
			X.CE = x_CE[0];
		} else {
			PrePIRCOT preksearch = new PrePIRCOT(con1, con2);
			preksearch.runE(predata, pathTuples, timer);

			byte[][] u = new byte[pathTuples][];
			for (int i = 0; i < pathTuples; i++) {
				u[i] = ArrayUtils.addAll(pathTuples_CE[i].getF(), pathTuples_CE[i].getN());
			}
			byte[] v = ArrayUtils.addAll(new byte[] { 1 }, N.CE);

			PIRCOT ksearch = new PIRCOT(con1, con2);
			j = ksearch.runE(predata, u, v, timer);

			ThreeShiftPIR threeshiftpir = new ThreeShiftPIR(con1, con2);
			X = threeshiftpir.runE(predata, x_DE, x_CE, j, timer);

			dN21.t_E = dN.CE ^ dN.DE;
			dN21.s_CE = dN.CE;
			dN21.s_DE = dN.DE;
		}

		TwoThreeXorByte nextL = null;
		byte[] Lip1 = null;
		if (!isLastTree) {
			ThreeShiftXorPIR threeshiftxorpir = new ThreeShiftXorPIR(con1, con2);
			nextL = threeshiftxorpir.runE(predata, x_DE, x_CE, j, dN21, ttp, timer);
			Lip1 = Util.xor(Util.xor(nextL.DE, nextL.CE), nextL.CD);
		}

		OutPIRAccess out = new OutPIRAccess(null, pathTuples_CE, pathTuples_DE, j, X, nextL, Lip1);

		timer.stop(pid, M.online_comp);
		return out;
	}

	public OutPIRAccess runD(Metadata md, PreData predata, Tree tree_DE, Tree tree_CD, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		timer.start(pid, M.online_comp);

		Bucket[] pathBuckets_DE = tree_DE.getBucketsOnPath(Li);
		Tuple[] pathTuples_DE = Bucket.bucketsToTuples(pathBuckets_DE);
		Bucket[] pathBuckets_CD = tree_CD.getBucketsOnPath(Li);
		Tuple[] pathTuples_CD = Bucket.bucketsToTuples(pathBuckets_CD);

		int pathTuples = pathTuples_CD.length;
		int ttp = md.getTwoTauPow();
		int treeIndex = tree_DE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		byte[][] x_DE = new byte[pathTuples][];
		byte[][] x_CD = new byte[pathTuples][];
		for (int i = 0; i < pathTuples; i++) {
			x_DE[i] = pathTuples_DE[i].getA();
			x_CD[i] = pathTuples_CD[i].getA();
		}
		OutPIRCOT j = new OutPIRCOT();
		TwoOneXor dN21 = new TwoOneXor();
		TwoThreeXorByte X = new TwoThreeXorByte();

		if (isFirstTree) {
			X.DE = x_DE[0];
			X.CD = x_CD[0];
		} else {
			PrePIRCOT preksearch = new PrePIRCOT(con1, con2);
			preksearch.runD(predata, pathTuples, timer);

			byte[][] u = new byte[pathTuples][];
			for (int i = 0; i < pathTuples; i++) {
				u[i] = ArrayUtils.addAll(pathTuples_DE[i].getF(), pathTuples_DE[i].getN());
				Util.setXor(u[i], ArrayUtils.addAll(pathTuples_CD[i].getF(), pathTuples_CD[i].getN()));
			}
			byte[] v = ArrayUtils.addAll(new byte[] { 1 }, N.CE);
			Util.setXor(v, ArrayUtils.addAll(new byte[] { 1 }, N.CD));

			PIRCOT ksearch = new PIRCOT(con1, con2);
			j = ksearch.runD(predata, u, v, timer);

			ThreeShiftPIR threeshiftpir = new ThreeShiftPIR(con1, con2);
			X = threeshiftpir.runD(predata, x_DE, x_CD, j, timer);

			dN21.t_D = dN.CD ^ dN.DE;
			dN21.s_CD = dN.CD;
			dN21.s_DE = dN.DE;
		}

		TwoThreeXorByte nextL = null;
		byte[] Lip1 = null;
		if (!isLastTree) {
			ThreeShiftXorPIR threeshiftxorpir = new ThreeShiftXorPIR(con1, con2);
			nextL = threeshiftxorpir.runD(predata, x_DE, x_CD, j, dN21, ttp, timer);
			Lip1 = Util.xor(Util.xor(nextL.DE, nextL.CE), nextL.CD);
		}

		OutPIRAccess out = new OutPIRAccess(pathTuples_CD, null, pathTuples_DE, j, X, nextL, Lip1);

		timer.stop(pid, M.online_comp);
		return out;
	}

	public OutPIRAccess runC(Metadata md, PreData predata, Tree tree_CD, Tree tree_CE, byte[] Li, TwoThreeXorByte L,
			TwoThreeXorByte N, TwoThreeXorInt dN, Timer timer) {
		timer.start(pid, M.online_comp);

		Bucket[] pathBuckets_CD = tree_CD.getBucketsOnPath(Li);
		Tuple[] pathTuples_CD = Bucket.bucketsToTuples(pathBuckets_CD);
		Bucket[] pathBuckets_CE = tree_CE.getBucketsOnPath(Li);
		Tuple[] pathTuples_CE = Bucket.bucketsToTuples(pathBuckets_CE);

		int pathTuples = pathTuples_CE.length;
		int ttp = md.getTwoTauPow();
		int treeIndex = tree_CE.getTreeIndex();
		boolean isLastTree = treeIndex == md.getNumTrees() - 1;
		boolean isFirstTree = treeIndex == 0;

		byte[][] x_CE = new byte[pathTuples][];
		byte[][] x_CD = new byte[pathTuples][];
		for (int i = 0; i < pathTuples; i++) {
			x_CE[i] = pathTuples_CE[i].getA();
			x_CD[i] = pathTuples_CD[i].getA();
		}
		OutPIRCOT j = new OutPIRCOT();
		TwoOneXor dN21 = new TwoOneXor();
		TwoThreeXorByte X = new TwoThreeXorByte();

		if (isFirstTree) {
			X.CE = x_CE[0];
			X.CD = x_CD[0];
		} else {
			PrePIRCOT preksearch = new PrePIRCOT(con1, con2);
			preksearch.runC(predata, timer);

			PIRCOT ksearch = new PIRCOT(con1, con2);
			j = ksearch.runC(predata, timer);

			ThreeShiftPIR threeshiftpir = new ThreeShiftPIR(con1, con2);
			X = threeshiftpir.runC(predata, x_CD, x_CE, j, timer);

			dN21.t_C = dN.CD ^ dN.CE;
			dN21.s_CD = dN.CD;
			dN21.s_CE = dN.CE;
		}

		TwoThreeXorByte nextL = null;
		byte[] Lip1 = null;
		if (!isLastTree) {
			ThreeShiftXorPIR threeshiftxorpir = new ThreeShiftXorPIR(con1, con2);
			nextL = threeshiftxorpir.runC(predata, x_CD, x_CE, j, dN21, ttp, timer);
			Lip1 = Util.xor(Util.xor(nextL.DE, nextL.CE), nextL.CD);
		}

		OutPIRAccess out = new OutPIRAccess(pathTuples_CD, pathTuples_CE, null, j, X, nextL, Lip1);

		timer.stop(pid, M.online_comp);
		return out;
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
					OutPIRAccess out = this.runE(md, predata, tree_DE, tree_CE, Li, L, N, dN, timer);
					out.j.t_D = con1.readInt();
					out.j.t_C = con2.readInt();
					out.X.CD = con1.read();
					int pathTuples = out.pathTuples_CE.length;
					int index = (out.j.t_D + out.j.s_CE) % pathTuples;
					byte[] X = Util.xor(Util.xor(out.X.DE, out.X.CE), out.X.CD);

					boolean fail = false;
					if (index != 0) {
						System.err.println(test + " " + treeIndex + ": PIRAcc test failed on KSearch index");
						fail = true;
					}
					if (new BigInteger(1, X).intValue() != 0) {
						System.err.println(test + " " + treeIndex + ": PIRAcc test failed on 3ShiftPIR X");
						fail = true;
					}
					if (treeIndex < md.getNumTrees() - 1 && new BigInteger(1, out.Lip1).intValue() != 0) {
						System.err.println(test + " " + treeIndex + ": PIRAcc test failed on 3ShiftXorPIR Lip1");
						fail = true;
					}
					if (!fail)
						System.out.println(test + " " + treeIndex + ": PIRAcc test passed");

				} else if (party == Party.Debbie) {
					OutPIRAccess out = this.runD(md, predata, tree_DE, tree_CD, Li, L, N, dN, timer);
					con1.write(out.j.t_D);
					con1.write(out.X.CD);

				} else if (party == Party.Charlie) {
					OutPIRAccess out = this.runC(md, predata, tree_CD, tree_CE, Li, L, N, dN, timer);
					con1.write(out.j.t_C);

				} else {
					throw new NoSuchPartyException(party + "");
				}
			}

		}
	}

	// on second path
	public OutAccess runE2(Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();
		timer.stop(pid, M.online_read);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);

		// step 5
		Tuple Ti = null;
		if (OTi.getTreeIndex() == 0)
			Ti = pathTuples[0];
		else {
			Ti = new Tuple(1, OTi.getNBytes(), OTi.getLBytes(), OTi.getABytes(), Crypto.sr);
			Ti.setF(new byte[1]);
		}

		OutAccess outaccess = new OutAccess(Li, null, null, null, null, Ti, pathTuples);

		timer.stop(pid, M.online_comp);
		return outaccess;
	}

	public byte[] runD2(Tree OTi, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: get Li from C
		byte[] Li = new byte[0];
		timer.start(pid, M.online_read);
		if (OTi.getTreeIndex() > 0)
			Li = con2.read();
		timer.stop(pid, M.online_read);

		// step 1
		// Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		// Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);

		// step 2
		// timer.start(pid, M.online_write);
		// con2.write(pid, pathTuples);
		// timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
		return Li;
	}

	public OutAccess runC2(Metadata md, Tree OTi, int treeIndex, byte[] Li, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 0: send Li to E and D
		timer.start(pid, M.online_write);
		if (treeIndex > 0) {
			con1.write(Li);
			con2.write(Li);
		}
		timer.stop(pid, M.online_write);

		// step 1
		Bucket[] pathBuckets = OTi.getBucketsOnPath(Li);
		Tuple[] pathTuples = Bucket.bucketsToTuples(pathBuckets);

		// step 2
		// timer.start(pid, M.online_read);
		// Tuple[] pathTuples = con2.readTupleArray(pid);
		// timer.stop(pid, M.online_read);

		// step 5
		Tuple Ti = null;
		if (treeIndex == 0) {
			Ti = pathTuples[0];
		} else {
			Ti = new Tuple(1, md.getNBytesOfTree(treeIndex), md.getLBytesOfTree(treeIndex),
					md.getABytesOfTree(treeIndex), Crypto.sr);
			Ti.setF(new byte[1]);
		}

		OutAccess outaccess = new OutAccess(Li, null, Ti, pathTuples, null, null, null);

		timer.stop(pid, M.online_comp);
		return outaccess;
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		// int records = 5;
		// int repeat = 5;
		//
		// int tau = md.getTau();
		// int numTrees = md.getNumTrees();
		// long numInsert = md.getNumInsertRecords();
		// int addrBits = md.getAddrBits();
		//
		// Timer timer = new Timer();
		//
		// sanityCheck();
		//
		// System.out.println();
		//
		// for (int i = 0; i < records; i++) {
		// long N = Global.cheat ? 0 : Util.nextLong(numInsert, Crypto.sr);
		//
		// for (int j = 0; j < repeat; j++) {
		// System.out.println("Test: " + i + " " + j);
		// System.out.println("N=" + BigInteger.valueOf(N).toString(2));
		//
		// byte[] Li = new byte[0];
		//
		// for (int ti = 0; ti < numTrees; ti++) {
		// long Ni_value = Util.getSubBits(N, addrBits, addrBits -
		// md.getNBitsOfTree(ti));
		// long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
		// Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
		// byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
		// byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);
		//
		// PreData predata = new PreData();
		// PreAccess preaccess = new PreAccess(con1, con2);
		//
		// if (party == Party.Eddie) {
		// Tree OTi = forest.getTree(ti);
		// int numTuples = (OTi.getD() - 1) * OTi.getW() + OTi.getStashSize();
		// int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti),
		// md.getLBytesOfTree(ti),
		// md.getABytesOfTree(ti) };
		// preaccess.runE(predata, md.getTwoTauPow(), numTuples, tupleParam, timer);
		//
		// byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
		// byte[] sD_Ni = Util.xor(Ni, sE_Ni);
		// con1.write(sD_Ni);
		//
		// byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
		// byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
		// con1.write(sD_Nip1_pr);
		//
		// OutAccess outaccess = runE(predata, OTi, sE_Ni, sE_Nip1_pr, timer);
		//
		// if (ti == numTrees - 1) {
		// con2.write(N);
		// con2.write(outaccess.E_Ti);
		// }
		//
		// } else if (party == Party.Debbie) {
		// Tree OTi = forest.getTree(ti);
		// preaccess.runD(predata, timer);
		//
		// byte[] sD_Ni = con1.read();
		//
		// byte[] sD_Nip1_pr = con1.read();
		//
		// runD(predata, OTi, sD_Ni, sD_Nip1_pr, timer);
		//
		// } else if (party == Party.Charlie) {
		// Tree OTi = forest.getTree(ti);
		// preaccess.runC(timer);
		//
		// System.out.println("L" + ti + "=" + new BigInteger(1, Li).toString(2));
		//
		// OutAccess outaccess = runC(md, OTi, ti, Li, timer);
		//
		// Li = outaccess.C_Lip1;
		//
		// if (ti == numTrees - 1) {
		// N = con1.readLong();
		// Tuple E_Ti = con1.readTuple();
		// long data = new BigInteger(1, Util.xor(outaccess.C_Ti.getA(),
		// E_Ti.getA())).longValue();
		// if (N == data) {
		// System.out.println("PIR Access passed");
		// System.out.println();
		// } else {
		// throw new AccessException("PIR Access failed: " + N + " != " + data);
		// }
		// }
		//
		// } else {
		// throw new NoSuchPartyException(party + "");
		// }
		// }
		// }
		// }
		//
		// // timer.print();
	}
}

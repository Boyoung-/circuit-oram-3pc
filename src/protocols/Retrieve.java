package protocols;

import java.math.BigInteger;
import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.AccessException;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Global;
import oram.Metadata;
import oram.Tree;
import oram.Tuple;
import protocols.precomputation.PreRetrieve;
import protocols.struct.OutAccess;
import protocols.struct.OutRetrieve;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Bandwidth;
import util.P;
import util.StopWatch;
import util.Timer;
import util.Util;

public class Retrieve extends Protocol {

	Communication[] cons1;
	Communication[] cons2;

	public Retrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void setCons(Communication[] a, Communication[] b) {
		cons1 = a;
		cons2 = b;
	}

	public void runE(PreData[] predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, int h, Timer timer) {
		// 1st eviction
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		OutAccess outaccess = access.runE(predata[0], OTi, Ni, Nip1_pr, timer);
		Tuple[] path = reshuffle.runE(predata[0], outaccess.E_P, OTi.getTreeIndex() == 0, timer);
		Tuple Ti = postprocesst.runE(predata[0], outaccess.E_Ti, OTi.getTreeIndex() == h - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, OTi.getStashSize());
		root = updateroot.runE(predata[0], OTi.getTreeIndex() == 0, outaccess.Li, root, Ti, timer);
		System.arraycopy(root, 0, path, 0, root.length);
		eviction.runE(predata[0], OTi.getTreeIndex() == 0, outaccess.Li,
				OTi.getTreeIndex() == 0 ? new Tuple[] { Ti } : path, OTi, timer);

		// 2nd eviction
		OutAccess outaccess2 = access.runE2(OTi, timer);
		Tuple[] path2 = outaccess2.E_P;
		Tuple Ti2 = outaccess2.E_Ti;
		Tuple[] root2 = Arrays.copyOfRange(path2, 0, OTi.getStashSize());
		root2 = updateroot.runE(predata[1], OTi.getTreeIndex() == 0, outaccess2.Li, root2, Ti2, timer);
		System.arraycopy(root2, 0, path2, 0, root2.length);
		eviction.runE(predata[1], OTi.getTreeIndex() == 0, outaccess2.Li,
				OTi.getTreeIndex() == 0 ? new Tuple[] { Ti2 } : path2, OTi, timer);
	}

	public void runD(PreData predata[], Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer timer) {
		// 1st eviction
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		byte[] Li = access.runD(predata[0], OTi, Ni, Nip1_pr, timer);
		reshuffle.runD();
		postprocesst.runD();
		updateroot.runD(predata[0], OTi.getTreeIndex() == 0, Li, OTi.getW(), timer);
		eviction.runD(predata[0], OTi.getTreeIndex() == 0, Li, OTi, timer);

		// 2nd eviction
		byte[] Li2 = access.runD2(OTi, timer);
		updateroot.runD(predata[1], OTi.getTreeIndex() == 0, Li2, OTi.getW(), timer);
		eviction.runD(predata[1], OTi.getTreeIndex() == 0, Li2, OTi, timer);
	}

	public OutAccess runC(PreData[] predata, Metadata md, int ti, byte[] Li, Timer timer) {
		// 1st eviction
		Access access = new Access(con1, con2);
		Reshuffle reshuffle = new Reshuffle(con1, con2);
		PostProcessT postprocesst = new PostProcessT(con1, con2);
		UpdateRoot updateroot = new UpdateRoot(con1, con2);
		Eviction eviction = new Eviction(con1, con2);

		OutAccess outaccess = access.runC(md, ti, Li, timer);
		Tuple[] path = reshuffle.runC(predata[0], outaccess.C_P, ti == 0, timer);
		Tuple Ti = postprocesst.runC(predata[0], outaccess.C_Ti, Li, outaccess.C_Lip1, outaccess.C_j2,
				ti == md.getNumTrees() - 1, timer);
		Tuple[] root = Arrays.copyOfRange(path, 0, md.getStashSizeOfTree(ti));
		root = updateroot.runC(predata[0], ti == 0, root, Ti, timer);
		System.arraycopy(root, 0, path, 0, root.length);
		eviction.runC(predata[0], ti == 0, ti == 0 ? new Tuple[] { Ti } : path, md.getLBitsOfTree(ti) + 1,
				md.getStashSizeOfTree(ti), md.getW(), timer);

		// 2nd eviction
		byte[] Li2 = Util.nextBytes(md.getLBytesOfTree(ti), Crypto.sr);
		OutAccess outaccess2 = access.runC2(md, ti, Li2, timer);
		Tuple[] path2 = outaccess2.C_P;
		Tuple Ti2 = outaccess2.C_Ti;
		Tuple[] root2 = Arrays.copyOfRange(path2, 0, md.getStashSizeOfTree(ti));
		root2 = updateroot.runC(predata[1], ti == 0, root2, Ti2, timer);
		System.arraycopy(root2, 0, path2, 0, root2.length);
		eviction.runC(predata[1], ti == 0, ti == 0 ? new Tuple[] { Ti2 } : path2, md.getLBitsOfTree(ti) + 1,
				md.getStashSizeOfTree(ti), md.getW(), timer);

		return outaccess;
	}

	public Pipeline pipelineE(PreData[] predata, Tree OTi, byte[] Ni, byte[] Nip1_pr, int h, Timer[] timer) {
		Access access = new Access(con1, con2);
		OutAccess outaccess = access.runE(predata[0], OTi, Ni, Nip1_pr, timer[0]);

		int ti = OTi.getTreeIndex();
		Pipeline pipeline = new Pipeline(cons1[ti + 1], cons2[ti + 1], Party.Eddie, predata, OTi, h, timer[ti + 1],
				null, ti, outaccess.Li, outaccess);
		pipeline.start();

		return pipeline;
	}

	public Pipeline pipelineD(PreData predata[], Tree OTi, byte[] Ni, byte[] Nip1_pr, Timer[] timer) {
		Access access = new Access(con1, con2);
		byte[] Li = access.runD(predata[0], OTi, Ni, Nip1_pr, timer[0]);

		int ti = OTi.getTreeIndex();
		Pipeline pipeline = new Pipeline(cons1[ti + 1], cons2[ti + 1], Party.Debbie, predata, OTi, 0, timer[ti + 1],
				null, ti, Li, null);
		pipeline.start();

		return pipeline;
	}

	public OutRetrieve pipelineC(PreData[] predata, Metadata md, int ti, byte[] Li, Timer[] timer) {
		Access access = new Access(con1, con2);
		OutAccess outaccess = access.runC(md, ti, Li, timer[0]);

		Pipeline pipeline = new Pipeline(cons1[ti + 1], cons2[ti + 1], Party.Charlie, predata, null, 0, timer[ti + 1],
				md, ti, Li, outaccess);
		pipeline.start();

		return new OutRetrieve(outaccess, pipeline);
	}

	// for testing correctness
	@Override
	public void run(Party party, Metadata md, Forest forest) {
		if (Global.pipeline)
			System.out.println("Pipeline Mode is On");
		if (Global.cheat)
			System.out.println("Cheat Mode is On");

		int records = 25;
		int repeat = 10;
		int reset = 0;

		int tau = md.getTau();
		int numTrees = md.getNumTrees();
		long numInsert = md.getNumInsertRecords();
		int addrBits = md.getAddrBits();

		int numTimer = Global.pipeline ? numTrees + 1 : 1;
		Timer[] timer = new Timer[numTimer];
		for (int i = 0; i < numTimer; i++)
			timer[i] = new Timer();

		StopWatch ete_off = new StopWatch("ETE_offline");
		StopWatch ete_on = new StopWatch("ETE_online");

		long[] gates = new long[2];

		Pipeline[] threads = new Pipeline[numTrees];

		sanityCheck();
		System.out.println();

		for (int i = 0; i < records; i++) {
			long N = Global.cheat ? 0 : Util.nextLong(numInsert, Crypto.sr);

			for (int j = 0; j < repeat; j++) {
				int cycleIndex = i * repeat + j;
				if (cycleIndex == reset * repeat) {
					for (int k = 0; k < timer.length; k++)
						timer[k].reset();
					ete_on.reset();
					ete_off.reset();
				}
				if (cycleIndex == 1) {
					for (int k = 0; k < cons1.length; k++) {
						cons1[k].bandSwitch = false;
						cons2[k].bandSwitch = false;
					}
				}

				System.out.println("Test: " + i + " " + j);
				System.out.println("N=" + BigInteger.valueOf(N).toString(2));

				System.out.print("Precomputation... ");
				PreData[][] predata = new PreData[numTrees][2];
				PreRetrieve preretrieve = new PreRetrieve(con1, con2);
				for (int ti = 0; ti < numTrees; ti++) {
					predata[ti][0] = new PreData();
					predata[ti][1] = new PreData();

					if (party == Party.Eddie) {
						ete_off.start();
						preretrieve.runE(predata[ti], md, ti, timer[0]);
						ete_off.stop();

					} else if (party == Party.Debbie) {
						ete_off.start();
						long[] cnt = preretrieve.runD(predata[ti], md, ti, ti == 0 ? null : predata[ti - 1][0],
								timer[0]);
						ete_off.stop();

						if (cycleIndex == 0) {
							gates[0] += cnt[0];
							gates[1] += cnt[1];
						}

					} else if (party == Party.Charlie) {
						ete_off.start();
						preretrieve.runC(predata[ti], md, ti, ti == 0 ? null : predata[ti - 1][0], timer[0]);
						ete_off.stop();

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}

				sanityCheck();
				System.out.println("done!");

				byte[] Li = new byte[0];
				for (int ti = 0; ti < numTrees; ti++) {
					long Ni_value = Util.getSubBits(N, addrBits, addrBits - md.getNBitsOfTree(ti));
					long Nip1_pr_value = Util.getSubBits(N, addrBits - md.getNBitsOfTree(ti),
							Math.max(addrBits - md.getNBitsOfTree(ti) - tau, 0));
					byte[] Ni = Util.longToBytes(Ni_value, md.getNBytesOfTree(ti));
					byte[] Nip1_pr = Util.longToBytes(Nip1_pr_value, (tau + 7) / 8);

					if (party == Party.Eddie) {
						Tree OTi = forest.getTree(ti);

						byte[] sE_Ni = Util.nextBytes(Ni.length, Crypto.sr);
						byte[] sD_Ni = Util.xor(Ni, sE_Ni);
						con1.write(sD_Ni);

						byte[] sE_Nip1_pr = Util.nextBytes(Nip1_pr.length, Crypto.sr);
						byte[] sD_Nip1_pr = Util.xor(Nip1_pr, sE_Nip1_pr);
						con1.write(sD_Nip1_pr);

						if (!Global.pipeline) {
							ete_on.start();
							runE(predata[ti], OTi, sE_Ni, sE_Nip1_pr, numTrees, timer[0]);
							ete_on.stop();
						} else {
							if (ti == 0)
								ete_on.start();
							threads[ti] = pipelineE(predata[ti], OTi, sE_Ni, sE_Nip1_pr, numTrees, timer);
						}

						if (ti == numTrees - 1)
							con2.write(N);

					} else if (party == Party.Debbie) {
						Tree OTi = forest.getTree(ti);

						byte[] sD_Ni = con1.read();
						byte[] sD_Nip1_pr = con1.read();

						if (!Global.pipeline) {
							ete_on.start();
							runD(predata[ti], OTi, sD_Ni, sD_Nip1_pr, timer[0]);
							ete_on.stop();
						} else {
							if (ti == 0)
								ete_on.start();
							threads[ti] = pipelineD(predata[ti], OTi, sD_Ni, sD_Nip1_pr, timer);
						}

					} else if (party == Party.Charlie) {
						int lBits = md.getLBitsOfTree(ti);
						System.out.println("L" + ti + "="
								+ Util.addZeros(Util.getSubBits(new BigInteger(1, Li), lBits, 0).toString(2), lBits));

						OutAccess outaccess = null;
						if (!Global.pipeline) {
							ete_on.start();
							outaccess = runC(predata[ti], md, ti, Li, timer[0]);
							ete_on.stop();
						} else {
							if (ti == 0)
								ete_on.start();
							OutRetrieve outretrieve = pipelineC(predata[ti], md, ti, Li, timer);
							outaccess = outretrieve.outaccess;
							threads[ti] = outretrieve.pipeline;
						}

						Li = outaccess.C_Lip1;

						if (ti == numTrees - 1) {
							N = con1.readLong();
							long data = new BigInteger(1, outaccess.C_Ti.getA()).longValue();
							if (N == data) {
								System.out.println("Access passed");
								System.out.println();
							} else {
								throw new AccessException("Access failed");
							}
						}

					} else {
						throw new NoSuchPartyException(party + "");
					}
				}

				if (Global.pipeline) {
					for (int ti = 0; ti < numTrees; ti++) {
						try {
							threads[ti].join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					ete_on.stop();
				}
			}
		}
		System.out.println();

		Timer sum = new Timer();
		for (int i = 0; i < timer.length; i++)
			sum = sum.add(timer[i]);
		sum.noPrePrint();
		System.out.println();

		if (Global.pipeline)
			ete_on.elapsedCPU = 0;
		System.out.println(ete_on.noPreToMS());
		System.out.println(ete_off.noPreToMS());
		System.out.println();

		Bandwidth[] bandwidth = new Bandwidth[P.size];
		for (int i = 0; i < P.size; i++) {
			bandwidth[i] = new Bandwidth(P.names[i]);
			for (int j = 0; j < cons1.length; j++)
				bandwidth[i] = bandwidth[i].add(cons1[j].bandwidth[i].add(cons2[j].bandwidth[i]));
			System.out.println(bandwidth[i].noPreToString());
		}
		System.out.println();

		System.out.println(gates[0]);
		System.out.println(gates[1]);
		System.out.println();

		sanityCheck();
	}
}

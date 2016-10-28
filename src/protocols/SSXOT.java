package protocols;

import java.math.BigInteger;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Metadata;
import oram.Tuple;
import protocols.precomputation.PreSSXOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class SSXOT extends Protocol {

	private int id;
	private int pid;

	public SSXOT(Communication con1, Communication con2) {
		super(con1, con2);
		this.id = 0;
		pid = id == 0 ? P.URXOT : P.XOT;
	}

	public SSXOT(Communication con1, Communication con2, int id) {
		super(con1, con2);
		this.id = id;
		pid = id == 0 ? P.URXOT : P.XOT;
	}

	public Tuple[] runE(PreData predata, Tuple[] m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Tuple[] a = new Tuple[m.length];
		for (int i = 0; i < m.length; i++)
			a[i] = m[predata.ssxot_E_pi[id][i]].xor(predata.ssxot_E_r[id][i]);

		timer.start(pid, M.online_write);
		con2.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con2.readTupleArray(pid);

		// step 2
		int[] j = con1.readIntArray(pid);
		Tuple[] p = con1.readTupleArray(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Tuple[] z = new Tuple[j.length];
		for (int i = 0; i < j.length; i++)
			z[i] = a[j[i]].xor(p[i]);

		timer.stop(pid, M.online_comp);
		return z;
	}

	public void runD(PreData predata, int[] index, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 2
		int k = index.length;
		int[] E_j = new int[k];
		int[] C_j = new int[k];
		Tuple[] E_p = new Tuple[k];
		Tuple[] C_p = new Tuple[k];
		for (int i = 0; i < k; i++) {
			E_j[i] = predata.ssxot_E_pi_ivs[id][index[i]];
			C_j[i] = predata.ssxot_C_pi_ivs[id][index[i]];
			E_p[i] = predata.ssxot_E_r[id][E_j[i]].xor(predata.ssxot_delta[id][i]);
			C_p[i] = predata.ssxot_C_r[id][C_j[i]].xor(predata.ssxot_delta[id][i]);
		}

		timer.start(pid, M.online_write);
		con2.write(pid, E_j);
		con2.write(pid, E_p);
		con1.write(pid, C_j);
		con1.write(pid, C_p);
		timer.stop(pid, M.online_write);

		timer.stop(pid, M.online_comp);
	}

	public Tuple[] runC(PreData predata, Tuple[] m, Timer timer) {
		timer.start(pid, M.online_comp);

		// step 1
		Tuple[] a = new Tuple[m.length];
		for (int i = 0; i < m.length; i++)
			a[i] = m[predata.ssxot_C_pi[id][i]].xor(predata.ssxot_C_r[id][i]);

		timer.start(pid, M.online_write);
		con1.write(pid, a);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		a = con1.readTupleArray(pid);

		// step 2
		int[] j = con2.readIntArray(pid);
		Tuple[] p = con2.readTupleArray(pid);
		timer.stop(pid, M.online_read);

		// step 3
		Tuple[] z = new Tuple[j.length];
		for (int i = 0; i < j.length; i++)
			z[i] = a[j[i]].xor(p[i]);

		timer.stop(pid, M.online_comp);
		return z;
	}

	// for testing correctness
	@Override
	public void run(protocols.struct.Party party, Metadata md, oram.Forest forest) {
		Timer timer = new Timer();

		for (int j = 0; j < 100; j++) {
			int n = 100;
			int k = Crypto.sr.nextInt(50) + 50;
			int[] index = Util.randomPermutation(k, Crypto.sr);
			int[] tupleParam = new int[] { 1, 2, 3, 4 };
			Tuple[] E_m = new Tuple[n];
			Tuple[] C_m = new Tuple[n];
			for (int i = 0; i < n; i++) {
				E_m[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], Crypto.sr);
				C_m[i] = new Tuple(tupleParam[0], tupleParam[1], tupleParam[2], tupleParam[3], null);
			}

			PreData predata = new PreData();
			PreSSXOT pressxot = new PreSSXOT(con1, con2, 0);

			if (party == Party.Eddie) {
				pressxot.runE(predata, timer);
				Tuple[] E_out_m = runE(predata, E_m, timer);

				con2.write(E_m);
				con2.write(E_out_m);

			} else if (party == Party.Debbie) {
				pressxot.runD(predata, n, k, tupleParam, timer);
				runD(predata, index, timer);

				con2.write(index);

			} else if (party == Party.Charlie) {
				pressxot.runC(predata, timer);
				Tuple[] C_out_m = runC(predata, C_m, timer);

				index = con2.readIntArray();
				E_m = con1.readTupleArray();
				Tuple[] E_out_m = con1.readTupleArray();

				boolean pass = true;
				for (int i = 0; i < index.length; i++) {
					int input = new BigInteger(1, E_m[index[i]].getA()).intValue();
					int output = new BigInteger(1, Util.xor(E_out_m[i].getA(), C_out_m[i].getA())).intValue();
					if (input != output) {
						System.err.println("SSXOT test failed");
						pass = false;
						break;
					}
				}
				if (pass)
					System.out.println("SSXOT test passed");

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}

		// timer.print();
	}
}

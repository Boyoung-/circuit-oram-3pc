package pir;

import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import oram.Tuple;
import protocols.Protocol;
import protocols.struct.OutULiT;
import protocols.struct.Party;
import protocols.struct.PreData;
import protocols.struct.TwoThreeXorByte;
import protocols.struct.TwoThreeXorInt;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class ULiT extends Protocol {

	private int pid = P.ULiT;

	public ULiT(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public OutULiT runE(PreData predata, TwoThreeXorByte X, TwoThreeXorByte N, TwoThreeXorInt dN, TwoThreeXorByte Lp,
			TwoThreeXorByte Lpi, TwoThreeXorByte Li, int ttp, Timer timer) {
		timer.start(pid, M.offline_comp);

		int l = Li.CE.length;

		timer.start(pid, M.offline_read);
		byte[] x2 = con1.read();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int dN_E = dN.CE;
		byte[] xorLi_E = Util.xor(Lpi.CE, Li.CE);

		InsLbl inslbl = new InsLbl(con1, con2);
		inslbl.runP1(predata, dN_E, xorLi_E, ttp, timer);

		inslbl = new InsLbl(con2, con1);
		byte[] b1 = inslbl.runP3(predata, ttp, l, timer);

		timer.start(pid, M.online_read);
		byte[] me = con1.read(pid);
		timer.stop(pid, M.online_read);

		byte[] x3 = Util.xor(me, b1);

		Util.setXor(X.CE, x3);
		Util.setXor(X.DE, x2);

		OutULiT out = new OutULiT();
		out.CE = new Tuple(new byte[] { 1 }, N.CE, Lp.CE, X.CE);
		out.DE = new Tuple(new byte[] { 1 }, N.DE, Lp.DE, X.DE);

		timer.stop(pid, M.online_comp);
		return out;
	}

	public OutULiT runD(PreData predata, TwoThreeXorByte X, TwoThreeXorByte N, TwoThreeXorInt dN, TwoThreeXorByte Lp,
			TwoThreeXorByte Lpi, TwoThreeXorByte Li, int ttp, Timer timer) {
		timer.start(pid, M.offline_comp);

		byte[] x1 = Util.nextBytes(X.CD.length, Crypto.sr);
		byte[] x2 = Util.nextBytes(X.CD.length, Crypto.sr);

		timer.start(pid, M.offline_write);
		con2.write(x1);
		con1.write(x2);
		timer.stop(pid, M.offline_write);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		int dN_D = dN.CD ^ dN.DE;
		byte[] xorLi_D = Util.xor(Util.xor(Lpi.CD, Li.CD), Util.xor(Lpi.DE, Li.DE));

		InsLbl inslbl = new InsLbl(con1, con2);
		byte[] a2 = inslbl.runP2(predata, dN_D, xorLi_D, ttp, timer);

		inslbl = new InsLbl(con2, con1);
		byte[] a1 = inslbl.runP2(predata, dN_D, xorLi_D, ttp, timer);

		Util.setXor(a1, x1);
		Util.setXor(a1, x2);
		Util.setXor(a2, x1);
		Util.setXor(a2, x2);

		timer.start(pid, M.online_write);
		con1.write(pid, a1);
		con2.write(pid, a2);
		timer.stop(pid, M.online_write);

		Util.setXor(X.CD, x1);
		Util.setXor(X.DE, x2);

		OutULiT out = new OutULiT();
		out.CD = new Tuple(new byte[] { 1 }, N.CD, Lp.CD, X.CD);
		out.DE = new Tuple(new byte[] { 1 }, N.DE, Lp.DE, X.DE);

		timer.stop(pid, M.online_comp);
		return out;
	}

	public OutULiT runC(PreData predata, TwoThreeXorByte X, TwoThreeXorByte N, TwoThreeXorInt dN, TwoThreeXorByte Lp,
			TwoThreeXorByte Lpi, TwoThreeXorByte Li, int ttp, Timer timer) {
		timer.start(pid, M.offline_comp);

		int l = Li.CE.length;

		timer.start(pid, M.offline_read);
		byte[] x1 = con2.read();
		timer.stop(pid, M.offline_read);

		timer.stop(pid, M.offline_comp);

		// ----------------------------------------- //

		timer.start(pid, M.online_comp);

		InsLbl inslbl = new InsLbl(con1, con2);
		byte[] b2 = inslbl.runP3(predata, ttp, l, timer);

		int dN_C = dN.CE;
		byte[] xorLi_C = Util.xor(Lpi.CE, Li.CE);

		inslbl = new InsLbl(con2, con1);
		inslbl.runP1(predata, dN_C, xorLi_C, ttp, timer);

		timer.start(pid, M.online_read);
		byte[] mc = con2.read(pid);
		timer.stop(pid, M.online_read);

		byte[] x3 = Util.xor(mc, b2);

		Util.setXor(X.CD, x1);
		Util.setXor(X.CE, x3);

		OutULiT out = new OutULiT();
		out.CD = new Tuple(new byte[] { 1 }, N.CD, Lp.CD, X.CD);
		out.CE = new Tuple(new byte[] { 1 }, N.CE, Lp.CE, X.CE);

		timer.stop(pid, M.online_comp);
		return out;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int ttp = (int) Math.pow(2, 8);
			int l = 10;
			int Llen = 9;
			int Nlen = 20;
			int Xlen = ttp * l;

			TwoThreeXorInt dN = new TwoThreeXorInt();
			dN.CD = Crypto.sr.nextInt(ttp);
			dN.DE = Crypto.sr.nextInt(ttp);
			dN.CE = Crypto.sr.nextInt(ttp);
			int trueDN = dN.CD ^ dN.CE ^ dN.DE;

			TwoThreeXorByte X = new TwoThreeXorByte();
			X.CD = Util.nextBytes(Xlen, Crypto.sr);
			X.DE = Util.nextBytes(Xlen, Crypto.sr);
			X.CE = Util.nextBytes(Xlen, Crypto.sr);
			TwoThreeXorByte N = new TwoThreeXorByte();
			N.CD = Util.nextBytes(Nlen, Crypto.sr);
			N.DE = Util.nextBytes(Nlen, Crypto.sr);
			N.CE = Util.nextBytes(Nlen, Crypto.sr);
			TwoThreeXorByte Lp = new TwoThreeXorByte();
			Lp.CD = Util.nextBytes(Llen, Crypto.sr);
			Lp.DE = Util.nextBytes(Llen, Crypto.sr);
			Lp.CE = Util.nextBytes(Llen, Crypto.sr);
			TwoThreeXorByte Lpi = new TwoThreeXorByte();
			Lpi.CD = Util.nextBytes(l, Crypto.sr);
			Lpi.DE = Util.nextBytes(l, Crypto.sr);
			Lpi.CE = Util.nextBytes(l, Crypto.sr);

			byte[] trueX = Util.xor(X.CD, X.CE);
			Util.setXor(trueX, X.DE);

			TwoThreeXorByte Li = new TwoThreeXorByte();
			Li.CD = Util.nextBytes(l, Crypto.sr);
			Li.DE = Util.nextBytes(l, Crypto.sr);
			Li.CE = Arrays.copyOfRange(trueX, trueDN * l, trueDN * l + l);
			Util.setXor(Li.CE, Li.CD);
			Util.setXor(Li.CE, Li.DE);

			if (party == Party.Eddie) {
				con1.write(X.CD);
				con1.write(X.DE);
				con1.write(N.CD);
				con1.write(N.DE);
				con1.write(Lp.CD);
				con1.write(Lp.DE);
				con1.write(Lpi.CD);
				con1.write(Lpi.DE);
				con1.write(Li.CD);
				con1.write(Li.DE);
				con1.write(dN.CD);
				con1.write(dN.DE);

				con2.write(X.CD);
				con2.write(X.CE);
				con2.write(N.CD);
				con2.write(N.CE);
				con2.write(Lp.CD);
				con2.write(Lp.CE);
				con2.write(Lpi.CD);
				con2.write(Lpi.CE);
				con2.write(Li.CD);
				con2.write(Li.CE);
				con2.write(dN.CD);
				con2.write(dN.CE);

				OutULiT out = this.runE(predata, X, N, dN, Lp, Lpi, Li, ttp, timer);
				out.CD = con1.readTuple();
				Tuple T = out.CD.xor(out.CE);
				T.setXor(out.DE);
				byte[] trueN = Util.xor(N.CD, N.CE);
				Util.setXor(trueN, N.DE);
				byte[] trueLp = Util.xor(Lp.CD, Lp.CE);
				Util.setXor(trueLp, Lp.DE);
				byte[] trueLpi = Util.xor(Lpi.CD, Lpi.CE);
				Util.setXor(trueLpi, Lpi.DE);
				byte[] expectLpi = Arrays.copyOfRange(T.getA(), trueDN * l, trueDN * l + l);
				byte[] expectX = T.getA();

				boolean fail = false;
				if ((T.getF()[0] & 1) != 1) {
					System.err.println(j + ": ULiT test failed on F");
					fail = true;
				}
				if (!Util.equal(T.getN(), trueN)) {
					System.err.println(j + ": ULiT test failed on N");
					fail = true;
				}
				if (!Util.equal(T.getL(), trueLp)) {
					System.err.println(j + ": ULiT test failed on Lp");
					fail = true;
				}
				if (!Util.equal(expectLpi, trueLpi)) {
					System.err.println(j + ": ULiT test failed on Lpi");
					fail = true;
				}
				for (int i = 0; i < trueDN * l; i++) {
					if (expectX[i] != trueX[i]) {
						System.err.println(j + ": ULiT test failed 1");
						fail = true;
						break;
					}
				}
				for (int i = trueDN * l + l; i < trueX.length; i++) {
					if (expectX[i] != trueX[i]) {
						System.err.println(j + ": ULiT test failed 1");
						fail = true;
						break;
					}
				}
				if (!fail)
					System.out.println(j + ": ULiT test passed");

			} else if (party == Party.Debbie) {
				X.CD = con1.read();
				X.DE = con1.read();
				N.CD = con1.read();
				N.DE = con1.read();
				Lp.CD = con1.read();
				Lp.DE = con1.read();
				Lpi.CD = con1.read();
				Lpi.DE = con1.read();
				Li.CD = con1.read();
				Li.DE = con1.read();
				dN.CD = con1.readInt();
				dN.DE = con1.readInt();

				OutULiT out = this.runD(predata, X, N, dN, Lp, Lpi, Li, ttp, timer);
				con1.write(out.CD);

			} else if (party == Party.Charlie) {
				X.CD = con1.read();
				X.CE = con1.read();
				N.CD = con1.read();
				N.CE = con1.read();
				Lp.CD = con1.read();
				Lp.CE = con1.read();
				Lpi.CD = con1.read();
				Lpi.CE = con1.read();
				Li.CD = con1.read();
				Li.CE = con1.read();
				dN.CD = con1.readInt();
				dN.CE = con1.readInt();

				this.runC(predata, X, N, dN, Lp, Lpi, Li, ttp, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

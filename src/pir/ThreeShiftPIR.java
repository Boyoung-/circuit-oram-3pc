package pir;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.OutPIRCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import protocols.struct.TwoThreeXorByte;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class ThreeShiftPIR extends Protocol {

	private int pid = P.TSPIR;

	public ThreeShiftPIR(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public TwoThreeXorByte runE(PreData predata, byte[][] x_DE, byte[][] x_CE, OutPIRCOT i, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftPIR sftpir = new ShiftPIR(con1, con2);
		byte[] e1 = sftpir.runP1(predata, x_DE, i.s_DE, timer);
		sftpir = new ShiftPIR(con2, con1);
		byte[] e2 = sftpir.runP2(predata, x_CE, i.s_CE, timer);
		sftpir = new ShiftPIR(con1, con2);
		sftpir.runP3(predata, i.t_E, timer);
		Util.setXor(e1, e2);

		TwoThreeXorByte X = new TwoThreeXorByte();
		X.DE = e1;

		timer.start(pid, M.online_write);
		con1.write(pid, X.DE);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		X.CE = con2.read(pid);
		timer.stop(pid, M.online_read);

		timer.stop(pid, M.online_comp);
		return X;
	}

	public TwoThreeXorByte runD(PreData predata, byte[][] x_DE, byte[][] x_CD, OutPIRCOT i, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftPIR sftpir = new ShiftPIR(con1, con2);
		byte[] d1 = sftpir.runP2(predata, x_DE, i.s_DE, timer);
		sftpir = new ShiftPIR(con2, con1);
		sftpir.runP3(predata, i.t_D, timer);
		sftpir = new ShiftPIR(con2, con1);
		byte[] d2 = sftpir.runP1(predata, x_CD, i.s_CD, timer);
		Util.setXor(d1, d2);

		TwoThreeXorByte X = new TwoThreeXorByte();
		X.CD = d1;

		timer.start(pid, M.online_write);
		con2.write(pid, X.CD);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		X.DE = con1.read(pid);
		timer.stop(pid, M.online_read);

		timer.stop(pid, M.online_comp);
		return X;
	}

	public TwoThreeXorByte runC(PreData predata, byte[][] x_CD, byte[][] x_CE, OutPIRCOT i, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftPIR sftpir = new ShiftPIR(con1, con2);
		sftpir.runP3(predata, i.t_C, timer);
		sftpir = new ShiftPIR(con1, con2);
		byte[] c1 = sftpir.runP1(predata, x_CE, i.s_CE, timer);
		sftpir = new ShiftPIR(con2, con1);
		byte[] c2 = sftpir.runP2(predata, x_CD, i.s_CD, timer);
		Util.setXor(c1, c2);

		TwoThreeXorByte X = new TwoThreeXorByte();
		X.CE = c1;

		timer.start(pid, M.online_write);
		con1.write(pid, X.CE);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		X.CD = con2.read(pid);
		timer.stop(pid, M.online_read);

		timer.stop(pid, M.online_comp);
		return X;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int l = 500;
			int m = 50;
			byte[][] x_CD = new byte[l][m];
			byte[][] x_CE = new byte[l][m];
			byte[][] x_DE = new byte[l][m];
			for (int i = 0; i < l; i++) {
				Crypto.sr.nextBytes(x_CD[i]);
				Crypto.sr.nextBytes(x_DE[i]);
				Crypto.sr.nextBytes(x_CE[i]);
			}
			int index = Crypto.sr.nextInt(l);
			OutPIRCOT ks = new OutPIRCOT();
			ks.t_C = Crypto.sr.nextInt(l);
			ks.t_D = Crypto.sr.nextInt(l);
			ks.t_E = Crypto.sr.nextInt(l);
			ks.s_DE = (index - ks.t_C + l) % l;
			ks.s_CE = (index - ks.t_D + l) % l;
			ks.s_CD = (index - ks.t_E + l) % l;

			TwoThreeXorByte X = new TwoThreeXorByte();

			if (party == Party.Eddie) {
				con1.write(x_CD);
				con1.write(x_DE);
				con2.write(x_CD);
				con2.write(x_CE);
				con1.write(ks.t_D);
				con1.write(ks.s_DE);
				con1.write(ks.s_CD);
				con2.write(ks.t_C);
				con2.write(ks.s_CE);
				con2.write(ks.s_CD);

				X = this.runE(predata, x_DE, x_CE, ks, timer);
				X.CD = con1.read();
				byte[] e = X.CE;
				Util.setXor(e, X.CD);
				Util.setXor(e, X.DE);
				byte[] x = x_DE[index];
				Util.setXor(x, x_CE[index]);
				Util.setXor(x, x_CD[index]);

				if (!Util.equal(x, e))
					System.err.println(j + ": 3ShiftPIR test failed");
				else
					System.out.println(j + ": 3ShiftPIR test passed");

			} else if (party == Party.Debbie) {
				x_CD = con1.readDoubleByteArray();
				x_DE = con1.readDoubleByteArray();
				ks.t_D = con1.readInt();
				ks.s_DE = con1.readInt();
				ks.s_CD = con1.readInt();

				X = this.runD(predata, x_DE, x_CD, ks, timer);
				con1.write(X.CD);

			} else if (party == Party.Charlie) {
				x_CD = con1.readDoubleByteArray();
				x_CE = con1.readDoubleByteArray();
				ks.t_C = con1.readInt();
				ks.s_CE = con1.readInt();
				ks.s_CD = con1.readInt();

				this.runC(predata, x_CD, x_CE, ks, timer);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

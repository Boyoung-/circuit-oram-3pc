package pir;

import java.util.Arrays;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.OutPIRCOT;
import protocols.struct.Party;
import protocols.struct.PreData;
import protocols.struct.TwoOneXor;
import util.M;
import util.P;
import util.Timer;
import util.Util;

public class ThreeShiftXorPIR extends Protocol {

	private int pid = P.TSXPIR;

	public ThreeShiftXorPIR(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public byte[] runE(PreData predata, byte[][] x_DE, byte[][] x_CE, OutPIRCOT i, TwoOneXor j, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftXorPIR sftpir = new ShiftXorPIR(con1, con2);
		byte[] e1 = sftpir.runP1(predata, x_DE, i.s_DE, j.s_DE, m, timer);
		sftpir = new ShiftXorPIR(con2, con1);
		byte[] e2 = sftpir.runP2(predata, x_CE, i.s_CE, j.s_CE, m, timer);
		sftpir = new ShiftXorPIR(con1, con2);
		sftpir.runP3(predata, i.t_E, j.t_E, m, timer);
		Util.setXor(e1, e2);

		timer.start(pid, M.online_write);
		con1.write(pid, e1);
		con2.write(pid, e1);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[] d = con1.read(pid);
		byte[] c = con2.read(pid);
		timer.stop(pid, M.online_read);

		Util.setXor(e1, d);
		Util.setXor(e1, c);

		timer.stop(pid, M.online_comp);
		return e1;
	}

	public byte[] runD(PreData predata, byte[][] x_DE, byte[][] x_CD, OutPIRCOT i, TwoOneXor j, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftXorPIR sftpir = new ShiftXorPIR(con1, con2);
		byte[] d1 = sftpir.runP2(predata, x_DE, i.s_DE, j.s_DE, m, timer);
		sftpir = new ShiftXorPIR(con2, con1);
		sftpir.runP3(predata, i.t_D, j.t_D, m, timer);
		sftpir = new ShiftXorPIR(con2, con1);
		byte[] d2 = sftpir.runP1(predata, x_CD, i.s_CD, j.s_CD, m, timer);
		Util.setXor(d1, d2);

		timer.start(pid, M.online_write);
		con1.write(pid, d1);
		con2.write(pid, d1);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[] e = con1.read(pid);
		byte[] c = con2.read(pid);
		timer.stop(pid, M.online_read);

		Util.setXor(d1, e);
		Util.setXor(d1, c);

		timer.stop(pid, M.online_comp);
		return d1;
	}

	public byte[] runC(PreData predata, byte[][] x_CD, byte[][] x_CE, OutPIRCOT i, TwoOneXor j, int m, Timer timer) {
		timer.start(pid, M.online_comp);

		ShiftXorPIR sftpir = new ShiftXorPIR(con1, con2);
		sftpir.runP3(predata, i.t_C, j.t_C, m, timer);
		sftpir = new ShiftXorPIR(con1, con2);
		byte[] c1 = sftpir.runP1(predata, x_CE, i.s_CE, j.s_CE, m, timer);
		sftpir = new ShiftXorPIR(con2, con1);
		byte[] c2 = sftpir.runP2(predata, x_CD, i.s_CD, j.s_CD, m, timer);
		Util.setXor(c1, c2);

		timer.start(pid, M.online_write);
		con1.write(pid, c1);
		con2.write(pid, c1);
		timer.stop(pid, M.online_write);

		timer.start(pid, M.online_read);
		byte[] e = con1.read(pid);
		byte[] d = con2.read(pid);
		timer.stop(pid, M.online_read);

		Util.setXor(c1, e);
		Util.setXor(c1, d);

		timer.stop(pid, M.online_comp);
		return c1;
	}

	@Override
	public void run(Party party, Metadata md, Forest[] forest) {

		Timer timer = new Timer();
		PreData predata = new PreData();

		for (int j = 0; j < 100; j++) {
			int n = 500;
			int m = 16;
			int l = 4;
			byte[][] x_CD = new byte[n][m * l];
			byte[][] x_CE = new byte[n][m * l];
			byte[][] x_DE = new byte[n][m * l];
			for (int i = 0; i < n; i++) {
				Crypto.sr.nextBytes(x_CD[i]);
				Crypto.sr.nextBytes(x_DE[i]);
				Crypto.sr.nextBytes(x_CE[i]);
			}
			int i1 = Crypto.sr.nextInt(n);
			OutPIRCOT ks = new OutPIRCOT();
			ks.t_C = Crypto.sr.nextInt(n);
			ks.t_D = Crypto.sr.nextInt(n);
			ks.t_E = Crypto.sr.nextInt(n);
			ks.s_DE = (i1 - ks.t_C + n) % n;
			ks.s_CE = (i1 - ks.t_D + n) % n;
			ks.s_CD = (i1 - ks.t_E + n) % n;

			int i2 = Crypto.sr.nextInt(m);
			TwoOneXor tox = new TwoOneXor();
			tox.t_C = Crypto.sr.nextInt(m);
			tox.t_D = Crypto.sr.nextInt(m);
			tox.t_E = Crypto.sr.nextInt(m);
			tox.s_DE = i2 ^ tox.t_C;
			tox.s_CE = i2 ^ tox.t_D;
			tox.s_CD = i2 ^ tox.t_E;

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
				con1.write(tox.t_D);
				con1.write(tox.s_DE);
				con1.write(tox.s_CD);
				con2.write(tox.t_C);
				con2.write(tox.s_CE);
				con2.write(tox.s_CD);

				byte[] e = this.runE(predata, x_DE, x_CE, ks, tox, m, timer);
				byte[] d = con1.read();
				byte[] c = con2.read();

				byte[] x = x_DE[i1];
				Util.setXor(x, x_CE[i1]);
				Util.setXor(x, x_CD[i1]);
				byte[] expect = Arrays.copyOfRange(x, i2 * l, (i2 + 1) * l);

				if (!Util.equal(expect, e) || !Util.equal(expect, d) || !Util.equal(expect, c))
					System.err.println(j + ": 3ShiftXorPIR test failed");
				else
					System.out.println(j + ": 3ShiftXorPIR test passed");

			} else if (party == Party.Debbie) {
				x_CD = con1.readDoubleByteArray();
				x_DE = con1.readDoubleByteArray();
				ks.t_D = con1.readInt();
				ks.s_DE = con1.readInt();
				ks.s_CD = con1.readInt();
				tox.t_D = con1.readInt();
				tox.s_DE = con1.readInt();
				tox.s_CD = con1.readInt();

				byte[] d = this.runD(predata, x_DE, x_CD, ks, tox, m, timer);
				con1.write(d);

			} else if (party == Party.Charlie) {
				x_CD = con1.readDoubleByteArray();
				x_CE = con1.readDoubleByteArray();
				ks.t_C = con1.readInt();
				ks.s_CE = con1.readInt();
				ks.s_CD = con1.readInt();
				tox.t_C = con1.readInt();
				tox.s_CE = con1.readInt();
				tox.s_CD = con1.readInt();

				byte[] c = this.runC(predata, x_CD, x_CE, ks, tox, m, timer);
				con1.write(c);

			} else {
				throw new NoSuchPartyException(party + "");
			}
		}
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

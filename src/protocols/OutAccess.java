package protocols;

import oram.Tuple;

public class OutAccess {
	public byte[] Li;
	public byte[] C_Lip1;
	public Tuple E_Ti;
	public Tuple C_Ti;
	public Tuple[] E_P;
	public Tuple[] C_P;
	public Integer C_j2;

	public OutAccess(byte[] Li, byte[] C_Lip1, Tuple C_Ti, Tuple[] C_P, Integer C_j2, Tuple E_Ti, Tuple[] E_P) {
		this.Li = Li;
		this.C_Lip1 = C_Lip1;
		this.E_Ti = E_Ti;
		this.C_Ti = C_Ti;
		this.E_P = E_P;
		this.C_P = C_P;
		this.C_j2 = C_j2;
	}
}

package protocols;

import oram.Tuple;

public class PreData {
	public byte[] sscot_k;
	public byte[] sscot_kprime;
	public byte[][] sscot_r;

	public byte[] ssiot_k;
	public byte[] ssiot_kprime;
	public byte[] ssiot_r;

	public int[] access_sigma;
	// public int[] access_rho;
	public Tuple[] access_p;
}

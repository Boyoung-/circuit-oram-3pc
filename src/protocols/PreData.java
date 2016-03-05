package protocols;

import oram.Bucket;

public class PreData {
	public byte[] sscot_k;
	public byte[] sscot_kprime;
	public byte[][] sscot_r;

	public byte[] ssiot_k;
	public byte[] ssiot_kprime;
	public byte[] ssiot_r;

	public int[] access_sigma;
	public int[] access_delta;
	public int[] access_rho;
	public Bucket[] access_p;
}

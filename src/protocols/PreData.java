package protocols;

import crypto.PRF;
import oram.Tuple;

public class PreData {
	public byte[] sscot_k;
	public byte[] sscot_kprime;
	public byte[][] sscot_r;
	public PRF sscot_F_k;
	public PRF sscot_F_kprime;

	public byte[] ssiot_k;
	public byte[] ssiot_kprime;
	public byte[] ssiot_r;
	public PRF ssiot_F_k;
	public PRF ssiot_F_kprime;

	public int[] access_sigma;
	public Tuple[] access_p;

	public Tuple[] ssxot_delta;
	public int[] ssxot_E_pi;
	public int[] ssxot_C_pi;
	public int[] ssxot_E_pi_ivs;
	public int[] ssxot_C_pi_ivs;
	public Tuple[] ssxot_E_r;
	public Tuple[] ssxot_C_r;
}

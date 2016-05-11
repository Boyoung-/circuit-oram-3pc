package protocols.struct;

import java.math.BigInteger;

import com.oblivm.backend.gc.GCSignal;

import crypto.PRF;
import gc.GCRoute;
import gc.GCUpdateRoot;
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

	public Tuple[][] ssxot_delta = new Tuple[2][];
	public int[][] ssxot_E_pi = new int[2][];
	public int[][] ssxot_C_pi = new int[2][];
	public int[][] ssxot_E_pi_ivs = new int[2][];
	public int[][] ssxot_C_pi_ivs = new int[2][];
	public Tuple[][] ssxot_E_r = new Tuple[2][];
	public Tuple[][] ssxot_C_r = new Tuple[2][];

	public byte[] ppt_Li;
	public byte[] ppt_Lip1;
	public int ppt_alpha;
	public byte[][] ppt_r;
	public byte[][] ppt_s;

	public int[] reshuffle_pi;
	public Tuple[] reshuffle_p;
	public Tuple[] reshuffle_r;
	public Tuple[] reshuffle_a_prime;

	public GCSignal[][] ur_j1KeyPairs;
	public GCSignal[][] ur_LiKeyPairs;
	public GCSignal[][] ur_E_feKeyPairs;
	public GCSignal[][] ur_C_feKeyPairs;
	public GCSignal[][][] ur_E_labelKeyPairs;
	public GCSignal[][][] ur_C_labelKeyPairs;
	public BigInteger[][] ur_outKeyHashes;
	public GCUpdateRoot<GCSignal> ur_gcur;

	public GCSignal[][] evict_LiKeyPairs;
	public GCSignal[][][] evict_E_feKeyPairs;
	public GCSignal[][][] evict_C_feKeyPairs;
	public GCSignal[][][][] evict_E_labelKeyPairs;
	public GCSignal[][][][] evict_C_labelKeyPairs;
	public GCSignal[][][] evict_deltaKeyPairs;
	public BigInteger[][] evict_tiOutKeyHashes;
	public GCSignal[][][] evict_targetOutKeyPairs;
	public GCRoute<GCSignal> evict_gcroute;
	public int[] evict_pi;
	public BigInteger[] evict_delta;
	public BigInteger[] evict_rho;
	public int[][] evict_delta_p;
	public int[][] evict_rho_p;

	// PermuteTargetI
	public BigInteger[][] pt_maskT;
	public BigInteger[][] pt_keyT;
	public BigInteger[][] pt_targetT;
	// PermuteTargetII
	public BigInteger[] pt_p;
	public BigInteger[] pt_r;
	public BigInteger[] pt_a;

	// PermuteIndex
	public BigInteger[] pi_p;
	public BigInteger[] pi_r;
	public BigInteger[] pi_a;
}

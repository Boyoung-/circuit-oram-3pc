package precomputation;

import communication.Communication;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.precomputation.PreEviction;
import protocols.precomputation.PrePostProcessT;
import protocols.precomputation.PreReshuffle;
import protocols.precomputation.PreUpdateRoot;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Timer;

public class PrePIRRetrieve extends Protocol {
	public PrePIRRetrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	// TODO: not all protocols run on all trees (remove unnecessary precomp)

	public void runE(PreData[] predata, Metadata md, int ti, Timer timer) {
		// 1st eviction
		PrePIRAccess preaccess = new PrePIRAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		PreEviction preeviction = new PreEviction(con1, con2);

		int numTuples = md.getStashSizeOfTree(ti) + md.getLBitsOfTree(ti) * md.getW();
		int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
				md.getABytesOfTree(ti) };

		preaccess.runE(predata[0], md.getTwoTauPow(), numTuples, tupleParam, timer);
		prereshuffle.runE(predata[0], timer);
		prepostprocesst.runE(predata[0], timer);
		preupdateroot.runE(predata[0], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), timer);
		preeviction.runE(predata[0], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), timer);

		// 2nd eviction
		preupdateroot.runE(predata[1], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), timer);
		preeviction.runE(predata[1], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), timer);
	}

	public long[] runD(PreData[] predata, Metadata md, int ti, PreData prev, Timer timer) {
		// 1st eviction
		PrePIRAccess preaccess = new PrePIRAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		PreEviction preeviction = new PreEviction(con1, con2);

		int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
				md.getABytesOfTree(ti) };
		long[] cnt = new long[2];

		preaccess.runD(predata[0], timer);
		prereshuffle.runD(predata[0], tupleParam, timer);
		prepostprocesst.runD(predata[0], prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), md.getTau(), timer);
		cnt[0] += preupdateroot.runD(predata[0], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), tupleParam,
				timer);
		cnt[1] += preeviction.runD(predata[0], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), tupleParam, timer);

		// 2nd eviction
		cnt[0] += preupdateroot.runD(predata[1], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), tupleParam,
				timer);
		cnt[1] += preeviction.runD(predata[1], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), tupleParam, timer);

		return cnt;
	}

	public void runC(PreData[] predata, Metadata md, int ti, PreData prev, Timer timer) {
		// 1st eviction
		PrePIRAccess preaccess = new PrePIRAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		PreEviction preeviction = new PreEviction(con1, con2);

		preaccess.runC(timer);
		prereshuffle.runC(predata[0], timer);
		prepostprocesst.runC(predata[0], prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), timer);
		preupdateroot.runC(predata[0], ti == 0, timer);
		preeviction.runC(predata[0], ti == 0, timer);

		// 2nd eviction
		preupdateroot.runC(predata[1], ti == 0, timer);
		preeviction.runC(predata[1], ti == 0, timer);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

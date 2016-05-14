package protocols.precomputation;

import communication.Communication;
import oram.Forest;
import oram.Metadata;
import protocols.Protocol;
import protocols.struct.Party;
import protocols.struct.PreData;
import util.Timer;

public class PreRetrieve extends Protocol {
	public PreRetrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	// TODO: not all protocols run on all trees (remove unnecessary precomp)

	public void runE(PreData[] predata, Metadata md, int ti, Timer timer) {
		// 1st eviction
		PreAccess preaccess = new PreAccess(con1, con2);
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

	public void runD(PreData[] predata, Metadata md, int ti, PreData prev, Timer timer) {
		// 1st eviction
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		PreUpdateRoot preupdateroot = new PreUpdateRoot(con1, con2);
		PreEviction preeviction = new PreEviction(con1, con2);

		int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
				md.getABytesOfTree(ti) };

		preaccess.runD(predata[0], timer);
		prereshuffle.runD(predata[0], tupleParam, timer);
		prepostprocesst.runD(predata[0], prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), md.getTau(), timer);
		preupdateroot.runD(predata[0], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), tupleParam, timer);
		preeviction.runD(predata[0], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), tupleParam, timer);

		// 2nd eviction
		preupdateroot.runD(predata[1], ti == 0, md.getStashSizeOfTree(ti), md.getLBitsOfTree(ti), tupleParam, timer);
		preeviction.runD(predata[1], ti == 0, md.getLBitsOfTree(ti) + 1, md.getW(), tupleParam, timer);
	}

	public void runC(PreData[] predata, Metadata md, int ti, PreData prev, Timer timer) {
		// 1st eviction
		PreAccess preaccess = new PreAccess(con1, con2);
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

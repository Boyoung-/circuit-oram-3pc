package protocols;

import communication.Communication;
import measure.Timer;
import oram.Forest;
import oram.Metadata;

public class PreRetrieve extends Protocol {
	public PreRetrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Metadata md, int ti, Timer timer) {
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		
		int numTuples = md.getStashSizeOfTree(ti) + md.getLBitsOfTree(ti) * md.getW();
		int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
				md.getABytesOfTree(ti) };
		
		preaccess.runE(predata, md.getTwoTauPow(), numTuples, tupleParam, timer);
		prereshuffle.runE(predata, timer);
		prepostprocesst.runE(predata, timer);
	}

	public void runD(PreData predata, Metadata md, int ti, PreData prev, Timer timer) {
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2); 
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		
		int[] tupleParam = new int[] { ti == 0 ? 0 : 1, md.getNBytesOfTree(ti), md.getLBytesOfTree(ti),
				md.getABytesOfTree(ti) };
		
		preaccess.runD(predata, timer);
		prereshuffle.runD(predata, tupleParam, timer);
		prepostprocesst.runD(predata, prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), md.getTau(), timer);
	}

	public void runC(PreData predata, Metadata md, int ti, PreData prev, Timer timer) {
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2); 
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		
		preaccess.runC(timer);
		prereshuffle.runC(predata, timer);
		prepostprocesst.runC(predata, prev, md.getLBytesOfTree(ti), md.getAlBytesOfTree(ti), timer);
	}

	@Override
	public void run(Party party, Metadata md, Forest forest) {
	}
}

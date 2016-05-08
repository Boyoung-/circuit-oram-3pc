package protocols;

import communication.Communication;
import measure.Timer;
import oram.Forest;
import oram.Metadata;
import oram.Tree;

public class PreRetrieve extends Protocol {
	public PreRetrieve(Communication con1, Communication con2) {
		super(con1, con2);
	}

	public void runE(PreData predata, Metadata md, int ti, Tree OT, int numTuples, Timer timer) {
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2);
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		
		preaccess.runE(predata, OT, numTuples, timer);
		prereshuffle.runE(predata, timer);
		prepostprocesst.runE(predata, timer);
	}

	public void runD(PreData predata, Metadata md, int ti, PreData prev, int[] tupleParam, Timer timer) {
		PreAccess preaccess = new PreAccess(con1, con2);
		PreReshuffle prereshuffle = new PreReshuffle(con1, con2); 
		PrePostProcessT prepostprocesst = new PrePostProcessT(con1, con2);
		
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

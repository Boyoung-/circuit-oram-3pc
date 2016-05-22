package protocols.struct;

import protocols.Pipeline;

public class OutRetrieve {
	public OutAccess outaccess;
	public Pipeline pipeline;

	public OutRetrieve(OutAccess outaccess, Pipeline pipeline) {
		this.outaccess = outaccess;
		this.pipeline = pipeline;
	}
}

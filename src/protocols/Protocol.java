package protocols;

import communication.Communication;
import crypto.Crypto;
import exceptions.NoSuchPartyException;
import oram.Forest;
import oram.Metadata;
import protocols.struct.Party;

public abstract class Protocol {
	protected Communication con1;
	protected Communication con2;

	/*
	 * Connections are alphabetized so:
	 * 
	 * For Eddie con1 = debbie con2 = charlie
	 * 
	 * For Debbie con1 = eddie con2 = charlie
	 * 
	 * For Charlie con1 = eddie con2 = debbie
	 */
	public Protocol(Communication con1, Communication con2) {
		this.con1 = con1;
		this.con2 = con2;
	}

	private static final boolean ENSURE_SANITY = true;

	public boolean ifSanityCheck() {
		return ENSURE_SANITY;
	}

	// Utility function will test for synchrony between the parties.
	public void sanityCheck() {
		if (ENSURE_SANITY) {

			// System.out.println("Sanity check");
			con1.write("sanity");
			con2.write("sanity");

			if (!con1.readString().equals("sanity")) {
				System.out.println("Sanity check failed for con1");
			}
			if (!con2.readString().equals("sanity")) {
				System.out.println("Sanity check failed for con2");
			}
		}
	}

	public void run(Party party, String configFile, String forestFile) {
		Metadata md = new Metadata(configFile);
		Forest forest = null;

		Metadata.cheat = true;

		if (party == Party.Eddie) {
			if (Metadata.cheat)
				forest = new Forest(md, Crypto.sr);
			else if (forestFile == null)
				forest = Forest.readFromFile(md.getDefaultSharesName1());
			else
				forest = Forest.readFromFile(forestFile);
		} else if (party == Party.Debbie) {
			if (Metadata.cheat)
				forest = new Forest(md, null);
			else if (forestFile == null)
				forest = Forest.readFromFile(md.getDefaultSharesName2());
			else
				forest = Forest.readFromFile(forestFile);
		} else if (party == Party.Charlie) {

		} else {
			throw new NoSuchPartyException(party.toString());
		}

		run(party, md, forest);
	}

	/*
	 * This is mostly just testing code and may need to change for the purpose
	 * of an actual execution
	 */
	public abstract void run(Party party, Metadata md, Forest forest);
}

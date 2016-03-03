package ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import communication.Communication;
import exceptions.NoSuchPartyException;
import protocols.Party;
import protocols.Protocol;
import protocols.SSCOT;

public class CLI {
	public static final int DEFAULT_PORT = 8000;
	public static final String DEFAULT_IP = "localhost";

	public static void main(String[] args) {
		// Setup command line argument parser
		Options options = new Options();
		options.addOption("config", true, "Config file");
		options.addOption("forest", true, "Forest file");
		options.addOption("eddie_ip", true, "IP to look for eddie");
		options.addOption("debbie_ip", true, "IP to look for debbie");
		options.addOption("protocol", true, "Algorithim to test");

		// Parse the command line arguments
		CommandLineParser cmdParser = new GnuParser();
		CommandLine cmd = null;
		try {
			cmd = cmdParser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		String configFile = cmd.getOptionValue("config", "config.yaml");
		String forestFile = cmd.getOptionValue("forest", null);

		String party = null;
		String[] positionalArgs = cmd.getArgs();
		if (positionalArgs.length > 0) {
			party = positionalArgs[0];
		} else {
			try {
				throw new ParseException("No party specified");
			} catch (ParseException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		int extra_port = 1;
		int eddiePort1 = DEFAULT_PORT;
		int eddiePort2 = eddiePort1 + extra_port;
		int debbiePort = eddiePort2 + extra_port;

		String eddieIp = cmd.getOptionValue("eddie_ip", DEFAULT_IP);
		String debbieIp = cmd.getOptionValue("debbie_ip", DEFAULT_IP);

		Class<? extends Protocol> operation = null;
		String protocol = cmd.getOptionValue("protocol", "retrieve").toLowerCase();

		if (protocol.equals("sscot")) {
			operation = SSCOT.class;
		} else {
			System.out.println("Protocol " + protocol + " not supported");
			System.exit(-1);
		}

		Constructor<? extends Protocol> operationCtor = null;
		try {
			operationCtor = operation.getDeclaredConstructor(Communication.class, Communication.class);
		} catch (NoSuchMethodException | SecurityException e1) {
			e1.printStackTrace();
		}

		// For now all logic happens here. Eventually this will get wrapped
		// up in party specific classes.
		System.out.println("Starting " + party + "...");

		if (party.equals("eddie")) {
			Communication debbieCon = new Communication();
			debbieCon.start(eddiePort1);

			Communication charlieCon = new Communication();
			charlieCon.start(eddiePort2);

			System.out.println("Waiting to establish connections...");
			while (debbieCon.getState() != Communication.STATE_CONNECTED)
				;
			while (charlieCon.getState() != Communication.STATE_CONNECTED)
				;
			System.out.println("Connection established.");

			debbieCon.setTcpNoDelay(true);
			charlieCon.setTcpNoDelay(true);

			debbieCon.write("start");
			charlieCon.write("start");
			debbieCon.readString();
			charlieCon.readString();

			try {
				operationCtor.newInstance(debbieCon, charlieCon).run(Party.Eddie, configFile, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

			debbieCon.write("end");
			charlieCon.write("end");
			debbieCon.readString();
			charlieCon.readString();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			debbieCon.stop();
			charlieCon.stop();

		} else if (party.equals("debbie")) {
			Communication eddieCon = new Communication();
			InetSocketAddress eddieAddr = new InetSocketAddress(eddieIp, eddiePort1);
			eddieCon.connect(eddieAddr);

			Communication charlieCon = new Communication();
			charlieCon.start(debbiePort);

			System.out.println("Waiting to establish connections...");
			while (eddieCon.getState() != Communication.STATE_CONNECTED)
				;
			while (charlieCon.getState() != Communication.STATE_CONNECTED)
				;
			System.out.println("Connection established");

			eddieCon.setTcpNoDelay(true);
			charlieCon.setTcpNoDelay(true);

			eddieCon.write("start");
			charlieCon.write("start");
			eddieCon.readString();
			charlieCon.readString();

			try {
				operationCtor.newInstance(eddieCon, charlieCon).run(Party.Debbie, configFile, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

			eddieCon.write("end");
			charlieCon.write("end");
			eddieCon.readString();
			charlieCon.readString();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			eddieCon.stop();
			charlieCon.stop();

		} else if (party.equals("charlie")) {
			Communication debbieCon = new Communication();
			Communication eddieCon = new Communication();
			InetSocketAddress eddieAddr = new InetSocketAddress(eddieIp, eddiePort2);
			eddieCon.connect(eddieAddr);
			InetSocketAddress debbieAddr = new InetSocketAddress(debbieIp, debbiePort);
			debbieCon.connect(debbieAddr);

			System.out.println("Waiting to establish connections...");
			while (eddieCon.getState() != Communication.STATE_CONNECTED)
				;
			while (debbieCon.getState() != Communication.STATE_CONNECTED)
				;
			System.out.println("Connection established");

			eddieCon.setTcpNoDelay(true);
			debbieCon.setTcpNoDelay(true);

			eddieCon.write("start");
			debbieCon.write("start");
			eddieCon.readString();
			debbieCon.readString();

			try {
				operationCtor.newInstance(eddieCon, debbieCon).run(Party.Charlie, configFile, forestFile);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

			eddieCon.write("end");
			debbieCon.write("end");
			eddieCon.readString();
			debbieCon.readString();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			eddieCon.stop();
			debbieCon.stop();

		} else {
			throw new NoSuchPartyException(party);
		}

		System.out.println(party + " exiting...");
	}
}

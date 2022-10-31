package com.xforgie.simplednsclient;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Main {

	@SuppressWarnings("unused")
	private static String[] testargs = {"-s", "f.root-servers.net"};
	
	@SuppressWarnings("unused")
	private static String[] testargshelp = {"-h"};
	
	private static final String VERSION = "1.0.0";
	
	private static enum Opt {
		
		SERVER(Option.builder("s").longOpt("server").
				desc("The hostname of the root nameserver").hasArg().argName("IPV4").required().build()),
		TRACE(Option.builder("t").longOpt("trace").
				desc("Enables tracing").required(false).build()),
		VERSION(Option.builder("v").longOpt("version").
				desc("Prints the version").required(false).build()),
		HELP(Option.builder("h").longOpt("help").
				desc("Prints usage").required(false).build());
		
		private final Option opt;
		private Opt(Option opt) {this.opt = opt;}
		public Option getOption() {return opt;}
		public String getOptString() {return opt.getOpt();}
	};
	
	private static boolean printHelpOrVersion(String[] args, Options completeOptions) {
		
		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(Opt.HELP.getOption());
		optionGroup.addOption(Opt.VERSION.getOption());
		
		Options options = new Options();
		options.addOptionGroup(optionGroup);
		
		try {
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args, false);
		
		if (cmd.hasOption(Opt.HELP.getOptString())) {
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(
					"Simple DNS Server", 
					"Runs a simple DNS client", 
					completeOptions, "\n", true);
			
			return true;
		} else if (cmd.hasOption(Opt.VERSION.getOptString())) {
			
			System.out.println("Simple DNS Server\n\tv" + VERSION);
			return true;
		}
		
		} catch(ParseException e) {
			// Ignore this parse exception
			return false;
		}
		
		return false;
	}
	
	private static SimpleDNSClient parseArgs(String[] args) {
		
		Options options = new Options();
		for (Opt opt : Opt.values())
			options.addOption(opt.getOption());

		try {
			
			if (printHelpOrVersion(args, options))
				return null;

			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args, false);
			
			if (cmd.hasOption(Opt.SERVER.getOptString())) {
				
				InetAddress rootAddress = InetAddress.getByName(
						((String)cmd.getParsedOptionValue(Opt.SERVER.getOptString())).trim());

				return new SimpleDNSClient(rootAddress, cmd.hasOption(Opt.TRACE.getOption()));
				
			}
			
		} catch (ParseException e) {
			
			// e.printStackTrace();
			System.out.println("Invalid argument(s). Use -h or --help for usage.");			
		} catch (UnknownHostException e) {
			// e.printStackTrace();
			System.out.println("Root address hostname could not be resolved.");
		}
		
		return null;
	}
	
	public static void main(String[] args) throws SocketException {
		
		SimpleDNSClient client = parseArgs(args);
		if (client == null)
			System.exit(1);

		client.run();
	}
}

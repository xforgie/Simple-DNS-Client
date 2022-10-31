package com.xforgie.simplednsclient;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.xforgie.simplednsclient.responseerrors.QueryHaltException;

public class SimpleDNSClient {
	
	private static final int MAX_INDIRECTIONS = 10;
	
	private InetAddress rootAddress;
	private boolean tracing;
	private Cache cache;
	
	public SimpleDNSClient(InetAddress rootAddress, boolean tracing) {
		this.rootAddress = rootAddress;
		this.tracing = tracing;
		cache = Cache.getCache();
	}
	
	public SimpleDNSClient(InetAddress rootAddress) {
		this(rootAddress, false);
	}
	
	// Initiates a new query to the next server in the iterative query
	// returns a boolean indicating whether the query should close
	private boolean queryNextLevel(SearchNode node, Set<ResourceRecord> nameservers) {

        // if there are results in the cache that match node or no NS were returned, exit
        if (getResultFromCache(node) != null || nameservers.isEmpty())
            return false;

        // if we reach an SOA, end query
        for (ResourceRecord r : nameservers) {
            if (r.getType() == RecordType.SOA)
                return true;
        }

        // a set containing NS records for which we have IPv4 addresses for
        Set<ResourceRecord> setToSearch = new LinkedHashSet<>();

        // fill setToSearch with NS records that are in the cache that have IPv4 addresses
        nameservers.forEach(ns -> setToSearch.addAll(
                cache.getResourceRecords(new SearchNode(ns.getTextResult(), RecordType.A))));

        // if there is nothing to search, search for an IPv4 address for the first nameserver
        // otherwise, continue the search using the first match that we found
        if (setToSearch.isEmpty()) {
            ResourceRecord ns = nameservers.iterator().next();
            SearchNode nameserverSearchNode = new SearchNode(ns.getTextResult(), RecordType.A);
            Set<ResourceRecord> searchNS = searchDNS(nameserverSearchNode, 0);

            // if we couldn't find an address for the nameserver, fail the query
            if (searchNS.isEmpty())
                return true;

            // if we found an address, resume searching from original position in which we were stuck
            return queryServer(node, searchNS.iterator().next().getInetResult());

        } else {
            ResourceRecord r = setToSearch.iterator().next();
            return queryServer(node, r.getInetResult());
        }
    }
	
	// Begins an iterative query to the specified server
	// returns a boolean indicating whether the query should close
	private boolean queryServer(SearchNode node, InetAddress server) {

        try {

            Set<ResourceRecord> nameservers = QueryHandler.sendQueryAndCacheResponse(server, node);

            // if server response failed, fail query
            if (nameservers == null)
                return true;

            // begin iterative query
            return queryNextLevel(node, nameservers);

        } catch (QueryHaltException e) {
            return true;
        }
    }
	
	// returns DNSNode if there's a match to node, or a CNAME match to node
    private SearchNode getResultFromCache(SearchNode node) {

        // Check for record matching node being searched for
        Set<ResourceRecord> rrSet = cache.getResourceRecords(node);
        if (!rrSet.isEmpty())
            return new SearchNode(node.getHostName(), node.getType());

        // check for CNAME records for node
        rrSet = cache.getResourceRecords(new SearchNode(node.getHostName(), RecordType.CNAME));
        if (!rrSet.isEmpty())
            return new SearchNode(rrSet.iterator().next().getTextResult(), RecordType.CNAME);

        return null;
    }
	
	private Set<ResourceRecord> searchDNS(SearchNode node, int indirections) {

        if (indirections >= MAX_INDIRECTIONS) {
            System.out.println("Maximum number of indirection levels reached: Query was cancelled.");
            return Collections.emptySet();
        }

        // check if cache has result or matching CNAME
        // if there is a matching CNAME, increment indirection level and recursively search
        SearchNode cachedNode = getResultFromCache(node);
        if (cachedNode != null) {
            if (cachedNode.getType() == RecordType.CNAME && node.getType() != RecordType.CNAME) {
                return searchDNS(new SearchNode(cachedNode.getHostName(), node.getType()), ++indirections);
            } else {
                return cache.getResourceRecords(cachedNode);
            }
        }

        // retrieve new result if nothing is found,
        // end query if it returns true
        if (queryServer(node, rootAddress))
        	return Collections.emptySet();

        // call next level of getResults after new rr's have been stored in the cache
        return searchDNS(node, 0);
    }
	
	private void searchAndPrint(String hostname, RecordType rtype) {
		
		SearchNode node = new SearchNode(hostname, rtype);
		Set<ResourceRecord> results = searchDNS(node, 0);
		
		if (results.isEmpty())
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), -1, "0.0.0.0");
		
        for (ResourceRecord record : results)
            System.out.printf("%-30s %-5s %-8d %s\n", node.getHostName(),
                    node.getType(), record.getTTL(), record.getTextResult());
	}
	
	public void run() {
		
		if (rootAddress == null) {
			System.err.println("Root address is null. Server could not be started.");
			System.exit(1);
		}

		System.out.printf("Simple DNS Client\nRoot Server: %s\n", 
				rootAddress.getHostAddress());
		
		try {
			QueryHandler.openSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
		
		QueryHandler.setTracing(tracing);
		
		Scanner scanner = new Scanner(System.in);
		
		Option lookupOption = Option.builder().longOpt("search").
				desc("Search using a fully qualified domain address").hasArg().argName("HOSTNAME").build();
		Option helpOption = Option.builder().longOpt("help").
				desc("Displays list of available commands").build();
		Option quitOption = Option.builder().longOpt("quit").
				desc("Quits the application").build();
		
		OptionGroup optionGroup = new OptionGroup();
		optionGroup.addOption(lookupOption);
		optionGroup.addOption(helpOption);
		optionGroup.addOption(quitOption);
		
		Options options = new Options();
		options.addOptionGroup(optionGroup);

		while(true) {
			
			System.out.print("SDNS>");

			/*
			 *  Input is prefixed with double hyphens to comply with Apache command-cli API
			 *  
			 *  For the lookup command:
			 *  Although this will cause the API to catch successive input with prefixed double hyphens
			 *  as an invalid command, RFC 5891 Section 4.2.3.1 specifies that a domain name may not 
			 *  start or end with a hyphen anyways, so this will be considered a valid outcome. 
			 *  (Perhaps slightly misleading, however.)
			 *  
			 *  For quit or help:
			 *  These commands require zero arguments, so the outcome is still valid.
			 *  
			 */
			String input = "--" + scanner.nextLine();
			String[] args = input.split(" ");
			
			try {
			
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args, false);
			
			if (cmd.hasOption(lookupOption.getLongOpt())) {
				
				String hostname = (String)cmd.getParsedOptionValue(lookupOption);
				searchAndPrint(hostname, RecordType.A);
				
			} else if (cmd.hasOption(quitOption.getLongOpt())) {
				break;
			} else if (cmd.hasOption(helpOption.getLongOpt())) {
				System.out.printf("Valid commands are:\n");
				System.out.printf("    %-20s %s\n", lookupOption.getLongOpt() + 
						" <" + lookupOption.getArgName() + ">", lookupOption.getDescription());				
				System.out.printf("    %-20s %s\n", helpOption.getLongOpt(), helpOption.getDescription());
				System.out.printf("    %-20s %s\n", quitOption.getLongOpt(), quitOption.getDescription());
			}
			
			} catch(ParseException e) {
				System.out.printf("Unknown command. Enter 'help' to see a list of commands\n");
			}
		}
		
		scanner.close();
		
		System.out.println("Goodbye!");
	}
		
}

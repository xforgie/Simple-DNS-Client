package com.xforgie.simplednsclient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.IllegalBlockingModeException;
import java.util.*;

import com.xforgie.simplednsclient.responseerrors.*;

public class QueryHandler {

    private static final int DEFAULT_DNS_PORT = 53;
    private static final int QUERY_MAX_SIZE = 512;
    private static DatagramSocket socket;
    private static boolean tracing = false;
    
    private static Cache cache = Cache.getCache();
    
    public static void openSocket() throws SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(5000);
    }

    public static void closeSocket() {
        socket.close();
    }

    public static void setTracing(boolean tracing) {
        QueryHandler.tracing = tracing;
    }

    public static Set<ResourceRecord> sendQueryAndCacheResponse(
    		InetAddress server, SearchNode node) throws QueryHaltException {

        int numAttempts = 2; // decreases by 1 on a timeout

        // build a query packet
        String serverName = node.getHostName().trim();
        byte[] queryPacket = PacketHandler.getQueryToSend(serverName, node.getType());
        byte[] message = new byte[QUERY_MAX_SIZE];
        int id = PacketHandler.getIDFromPacket(queryPacket);

        while (numAttempts > 0) {
        	
            try {

                DatagramPacket queryDatagramPacket = new DatagramPacket(
                        queryPacket, queryPacket.length, server, DEFAULT_DNS_PORT);

                DatagramPacket responseDatagramPacket = new DatagramPacket(
                        message, message.length, server, DEFAULT_DNS_PORT);

                socket.send(queryDatagramPacket);

                if (tracing)
                    System.out.printf("\n\nQuery ID    %d %s %s --> %s\n",
                            id,
                            serverName,
                            node.getType(),
                            server.getHostAddress());

                socket.receive(responseDatagramPacket);

                System.arraycopy(responseDatagramPacket.getData(), 0, message, 0, responseDatagramPacket.getLength());

                ByteBuffer response = ByteBuffer.wrap(message, 0, message.length);

                return decodeAndCacheResponse(id, response);

            } catch (SocketTimeoutException e) {
                numAttempts--;
            } catch (IOException | IllegalBlockingModeException e) {
                break;
            }

        }

        // if something went wrong, return null.
        return null;
    }

    private static Set<ResourceRecord> decodeAndCacheResponse(
    		int transactionID, ByteBuffer responseBuffer) throws QueryHaltException {

        // decode the packet from the responseBuffer
        try {
            DecodedPacket decodedPacket = PacketHandler.decodePacket(responseBuffer);

            // get records from decoded packet
            ResourceRecord[] an = decodedPacket.getAnswers();
            ResourceRecord[] ns = decodedPacket.getAuthorities();
            ResourceRecord[] ar = decodedPacket.getAdditionals();

            if (tracing) {
                System.out.printf("Response ID: %d Authoritative = %b\n",
                        decodedPacket.getResponseID(),
                        decodedPacket.isAuthoritative());

                System.out.printf("  Answers (%d)\n", decodedPacket.getANCount());
                for (ResourceRecord record : an)
                	printResourceRecord(record, record.getType().getCode());

                System.out.printf("  Nameservers (%d)\n", decodedPacket.getNSCount());
                for (ResourceRecord n : ns)
                	printResourceRecord(n, n.getType().getCode());

                System.out.printf("  Additional Information (%d)\n", decodedPacket.getARCount());
                for (ResourceRecord resourceRecord : ar)
                	printResourceRecord(resourceRecord, resourceRecord.getType().getCode());
            }

            // place records in an ordered hash set
            Set<ResourceRecord> anSet = new LinkedHashSet<>(Arrays.asList(an));
            Set<ResourceRecord> nsSet = new LinkedHashSet<>(Arrays.asList(ns));
            Set<ResourceRecord> arSet = new LinkedHashSet<>(Arrays.asList(ar));

            // add answer and additional records to cache
            anSet.forEach(cache::addResourceRecord);
            arSet.forEach(cache::addResourceRecord);

            // return set of nameservers
            return nsSet;

        } catch (ResponseException e) {
            throw new QueryHaltException();
        }
    }

    private static void printResourceRecord(ResourceRecord record, int rtype) {
    	System.out.printf("    %-30s %-10d %-4s %s\n", 
        		record.getHostName(),
                record.getTTL(),
                record.getType() == RecordType.OTHER ? rtype : record.getType(),
                record.getTextResult());
    }
}


package com.xforgie.simplednsclient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

import com.xforgie.simplednsclient.responseerrors.*;

public abstract class PacketHandler {

    private static Random random = new Random();

    // returns a query for the requested domain name
    public static byte[] getQueryToSend(String domainName, RecordType recordType) {

        String[] labels = domainName.trim().split("\\.");
        byte[] packet = new byte[18 + domainName.length()];

        // create random transaction ID
        byte[] queryID = new byte[2];
        random.nextBytes(queryID);

        // transaction ID
        packet[0] = queryID[0];
        packet[1] = queryID[1];
        // QR, OPCODE, AA, TC, RD, RA, Z, RCODE
        packet[2] = 0b00000000;
        packet[3] = 0b00000000;
        // QDCOUNT
        packet[4] = 0b00000000;
        packet[5] = 0b00000001;
        // ANCOUNT
        packet[6] = 0b00000000;
        packet[7] = 0b00000000;
        // NSCOUNT
        packet[8] = 0b00000000;
        packet[9] = 0b00000000;
        // ARCOUNT
        packet[10] = 0b00000000;
        packet[11] = 0b00000000;
        // QNAME
        int byteCounter = 12;
        for (int i = 0; i < labels.length; i++) {
            packet[byteCounter++] = (byte) labels[i].length();
            byte[] label = labels[i].getBytes();
            for (int j = 0; j < labels[i].length(); j++) {
                packet[byteCounter++] = label[j];
            }
        }
        packet[byteCounter++] = 0b00000000;
        // QTYPE
        packet[byteCounter++] = (byte)((recordType.getCode() >> 8) & 0xFF);
        packet[byteCounter++] = (byte)(recordType.getCode() & 0xFF);
        // QCLASS
        packet[byteCounter++] = 0b00000000;
        packet[byteCounter]   = 0b00000001;

        return packet;
    }

    // retrieve transaction ID from a packet
    public static int getIDFromPacket(byte[] packet) {
        return ((packet[0] & 0xff) << 8) | (packet[1] & 0xff);
    }

    // Creates a DecodedPacket object with extracted information from ByteBuffer
    public static DecodedPacket decodePacket(ByteBuffer buf) throws ResponseException {

        DecodedPacket dp = new DecodedPacket();

        // get header
        dp.setResponseID(getIDFromPacket(new byte[] {buf.get(), buf.get()}));
        short temp = buf.getShort();
        dp.setFlags((byte) (((temp & 0x780) >> 7) | ((temp & 0x8000) >> 11)));
        dp.setOpCode(temp >> 11);
        dp.setRCode(temp);
        dp.setQdCount(buf.getShort());
        dp.setAnCount(buf.getShort());
        dp.setNsCount(buf.getShort());
        dp.setArCount(buf.getShort());

        // If flags are not 0, there was an error
        int responseFlag = dp.getRCode();
        if (responseFlag != 0x0) {
            switch (responseFlag) {
                case 0x1:
                    // Invalid format in query sent to server
                    throw new QueryFormatException();
                case 0x2:
                    // Failure in nameserver
                    throw new NameserverFailureException();
                case 0x3:
                    // Domain name in query to authoritative nameserver does not exist
                    throw new AuthoritativeNameErrorException();
                case 0x4:
                    // requested action to server is not implemented
                    throw new NotImplementedException();
                case 0x5:
                    // refused to perform operation requested
                    throw new RefusedOperationException();
                default:
                    // if flag is not recognized
                    throw new ResponseException();
            }
        }

        // get domain name
        int strLength = buf.get() & 0xFF;
        StringBuilder domainName = new StringBuilder();
        do {
            for (; strLength > 0; strLength--)
                domainName.append((char) buf.get());
            strLength = buf.get() & 0xFF;
            if (strLength != 0)
                domainName.append('.');
        } while (strLength > 0);
        dp.setDomainName(domainName.toString());

        // get Q type and class
        dp.setQType(buf.getShort());
        dp.setQClass(buf.getShort());

        // get records
        dp.setANRecords(getResourceRecords(buf, dp.getANCount()));
        dp.setNSRecords(getResourceRecords(buf, dp.getNSCount()));
        dp.setARRecords(getResourceRecords(buf, dp.getARCount()));

        return dp;
    }

    private static ResourceRecord[] getResourceRecords(ByteBuffer buf, int numResources) {
        ResourceRecord[] records = new ResourceRecord[numResources];

        try {
        	
            for (int i = 0; i < numResources; i++) {

                String recordName = getResourceName(buf, buf.get() & 0xFF);
                RecordType recordType = RecordType.getByCode(buf.getShort() & 0xFFFF);
                int recordClass = buf.getShort() & 0xFFFF;
                long recordTTL = buf.getInt();

                // release record length from buffer (unused)
                buf.getShort();

                // release preference from buffer if RR is an MX record
                if (recordType == RecordType.MX)
                    buf.getShort();

                // put InetAddresses in A and AAAA records and text records in others
                if (recordType == RecordType.A && recordClass == 0x1) {
                    InetAddress inet = InetAddress.getByAddress(getIPv4Address(buf));
                    records[i] = new ResourceRecord(recordName, recordType, recordTTL, inet);
                } else if (recordType == RecordType.AAAA && recordClass == 0x1) {
                    InetAddress inet = InetAddress.getByAddress(getIPv6Address(buf));
                    records[i] = new ResourceRecord(recordName, recordType, recordTTL, inet);
                } else {
                    String recordData = getResourceName(buf, buf.get() & 0xFF);
                    records[i] = new ResourceRecord(recordName, recordType, recordTTL, recordData);
                }
            }
            
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception in decoding packet.");
        }

        return records;
    }

    private static boolean isPointer(int num) {
        return (num & 0xc0) == 0xc0;
    }

    // follows pointers (if any) and finds domain name
    // isPointing is a check whether or not to increment the ByteBuffer
    private static String getResourceName(ByteBuffer buf, int nextByte) {
        StringBuilder resName = new StringBuilder();

        // If the next byte is a pointer, retrieve rest of pointer and search from that position
        if (isPointer(nextByte)) {
            resName.append(getResourceNameByAbsoluteToPointer(buf, nextByte));
            return resName.toString();
        }

        // If nextByte is not a pointer, iterate through ByteBuffer normally
        resName.append(readLabels(buf, nextByte));

        return resName.toString();
    }

    // read next position via pointer
    private static String getResourceNameByPointer(ByteBuffer buf, int pointer) {
        StringBuilder resName = new StringBuilder();
        int offset = (int)(((long)(buf.get(pointer) << 8) | (buf.get(pointer + 1) & 0xFF))) & 0x3FF;
        resName.append(readLabelsByPointer(buf, offset));
        return resName.toString();
    }

    // read next position by incrementing the buffer and then read by pointer
    private static String getResourceNameByAbsoluteToPointer(ByteBuffer buf, int pointer) {
        StringBuilder resName = new StringBuilder();
        int offset = (int)(((long)(pointer << 8) | (buf.get() & 0xFF))) & 0x3FF;
        resName.append(readLabelsByPointer(buf, offset));
        return resName.toString();
    }

    // reads label from buffer
    private static String readLabels(ByteBuffer buf, int nextByte) {
        StringBuilder labelName = new StringBuilder();

        do {
            // read the label of size nextByte
            for (int i = 0; i < nextByte; i++)
                labelName.append((char) buf.get());

            // retrieve the next byte in the sequence
            nextByte = buf.get() & 0xFF;

            // if the next byte is a pointer, get the rest of pointer and continue search from that position
            // otherwise, continue building the label until nextByte is 0x00
            if (isPointer(nextByte)) {
                labelName.append('.');
                labelName.append(getResourceNameByAbsoluteToPointer(buf, nextByte));
                return labelName.toString();
            } else if (nextByte != 0)
                labelName.append('.');

        } while (nextByte > 0);

        return labelName.toString();
    }

    // read label from buffer using a pointer (not incrementing buffer)
    private static String readLabelsByPointer(ByteBuffer buf, int pointer) {
        StringBuilder labelName = new StringBuilder();
        int nextByte = buf.get(pointer) & 0xFF;

        // if the byte at the pointer position is a pointer, redirect at new position
        if (isPointer(nextByte))
            return getResourceNameByPointer(buf, pointer);

        do {
            // read the label of size nextByte
            for (int i = 0; i < nextByte; i++)
                    labelName.append((char) buf.get(pointer + i + 1));

            // set pointer to new position after the label and get the next byte at that position
            pointer += nextByte + 1;
            nextByte = buf.get(pointer) & 0xFF;

            // if the next byte is a pointer, get the rest of pointer (via pointer) and continue search from that position
            // otherwise, continue building the label until nextByte is 0x00
            if (isPointer(nextByte)) {
                labelName.append('.');
                labelName.append(getResourceNameByPointer(buf, pointer));
                return labelName.toString();
            } else if (nextByte != 0)
                labelName.append('.');

        } while (nextByte > 0);

        return labelName.toString();
    }

    private static byte[] getIPv4Address(ByteBuffer buf) {
        byte[] address = new byte[4];
        for (int i = 0; i < 4; i++)
            address[i] = buf.get();
        return address;
    }

    private static byte[] getIPv6Address(ByteBuffer buf) {
        byte[] address = new byte[16];
        for (int i = 0; i < 16; i++)
            address[i] = buf.get();
        return address;
    }
}

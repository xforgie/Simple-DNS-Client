package com.xforgie.simplednsclient;

import java.net.InetAddress;
import java.util.Date;
import java.util.Objects;

public class ResourceRecord {

    private SearchNode node;
    private Date expirationTime;
    private String textResult;
    private InetAddress inetResult;

    public ResourceRecord(String hostName, RecordType type, long ttl, String result) {
        this.node = new SearchNode(hostName, type);
        this.expirationTime = new Date(System.currentTimeMillis() + (ttl * 1000));
        this.textResult = result;
        this.inetResult = null;
    }

    public ResourceRecord(String hostName, RecordType type, long ttl, InetAddress result) {
        this(hostName, type, ttl, result.getHostAddress());
        this.inetResult = result;
    }

    public SearchNode getNode() {
        return node;
    }

    public String getHostName() {
        return node.getHostName();
    }

    public RecordType getType() {
        return node.getType();
    }

    // Returns TTL in seconds rounded up
    public long getTTL() {
        return (expirationTime.getTime() - System.currentTimeMillis() + 999) / 1000;
    }

    public boolean isStillValid() {
        return expirationTime.after(new Date());
    }

    public boolean expiresBefore(ResourceRecord record) {
        return this.expirationTime.before(record.expirationTime);
    }

    public String getTextResult() {
        return textResult;
    }

    public InetAddress getInetResult() {
        return inetResult;
    }

    @Override
    public boolean equals(Object o) {
    	
        if (this == o) 
        	return true;
        
        if (o == null || getClass() != o.getClass()) 
        	return false;

        ResourceRecord record = (ResourceRecord) o;

        return node.equals(record.node) &&
        		textResult.equals(record.textResult) &&
        		Objects.equals(inetResult, record.inetResult);
    }

    @Override
    public int hashCode() {
        return 31 * node.hashCode() + textResult.hashCode();
    }
}

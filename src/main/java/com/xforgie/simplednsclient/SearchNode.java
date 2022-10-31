package com.xforgie.simplednsclient;

public class SearchNode implements Comparable<SearchNode> {

	private String hostName;
    private RecordType type;

    public SearchNode(String hostName, RecordType type) {
        this.hostName = hostName;
        this.type = type;
    }

    public String getHostName() {
        return hostName;
    }

    public RecordType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
    	
        if (this == o) 
        	return true;
        
        if (o == null || getClass() != o.getClass()) 
        	return false;

        SearchNode dnsNode = (SearchNode) o;

        return hostName.equals(dnsNode.hostName) && type == dnsNode.type;
    }

    @Override
    public int hashCode() {
        return 31 * hostName.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return hostName + " (" + type + ")";
    }

    @Override
    public int compareTo(SearchNode o) {
    	return hostName.equalsIgnoreCase(o.hostName) ? 
    			type.compareTo(o.type) : hostName.compareToIgnoreCase(o.hostName);
    }
}

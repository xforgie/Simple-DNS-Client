package com.xforgie.simplednsclient;

import java.util.*;

public class Cache {

    private static Cache cache = new Cache();

    private Map<SearchNode, Map<ResourceRecord, ResourceRecord>> cachedResults = new HashMap<>();

    public static Cache getCache() {
        return cache;
    }

    public Set<ResourceRecord> getResourceRecords(SearchNode node) {
    	
        Map<ResourceRecord, ResourceRecord> results = cachedResults.get(node);
        if (results == null)
            return Collections.emptySet();

        results.keySet().removeIf(record -> !record.isStillValid());
        return Collections.unmodifiableSet(results.keySet());
    }

    public void addResourceRecord(ResourceRecord record) {

        if (!record.isStillValid())
        	return;

        Map<ResourceRecord, ResourceRecord> results = cachedResults.get(record.getNode());
        if (results == null) {
            results = new HashMap<>();
            cachedResults.put(record.getNode(), results);
        }

        ResourceRecord oldRecord = results.get(record);
        if (oldRecord == null || oldRecord.expiresBefore(record))
            results.put(record, record);
    }
}
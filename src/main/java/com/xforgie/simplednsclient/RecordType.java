package com.xforgie.simplednsclient;

public enum RecordType {
	
    A(1), NS(2), CNAME(5), SOA(6), 
    MX(15), AAAA(28), OTHER(0);

    private int code;

    RecordType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RecordType getByCode(int code) {
    	
        for (RecordType type : values())
            if (type.code == code)
                return type;
        
        return OTHER;
    }
}
package com.xforgie.simplednsclient.responseerrors;

public class QueryFormatException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8638066730995444768L;

	@Override
    public String getMessage() {
        return "QueryFormatException -- Nameserver could not understand query.";
    }
}

package com.xforgie.simplednsclient.responseerrors;

public class QueryHaltException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3843405929159400401L;

	@Override
    public String getMessage() {
        return "QueryHaltException -- Server response resulted in end of query.";
    }
}

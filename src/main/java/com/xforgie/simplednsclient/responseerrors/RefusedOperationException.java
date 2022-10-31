package com.xforgie.simplednsclient.responseerrors;

public class RefusedOperationException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -498902936280306584L;

	@Override
    public String getMessage() {
        return "RefusedOperationException -- Nameserver refused to perform query.";
    }
}

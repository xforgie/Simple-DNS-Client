package com.xforgie.simplednsclient.responseerrors;

public class NameserverFailureException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8999707917592928667L;

	@Override
    public String getMessage() {
        return "NameserverFailureException -- Nameserver responded with a failure.";
    }
}

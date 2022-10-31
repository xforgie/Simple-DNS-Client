package com.xforgie.simplednsclient.responseerrors;

public class AuthoritativeNameErrorException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1593566307459930481L;

	@Override
    public String getMessage() {
        return "AuthoritativeNameErrorException -- Domain name in query to nameserver does not exist";
    }
}
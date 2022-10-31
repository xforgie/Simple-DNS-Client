package com.xforgie.simplednsclient.responseerrors;

public class NotImplementedException extends ResponseException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2467963229581362295L;

	@Override
    public String getMessage() {
        return "NotImplementedException -- Requested action to server is not implemented";
    }
}

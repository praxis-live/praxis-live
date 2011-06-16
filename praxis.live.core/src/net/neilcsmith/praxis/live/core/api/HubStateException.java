package net.neilcsmith.praxis.live.core.api;

public class HubStateException extends Exception {

     /**
     * Creates a new instance of <code>HubUnavailableException</code> without detail message.
     */
    public HubStateException() {
    }


    /**
     * Constructs an instance of <code>HubUnavailableException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public HubStateException(String msg) {
        super(msg);
    }

    public HubStateException(Throwable cause) {
        super(cause);
    }

    public HubStateException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

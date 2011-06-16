/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.core.api;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class HubUnavailableException extends Exception {

    /**
     * Creates a new instance of <code>HubUnavailableException</code> without detail message.
     */
    public HubUnavailableException() {
    }


    /**
     * Constructs an instance of <code>HubUnavailableException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public HubUnavailableException(String msg) {
        super(msg);
    }

    public HubUnavailableException(Throwable cause) {
        super(cause);
    }

    public HubUnavailableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

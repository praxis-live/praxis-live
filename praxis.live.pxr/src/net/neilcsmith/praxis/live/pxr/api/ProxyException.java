/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.pxr.api;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ProxyException extends Exception {

    /**
     * Creates a new instance of <code>ProxyException</code> without detail message.
     */
    public ProxyException() {
    }


    /**
     * Constructs an instance of <code>ProxyException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ProxyException(String msg) {
        super(msg);
    }

    public ProxyException(Throwable cause) {
        super(cause);
    }
}

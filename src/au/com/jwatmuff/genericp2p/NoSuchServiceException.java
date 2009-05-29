/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericp2p;

/**
 *
 * @author James
 */
public class NoSuchServiceException extends Exception {

    /**
     * Creates a new instance of <code>NoSuchServiceException</code> without detail message.
     */
    public NoSuchServiceException() {
    }


    /**
     * Constructs an instance of <code>NoSuchServiceException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public NoSuchServiceException(String msg) {
        super(msg);
    }
}

/* $Id:$ */

package ibis.impl;

/**
 * Abstract class for implementation-dependent connection info for sendports.
 */
public abstract class SendPortConnectionInfo implements Config {
    protected abstract void closeConnection();
}

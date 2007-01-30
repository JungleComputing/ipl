/* $Id:$ */

package ibis.impl;

/**
 * Abstract class for implementation-dependent connection info for sendports.
 */
public abstract class SendPortConnectionInfo {
    protected abstract void closeConnection();
}

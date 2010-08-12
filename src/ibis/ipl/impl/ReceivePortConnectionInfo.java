/* $Id$ */

package ibis.ipl.impl;

import ibis.io.BufferedArrayInputStream;
import ibis.io.DataInputStream;
import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.impl.stacking.sns.SNSProperties;

import java.io.IOException;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the information about a particular sendport/receiveport
 * connection, on the receiver side.
 */
public class ReceivePortConnectionInfo {

    /** Debugging. */
    protected static final Logger logger
            = LoggerFactory.getLogger("ibis.ipl.impl.ReceivePortConnectionInfo");

    /** Identifies the sendport side of the connection. */
    public final SendPortIdentifier origin;

    /** The serialization input stream of the connection. */
    public SerializationInput in;

    /** The receiveport of the connection. */
    public final ReceivePort port;

    /** The message that arrives on this connection. */
    public ReadMessage message;

    /**
     * The underlying data stream for this connection.
     * The serialization stream lies on top of this.
     */
    public DataInputStream dataIn;

    private long cnt = 0;

    /**
     * Constructs a new <code>ReceivePortConnectionInfo</code> with the
     * specified parameters.
     * @param origin identifies the sendport of this connection.
     * @param port the receiveport.
     * @param dataIn the inputstream on which a serialization input stream
     * can be built.
     * @exception IOException is thrown in case of trouble.
     */
    public ReceivePortConnectionInfo(SendPortIdentifier origin,
            ReceivePort port, DataInputStream dataIn) throws IOException {
        this.origin = origin;
        this.port = port;
        this.dataIn = dataIn;
        // newStream(); 
        // Moved to subtypes. Calling it here may cause deadlocks!
        port.addInfo(origin, this);
    }

    /**
     * Returns the number of bytes read from the data stream.
     * @return the number of bytes.
     */
    public long bytesRead() {
        long rd = dataIn.bytesRead();
        if (rd != 0) {
            dataIn.resetBytesRead();
            port.addDataIn(rd);
            cnt += rd;
        }
        return cnt;
    }

    /**
     * This method must be called each time a connected sendport adds a new
     * connection. This new connection may either be to the current receiveport,
     * or to another one. In both cases, the serialization stream must be
     * recreated.
     * @exception IOException is thrown in case of trouble.
     */
    public void newStream() throws IOException {
        bytesRead();
        if (in != null) {
            in.close();
        }
        
        if (port.ibis.encryptedStream) {
        	if (logger.isDebugEnabled()) {
                logger.debug("Building encrypted stream");
            }
       
        	BufferedArrayInputStream encryptedDataIn;
        	CipherInputStream cis;
	        
	        try {	        	
	        	Properties p = port.ibis.properties();
	        	String keystorePassword = p.getProperty(SNSProperties.KEYSTORE_PASSWORD);
	        	String keystoreAlias = p.getProperty(SNSProperties.KEYSTORE_ALIAS);

			    PasswordProtection keyStorePassword = new PasswordProtection(keystorePassword.toCharArray());
			    SecretKeyEntry skEntry = (SecretKeyEntry) port.ibis.keyStore.getEntry(keystoreAlias, keyStorePassword);
			    SecretKey key = skEntry.getSecretKey();

			    Cipher deCipher = Cipher.getInstance("DES/CFB8/NoPadding");
			    deCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(new byte[8]));

		        cis = new CipherInputStream(dataIn, deCipher);		        
		        encryptedDataIn = new BufferedArrayInputStream(cis);
		        
	        	in = SerializationFactory.createSerializationInput(port.serialization, encryptedDataIn);
			} catch (Exception e) {
				throw new IOException("SNSIbis: Failed to create ingoing encrypted stream");
			}	
        }
        else {
        	in = SerializationFactory.createSerializationInput(port.serialization,
        			dataIn);        
        }
        message = port.createReadMessage(in, this);
    }

    /**
     * This method closes the connection, as the result of the specified
     * exception. Implementations may need to redefine this method 
     * @param e the exception.
     */
    public void close(Throwable e) {
        try {
            in.close();
        } catch(Throwable z) {
            // ignore
        }
        try {
            dataIn.close();
        } catch(Throwable z) {
            // ignore
        }
        in = null;
        if (logger.isDebugEnabled()) {
            logger.debug(port.name + ": connection with " + origin
                    + " closing", e);
        }
        port.lostConnection(origin, e);
    }

    /**
     * This method gets called when the upcall for the message explicitly
     * called {@link ReadMessage#finish()}.
     * The default implementation just allocates a new message.
     */
    protected void upcallCalledFinish() {
        message = port.createReadMessage(in, this);
        if (logger.isDebugEnabled()) {
            logger.debug(port.name + ": new connection handler for " + origin
                    + ", finish called from upcall");
        }
    }
}

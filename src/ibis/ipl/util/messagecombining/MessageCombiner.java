/* $Id$ */

package ibis.ipl.util.messagecombining;

import ibis.io.Replacer;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.util.TypedProperties;

import java.io.IOException;

public class MessageCombiner {

    private SendPort sp;
    
    private StoreArrayOutputStream storeOut;
        
    private CombiningWriteMessage wm; 
    private boolean wmInUse = false;
        
    private long currentMessageSize = 0;
    
    /**
     * Construct a new MessageCombiner
     * 
     * @param prop properties for the combiner (currently only the 
     *        Serialization property is used).   
     * @param sp Sendport that will produce the messages
     * 
     */
    public MessageCombiner(TypedProperties prop, SendPort sp)
            throws IOException {

        this.sp = sp;
                
        String ser = prop.getProperty("ibis.serialization");

        if (ser == null) ser = "sun";

        storeOut = new StoreArrayOutputStream();
        
        wm = new CombiningWriteMessage(this, storeOut, ser);
        wm.clear();
    }
    
    /**
     * Create a new message to write to. The data written to this message will 
     * be added to the combined message.
     * 
     * @return message to write to.
     */
    public synchronized WriteMessage newMessage() {

        while (wmInUse) { 
            try { 
                wait();
            } catch (InterruptedException e) { 
                // ignore
            }
        }
        
        wmInUse = true;        
        currentMessageSize = storeOut.bytesWritten();       
        return wm;
    }

    protected synchronized long messageIsFinished() {
        wmInUse = false;
        notifyAll();        
        return storeOut.bytesWritten() - currentMessageSize;
    }
   
    protected SendPort getSendPort() { 
        return sp;
    }
    
    /**
     * Sets the replacer used by the serialization. 
     * 
     * @param r The replacer
     * @throws IOException serialization failed to set replacer.
     */
    public void setReplacer(Replacer r) throws IOException {
        wm.setReplacer(r);
    }
    
    /**
     * Sends the accumulated messages using one big message.
     * 
     * @throws IOException a network problem occurred.
     */
    public void sendAccumulatedMessages() throws IOException {         
        storeOut.writeToMessage(sp.newMessage());        
        wm.clear();
        // preferably clear immediately after sending the message, to give
        // GC more opportunities.
    }       
}

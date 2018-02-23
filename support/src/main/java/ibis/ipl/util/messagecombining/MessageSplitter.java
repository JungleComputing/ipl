/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.util.messagecombining;

import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.util.TypedProperties;

import java.io.IOException;

/**
 *  
 * TODO Description 
 *  
 * @author Jason Maassen
 * @version 1.0 Jul 6, 2005
 * @since 1.0
 * 
 */
public class MessageSplitter {

    private ReceivePort rp;
    private StoreArrayInputStream storeIn;    
    
    private SendPortIdentifier origin;
    private CombiningReadMessage rm;
    private boolean rmInUse = false;
        
    private long count = 0;
    
    /**
     * Construct a new CombiningReceivePort
     * @param prop  properties for the splitter (currently only the 
     *        Serialization property is used).   
     * @param rp The ReceivePort that will produce the messages 
     * 
     */
    public MessageSplitter(TypedProperties prop, ReceivePort rp) {
        this.rp = rp;
                        
        String ser = prop.getProperty("ibis.serialization");

        if (ser == null) ser = "sun";
        
        storeIn = new StoreArrayInputStream();        
        
        rm = new CombiningReadMessage(this, storeIn, ser);
    }

    /**
     * Use this method to provide the message that must be split.
     * The original message will not be finished! 
     * 
     * @param m The message that must be split.
     * @throws IOException reading the message failed.
     */
    public synchronized void setMessageToSplit(ReadMessage m) throws IOException {
    
        while (rmInUse || storeIn.available() > 0) { 
            try { 
                System.out.println("Waiting " + storeIn.available() + " " + rmInUse);
                wait();              
            } catch (InterruptedException e) { 
                // ignore
            }
        }
                       
        storeIn.reset(m);
        origin = m.origin();
        rm.clear();
    } 

    /**
     * Use this method to receive the next message from the combined message.
     * 
     * @return next message to read or null if all messages have been read. 
     * @throws IOException reading the message failed
     */
    public synchronized ReadMessage receive() throws IOException {

        while (rmInUse) { 
            try { 
                System.out.println("Waiting " + rmInUse);
                wait();              
            } catch (InterruptedException e) { 
                // ignore
            }
        }
                       
        if (storeIn.available() == 0) {
            return null;
        }
                
        rmInUse = true;
        count = storeIn.bytesRead(); // TODO: Not sure if this is correct...       
        return rm;
    }

    protected synchronized long messageIsFinished() {
        rmInUse = false;
        notifyAll();        
        return storeIn.bytesRead() - count; 
    }
    
    protected ReceivePort getReceivePort() { 
        return rp;
    }
    
    protected SendPortIdentifier origin() {
        return origin;
    }
}

/* $Id$ */

package ibis.satin.impl.sharedObjects;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;
import ibis.satin.SharedObject;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Protocol;
import ibis.util.messagecombining.MessageSplitter;

import java.io.IOException;

final class SOInvocationHandler implements Upcall, Config, Protocol {
    private Satin s;

    private MessageSplitter messageSplitter;

    protected SOInvocationHandler(Satin s) {
        this.s = s;
    }

    protected void setMessageSplitter(MessageSplitter ms) {
        messageSplitter = ms;
    }

    private void handleMessage(ReadMessage m) {
        SharedObject obj = null;
        SOInvocationRecord soir = null;

        try {
            byte opt_code = m.readByte();
            switch (opt_code) {
            case SO_TRANSFER: // exportObject
                //                System.err.print("X");
                s.stats.soBroadcastDeserializationTimer.start();
                synchronized (s) {
                    s.so.receivingMcast = true;
                }
                obj = (SharedObject) m.readObject();
                s.stats.soBroadcastDeserializationTimer.stop();

                // no need to finish the message
                synchronized (s) {
                    s.so.sharedObjects.put(obj.objectId, obj);
                    s.so.receivingMcast = false;
                    s.notifyAll();
                }
                break;
            case SO_INVOCATION: // normal invocation, can be message combined
                s.stats.soInvocationDeserializationTimer.start();
                synchronized (s) {
                    s.so.receivingMcast = true;
                }
                soir = (SOInvocationRecord) m.readObject();
                s.stats.soInvocationDeserializationTimer.stop();

                // no need to finish here
                s.so.addSOInvocation(soir);
                break;
            default:
                System.err.println("SATIN '" + s.ident.name()
                    + "': invalid opcode in SOInvocationHandler: " + opt_code);
            }
        } catch (IOException e) {
            System.err.println("SATIN '" + s.ident.name()
                + "': got exception in SOInvocationHandler: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("SATIN '" + s.ident.name()
                + "': got exception in SOInvocationHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void upcall(ReadMessage m) {
        try {
            if (SO_MAX_INVOCATION_DELAY > 0) { // message combining enabled
                messageSplitter.setMessageToSplit(m);
                ReadMessage m1 = messageSplitter.receive();
                while (m1 != null) {
                    handleMessage(m1);
                    // message combining needs all messages to be finished
                    m1.finish();
                    m1 = messageSplitter.receive();
                }
            } else {
                handleMessage(m);
            }
        } catch (IOException e) {
            System.err.println("SATIN '" + s.ident.name()
                + "': got exception while reading"
                + " opcode in SOInvocationHandler: " + e.getMessage());
        }
    }
}

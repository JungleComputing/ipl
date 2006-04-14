/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;
import ibis.satin.SharedObject;
import ibis.util.messagecombining.MessageSplitter;

import java.io.IOException;

final class SOInvocationHandler implements Upcall, Config, Protocol {
    Satin satin;

    MessageSplitter messageSplitter;

    SOInvocationHandler(Satin s) {
        satin = s;
    }

    public void setMessageSplitter(MessageSplitter ms) {
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
                if (SO_TIMING) {
                    satin.soBroadcastDeserializationTimer.start();
                }
                synchronized(satin) {
                    satin.receivingObject = true;
                }
                obj = (SharedObject) m.readObject();
                if (SO_TIMING) {
                    satin.soBroadcastDeserializationTimer.stop();
                }

                // no need to finish the message
                synchronized (satin) {
                    satin.sharedObjects.put(obj.objectId, obj);
                    satin.receivingObject = false;
                    satin.notifyAll();
                }
                break;
            case SO_INVOCATION: // normal invocation, can be message combined
//                System.err.print("Y");
                if (SO_TIMING) {
                    satin.soInvocationDeserializationTimer.start();
                }
                soir = (SOInvocationRecord) m.readObject();
                if (SO_TIMING) {
                    satin.soInvocationDeserializationTimer.stop();
                }

                // no need to finish here
                satin.addSOInvocation(soir);
                break;
            default:
                System.err.println("SATIN '" + satin.ident.name()
                    + "': invalid opcode in SOInvocationHandler: " + opt_code);
            }
        } catch (IOException e) {
            System.err.println("SATIN '" + satin.ident.name()
                + "': got exception in SOInvocationHandler: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("SATIN '" + satin.ident.name()
                + "': got exception in SOInvocationHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void upcall(ReadMessage m) {
        try {
            if (satin.soInvocationsDelay > 0) { // message combining enabled
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
            System.err.println("SATIN '" + satin.ident.name()
                + "': got exception while reading"
                + " opcode in SOInvocationHandler: " + e.getMessage());
        }
    }
}

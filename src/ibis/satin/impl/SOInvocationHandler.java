package ibis.satin.impl;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;
import ibis.satin.SharedObject;
import ibis.util.Timer;
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
        Timer handleSOInvocationsTimer = null;
        Timer soTransferTimer = null;
        Timer soDeserializationTimer = null;

        try {
            byte opt_code = m.readByte();
            switch (opt_code) {
            case SO_TRANSFER:
                if (SO_TIMING) {
                    soTransferTimer = satin.createTimer();
                    soTransferTimer.start();
                    soDeserializationTimer = satin.createTimer();
                    soDeserializationTimer.start();
                }
                obj = (SharedObject) m.readObject();

                if (SO_TIMING) {
                    soTransferTimer.stop();
                    satin.soTransferTimer.add(soTransferTimer);
                    soDeserializationTimer.stop();
                    satin.soDeserializationTimer.add(soDeserializationTimer);
                }
                m.finish();
                synchronized (satin) {
                    satin.sharedObjects.put(obj.objectId, obj);
                }
                /*		System.err.println("SATIN '" + satin.ident.name() 
                 + "': received broadcast shared object from " 
                 + m.origin());	    */
                break;
            case SO_INVOCATION:
                if (SO_TIMING) {
                    handleSOInvocationsTimer = satin.createTimer();
                    handleSOInvocationsTimer.start();
                }
                soir = (SOInvocationRecord) m.readObject();
                m.finish();
                satin.addSOInvocation(soir);
                if (SO_TIMING) {
                    handleSOInvocationsTimer.stop();
                    satin.handleSOInvocationsTimer
                        .add(handleSOInvocationsTimer);
                }
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
        //	SOInvocationRecord soir = null;
        //	int num = 0;

        /*	System.err.println("SATIN '" + satin.ident.name()
         + "': got so invocation");*/

        try {
            if (satin.soInvocationsDelay > 0) {
                //		System.err.println("reading a combined message");
                messageSplitter.setMessageToSplit(m);
                ReadMessage m1 = messageSplitter.receive();
                while (m1 != null) {
                    handleMessage(m1); //finishes the message
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

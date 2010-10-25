package ibis.ipl.util.rpc;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

class RemoteObject implements MessageUpcall {

    private final ReceivePort receivePort;

    private final Ibis ibis;

    private final Object theObject;
    
    private final Class<?> interfaceClass;

    RemoteObject(Ibis ibis, String name, Object theObject,
            Class<?> interfaceClass) throws IOException {
        this.ibis = ibis;
        this.theObject = theObject;
        this.interfaceClass = interfaceClass;

        receivePort = ibis
                .createReceivePort(RPC.rpcRequestPortType, name, this);
        // enable connections
        receivePort.enableConnections();
        // enable upcalls
        receivePort.enableMessageUpcalls();

        System.err.println("remote object " + receivePort.name() + " created");

    }

    void unexport() throws IOException {
        receivePort.close();
    }

    /**
     * Function called by Ibis to give us a newly arrived message. This message
     * will contain the ReceivePortIdentifier of the receive port of the ibis
     * that send the request. We connect to this receive port, and send the
     * reply.
     */
    public void upcall(ReadMessage message) throws IOException,
            ClassNotFoundException {
        ReceivePortIdentifier requestor = (ReceivePortIdentifier) message
                .readObject();
        String methodName = message.readString();
        Class<?>[] parameterTypes = (Class<?>[]) message.readObject();

        Method method;
        try {
            method = interfaceClass.getDeclaredMethod(methodName, parameterTypes);

            Object[] args = (Object[]) message.readObject();

            System.err.println("received request from: " + requestor);

            // finish the request message. This MUST be done before sending
            // the reply message. It ALSO means Ibis may now call this upcall
            // method agian with the next request message
            message.finish();

            // create a sendport for the reply
            SendPort replyPort = ibis.createSendPort(RPC.rpcReplyPortType);

            // connect to the requestor's receive port
            replyPort.connect(requestor);

            // create a reply message
            WriteMessage reply = replyPort.newMessage();

            reply.writeObject(method.invoke(theObject, args));

            reply.writeString("the time is " + new Date());
            reply.finish();

            replyPort.close();

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

}

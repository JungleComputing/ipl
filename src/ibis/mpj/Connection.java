/* $Id$ */

/*
 * Created on 04.02.2005
 */
package ibis.mpj;

import ibis.ipl.*;
import java.io.*;

/**
 * Holds all information about a connection between two nodes.
 */
public class Connection {
    private static Integer SENDLOCK = new Integer(0);

    //	private MPJObject readObj = new MPJObject();

    private final boolean DEBUG = false;

    private ReceivePort receiver = null;
    private SendPort sender = null;
    private WriteMessage writeMessage = null;
    private int commPartnerRank = -1;
    private int myRank = -1;
    private String portName = null;
    private PortType portType = null;
    private int messageCount = 0;
    private MPJObjectQueue recvQueue = null;
    private ReceivePortIdentifier client = null; 
    private Registry registry = null;
    //	private boolean sync = false;
    private ReadMessage msg = null;

    public Connection(Registry registry, PortType portType, int myRank, int commPartnerRank) {
        this.portName = portType.name();
        this.commPartnerRank = commPartnerRank;
        this.myRank = myRank;
        this.portType = portType;
        this.registry = registry;
    }




    protected void setupReceivePort() {
        String portString = portName + '_' + commPartnerRank;

        if (DEBUG) {
            System.err.println("Receive on: " + portString + "; Index: " + commPartnerRank);
        }
        try {
            this.recvQueue = new MPJObjectQueue();
            this.receiver = portType.createReceivePort(portString);
            this.receiver.enableConnections();
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected void setupSendPort() {
        String portString = portName + '_' + myRank;

        if (DEBUG) {
            System.err.println("Send on: " + portString + "; Index: " + commPartnerRank);
        }
        try {
            sender = portType.createSendPort();
            IbisIdentifier rpHolder
                    = registry.getElectionResult("" + commPartnerRank);
            sender.connect(rpHolder, portString);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    protected ReadMessage getNextMessage() throws MPJException{
        try {
            msg = this.receiver.receive();
            return(msg);
        }
        catch(Exception e) {
            throw new MPJException(e.getMessage());
        }
    }

    protected boolean isConnectionEstablished() {
    	return ((sender.connectedTo().length != 0) &&
    			 (receiver.connectedTo().length != 0));
    }

    protected void putMPJObject(MPJObject obj, Object buf, int offset, int count) throws MPJException {
        synchronized (SENDLOCK) {
            try {
                if (messageCount == 0) { 
                    writeMessage = sender.newMessage();
                }
                writeMessage.writeArray(obj.desc);

                if (buf instanceof byte[]) {        			
                    writeMessage.writeArray(((byte[])buf), offset, count);
                }
                else if ((buf instanceof char[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)) {
                    writeMessage.writeArray(((char[])buf), offset, count);    			
                }
                else if ((buf instanceof short[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)) {
                    writeMessage.writeArray(((short[])buf), offset, count);    			
                }
                else if ((buf instanceof boolean[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)) {
                    writeMessage.writeArray(((boolean[])buf), offset, count);    			
                }
                else if ((buf instanceof int[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT))  {
                    writeMessage.writeArray(((int[])buf), offset, count);    			
                }
                else if ((buf instanceof long[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)) {
                    writeMessage.writeArray(((long[])buf), offset, count);    			
                }
                else if ((buf instanceof float[])  && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)){
                    writeMessage.writeArray(((float[])buf), offset, count);    			 
                }
                else if ((buf instanceof double[]) && (obj.getBaseDatatype() != Datatype.BASE_TYPE_OBJECT)) {

                    writeMessage.writeArray(((double[])buf), offset, count);    			
                }
                else {

                    writeMessage.writeArray(((Object[])buf), offset, count);
                }

                messageCount++;
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected void sendMPJObject() {
        synchronized(SENDLOCK) {
            if (messageCount != 0) {
                try {
                    writeMessage.send();
                    writeMessage.finish();
                    messageCount = 0;
                }
                catch (Exception e) {
                    System.err.println(e.getMessage());
                    e.printStackTrace();				
                }
            }
        }

    }



    protected ReadMessage pollForMessage() throws MPJException {
        try {

            return(receiver.poll());

        }
        catch (Exception e) {
            throw new MPJException(e.getMessage());


        }
    }

    protected void receiveHeader(ReadMessage readMsg, int[] header) throws MPJException {
        try {
            readMsg.readArray(header);
        }
        catch (IOException e) {
            throw new MPJException(e.getMessage());
        }

    }


    protected void receiveData(ReadMessage readMsg, Object buf, int offset, int count) throws MPJException {
        try {
            if (buf instanceof byte[]) {

                readMsg.readArray(((byte[])buf), offset, count);
            }
            else if (buf instanceof char[]) {
                readMsg.readArray(((char[])buf), offset, count);    			
            }
            else if (buf instanceof short[]) {
                readMsg.readArray(((short[])buf), offset, count);    			
            }
            else if (buf instanceof boolean[]) {
                readMsg.readArray(((boolean[])buf), offset, count);    			
            }
            else if (buf instanceof int[])  {
                readMsg.readArray(((int[])buf), offset, count);    			
            }
            else if (buf instanceof long[]) {
                readMsg.readArray(((long[])buf), offset, count);    			
            }
            else if (buf instanceof float[]) {
                readMsg.readArray(((float[])buf), offset, count);    			
            }
            else if (buf instanceof double[]) {
                readMsg.readArray(((double[])buf), offset, count);    			
            }
            else {
                readMsg.readArray(((Object[])buf), offset, count);
            }


        }
        catch (ClassNotFoundException e) {
            throw new MPJException(e.getMessage());
        }
        catch (IOException e) {
            throw new MPJException(e.getMessage());
        }

        try {
            readMsg.finish();
        }
        catch (IOException e) {
            throw new MPJException(e.getMessage());
        }

    }




    protected synchronized int getMessageCount() {
        return messageCount;
    }

    protected synchronized MPJObjectQueue getRecvQueue() {
        return recvQueue;
    }

    protected void close() {
        try {

            sender.close();
            receiver.close();

        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}

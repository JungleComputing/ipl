package ibis.ipl.impl.net;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.EOFException;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public final class NetServiceLink {

        private final int _OP_eof                  = 0;
        private final int _OP_request_substream_id = 1;
        private final int _OP_receive_substream_id = 2;

        private volatile boolean closed = false;

        private boolean      incoming = false;
        private Socket       socket   = null;
        private OutputStream os       = null;
        private InputStream  is       = null;

        private ObjectOutputStream main_oos = null;
        private ObjectInputStream  main_ois = null;

        private HashMap            outputMap    = null;
        private HashMap            inputMap     = null;
        private Vector             inputVector  = null;

        private int                nextId       =    1;

        private ListenerThread     listenThread = null;

        private ServiceThread      serviceThread     = null;
        private NetMutex           requestCompletion = new NetMutex(true);
        private NetMutex           requestReady      = new NetMutex(false);
        private Object             requestResult     = null;

        private NetEventQueue      portEventQueue    = null;

        private Integer            num               = null;


        /* ___ CONSTRUCTORS ________________________________________________ */

        protected NetServiceLink(NetEventQueue portEventQueue, ServerSocket ss) throws NetIbisException {
                this.portEventQueue = portEventQueue;
                incoming = true;

                try {
                        socket = ss.accept();
                } catch (SocketException e) {
                        throw new NetIbisInterruptedException(e);
                } catch (IOException e) {
                        throw new NetIbisIOException(e);
		} catch (Throwable t) {
			throw new NetIbisException(t);
                }
        }

        protected NetServiceLink(NetEventQueue portEventQueue, Hashtable nfo) throws NetIbisException {
                this.portEventQueue = portEventQueue;
                incoming = false;
                InetAddress raddr =  (InetAddress)nfo.get("accept_address");
		int         rport = ((Integer)    nfo.get("accept_port"   )).intValue();

                try {
			socket = new Socket(raddr, rport);
		} catch (IOException e) {
			throw new NetIbisIOException(e);
		} catch (Throwable t) {
			throw new NetIbisException(t);
                }
        }





        /* ___ CONNECTION MANAGEMENT ROUTINES ______________________________ */

        protected synchronized void init(Integer num) throws NetIbisException {
                if (this.num != null) {
                        throw new Error("invalid call");
                }
                this.num = num;

		try {
			os = socket.getOutputStream();
			is = socket.getInputStream();
		} catch (IOException e) {
			throw new NetIbisIOException(e);
		} catch (Throwable t) {
			throw new NetIbisException(t);
                }

                inputMap  = new HashMap();
                outputMap = new HashMap();

                inputVector = new Vector(1, 1);

                listenThread = new ListenerThread("is = "+is);

                ServiceInputStream  sis = new ServiceInputStream(0);
                InputClient         ic  = new InputClient();
                ic.name = "__main__";
                ic.id  = 0;
                ic.sis = sis;

                inputMap.put("__main__", ic);
                inputVector.setSize(1);
                inputVector.setElementAt(sis, 0);


                ServiceOutputStream sos = new ServiceOutputStream(0);
                OutputClient        oc  = new OutputClient();
                oc.name = "__main__";
                oc.id  = 0;
                oc.sos = sos;

                outputMap.put("__main__", oc);


                listenThread.start();

                serviceThread = new ServiceThread("anonymous");

                try {
                        if (incoming) {
                                main_ois = new ObjectInputStream(sis);
                                main_oos = new ObjectOutputStream(sos);
                                main_oos.flush();
                        } else {
                                main_oos = new ObjectOutputStream(sos);
                                main_oos.flush();
                                main_ois = new ObjectInputStream(sis);
                        }
                } catch (IOException e) {
                        throw new NetIbisIOException(e);
                }


                serviceThread.start();
        }

        public synchronized void close() throws NetIbisException {
                // System.err.println("NetServiceLink: close-->");
                if (closed) {
                        return;
                }

                closed = true;

                if (listenThread != null) {
                        listenThread.end();
                        //System.err.println("waiting for ServiceLink listen thread to join");
                        while (true) {
                                try {
                                        listenThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("ServiceLink listen thread joined");

                        listenThread = null;
                }

                if (serviceThread != null) {
                        serviceThread.end();

                        //System.err.println("waiting for ServiceLink service thread to join");
                        while (true) {
                                try {
                                        serviceThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("ServiceLink service thread joined");

                        serviceThread = null;
                }

                synchronized(outputMap) {
                        if (outputMap != null) {
                                Iterator i = outputMap.values().iterator();

                                while (i.hasNext()) {
                                        OutputClient oc = (OutputClient)i.next();

                                        try {
                                                oc.sos.close();
                                        } catch (IOException e) {
                                                throw new NetIbisIOException(e);
                                        }
                                }

                                outputMap.clear();
                                outputMap = null;
                        }
                }


                inputVector = null;

                synchronized(inputMap) {
                        if (inputMap != null) {
                                Iterator i = inputMap.values().iterator();

                                while (i.hasNext()) {
                                        InputClient ic  = (InputClient)i.next();

                                        try {
                                                ic.sis.close();
                                        } catch (IOException e) {
                                                throw new NetIbisIOException(e);
                                        }
                                }


                                inputMap.clear();
                                inputMap = null;
                        }
                }

		try {
			os.close();
			is.close();
                        socket.close();
		} catch (IOException e) {
			throw new NetIbisIOException(e);
		} catch (Throwable t) {
			throw new NetIbisException(t);
                }

                // System.err.println("NetServiceLink: close<--");
        }

        protected synchronized OutputStream getOutputSubStream(String name) throws NetIbisException {
                OutputClient oc = null;

                synchronized(outputMap) {
                        oc = (OutputClient)outputMap.get(name);

                        if (oc == null) {
                                oc = new OutputClient();

                                try {
                                        synchronized(main_oos) {
                                                main_oos.writeInt(_OP_request_substream_id);
                                                main_oos.writeUTF(name);
                                                main_oos.flush();
                                        }
                                } catch (IOException e) {
                                        throw new NetIbisIOException(e);
                                }

                                requestReady.unlock();
                                requestCompletion.lock();
                                oc.name = name;
                                oc.id = ((Integer)requestResult).intValue();

                                oc.sos = new ServiceOutputStream(oc.id);
                                outputMap.put(name, oc);
                        }
                }

                return oc.sos;
        }

        protected synchronized InputStream getInputSubStream(String name) throws NetIbisException {
                InputClient ic = null;

                synchronized(inputMap) {
                        ic = (InputClient)inputMap.get(name);

                        if (ic == null) {
                                ic = new InputClient();
                                ic.name = name;
                                ic.id = nextId++;
                                ic.sis = new ServiceInputStream(ic.id);

                                if (ic.id >= inputVector.size()) {
                                        inputVector.setSize(ic.id+1);
                                }

                                inputVector.setElementAt(ic.sis, ic.id);
                                inputMap.put(name, ic);
                        }
                }

                return ic.sis;
        }

        public OutputStream getOutputSubStream(NetIO io, String name) throws NetIbisException {
                return getOutputSubStream(io.context()+name);
        }

        public InputStream  getInputSubStream (NetIO io, String name) throws NetIbisException {
                return getInputSubStream(io.context()+name);
        }





        /* ___ INTERNAL CLASSES ____________________________________________ */

        private final class OutputClient {
                String              name = null;
                int                 id   = 0;
                ServiceOutputStream sos  = null;
        }



        private final class InputClient {
                String              name = null;
                int                id   = 0;
                ServiceInputStream sis = null;
        }



        public final class ServiceOutputStream extends OutputStream  {

                private boolean   closed    = false;
                private int       id        = -1;
                private final int length    = 65536;
                private byte []   buffer    = new byte[length];
                private int       offset    = 0;
                private byte []   intBuffer = new byte[4];

                private void writeBlock(byte[] b, int o, int l) throws IOException {
                        synchronized(os) {
                                NetConvert.writeInt(l, intBuffer);
                                os.write(id);
                                os.write(intBuffer);
                                os.write(b, o, l);
                        }
                }



                public ServiceOutputStream(int id) {
                        this.id = id;
                }

                private void doFlush() throws IOException {
                        // System.err.println("ServiceOutputStream("+id+").doFlush-->");
                        if (offset > 0) {
                                writeBlock(buffer, 0, offset);
                                offset = 0;
                        }
                        // System.err.println("ServiceOutputStream("+id+").doFlush<--");
                }


                public void close() throws IOException {
                        // System.err.println("ServiceOutputStream("+id+").close-->");
                        closed = true;
                        doFlush();
                        // System.err.println("ServiceOutputStream("+id+").close<--");
                }

                public void flush() throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }
                        doFlush();
                }

                public void write(byte[] buf) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        write(buf, 0, buf.length);
                }

                public void write(byte[] buf, int off, int len) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        if (len <= length-offset) {
                                System.arraycopy(buf, off, buffer, offset, len);
                                offset += len;

                                if (offset == length) {
                                        flush();
                                }
                        } else {
                                doFlush();
                                writeBlock(buf, off, len);
                        }
                }

                public void write(int val) throws IOException {
                        if (closed) {
                                throw new IOException("stream closed");
                        }

                        buffer[offset++] = (byte)(val & 0xFF);

                        if (offset == length) {
                                doFlush();
                        }
                }

        }



        public final class ServiceInputStream extends InputStream {

                private class BufferList {
                        BufferList    previous = null;
                        BufferList    next     = null;
                        byte       [] buf      = null;
                }


                private BufferList first  = null;
                private BufferList last   = null;
                private volatile boolean closed = false;
                private int id     = -1;
                private int avail  =  0;
                private int offset =  0;

                public ServiceInputStream(int id) {
                        this.id = id;
                }

                protected synchronized void addBuffer(byte [] b) {
                        BufferList bl = new BufferList();
                        bl.buf = b;

                        if (first == null) {
                                first = bl;
                                last  = bl;
                        } else {
                                bl.previous = last;
                                last.next   = bl;
                                last = bl;
                        }

                        avail += bl.buf.length;

                        notifyAll();
                }

                public synchronized int available() throws IOException {
                        return avail;
                }

                public synchronized void close() throws IOException {
                        closed = true;
                        notifyAll();
                }

                public boolean markSupported() {
                        return false;
                }

                private void nextBlock() {
                        offset = 0;
                        if (first.next == null) {
                                first = null;
                                last  = null;
                        } else {
                                first.next.previous = null;
                                BufferList temp = first.next;
                                first.next = null;
                                first = temp;
                        }
                }


                public synchronized int read() throws IOException {
                        if (closed && avail == 0) {
                                throw new EOFException("stream closed");
                        }

                        int result = 0;

                        if (avail == 0) {
                                try {
                                        wait();
                                        if (closed) {
                                                return -1;
                                        }
                                } catch (InterruptedException e) {
                                        throw new InterruptedIOException(e.getMessage());
                                }
                        }

                        if (avail > 0) {
                                result = 0xFF & (int)first.buf[offset++];
                                avail--;
                        }

                        if (offset == first.buf.length) {
                                nextBlock();
                        }

                        return result;
                }

                public int read(byte[] b) throws IOException {
                        return read(b, 0, b.length);
                }

                public synchronized int read(byte[] buf, int off, int len) throws IOException {
                        if (closed && avail == 0) {
                                throw new EOFException("stream closed");
                        }

                        int result = 0;

                        while (len > 0) {
                                if (avail == 0) {
                                        if (closed) {
                                                break;
                                        }

                                        try {
                                                wait();
                                                if (closed) {
                                                        break;
                                                }
                                        } catch (InterruptedException e) {
                                                throw new InterruptedIOException(e.getMessage());
                                        }
                                }

                                int copylength = Math.min(len, first.buf.length - offset);
                                System.arraycopy(first.buf, offset, buf, off, copylength);
                                result += copylength;
                                offset += copylength;
                                off    += copylength;
                                len    -= copylength;
                                avail  -= copylength;

                                if (offset == first.buf.length) {
                                        nextBlock();
                                }
                        }

                        return result;
                }
        }





        /* ..... LISTENER THREAD ___________________________________________ */

        private final class ListenerThread extends Thread {
                volatile boolean exit = false;
                private byte[]  intBuffer = new byte[4];

                ListenerThread(String name) {
                        super("ListenerThread: "+name);
                }

                public void run() {
                main_loop:
                        while (!exit) {

                                try {
                                        int id  = is.read();

                                        if (id == -1) {
                                                exit = true;
                                                continue;
                                        }

                                        ServiceInputStream sis = null;

                                        synchronized(inputMap) {
                                                sis = (ServiceInputStream)inputVector.elementAt(id);
                                        }

                                        if (sis == null) {
                                                throw new NetIbisException("invalid id");
                                        }

                                        is.read(intBuffer);
                                        byte [] b = new byte[NetConvert.readInt(intBuffer)];
                                        is.read(b);

                                        sis.addBuffer(b);
                                } catch (SocketException e) {
                                        exit = true;
                                        portEventQueue.put(new NetPortEvent(NetServiceLink.this, NetPortEvent.CLOSE_EVENT, num));
                                        continue;
                                } catch (EOFException e) {
                                        exit = true;
                                        continue;
                                } catch (InterruptedIOException e) {
                                        continue;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }

                protected void end() throws NetIbisException {
                        exit = true;
                        try {
                                is.close();
                        } catch (IOException e) {
                                throw new NetIbisIOException(e);
                        }
                }
        }

        private final class ServiceThread extends Thread {
                volatile boolean exit = false;

                ServiceThread(String name) {
                        super("ServiceThread: "+name);
                }

                protected void end() throws NetIbisException {
                        exit = true;
                        try {
                                main_oos.close();
                                main_ois.close();
                        } catch (IOException e) {
                                throw new NetIbisIOException(e);
                        }
                }

                private void requestSubstreamId(String name) {

                        synchronized(inputMap) {
                                InputClient ic = (InputClient)inputMap.get(name);

                                if (ic == null) {
                                        ic = new InputClient();
                                        ic.name = name;
                                        ic.id = nextId++;
                                        ic.sis = new ServiceInputStream(ic.id);
                                        if (ic.id >= inputVector.size()) {
                                                inputVector.setSize(ic.id+1);
                                        }

                                        inputVector.setElementAt(ic.sis, ic.id);
                                        inputMap.put(name, ic);
                                }

                                synchronized(main_oos) {
                                        try {
                                                main_oos.writeInt(_OP_receive_substream_id);
                                                main_oos.writeObject(new Integer(ic.id));
                                                main_oos.flush();
                                        } catch (IOException e) {
                                                throw new Error(e.getMessage());
                                        }
                                }
                        }
                }


                private void receiveSubstreamId(Object o) {
                        requestReady.lock();
                        requestResult = o;
                        requestCompletion.unlock();
                }

                public void run() {
                        while (!exit) {
                                try {
                                        int op = main_ois.readInt();

                                        switch (op) {

                                        case _OP_eof:
                                                {
                                                        exit = true;
                                                        close();
                                                }
                                                break;

                                        case _OP_request_substream_id:
                                                {
                                                        final String name = main_ois.readUTF();
                                                        Runnable r = new Runnable() {public void run() {requestSubstreamId(name);}};
                                                        (new Thread(r)).start();
                                                }
                                                break;

                                        case _OP_receive_substream_id:
                                                {
                                                        final Object o = main_ois.readObject();
                                                        Runnable r = new Runnable() {public void run() {receiveSubstreamId(o);}};
                                                        (new Thread(r)).start();
                                                }
                                                break;

                                        default:
                                                {
                                                        throw new NetIbisException("invalid operation");
                                                }
                                        }
                                } catch (InterruptedIOException e) {
                                        continue;
                                } catch (EOFException e) {
                                        exit = true;
                                        continue;
                                } catch (Exception e) {
                                        throw new Error(e);
                                }
                        }
                }
        }
}

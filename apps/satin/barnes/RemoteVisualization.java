import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class BodyList {

    private float[] bodies;

    int iteration;

    long runTime;
    
    BodyList(float[] bodies, int iteration, long runTime) {
        this.bodies = bodies;
        this.iteration = iteration;
        this.runTime = runTime;
    }

    float[] getBodies() {
        return bodies;
    }

    int getIteration() {
        return iteration;
    }

    public long getRunTime() {
        return runTime;
    }

    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }
}

public class RemoteVisualization extends Thread {

    private int maxLen = 10;

    private int port;

    private ServerSocket server;

    private boolean haveClient = false;

    private Socket client;

    private DataOutputStream out;

    private LinkedList list = new LinkedList();

    public RemoteVisualization() {
        getProperties();
        createServerSocket();
        start();
    }

    public RemoteVisualization(String file) throws IOException {
        haveClient = true;
        out = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(file), 128 * 1024));
        start();
    }

    private void getProperties() {
        try {
            port = Integer.parseInt(System.getProperty("nbody.port", "9889"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to find host properties");
        }
    }

    private void createServerSocket() {

        while (server == null) {
            try {
                server = new ServerSocket(port);
            } catch (Exception e) {
                System.out.println("Failed to create server socket, retry");

                try {
                    Thread.sleep(1000);
                } catch (Exception x) {
                    // ignore
                }
            }
        }
    }

    private void close() {
        try {
            out.close();
        } catch (Throwable e) {
            // ignore            
        }

        try {
            client.close();
        } catch (Throwable e) {
            // ignore
        }

        haveClient = false;
    }

    private void accept() {

        while (!haveClient) {
            try {
                client = server.accept();
                client.setSendBufferSize(128 * 1024);
                client.setTcpNoDelay(true);

                out = new DataOutputStream(new BufferedOutputStream(client
                    .getOutputStream(), 128 * 1024));

                haveClient = true;
            } catch (Exception e) {
                System.out.println("Failed to accept client, retry");
                close();
            }
        }
    }

    public void showBodies(Body[] bodies, int iteration, long runtime) {

        // Check if the buffer is already full. If so, we skip this round!
        synchronized (this) {
            if (list.size() > maxLen) {
                return;
            }
        }

        // There is space, so convert bodies to float[]...
        float[] tmp = new float[3 * bodies.length];

        int index = 0;

        for (int i = 0; i < bodies.length; i++) {
            tmp[index++] = (float) bodies[i].pos_x;
            tmp[index++] = (float) bodies[i].pos_y;
            tmp[index++] = (float) bodies[i].pos_z;
        }

        // And store it...
        synchronized (this) {
            list.addLast(new BodyList(tmp, iteration, runtime));
            notifyAll();
        }
    }

    private synchronized BodyList getBodies() {

        while (list.size() == 0) {
            try {
                wait();
            } catch (Exception e) {
                // ignore
            }
        }

        return (BodyList) list.removeFirst();
    }

    private void doSend() {
        long start = 0;
        try {
            //  System.out.println("Sending");

            BodyList bodies = getBodies();

            start = System.currentTimeMillis();

            out.writeInt(bodies.getBodies().length / 3);
            out.writeInt(bodies.getIteration());
            out.writeLong(bodies.getRunTime());

            float[] b = bodies.getBodies();
            for (int i = 0; i < b.length; i++) {
                out.writeFloat(b[i]);
            }

            long time = System.currentTimeMillis() - start;
            System.err.println("writes took " + time + " ms");

            out.flush();

            //   System.out.println("Sending Done");

        } catch (Exception e) {
            System.out.println("Lost connection during send!");
            close();
        }
        long time = System.currentTimeMillis() - start;
        System.err.println("send took " + time + " ms");
    }

    public void run() {

        while (true) {

            if (!haveClient) {
                accept();
            }

            doSend();
        }
    }
}

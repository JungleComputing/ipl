import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

class BodyList {

    private float[] bodies;

    int iteration;

    BodyList(float[] bodies, int iteration) {
        this.bodies = bodies;
        this.iteration = iteration;
    }

    float[] getBodies() {
        return bodies;
    }

    int getIteration() {
        return iteration;
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
        } catch (Exception e) {
            // ignore            
        }

        try {
            client.close();
        } catch (Exception e) {
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

    public void showBodies(Body[] bodies, int iteration) {

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
            list.addLast(new BodyList(tmp, iteration));
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
        long start = System.currentTimeMillis();
        try {
            //  System.out.println("Sending");

            BodyList bodies = getBodies();

            out.writeInt(bodies.getBodies().length / 3);
            out.writeInt(bodies.getIteration());

            for (int i = 0; i < bodies.getBodies().length; i++) {
                out.writeFloat(bodies.getBodies()[i]);
            }

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

        createServerSocket();

        while (true) {

            if (!haveClient) {
                accept();
            }

            doSend();
        }
    }
}

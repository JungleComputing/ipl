class Client {

    public static final int COUNT = 10000;

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err
                    .println("usage: java Client <server hostname> <port> <name>");
            System.exit(1);
        }

        int port = 0;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err
                    .println("usage: java Client <server hostname> <port> <name>");
            System.exit(1);
        }

        doClient(args[0], port, args[2]);
    }

    public static void doClient(String server, int port, String name) {
        try {

            String objname = "//" + server;
            if (port != 0) {
                objname = objname + ":" + port;
            }
            objname = objname + "/" + name;

            MyServer s = (MyServer) RMI_init.lookup(objname);

            for (int j = 0; j < 10; j++) {

                long start = System.currentTimeMillis();

                for (int i = 0; i < COUNT; i++) {
                    s.foo();
                }

                long end = System.currentTimeMillis();
                System.out.println("null latency (" + COUNT + ") = "
                        + ((1000.0 * (end - start)) / (COUNT)) + " usec/call");
            }
            s.quit();
        } catch (Exception e) {
            System.out.println("OOPS");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
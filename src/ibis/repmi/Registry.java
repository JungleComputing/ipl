package ibis.repmi;

import ibis.ipl.ReadMessage;

import java.io.IOException;

final class Registry {

    int number;

    public Registry() {
        number = 0;
    }

    private synchronized int getNumber() {
        return number++;
    }

    public void handleMessage(ReadMessage r) {

        try {
            // int rank = r.readInt();	
            // int ticket = r.readInt();
            r.finish();

        } catch (IOException e) {
            System.out.println("Error in Registry " + e);
            System.exit(1);
        }
    }
}

/* $Id$ */

class BtreeSender extends Thread {
    i_Asp dest;

    int[] row;

    int k;

    int owner;

    BtreeSender(i_Asp dest, int[] row, int k, int owner) {
        this.dest = dest;
        this.row = row;
        this.k = k;
        this.owner = owner;
    }

    public void run() {
        try {
            dest.btree_transfer(row, k, owner);

        } catch (Exception e) {
            System.out.println("Btree send failed ! " + e);
            e.printStackTrace();
        }
    }
}
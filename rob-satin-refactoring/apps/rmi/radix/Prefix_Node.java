/* $Id$ */


import java.io.Serializable;

class Prefix_Node implements Serializable {

    int[] densities;

    int[] ranks;

    boolean done;

    Prefix_Node(int max_Radix) {
        densities = new int[max_Radix];
        ranks = new int[max_Radix];
        done = false;
        init();
    }

    Prefix_Node(Prefix_Node node) {
        this.done = node.done;
        densities = new int[node.densities.length];
        ranks = new int[node.ranks.length];
        for (int i = 0; i < node.densities.length; i++) {
            this.densities[i] = node.densities[i];
        }
        for (int i = 0; i < node.ranks.length; i++) {
            this.ranks[i] = node.ranks[i];
        }
    }

    void init() {
        int length = densities.length;
        for (int i = 0; i < length; i++) {
            densities[i] = 0;
            ranks[i] = 0;
        }
    }

    public void waitPauze() {
        done = false;
    }

    public void clearPauze() {
        done = true;
    }

    public void setPauze() {
        done = false;
    }
}


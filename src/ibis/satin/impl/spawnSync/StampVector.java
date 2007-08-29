/* $Id$ */

package ibis.satin.impl.spawnSync;

import ibis.satin.impl.Config;

public final class StampVector implements Config {
    private Stamp[] stamps = new Stamp[50];

    private int count = 0;

    public void add(Stamp stamp) {
        if (count >= stamps.length) {
            Stamp[] nstamps = new Stamp[stamps.length * 2];
            System.arraycopy(stamps, 0, nstamps, 0, stamps.length);
            stamps = nstamps;
        }

        stamps[count] = stamp;
        count++;
    }

    public boolean containsParentOf(Stamp stamp) {
        for (int i = 0; i < count; i++) {
            if (stamps[i].stampEquals(stamp)) {
                return true;
            }
        }

        return false;
    }

    public void removeIndex(int i) {
        if (ASSERTS) {
            if (i >= count || i < 0) {
                System.err.println("removeIndex of strange index: " + i);
                new Exception().printStackTrace();
                System.exit(1); // Failed assertion
            }
        }

        count--;
        stamps[i] = stamps[count];
    }

    public Stamp getStamp(int index) {
        return stamps[index];
    }

    public int getCount() {
        return count;
    }
}

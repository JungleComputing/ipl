// File: $Id$

/** Helper methods. */

final class Helpers {
    /** Protect constructor since it is a static only class. */
    private Helpers() {
    }

    /**
     * Given an int array <code>a</code> and a size <code>sz</code>, create a new array of size <code>sz</code>
     * that contains the first <code>sz</code> elements of <code>a</code>.
     * @param a the array to clone
     * @param sz the number of elements to clone
     * @return the cloned array
     */
    static int[] cloneIntArray(int a[], int sz) {
        int res[] = new int[sz];

        System.arraycopy(a, 0, res, 0, sz);
        return res;
    }

    /**
     * Given two int arrays, return true iff they have equal elements.
     */
    static boolean areEqualArrays(int a[], int b[]) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    static int[] append(int a[], int v) {
        int sz = a.length;
        int res[] = new int[sz + 1];

        System.arraycopy(a, 0, res, 0, sz);
        res[sz] = v;
        return res;
    }

    static boolean contains(int a[], int v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == v) {
                return true;
            }
        }
        return false;
    }

    /**
     * Given an array and an index range in that array, return the
     * index of the smallest element. If the range is empty,
     * return the first index in the range.
     * @param l the array
     * @param from the first element to consider
     * @param to the first element not to consider
     */
    private static int selectSmallest(int l[], int from, int to) {
        int index = from;

        for (int i = from; i < to; i++) {
            if (l[i] < l[index]) {
                index = i;
            }
        }
        return index;
    }

    /**
     * Given an int array, sorts it in place.
     * @param l The array to sort.
     */
    public static void sortIntArray(int l[]) {
        // This is insertion sort.
        int sz = l.length;

        // We don't have to sort the last element
        for (int i = 0; i < sz - 1; i++) {
            // Find the smallest.
            int pos = selectSmallest(l, i, sz);

            if (pos != i) {
                // Put the smallest in element i, and put the
                // current element in it's old position.
                int temp = l[i];
                l[i] = l[pos];
                l[pos] = temp;
            }
        }
    }

    /**
     * Given an array, returns true iff every element in it is larger
     * than the ones before it in the array.
     * @param l The array to test.
     * @return True iff the array elements are monotonically increasing.
     */
    public static boolean isIncreasing(int l[]) {
        if (l.length == 0) {
            return true;
        }
        int prev = l[0];

        for (int i = 1; i < l.length; i++) {
            if (l[i] <= prev) {
                return false;
            }
            prev = l[i];
        }
        return true;
    }

    /**
     * Prints the specified array of assignments to the error stream.
     * @param assignment The array of assignments.
     */
    public static void dumpAssignments(String label, byte assignment[]) {
        System.err.print(label + ":");
        for (int j = 0; j < assignment.length; j++) {
            byte v = assignment[j];

            if (v != -1) {
                System.err.print(" v" + j + "=" + v);
            }
        }
        System.err.println();
    }

    /** Return a string describing the platform version that is used. */
    static String getPlatformVersion() {
        java.util.Properties p = System.getProperties();

        return "Java " + p.getProperty("java.version") + " ("
                + p.getProperty("java.vendor") + ") on "
                + p.getProperty("os.name") + " " + p.getProperty("os.version")
                + " (" + p.getProperty("os.arch") + ")";
    }
}
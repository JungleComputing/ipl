final public class DistanceTable implements java.io.Serializable {

    static final int MAXX = 100, MAXY = 100, INF = Integer.MAX_VALUE;

    static final private boolean PRINT_DIST = false;

    final int[] lowerBound;

    final byte[][] toCity;

    final int[][] dist;

    public DistanceTable(int size) {
        toCity = new byte[size][size];
        dist = new int[size][size];
        lowerBound = new int[size];
    }

    private static void putmin(int[] a, int pos) {
        int minpos = pos;
        int min = Integer.MAX_VALUE;

        for (int i = pos; i < a.length; i++) {
            if (a[i] == 0)
                a[i] = Integer.MAX_VALUE;
            if (a[i] < min) {
                minpos = i;
                min = a[i];
            }
        }

        int tmp;
        tmp = a[pos];
        a[pos] = a[minpos];
        a[minpos] = tmp;
    }

    private static void sort(int[] a) {
        for (int i = 0; i < a.length - 1; i++) {
            putmin(a, i); // zet min elt in a(vanaf i) op plaats i.
        }
    }

    private static int calcLowerBound(int hops, int[] table) {
        int res = 0;
        for (int i = 0; i < hops; i++) {
            res += table[i];
        }

        return res;
    }

    public static DistanceTable generate(int n, int seed) {
        DistanceTable pairs = new DistanceTable(n);
        int[] tempdist = new int[n];
        Coord[] towns = new Coord[n];
        int dx = 0, dy = 0, x = 0, tmp;
        OrcaRandom r = new OrcaRandom(seed);
        int[] minDists = new int[n * n];
        int minDistCount = 0;

        for (int i = 0; i < n; i++) {
            towns[i] = new Coord();
            towns[i].x = r.nextInt() % MAXX;
            towns[i].y = r.nextInt() % MAXY;
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                dx = towns[i].x - towns[j].x;
                dy = towns[i].y - towns[j].y;

                tempdist[j] = (int) Math.sqrt(dx * dx + dy * dy);
                if (i != j && tempdist[j] != 0) {
                    minDists[minDistCount] = tempdist[j];
                    minDistCount++;
                }
            }

            // Sort pairs[i]: nearest city first.
            for (int j = 0; j < n; j++) {
                tmp = INF;
                for (int k = 0; k < n; k++) {
                    if (tempdist[k] < tmp) {
                        tmp = tempdist[k];
                        x = k;
                    }
                }
                tempdist[x] = INF;
                pairs.toCity[i][j] = (byte) x;
                pairs.dist[i][j] = tmp;
            }
        }
        //		pairs.print(towns);

        sort(minDists);

        for (int i = 0; i < n; i++) {
            pairs.lowerBound[i] = calcLowerBound(i, minDists);
        }

        return pairs;
    }

    void print(Coord[] towns) {
        if (PRINT_DIST) {
            for (int i = 0; i < dist.length; i++) {
                for (int j = 0; j < dist.length; j++) {
                    System.out.println("dist from city " + i + " to "
                            + toCity[i][j] + " = " + dist[i][j]);
                }
            }
        } else {
            for (int i = 0; i < towns.length; i++) {
                System.out.print("(" + towns[i].x + "," + towns[i].y + ") ");
            }
            System.out.println();
        }
    }
}
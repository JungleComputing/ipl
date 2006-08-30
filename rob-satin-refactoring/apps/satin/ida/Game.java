/* $Id$ */

final class Game implements java.io.Serializable {
    private int[][] start = new int[Ida.NSQRT + 1][Ida.NSQRT + 1];

    private Position[] goal = new Position[Ida.NPUZZLE + 1];

    Game() {
        for (int i = 0; i <= Ida.NPUZZLE; i++) {
            goal[i] = new Position();
        }

        int v = 0;
        for (int j = 1; j <= Ida.NSQRT; j++) {
            for (int i = 1; i <= Ida.NSQRT; i++) {
                goal[v].x = i;
                goal[v].y = j;
                v++;
            }
        }
    }

    int value(int x, int y) {
        return start[x][y];
    }

    int distance(int v, int x, int y) {
        if (v == 0)
            return 0;

        // abs(goal[v].x - x) + abs(goal[v].y - y)
        int a = goal[v].x - x;
        a = a > 0 ? a : -a;
        int b = goal[v].y - y;
        b = b > 0 ? b : -b;
        return a + b;
    }

    int[][] step(int[][] board, int x, int y, int n, int m) {
        int v;

        v = board[x][y];
        board[x][y] = board[n][m];
        board[n][m] = v;
        return board;
    }

    void init(int length) {
        int[][][] path = new int[length + 1][Ida.NSQRT + 1][Ida.NSQRT + 1];
        int v, x, y, nx, ny, N;

        v = 0;
        for (int j = 1; j <= Ida.NSQRT; j++) {
            for (int i = 1; i <= Ida.NSQRT; i++) {
                path[0][i][j] = v;
                v++;
            }
        }

        // Generate a starting position by shuffling the blanc around
        // in cycles. Just cycling along the outer bounds of the
        // board yields low quality start positions whose solutions
        // require less than 'length' steps. Therefore we use two
        // alternating cycling dimensions.

        N = Ida.NSQRT - 1;
        x = 1;
        y = 1; // position of the blanc

        for (int i = 1; i <= length; i++) {
            if (x == 1) {
                if (y == N) {
                    nx = x + 1;
                    ny = y;
                } else {
                    nx = x;
                    ny = y + 1;
                    if (y == 1) {
                        if (N < Ida.NSQRT) {
                            N = Ida.NSQRT;
                        } else {
                            N = Ida.NSQRT - 1;
                        }
                    }
                }
            } else if (x == N) {
                if (y == 1) {
                    nx = x - 1;
                    ny = y;
                } else {
                    nx = x;
                    ny = y - 1;
                }
            } else {
                if (y == N) {
                    nx = x + 1;
                    ny = y;
                } else {
                    nx = x - 1;
                    ny = y;
                }
            }

            path[i] = step(path[i - 1], x, y, nx, ny);
            x = nx;
            y = ny;
        }

        for (int j = 1; j <= Ida.NSQRT; j++) {
            for (int i = 1; i <= Ida.NSQRT; i++) {
                v = path[length][i][j];
                start[i][j] = v;
            }
        }
    }

    void init(String fileName) {
        Input in = new Input(fileName);
        for (int j = 1; j <= Ida.NSQRT; j++) {
            for (int i = 1; i <= Ida.NSQRT; i++) {
                start[i][j] = in.readInt();
            }
            in.readln();
        }
    }
}
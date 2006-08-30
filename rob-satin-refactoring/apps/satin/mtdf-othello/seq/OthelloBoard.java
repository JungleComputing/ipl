/* $Id$ */

final class OthelloBoard extends NodeType {
    static final int SIZE = 8;
    static final int MAX_CHILDREN = 32;

    static final byte BLACK = 0, WHITE = 1, EMPTY = 2;

    boolean yielded = false;

    int childNr = -1;

    byte[] pos = new byte[SIZE*SIZE];

    private byte getPos(int x, int y) {
        return pos[y*SIZE+x];
    }

    private void setPos(int x, int y, byte value) {
        pos[y*SIZE+x] = value;
    }

    // assumes it's white's move
    private void fillPath(int x, int y, int dx, int dy) {
        x += dx;
        y += dy;

        do {
            setPos(x, y, WHITE);

            x += dx;
            y += dy;
        } while (getPos(x, y) == BLACK);
    }

    private boolean isValidPos(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    // assumes it's black's turn
    private boolean checkPath(int x, int y, int dx, int dy) {
        x += dx;
        y += dy;
        if (!isValidPos(x, y)) return false;

        if (getPos(x, y) != WHITE) return false;

        do {
            x += dx;
            y += dy;
            if (!isValidPos(x, y)) return false;

        } while (getPos(x, y) == WHITE);

        return getPos(x, y) == BLACK;
    }

    private boolean isValidMove(int x, int y) {
        if (getPos(x, y) == EMPTY) {
            if (checkPath(x, y, -1, -1)) return true;
            if (checkPath(x, y, -1, 0)) return true;
            if (checkPath(x, y, -1, 1)) return true;
            if (checkPath(x, y, 0, -1)) return true;
            if (checkPath(x, y, 0, 1)) return true;
            if (checkPath(x, y, 1, -1)) return true;
            if (checkPath(x, y, 1, 0)) return true;
            if (checkPath(x, y, 1, 1)) return true;
        }

        return false;
    }

    OthelloBoard invert() {
        OthelloBoard result = new OthelloBoard();

        for (int x = 0; x < SIZE; x++)
          for (int y = 0; y < SIZE; y++)
            switch (getPos(x, y)) {
              case BLACK : result.setPos(x, y, WHITE); break;
              case WHITE : result.setPos(x, y, BLACK); break;
              case EMPTY : result.setPos(x, y, EMPTY); break;
            }

        result.score = (short)-score;
        result.yielded = yielded;

        return result;
    }

    // return null for leaf node
    NodeType[] generateChildren() {
        Heap heap = new Heap(MAX_CHILDREN);
        OthelloBoard inverted = invert();
        int nrStones = 0, nrBlack = 0;

        // find all the children, adding them to an array
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (getPos(x, y) == BLACK) nrBlack++;
                if (getPos(x, y) != EMPTY) nrStones++;

                if (isValidMove(x, y)) {
                    OthelloBoard child = doMove(inverted, x, y);
                    child.childNr = x + y * SIZE;

                    heap.add(child);
                }
            }
        }

        // did we find any children at all?
        if (heap.size() == 0) {
            // did we reach an end position (by full board, win, or deadlock)?
            if (nrStones == SIZE*SIZE || yielded == true ||
                    nrBlack == 0 || nrBlack == nrStones) {
                // store the score for this board, now that we know it's over
                score = (short)(nrBlack * 2 - nrStones);

                // make sure the end positions stand out
                if (score > 0) score += 50;
                else if (score < 0) score -= 50;

                return null;
            }

            // pass the turn to the other side
            inverted.yielded = true;
            heap.add(inverted);
        }

        // now, sort the children according to their 'prescore'
        // and return the result
        return heap.get();
    }

    private OthelloBoard doMove(OthelloBoard inverted, int x, int y) {
        OthelloBoard child = inverted.copy();
        child.setPos(x, y, WHITE);

        if (checkPath(x, y, -1, -1)) child.fillPath(x, y, -1, -1);
        if (checkPath(x, y, -1, 0)) child.fillPath(x, y, -1, 0);
        if (checkPath(x, y, -1, 1)) child.fillPath(x, y, -1, 1);
        if (checkPath(x, y, 0, -1)) child.fillPath(x, y, 0, -1);
        if (checkPath(x, y, 0, 1)) child.fillPath(x, y, 0, 1);
        if (checkPath(x, y, 1, -1)) child.fillPath(x, y, 1, -1);
        if (checkPath(x, y, 1, 0)) child.fillPath(x, y, 1, 0);
        if (checkPath(x, y, 1, 1)) child.fillPath(x, y, 1, 1);

        child.yielded = false;

        // set the score for move ordering
        child.prescore =
            (short)(child.getStoneScore() + child.getSquareScore());

        return child;
    }

    private OthelloBoard copy() {
        OthelloBoard result = new OthelloBoard();

        System.arraycopy(pos, 0, result.pos, 0, SIZE*SIZE);

        result.yielded = yielded;

        return result;
    }

    // start of Marvin code (GPL)
    private static final int weightMatrix[][] = {
      { 90,   0,  5,  5,  5,  5,  0, 90},
      {  0, -60,  0,  0,  0,  0,-60,  0},
      {  5,   0,  0,  0,  0,  0,  0,  5},
      {  5,   0,  0,  0,  0,  0,  0,  5},
      {  5,   0,  0,  0,  0,  0,  0,  5},
      {  5,   0,  0,  0,  0,  0,  0,  5},
      {  0, -60,  0,  0,  0,  0,-60,  0},
      { 90,   0,  5,  5,  5,  5,  0, 90}
    };

    private int getCornerValue(int x, int y) {
        if (x <= 1 && y <= 1 && (x != 0 || y != 0) && getPos(0, 0) != EMPTY)
          return 10;
        if (x >= SIZE-2 && y >= SIZE-2 && (x != SIZE-1 || y != SIZE-1) &&
            getPos(SIZE-1, SIZE-1) != EMPTY)
          return 10;
        if (x <= 1 && y >= SIZE-2 && (x != 0 || y != SIZE-1) &&
            getPos(0, SIZE-1) != EMPTY)
          return 10;
        if (x >= SIZE-2 && y <= 1 && (x != SIZE-1 || y != 0) &&
            getPos(SIZE-1, 0) != EMPTY)
          return 10;

        return 0;
    }

    private int getSquareScore() {
        int sum = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                int value = getCornerValue(x, y);

                // weightMatrix is for 8x8 boards only
                if (SIZE == 8) value += weightMatrix[x][y];

                if (getPos(x, y) == BLACK) sum += value;
                if (getPos(x, y) == WHITE) sum -= value;
            }
        }

        return sum;
    }
 
    private int getMoveValue(int x, int y, int dx, int dy) {
        x += dx;
        y += dy;

        if (!isValidPos(x, y)) return 0;

        byte color = getPos(x, y);

        do {
            x += dx;
            y += dy;

            if (!isValidPos(x, y)) return 0;

        } while (getPos(x, y) == color);

        if (getPos(x, y) == BLACK) return 1;
        if (getPos(x, y) == WHITE) return -1;
        return 0;
    }

    private int getMobilityScore() {
        int sum = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (getPos(x, y) == EMPTY) {
                    sum += getMoveValue(x, y, -1, -1);
                    sum += getMoveValue(x, y, -1,  0);
                    sum += getMoveValue(x, y, -1,  1);
                    sum += getMoveValue(x, y,  0, -1);
                    sum += getMoveValue(x, y,  0,  1);
                    sum += getMoveValue(x, y,  1, -1);
                    sum += getMoveValue(x, y,  1,  0);
                    sum += getMoveValue(x, y,  1,  1);
                }
            }
        }

        return sum;
    }
   
    private int getAdjacentValue(int x, int y) {
        if (isValidPos(x, y)) {
            if (getPos(x, y) == WHITE) return 1;
            if (getPos(x, y) == BLACK) return -1;
        }

        return 0;
    }

    private int getPotentialMobilityScore() {
        int sum = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (getPos(x, y) == EMPTY) {
                    sum += getAdjacentValue(x - 1, y - 1);
                    sum += getAdjacentValue(x - 1, y    );
                    sum += getAdjacentValue(x - 1, y + 1);
                    sum += getAdjacentValue(x    , y - 1);
                    sum += getAdjacentValue(x    , y + 1);
                    sum += getAdjacentValue(x + 1, y - 1);
                    sum += getAdjacentValue(x + 1, y    );
                    sum += getAdjacentValue(x + 1, y + 1);
                }
            }
        }

        return sum;
    }

    private int getStoneScore() {
        int nrBlack = 0, nrWhite = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                switch (getPos(x, y)) {
                    case BLACK : nrBlack++; break;
                    case WHITE : nrWhite++; break;
                }
            }
        }

        return nrBlack - nrWhite;
    }

    private int countStones() {
        int nrStones = 0;

        for (int x = 0; x < SIZE; x++)
          for (int y = 0; y < SIZE; y++)
            if (getPos(x, y) != EMPTY) nrStones++;

        return nrStones;
    }

    void evaluate() {
        // if there already was a score, then either we set it before,
        // or generateChildren concluded that the game is over.
        if (score != 0) return;

        int nrStones = countStones();

        int squareScore = getSquareScore();
        int mobilityScore = getMobilityScore();
        int potentialScore = getPotentialMobilityScore();
        int stoneScore = getStoneScore();

        score = (short)((squareScore * 2 +
          mobilityScore * (3 + rising(nrStones)) + 
          potentialScore * falling(nrStones) +
          stoneScore * rising(nrStones)) / 8);
    }

    private int rising(int nrStones) {
        return 1 + (int)Math.round(nrStones * 0.1);
    }
    private int falling(int nrStones) {
        return 7 - (int)Math.round(nrStones * 0.1);
    }
    // end of Marvin code

    void print() {
        System.out.println(" 01234567".substring(0, SIZE + 1));

        for (int y = 0; y < SIZE; y++) {
            System.out.print(y);

            for (int x = 0; x < SIZE; x++) {
                switch (getPos(x, y)) {
                  case BLACK : System.out.print('*'); break;
                  case WHITE : System.out.print('o'); break;
                  case EMPTY : System.out.print('.'); break;
                }
            }

            System.out.println();
        }

        System.out.println("score = " + score + "\n");
    }

    static OthelloBoard getRoot() {
        OthelloBoard root = new OthelloBoard();

	for (int x = 0; x < SIZE; x++)
	  for (int y = 0; y < SIZE; y++)
            root.setPos(x, y, EMPTY);

        root.setPos(SIZE/2-1, SIZE/2-1, WHITE);
        root.setPos(SIZE/2-1, SIZE/2, BLACK);
        root.setPos(SIZE/2, SIZE/2-1, BLACK);
        root.setPos(SIZE/2, SIZE/2, WHITE);

        root.evaluate();

        return root;
    }

    static OthelloBoard readBoard(String file) {
        Input in = new Input(file);
        OthelloBoard res = new OthelloBoard();

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++)
              switch ((byte)in.readChar()) {
                case '.' : res.setPos(x, y, EMPTY); break;
                case '*' : res.setPos(x, y, BLACK); break;
                case 'o' : res.setPos(x, y, WHITE); break;
              }
            in.readln();
        }

        // XXX read in whose move it is (i.e. use res.invert()? and/or yielded?

        res.evaluate();

        return res;
    }

    static int getTagSize() {
        return OthelloTag.SIZE;
    }

    Tag getTag() {
        return new OthelloTag(pos);
    }
}

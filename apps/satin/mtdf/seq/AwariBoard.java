import java.util.Random;

final class AwariBoard extends NodeType implements Cloneable {
	static final int PITS = 12;
	static final int START_VAL = 4;
	
	private static final byte[][] next = new byte[][] {
		{ 1,  2,  3,  4,  5,  7,  7,  8,  9, 10, 11,  0 },
		{ 1,  2,  3,  4,  5,  6,  8,  8,  9, 10, 11,  0 },
		{ 1,  2,  3,  4,  5,  6,  7,  9,  9, 10, 11,  0 },
		{ 1,  2,  3,  4,  5,  6,  7,  8, 10, 10, 11,  0 },
		{ 1,  2,  3,  4,  5,  6,  7,  8,  9, 11, 11,  0 },
		{ 1,  2,  3,  4,  5,  6,  7,  8,  9, 10,  0,  0 },
	};

	byte[] pits = new byte[PITS];
	byte captures = 0;

	static long[][] rand = new long[12][49];

	static {
		Random r = new Random(42);

		for(int i=0; i<12; i++) {
			for(int j=0; j<49; j++) {
				rand[i][j] = r.nextLong();
			}
		}
	}

	boolean canDoMove() {
		for (int i=0; i<6; i++) {
			if (pits[i] != 0) {
				return true;
			}
		}

		return false;
	}

	AwariBoard mirror() {
		AwariBoard result = new AwariBoard();

		for (int i=0; i<6; i++) {
			result.pits[i] = pits[i+6];
			result.pits[i+6] = pits[i];
		}

		result.score = (short) -result.score;
		result.captures = (byte) -result.captures;

		return result;
	}

	// return null for leaf node
	NodeType[] generateChildren() {
		AwariBoard[] children = new AwariBoard[6];
		AwariBoard mirror = mirror();
		int nrChildren = 0;

		for (int i=0; i<6; i++) {
			if (pits[i] > 0) {
				AwariBoard child = doMove(mirror, i, pits[i]);
				if (child.canDoMove()) {
					children[nrChildren++] = child;
				}
			}
		}

		if (nrChildren == 0) {
			for (int i=0; i<6; i++) {
				if (pits[i] > 0) {
					children[nrChildren++] = doMove(mirror, i, pits[i]);
				}
			}
		}

		AwariBoard[] result = new AwariBoard[nrChildren];
		System.arraycopy(children, 0, result, 0, nrChildren);

		return result;
	}

	private AwariBoard copy() {
		AwariBoard result = null;

		try {
			result = (AwariBoard) clone();
			result.pits = (byte[]) pits.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("eek " + e);
		}

		return result;
	}

	private AwariBoard doMove(AwariBoard mirror, int pit1, int seeds) {
		int pit2 = pit1 + 6;

		AwariBoard child = mirror.copy();

		child.pits[pit2] = 0;

		do {
			pit2 = next[pit1][pit2];
			++child.pits[pit2];
		} while (--seeds > 0);

		while (pit2 >= 0 && pit2 < 6 && (child.pits[pit2] == 2 || child.pits[pit2] == 3)) {
			child.score -= child.pits[pit2];
			child.captures -= child.pits[pit2]; ///@@@@@ EEEK, SHOULD BE child.captures, used to be captures.
			child.pits[pit2--] = 0; // capture
		}

		child.calcSignature();
		return child;
	}

	void evaluate() { // set score
		score = (short) (64 * captures);

		for (int i=0; i<6; i++) {
			score += pits[i] - pits[i+6];
		}
	}

	private void printPit(int seeds) {
		if(seeds == 0) {
			System.out.print("    |");
		} else if (seeds < 10) {
			System.out.print("  " + seeds + " |");
		} else {
			System.out.print(" " + seeds + " |");
		}
	}

	void print() {
		System.out.print("+----+----+----+----+----+----+\n|");

		for (int i=12; --i >=6; ) {
			printPit(pits[i]);
		}

		System.out.print("\n+----+----+----+----+----+----+\n|");

		for (int i=0; i<6; i++) {
			printPit(pits[i]);
		}

		System.out.print("\n+----+----+----+----+----+----+\n");
		System.out.println("score = " + score + "\n");
	}

	static AwariBoard getRoot() {
		AwariBoard root = new AwariBoard();
		for(int i=0; i<PITS; i++) {
			root.pits[i] = START_VAL;
		}
		root.calcSignature();
		root.evaluate();

		return root;
	}

	static AwariBoard readBoard(String file) {
		Input in = new Input(file);
		AwariBoard res = new AwariBoard();

		for (int i=0; i<PITS; i++) {
			res.pits[i] = (byte) in.readInt();
		}
		in.readln();
		res.captures = (byte) in.readInt();

		res.calcSignature();
		res.evaluate();

		return res;
	}

	private void calcSignature() {
		for(int i=0; i<12; i++) {
			signature ^= rand[i][pits[i]];
		}
	}
}

final class Job implements java.io.Serializable {
	private byte[] board;
	int distance;
	int bound;
	int blankX, blankY;
	int prevDx, prevDy;

	Job() {
		board = new byte[Ida.NSQRT * Ida.NSQRT];
	}


	Job(Job j) {
		// Copy constructor...
		board = new byte[Ida.NSQRT * Ida.NSQRT];
		System.arraycopy(j.board, 0, board, 0, Ida.NSQRT * Ida.NSQRT);
		distance = j.distance;
		bound = j.bound;
		blankX = j.blankX;
		blankY = j.blankY;
		prevDx = j.prevDx;
		prevDy = j.prevDy;
	}


	int getVal(int x, int y) {
		return board[((x-1) * Ida.NSQRT) + y-1];
	}


	void setVal(int x, int y, int val) {
		board[((x-1) * Ida.NSQRT) + y-1] = (byte)val;
	}
}

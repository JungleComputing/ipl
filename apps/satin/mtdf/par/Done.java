final class Done extends Exception {
	short score;
	short currChild;

	Done(short score, short currChild) {
		this.score = score;
		this.currChild = currChild;
	}
}

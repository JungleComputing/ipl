/* $Id$ */

final class Done extends Throwable {
    short score;

    short currChild;

    Done(short score, short currChild) {
        this.score = score;
        this.currChild = currChild;
    }

    public Throwable fillInStackTrace() {
        return null;
    }
}
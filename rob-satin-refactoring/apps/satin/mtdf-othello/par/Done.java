/* $Id$ */

final class Done extends ibis.satin.Inlet {
    short score;

    short currChild;

    Done(short score, short currChild) {
        this.score = score;
        this.currChild = currChild;
    }
}

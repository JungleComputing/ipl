/* $Id$ */

class Done extends Exception {
    int res;

    Done(int res) {
        this.res = res;
    }
}
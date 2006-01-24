import ibis.satin.SharedObject;

/* $Id$ */

final class SharedMatrix extends SharedObject implements SharedMatrixInterface  {
    Matrix m;

    // write method
    public void setMatrix(Matrix m) {
        this.m = m;
    }
}

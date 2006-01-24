import ibis.satin.WriteMethodsInterface;

interface SharedMatrixInterface extends WriteMethodsInterface {
    void setMatrix(Matrix m);
}

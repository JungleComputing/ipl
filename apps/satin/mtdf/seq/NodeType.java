public abstract class NodeType {
    short score = 0;
    long signature = 0;

    abstract NodeType[] generateChildren(); // return null for leaf node
    abstract void evaluate();
    abstract void print();
}

/* $Id$ */

public abstract class NodeType implements java.io.Serializable, Comparable {
    short score = 0;

    transient short prescore;

    abstract Tag getTag();

    abstract NodeType[] generateChildren(); // return null for leaf node

    public int compareTo(Object o) {
        NodeType node = (NodeType) o;

        return prescore - node.prescore;
    }

    abstract void evaluate();

    abstract void print();
}

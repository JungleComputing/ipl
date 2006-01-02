abstract class Tag implements java.io.Serializable {
    abstract public boolean equals(Object o);

    abstract boolean equals(int[] array, int index);

    abstract void store(int[] array, int index);

    abstract public int hashCode();
}

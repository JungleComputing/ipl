// special-purpose heap sorting class
class Heap {
    private NodeType[] array;
    private int items, max;

    Heap(int max) {
        this.array = new NodeType[max];
        this.max = max;
        this.items = 0;
    }

    public void add(NodeType item) {
        // the array itself is not a heap
        array[items++] = item;
    }

    private void swap(int parent, int child) {
        NodeType temp = array[parent];
        array[parent] = array[child];
        array[child] = temp;
    }

    private void heapify(int parent) {
        int child = 2 * parent + 1;

        if (child >= items) return;

        if (child + 1 < items &&
            array[child].compareTo(array[child + 1]) < 0)
         child++;

        if (array[child].compareTo(array[parent]) > 0) {
            swap(parent, child);

            heapify(child);
        }
    }

    public NodeType[] get() {
        NodeType[] heap = new NodeType[items];

        for (int i = (items - 1) / 2; i >= 0; i--)
          heapify(i);

        for (int i = items - 1; i >= 1; i--) {
            swap(0, i);

            items--;

            heapify(0);
        }

        System.arraycopy(array, 0, heap, 0, heap.length);

        return heap;
    }

    public int size() {
        return items;
    }
}

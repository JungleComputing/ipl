class Job implements java.io.Serializable {
    int[] path = null;

    int length;

    Job(int length, int[] path) {
        this.path = new int[path.length];
        this.length = length;

        // Copy the already known path.
        System.arraycopy(path, 0, this.path, 0, path.length);
    }
}
public class Job implements java.io.Serializable {

    ProcArg proc;

    int[] part;

    String[] slaves;

    int ncpus;

    Job() {
        proc = new ProcArg();
        ncpus = 0;
    }

    Job(ProcArg proc, Parts part, String[] slaves, int ncpus) {

        this.proc = proc;
        this.part = new int[part.part.length];
        for (int i = 0; i < part.part.length; i++) {
            this.part[i] = part.part[i];
        }
        this.slaves = slaves;
        this.ncpus = ncpus;
    }

}
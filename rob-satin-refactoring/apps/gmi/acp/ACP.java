/* $Id$ */


import java.io.*;
import ibis.gmi.*;

class ACP {

    public final int MAX_RELATIONS = 50;

    int numVariables;

    int numValues;

    int numConnections;

    int numRelations;

    int numRelationPairs;

    int seed;

    int cpu;

    Matrix matrix;

    i_Matrix group;

    i_Data data;

    i_Work work;

    Relation[] relations;

    Constraints constraints;

    int removed;

    long checks;

    int revise = 0;

    int change_ops = 0;

    int nr_modif = 0;

    int[] list_change;

    int poz_change;

    public ACP(int numVariables, int numValues, int numConnections,
            int numRelations, int numRelationPairs, int seed, i_Data data,
            i_Work work, Matrix matrix, i_Matrix group) {

        this.numVariables = numVariables;
        this.numValues = numValues;
        this.numConnections = numConnections;
        this.numRelations = numRelations;
        this.numRelationPairs = numRelationPairs;
        this.seed = seed;
        this.data = data;
        this.work = work;
        this.matrix = matrix;
        this.group = group;

        cpu = Group.rank();

        poz_change = removed = 0;
        checks = 0;

        list_change = new int[numValues];

        // GenerateRelations.
        relations = new Relation[numRelations];

        OrcaRandom random = new OrcaRandom(seed);

        for (int h = 0; h < numRelations; h++) {
            relations[h] = new Relation(numValues, h, random, numRelationPairs,
                    numValues);
        }

        // GenerateConstraints
        constraints = new Constraints(numVariables, numConnections,
                numRelations, relations, new OrcaRandom(seed));
    }

    boolean sprevise(int i, int j, boolean[][] local, Relation rel)
            throws Exception {

        /* Test if there is a relation between var_i and var_j :

         for every legal value of var_i :
         for every legal value of var_j :
         test if var_j supports var_i

         if var_i is not supported 
         remove it
         something is modified
         else 
         there is a solution

         if !solution found
         panic
         else 
         return if something was modified

         */

        boolean modified, solution, support;

        revise++;

        modified = solution = false;

        for (int k = 0; k < numValues; k++) {

            if (local[i][k]) {

                support = false;

                for (int l = 0; l < numValues; l++) {

                    if (local[j][l]) {

                        checks++;

                        if (i < j) {
                            if (rel.test(k, l)) {
                                support = true;
                                break;
                            }
                        } else {
                            if (rel.test(l, k)) {
                                support = true;
                                break;
                            }
                        }
                    }
                }

                if (!support) {
                    removed++;
                    local[i][k] = false;
                    list_change[poz_change++] = k;
                    modified = true;
                } else {
                    solution = true;
                }

            }

        }

        if (!solution) {
            System.out.println("No solution for " + i + " " + j);
            throw new NoSolutionException();
        }

        /*
         if (modified) {
         System.out.println("Removed " + removed);

         for ( i = 0;i<numVariables;i++) {
         for ( j= 0;j<numValues;j++) {
         System.out.print((local[i][j] ? "1" : "0"));
         }
         System.out.println();
         }
         }
         */

        return modified;
    }

    void spac_loop() throws Exception {

        boolean local_change, i_change, exists_work, temp;
        boolean[] work_list;
        boolean[][] local;

        while (work.workFor(cpu)) {

            local_change = true;

            while (local_change) {

                local_change = false;
                work_list = work.getWork(cpu);
                local = matrix.getValue();

                for (int i = 0; i < numVariables; i++) {

                    if (work_list[i]) {

                        work.vote(i, false); // HACK : i might have become true again....
                        i_change = false;
                        poz_change = 0;

                        for (int j = 0; j < numVariables; j++) {
                            if (i != j) {
                                if (constraints.relation(i, j)) {
                                    temp = sprevise(i, j, local, constraints
                                            .get(i, j));
                                    i_change = i_change || temp;
                                    // be careful with lazy evaluation here !
                                }
                            }
                        }

                        if (i_change) {
                            local_change = true;
                            group.change(i, list_change, poz_change);
                            //System.out.println(cpu + " change " + poz_change);
                            change_ops++;
                            work.announce(i);
                        }
                    }
                }
            }

            work.ready(cpu);
            nr_modif++;
        }

        //		System.out.println(cpu + " Revise : " + revise);
        //		System.out.println(cpu + " Checks : " + checks);
        //		System.out.println(cpu + " Modif  : " + nr_modif);

    }

    void start() throws Exception {

        long start, end, time;

        if (cpu == 0) {
            System.out.println("" + this);
        }

        // RuntimeSystem.barrier();

        start = System.currentTimeMillis();

        try {
            spac_loop();
        } catch (NoSolutionException e) {
            System.out.println("No solution found!");
        }

        end = System.currentTimeMillis();

        data.put(cpu, removed, checks, (end - start), nr_modif, change_ops,
                revise);

        if (cpu == 0) {
            System.out.println(data.result());
        }
    }

    public String toString() {

        String temp = "ACP\n";

        temp += "Number of variables            " + numVariables + "\n";
        temp += "Number of values               " + numValues + "\n";
        temp += "Number of connections/variable " + numConnections + "\n";
        temp += "Number of relations            " + numRelations + "\n";
        temp += "Number of relation pairs       " + numRelationPairs + "\n";

        return temp;
    }

    public static void main(String args[]) {

        try {

            int numVariables;
            int numValues;
            int numConnections;
            int numRelations;
            int numRelationPairs;
            int seed;
            i_Data data = null;
            i_Work work = null;
            Matrix matrix = null;
            i_Matrix group = null;
            Input in;

            int cpus = Group.size();
            int cpu = Group.rank();

            if (args.length != 1) {
                System.out.println("Usage : ACP <inputfile>");
                System.exit(1);
            }

            in = new Input(args[0]);

            numVariables = in.readInt();
            in.readln();

            numValues = in.readInt();
            in.readln();

            numConnections = in.readInt();
            in.readln();

            numRelations = in.readInt();
            in.readln();

            numRelationPairs = in.readInt();
            in.readln();

            seed = in.readInt();

            if (cpu == 0) {

                Group.create("Matrix", i_Matrix.class, cpus);
                Group.create("Data", i_Data.class, 1);
                Group.create("Work", i_Work.class, 1);

                Data d = new Data(cpus);
                Group.join("Data", d);

                Work w = new Work(numVariables, cpus, numVariables, numValues,
                        numConnections, numRelations, numRelationPairs, seed);
                Group.join("Work", w);
            }

            GroupMethod m;

            matrix = new Matrix(numVariables, numValues, true);
            Group.join("Matrix", matrix);

            data = (i_Data) Group.lookup("Data");

            m = Group.findMethod(data,
                    "void put(int, int, long, long, int, int, int)");
            m.configure(new SingleInvocation(0), new DiscardReply());
            m = Group.findMethod(data, "java.lang.String result()");
            m.configure(new SingleInvocation(0), new ReturnReply(0));

            group = (i_Matrix) Group.lookup("Matrix");

            m = Group.findMethod(group, "void change(int, int[], int)");
            m.configure(new GroupInvocation(), new DiscardReply());

            work = (i_Work) Group.lookup("Work");

            m = Group.findMethod(work, "boolean[] getWork(int)");
            m.configure(new SingleInvocation(0), new ReturnReply(0));
            m = Group.findMethod(work, "boolean workFor(int)");
            m.configure(new SingleInvocation(0), new ReturnReply(0));
            m = Group.findMethod(work, "void vote(int, boolean)");
            m.configure(new SingleInvocation(0), new DiscardReply());
            m = Group.findMethod(work, "void announce(int)");
            m.configure(new SingleInvocation(0), new DiscardReply());
            m = Group.findMethod(work, "void ready(int)");
            m.configure(new SingleInvocation(0), new DiscardReply());

            new ACP(numVariables, numValues, numConnections, numRelations,
                    numRelationPairs, seed, data, work, matrix, group).start();
            Group.exit();

        } catch (Exception e) {
            System.out.println("OOPS: main got exception " + e);
            e.printStackTrace();
        }

    }
}
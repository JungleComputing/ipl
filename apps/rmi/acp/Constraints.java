class Constraints implements java.io.Serializable {

    Relation[][] cons;

    int dimension;

    Constraints(int variables, int connections, int numRelations,
            Relation[] relations, OrcaRandom random) {

        int var, n;

        cons = new Relation[variables][variables];
        dimension = variables;

        // Make sure that connections is even.
        if ((connections % 2) != 0) {
            connections--;
        }

        // Connect each variable to connections/2 others.
        // If the number of connections is low, just connect each variable to
        // the right amout of other variables. If the number of connections 
        // is high, connect each variable to all others anf then remove the 
        // right amount of connections.

        connections = connections / 2;

        if (connections < variables / 2) {
            // low number. Just pick connections at random. 
            for (int i = 0; i < variables; i++) {
                for (int j = 0; j < connections; j++) {

                    do {
                        var = random.val() % variables;
                    } while (cons[i][var] != null);

                    n = (random.val() % numRelations);

                    cons[i][var] = cons[var][i] = relations[n];
                }
            }
        } else {
            // high number. First connect all-to-all....
            for (int i = 0; i < variables; i++) {
                for (int j = 0; j < connections; j++) {
                    n = (random.val() % numRelations);
                    cons[i][j] = cons[j][i] = relations[n];
                }
            }

            // ... then remove at random.
            for (int i = 0; i < variables; i++) {
                for (int j = 0; j < connections; j++) {

                    do {
                        var = random.val() % variables;
                    } while (cons[i][var] == null);

                    cons[i][j] = cons[j][i] = null;
                }
            }
        }

    }

    void add(int var1, int var2, Relation rel) {
        cons[var1][var2] = cons[var2][var1] = rel;
    }

    Relation get(int var1, int var2) {
        return cons[var1][var2];
    }

    boolean relation(int var1, int var2) {
        return (cons[var1][var2] != null);
    }

    boolean test(int var1, int val1, int var2, int val2) {

        boolean result = false;

        Relation r = cons[var1][var2];

        if (r != null) {
            if (var1 < var2) {
                result = r.test(val1, val2);
            } else {
                result = r.test(val2, val1);
            }
        }

        //System.out.println(var1 + "-" + val1 + " " + var2 + "-" + val2 + " " + result); 

        return result;
    }

    public void print() {

        System.out.println("Constraints");

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (cons[i][j] == null) {
                    System.out.print("0");
                } else {
                    System.out.print("1"); //cons[i][j].number;
                }
            }
            System.out.println();
        }

    }

}


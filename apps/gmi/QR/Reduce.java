/* $Id$ */


import ibis.gmi.GroupMember;

class Reduce extends GroupMember implements i_Reduce {

    PivotElt solution;

    boolean empty = true;

    i_Reduce group;

    Reduce() {
        super();
    }

    public void init(i_Reduce group) {
        this.group = group;
    }

    private void do_reduce(PivotElt elt) {

        if (elt.cols < elt.max_cols) {
            if (solution.cols < solution.max_cols) {
                if (elt.norm > solution.norm) {
                    solution.max_over_max_cols = elt.norm;
                    solution.index = elt.index;
                    solution.cols = elt.cols;
                }
            } else {
                solution.max_over_max_cols = elt.norm;
                solution.index = elt.index;
                solution.cols = elt.cols;
            }
        } else if (solution.cols >= solution.max_cols) {
            solution.max_over_max_cols = 0.0;
            if (elt.norm > solution.norm) {
                solution.index = elt.index;
            }
        }

        if (elt.norm > solution.norm) {
            solution.norm = elt.norm;
        }
    }

    public synchronized void reduce_it(PivotElt elt) {
        //		System.out.println("Got a reduce ... ");
        solution = elt;
        empty = false;
        notifyAll();
    }

    public synchronized PivotElt reduce(PivotElt elt) {

        empty = true;

        //		System.out.println("Doing a reduce ...");
        group.reduce_it(elt);

        while (empty) {
            try {
                wait();
            } catch (Exception e) {
                System.err.println("reduce got exception " + e);
            }
        }
        return solution;
    }
}
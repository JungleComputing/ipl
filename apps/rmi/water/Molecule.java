/* $Id$ */


import java.io.Serializable;

public class Molecule implements ConstInterface, Serializable {

    double vm[];

    double f[][][];

    boolean done;

    Molecule() {
        vm = new double[NDIR];
        f = new double[MAXODR][][];
        for (int i = 0; i < MAXODR; i++) {
            f[i] = new double[NDIR][];
            for (int j = 0; j < NDIR; j++) {
                f[i][j] = new double[NATOMS];
                for (int k = 0; k < NATOMS; k++) {
                    f[i][j][k] = 0.0;
                }
            }
        }
        done = true;
    }

    Molecule(Molecule mol) {
        vm = new double[NDIR];
        f = new double[MAXODR][][];

        for (int i = 0; i < NDIR; i++) {
            vm[i] = mol.getVm(i);
        }

        for (int i = 0; i < MAXODR; i++) {
            f[i] = new double[NDIR][];
            for (int j = 0; j < NDIR; j++) {
                f[i][j] = mol.getAtoms(i, j);
            }

        }
        done = mol.done;
    }

    public void setF(int order, int dir, int atom, double value) {
        // System.out.println("setF: value = " + (float) value);
        f[order][dir][atom] = value;
    }

    public void setVm(int dir, double value) {
        vm[dir] = value;
    }

    public void setAtoms(int order, int dir, double value) {
        for (int i = 0; i < NATOMS; i++) {
            f[order][dir][i] = value;
        }
    }

    public void incF(int order, int dir, int atom, double value) {
        f[order][dir][atom] += value;
    }

    public void decF(int order, int dir, int atom, double value) {
        f[order][dir][atom] -= value;
    }

    public double getF(int order, int dir, int atom) {
        return f[order][dir][atom];
    }

    public double getVm(int dir) {
        return vm[dir];
    }

    public double[] getAtoms(int order, int dir) {
        return f[order][dir];
    }

    public synchronized void pauze() {
        while (!done) {
            try {
                wait();
            } catch (Exception e) {
                System.out.println(" Problem in testDone" + e.getMessage());
            }
        }
        done = false;
    }

    public synchronized void clearPauze() {
        done = true;
        try {
            notifyAll();
        } catch (Exception e) {
            System.out.println(" Problem in clearPauze" + e.getMessage());
        }
    }

}
/*
 * This class contains arrays for each of the interaction dimensions
 * (DISP, ACC, FORCES, orders).
 */
public class MoleculeEnsemble implements ConstInterface, java.io.Serializable {

    public double[][][][] f;

    public double[][] vm;

    MoleculeEnsemble(int nmols) {
        vm = new double[nmols][NDIR];
        f = new double[MAXODR][nmols][NDIR][NATOMS];
    }

    static double[][] deepCopy(double[][] in) {
        double[][] d = new double[in.length][];

        for (int i = 0; i < in.length; i++) {
            try {
                d[i] = (double[]) in[i].clone();
            } catch (Exception e) {
                if (false)
                    System.err.println("Wanna clone but this " + in[i]
                            + " won't hear of it");
                System.exit(37);
            }
        }

        return d;
    }

}
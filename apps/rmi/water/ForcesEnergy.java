
import java.lang.Math;

public class ForcesEnergy implements ConstInterface {

    public static final double FC11 = 0.512596;

    public static final double FC12 = -0.005823;

    public static final double FC13 = 0.016452;

    public static final double FC33 = 0.048098;

    public static final double FC111 = -0.57191;

    public static final double FC333 = -0.007636;

    public static final double FC112 = -0.001867;

    public static final double FC113 = -0.002047;

    public static final double FC123 = -0.03083;

    public static final double FC133 = -0.0094245;

    public static final double FC1111 = 0.8431;

    public static final double FC3333 = -0.00193;

    public static final double FC1112 = -0.0030;

    public static final double FC1122 = 0.0036;

    public static final double FC1113 = -0.012;

    public static final double FC1123 = 0.0060;

    public static final double FC1133 = -0.0048;

    public static final double FC1233 = 0.0211;

    public static final double FC1333 = 0.006263;

    public static final double QQ = 0.07152158;

    public static final double QQ2 = 2.00 * QQ;

    public static final double QQ4 = 2.00 * QQ2;

    public static final double A1 = 455.313100;

    public static final double B1 = 5.15271070;

    public static final double A2 = 0.27879839;

    public static final double B2 = 2.76084370;

    public static final double A3 = 0.60895706;

    public static final double B3 = 2.96189550;

    public static final double A4 = 0.11447336;

    public static final double B4 = 2.23326410;

    public static final double CM = 0.45682590;

    public static final double AB1 = A1 * B1;

    public static final double AB2 = A2 * B2;

    public static final double AB3 = A3 * B3;

    public static final double AB4 = A4 * B4;

    public static final double C1 = 1.0D - CM;

    public static final double C2 = 0.50 * CM;

    int nmol;

    double boxh, boxl, cut2, ref1, fhm;

    double fom, cutoff, omas, hmas;

    Barrier bar;

    ForcesEnergy(int nmol, double boxh, double boxl, double cut2, double ref1,
            double fhm, double fom, double cutoff, double hmas, double omas) {

        this.nmol = nmol;
        this.boxh = boxh;
        this.boxl = boxl;
        this.cut2 = cut2;
        this.cutoff = cutoff;
        this.ref1 = ref1;
        this.fhm = fhm;
        this.fom = fom;
        this.hmas = hmas;
        this.omas = omas;
    }

    public void intraf(MoleculeEnsemble var, double[] vir, int nrmols) {
        /*
         .....this routine calculates the intra-molecular force/mass acting on
         each atom.
         FC11, FC12, FC13, AND FC33 are the quardratic force constants
         FC111, FC112, ....... ETC. are the cubic      force constants
         FC1111, FC1112 ...... ETC. are the quartic    force constants
         */

        double sum, r1, r2, cos, sin, tempp;
        double dt, dts, dr1, dr1s, dr2, dr2s, r1s, r2s;
        double f1, f2, f3, t1, t2;
        double[] vr1 = new double[4], vr2 = new double[4];
        double[] dr11 = new double[4], dr23 = new double[4];
        double[] dt1 = new double[4], dt3 = new double[4];
        double lvir;

        /* loop through the molecules */
        for (int mol = 0; mol < nrmols; mol++) {
            sum = 0.0;
            r1 = 0.0;
            r2 = 0.0;

            /* loop through the three directions */
            for (int dir = XDIR; dir <= ZDIR; dir++) {
                if (WaterMaster.USE_VM) {
                    var.vm[dir][mol] = C1
                            * var.f[DISP][mol][dir][O]
                            + C2
                            * (var.f[DISP][mol][dir][H1] + var.f[DISP][mol][dir][H2]);
                }
                tempp = vr1[dir] = var.f[DISP][mol][dir][O]
                        - var.f[DISP][mol][dir][H1];
                r1 += tempp * tempp;
                tempp = vr2[dir] = var.f[DISP][mol][dir][O]
                        - var.f[DISP][mol][dir][H2];
                r2 += tempp * tempp;
                sum += vr1[dir] * vr2[dir];
            }

            r1 = Math.sqrt(r1);
            r2 = Math.sqrt(r2);

            /* calculate cos(THETA), sin(THETA), delta(r1),
             delta(r2), and delta(THETA) */
            cos = sum / (r1 * r2);
            sin = Math.sqrt(1.0d - cos * cos);
            dt = (Math.acos(cos) - ANGLE) * ROH;
            dts = dt * dt;
            dr1 = r1 - ROH;
            dr1s = dr1 * dr1;
            dr2 = r2 - ROH;
            dr2s = dr2 * dr2;

            if (WaterMaster.VERBOSE) {
                System.out.println("r1 = " + (float) r1 + ", r2 = "
                        + (float) r2);
                System.out.println("cos = " + (float) cos + ", acos(cos) = "
                        + (float) Math.acos(cos) + ", sin = " + (float) sin);
                System.out.println("acos(cos) - ANGLE = "
                        + ((float) Math.acos(cos) - ANGLE)
                        + ", (Math.acos(cos) - ANGLE) * ROH = "
                        + (float) (Math.acos(cos) - ANGLE) * ROH);
                System.out.println("dt = " + (float) dt + ", dts = "
                        + (float) dts);
                System.out.println("dr1 = " + (float) dr1 + ", dr1s = "
                        + (float) dr1s);
                System.out.println("dr2 = " + (float) dr2 + ", dr2s = "
                        + (float) dr2s);
            }

            /* calculate derivatives of r1/X1, r2/X3, THETA/X1, and THETA/X3 */
            r1s = ROH / (r1 * sin);
            r2s = ROH / (r2 * sin);

            if (WaterMaster.VERBOSE) {
                System.out.println("r1s = " + (float) r1s + ", r2s = "
                        + (float) r2s);
            }

            for (int dir = XDIR; dir <= ZDIR; dir++) {
                dr11[dir] = vr1[dir] / r1;
                dr23[dir] = vr2[dir] / r2;
                dt1[dir] = (-dr23[dir] + dr11[dir] * cos) * r1s;
                dt3[dir] = (-dr11[dir] + dr23[dir] * cos) * r2s;

                if (WaterMaster.VERBOSE) {
                    System.out.println(dir + ": dr11 = " + (float) dr11[dir]
                            + ", dr23 = " + (float) dr23[dir] + ", dt1 = "
                            + (float) dt1[dir] + ", dt3 = " + (float) dt3[dir]);
                }

            }

            /* calculate forces */
            f1 = FC11 * dr1 + FC12 * dr2 + FC13 * dt;
            f2 = FC33 * dt + FC13 * (dr1 + dr2);
            f3 = FC11 * dr2 + FC12 * dr1 + FC13 * dt;
            f1 = f1
                    + (3.0 * FC111 * dr1s + FC112 * (2.0 * dr1 + dr2) * dr2
                            + 2.0 * FC113 * dr1 * dt + FC123 * dr2 * dt + FC133
                            * dts) * ROHI;
            f2 = f2
                    + (3.0 * FC333 * dts + FC113 * (dr1s + dr2s) + FC123 * dr1
                            * dr2 + 2.0 * FC133 * (dr1 + dr2) * dt) * ROHI;
            f3 = f3
                    + (3.0 * FC111 * dr2s + FC112 * (2.0 * dr2 + dr1) * dr1
                            + 2.0 * FC113 * dr2 * dt + FC123 * dr1 * dt + FC133
                            * dts) * ROHI;
            f1 = f1
                    + (4.0 * FC1111 * dr1s * dr1 + FC1112 * (3.0 * dr1s + dr2s)
                            * dr2 + 2.0 * FC1122 * dr1 * dr2s + 3.0 * FC1113
                            * dr1s * dt + FC1123 * (2.0 * dr1 + dr2) * dr2 * dt + (2.0
                            * FC1133 * dr1 + FC1233 * dr2 + FC1333 * dt)
                            * dts) * ROHI2;
            f2 = f2
                    + (4.0 * FC3333 * dts * dt + FC1113
                            * (dr1s * dr1 + dr2s * dr2) + FC1123 * (dr1 + dr2)
                            * dr1 * dr2 + 2.0 * FC1133 * (dr1s + dr2s) * dt
                            + 2.0 * FC1233 * dr1 * dr2 * dt + 3.0 * FC1333
                            * (dr1 + dr2) * dts) * ROHI2;
            f3 = f3
                    + (4.0 * FC1111 * dr2s * dr2 + FC1112 * (3.0 * dr2s + dr1s)
                            * dr1 + 2.0 * FC1122 * dr1s * dr2 + 3.0 * FC1113
                            * dr2s * dt + FC1123 * (2.0 * dr2 + dr1) * dr1 * dt + (2.0
                            * FC1133 * dr2 + FC1233 * dr1 + FC1333 * dt)
                            * dts) * ROHI2;

            if (WaterMaster.VERBOSE) {
                System.out.println("f1 = " + (float) f1 + ", f2 = "
                        + (float) f2 + ", f3 = " + (float) f3);
            }

            for (int dir = XDIR; dir <= ZDIR; dir++) {
                t1 = f1 * dr11[dir] + f2 * dt1[dir];
                var.f[FORCES][mol][dir][H1] = t1;
                t2 = f3 * dr23[dir] + f2 * dt3[dir];
                var.f[FORCES][mol][dir][H2] = t2;
                var.f[FORCES][mol][dir][O] = -(t1 + t2);

                if (WaterMaster.VERBOSE) {
                    System.out.println("t1 = " + (float) t1 + ", t2 = "
                            + (float) t2);
                }

            }
        }

        /* calculate summation of the product of the displacement and computed
         force for every molecule, direction, and atom */

        lvir = 0.0;
        for (int mol = 0; mol < nrmols; mol++)
            for (int dir = XDIR; dir <= ZDIR; dir++)
                for (int atom = 0; atom < NATOMS; atom++) {
                    lvir += var.f[DISP][mol][dir][atom]
                            * var.f[FORCES][mol][dir][atom];
                }
        vir[0] += lvir;

    }

    public int ubNmol(double[][][][] all, int cpu) {

        return all[cpu].length;
    }

    public void interf(MoleculeEnsemble var, double[][][][] allPos,
            double[][][][] allVal, int dest, double[] vir, int startMol,
            int nrmols, int nmol) { //int id mee?

        /* This routine gets called both from main() and from mdmain().
         When called from main(), it is used to estimate the initial
         accelerations by computing intermolecular forces.  When called
         from mdmain(), it is used to compute intermolecular forces.
         The parameter dest specifies whether results go into the
         accelerations or the forces. Uses routine UPDATE_FORCES in this
         file, and routine c_Shift in file c_Shift.U */
        /*
         .....this routine calculates inter-molecular interaction forces
         the distances are arranged in the order  M-M, M-H1, M-H3, H1-M,
         H3-M, H1-H3, H1-H1, H3-H1, H3-H3, O-O, O-H1, O-H3, H1-O, H3-O,
         where the M are "centers" of the molecules.
         */

        int mol, comp, dir, icomp, realMol;
        int compLast, halfMol;
        int kc, k, offset, ub, cpu;
        double[][] xl;
        double[] rs = new double[15], ff = new double[15], rl = new double[15];

        /* per-interaction arrays that hold some computed distances */
        double ftemp;
        double lvir = 0.0;

        cpu = 0;
        ub = 0;
        xl = new double[NDIR][];
        for (int i = 0; i < NDIR; i++) {
            xl[i] = new double[15];
        }

        halfMol = nmol / 2;
        for (mol = 0; mol < nrmols; mol++) {
            realMol = mol + startMol;
            compLast = realMol + halfMol;
            if (nmol % 2 == 0
                    && ((mol % 2 == 0 && (mol < halfMol)) || (mol % 2 == 1 && mol > halfMol))) {
                compLast -= 1;
            }
            //System.out.print(".");
            //System.out.flush();

            if (mol == (nrmols - 1)) {
                offset = 0;
                if (nmol != nrmols) {
                    cpu = 0;
                    ub = ubNmol(allPos, cpu);
                }
            } else {
                offset = mol + 1;
                cpu = -1;
                ub = nrmols;
            }

            if (WaterMaster.VERBOSE) {
                System.out.println("realMol = " + realMol);
                System.out.println("mol = " + mol);
                System.out.println("nmol = " + nmol);
                System.out.println("nrmols = " + nrmols);
                System.out.println("compLast = " + compLast);
                System.out.println("offset = " + offset);
            }

            for (icomp = realMol + 1; icomp <= compLast; icomp++) {
                comp = icomp;
                if (comp > (nmol - 1))
                    comp = comp % nmol; //?

                if (WaterMaster.VERBOSE) {
                    System.out.println("comp = " + comp);
                    System.out.println("cpu = " + cpu);
                    System.out.println("offset = " + offset);
                }

                /*  compute some intermolecular distances */
                if (cpu == -1)
                    computeDistances(var.f[DISP][mol], var.f[DISP][offset], xl,
                            boxh, boxl);
                else
                    computeDistances(var.f[DISP][mol], allPos[cpu][offset], xl,
                            boxh, boxl);

                kc = 0;

                for (k = 0; k < 9; k++) {
                    rs[k] = xl[0][k] * xl[0][k] + xl[1][k] * xl[1][k]
                            + xl[2][k] * xl[2][k];
                    if (rs[k] > cut2)
                        kc++;
                }
                if (kc != 9) {
                    for (k = 0; k < 14; k++)
                        ff[k] = 0.0;
                    if (rs[0] < cut2) {
                        ff[0] = QQ4 / (rs[0] * Math.sqrt(rs[0])) + (4 * ref1);
                        lvir = lvir + ff[0] * rs[0];
                    }
                    for (k = 1; k < 5; k++) {
                        if (rs[k] < cut2) {
                            ff[k] = -QQ2 / (rs[k] * Math.sqrt(rs[k]))
                                    - (2 * ref1);
                            lvir = lvir + ff[k] * rs[k];
                        }
                        if (rs[k + 4] <= cut2) {
                            rl[k + 4] = Math.sqrt(rs[k + 4]);
                            ff[k + 4] = QQ / (rs[k + 4] * rl[k + 4]) + ref1;
                            lvir = lvir + ff[k + 4] * rs[k + 4];
                        }
                    }
                    if (kc == 0) {
                        rs[9] = xl[0][9] * xl[0][9] + xl[1][9] * xl[1][9]
                                + xl[2][9] * xl[2][9];
                        rl[9] = Math.sqrt(rs[9]);
                        ff[9] = AB1 * Math.exp(-B1 * rl[9]) / rl[9];
                        lvir = lvir + ff[9] * rs[9];
                        for (k = 10; k < 14; k++) {
                            ftemp = AB2 * Math.exp(-B2 * rl[k - 5]) / rl[k - 5];
                            ff[k - 5] = ff[k - 5] + ftemp;
                            lvir = lvir + ftemp * rs[k - 5];
                            rs[k] = xl[0][k] * xl[0][k] + xl[1][k] * xl[1][k]
                                    + xl[2][k] * xl[2][k];
                            rl[k] = Math.sqrt(rs[k]);
                            ff[k] = (AB3 * Math.exp(-B3 * rl[k]) - AB4
                                    * Math.exp(-B4 * rl[k]))
                                    / rl[k];
                            lvir = lvir + ff[k] * rs[k];
                        }
                    }
                    if (cpu == -1)
                        updateForces(var.f[dest][mol], var.f[dest][offset], xl,
                                ff);
                    else
                        updateForces(var.f[dest][mol], allVal[cpu][offset], xl,
                                ff);
                }
                // System.out.println("offset = " + offset);
                // System.out.println("icomp = " + icomp);

                if (offset == (ub - 1)) {
                    //step to next processor
                    if (icomp < compLast) {
                        offset = 0;
                        if (nmol != nrmols) {
                            cpu += 1;
                            // System.out.println("cpu becomes " + cpu);
                            ub = ubNmol(allPos, cpu);
                        }
                    }
                } else {
                    offset += 1;
                }
            }
        }
        vir[0] = vir[0] + lvir;
    }

    public void multiplyForces(MoleculeEnsemble var, int dest, int nrmols) {

        for (int mol = 0; mol < nrmols; mol++) {
            for (int dir = XDIR; dir <= ZDIR; dir++) {
                var.f[dest][mol][dir][H1] = var.f[dest][mol][dir][H1] * fhm;
                var.f[dest][mol][dir][H2] = var.f[dest][mol][dir][H2] * fhm;
                var.f[dest][mol][dir][O] = var.f[dest][mol][dir][O] * fom;
            }
        }
    }

    public void computeDistances(double[][] myAtoms, double[][] farAtoms,
            double[][] xl, double boxh, double boxl) {

        /* compute some relevant distances between the two input molecules to
         this routine. if they are greater than the cutoff radius, compute
         these distances as if one of the particles were at its mirror image
         (periodic boundary conditions).
         used by the intermolecular interactions routines
         */

        double xma, xmb;

        for (int dir = XDIR; dir <= ZDIR; dir++) {
            xma = C1 * myAtoms[dir][O] + C2
                    * (myAtoms[dir][H1] + myAtoms[dir][H2]);
            xmb = C1 * farAtoms[dir][O] + C2
                    * (farAtoms[dir][H1] + farAtoms[dir][H2]);

            xl[dir][0] = xma - xmb;
            xl[dir][1] = xma - farAtoms[dir][0];
            xl[dir][2] = xma - farAtoms[dir][2];
            xl[dir][3] = myAtoms[dir][0] - xmb;
            xl[dir][4] = myAtoms[dir][2] - xmb;
            xl[dir][5] = myAtoms[dir][0] - farAtoms[dir][0];
            xl[dir][6] = myAtoms[dir][0] - farAtoms[dir][2];
            xl[dir][7] = myAtoms[dir][2] - farAtoms[dir][0];
            xl[dir][8] = myAtoms[dir][2] - farAtoms[dir][2];
            xl[dir][9] = myAtoms[dir][1] - farAtoms[dir][1];
            xl[dir][10] = myAtoms[dir][1] - farAtoms[dir][0];
            xl[dir][11] = myAtoms[dir][1] - farAtoms[dir][2];
            xl[dir][12] = myAtoms[dir][0] - farAtoms[dir][1];
            xl[dir][13] = myAtoms[dir][2] - farAtoms[dir][1];

            for (int i = 0; i < 14; i++) {
                /* if the value is greater than the cutoff radius */
                if (Math.abs(xl[dir][i]) > boxh) {
                    boxl = (xl[dir][i] < 0) ? ((boxl < 0) ? boxl : -boxl)
                            : ((boxl < 0) ? -boxl : boxl);
                    xl[dir][i] = xl[dir][i] - boxl;
                }
            }
        }
    }

    public void updateForces(double[][] myAtoms, double[][] farAtoms,
            double[][] xl, double ff[]) {
        /* from the computed distances etc., compute the
         intermolecular forces and update the force (or
         acceleration) locations */

        int k;
        double g110, g23, g45;
        double tt1, tt, tt2;
        double[] gg = new double[15];

        /*   CALCULATE X-COMPONENT FORCES */
        for (int i = XDIR; i <= ZDIR; i++) {
            for (k = 0; k < 14; k++) {
                gg[k + 1] = ff[k] * xl[i][k];
                if (WaterMaster.VERBOSE) {
                    System.out.println("gg[" + k + 1 + "] = "
                            + (float) gg[k + 1]);
                }
            }
            g110 = gg[10] + gg[1] * C1;
            g23 = gg[2] + gg[3];
            g45 = gg[4] + gg[5];
            tt1 = gg[1] * C2;
            tt = g23 * C2 + tt1;
            tt2 = g45 * C2 + tt1;

            /* lock locations for the molecule to be updated */
            myAtoms[i][O] += g110 + gg[11] + gg[12] + C1 * g23;
            myAtoms[i][H1] += gg[6] + gg[7] + gg[13] + tt + gg[4];
            myAtoms[i][H2] += gg[8] + gg[9] + gg[14] + tt + gg[5];

            farAtoms[i][O] -= g110 + gg[13] + gg[14] + C1 * g45;
            farAtoms[i][H1] -= gg[6] + gg[8] + gg[11] + tt2 + gg[2];
            farAtoms[i][H2] -= gg[7] + gg[9] + gg[12] + tt2 + gg[3];
        }
    }

    public void kineti(double[] sum, MoleculeEnsemble var, int nrmols) {
        double s;

        for (int dir = XDIR; dir <= ZDIR; dir++) {
            s = 0.0;
            for (int mol = 0; mol < nrmols; mol++) {
                s += (var.f[VEL][mol][dir][H1] * var.f[VEL][mol][dir][H1] + var.f[VEL][mol][dir][H2]
                        * var.f[VEL][mol][dir][H2])
                        * hmas
                        + (var.f[VEL][mol][dir][O] * var.f[VEL][mol][dir][O])
                        * omas;
            }
            sum[dir] += s;
        }
    }

    // this routine calculates the potential energy of the system.
    // FC11 ,FC12, FC13, and FC33 are the quardratic force constants
    public void poteng(double[][][][] allPos, double[] pota, double potr[],
            double ptrf[], MoleculeEnsemble var, int startMol, int nrmols) {

        int comp, compLast, realMol;
        int half_mol;
        int kc, k, offset, ub, cpu;
        double r1, r2, rx, cos, dt, dr1, dr2, dr1s, dr2s, drp;
        double[][] xl;
        double[] rs = new double[15], rl = new double[15];
        double dts;
        double lpota, lpotr, lptrf;
        double tempa, tempb, tempc;
        double ref2 = 2 * ref1;

        xl = new double[NDIR][];
        for (int i = 0; i < NDIR; i++) {
            xl[i] = new double[15];
        }
        /*  compute intra-molecular potential energy */
        lpota = 0.0;
        for (int mol = 0; mol < nrmols; mol++) {
            if (WaterMaster.USE_VM) {
                var.vm[XDIR][mol] = C1
                        * var.f[DISP][mol][XDIR][O]
                        + C2
                        * (var.f[DISP][mol][XDIR][H1] + var.f[DISP][mol][XDIR][H2]);
                var.vm[YDIR][mol] = C1
                        * var.f[DISP][mol][YDIR][O]
                        + C2
                        * (var.f[DISP][mol][YDIR][H1] + var.f[DISP][mol][YDIR][H2]);
                var.vm[ZDIR][mol] = C1
                        * var.f[DISP][mol][ZDIR][O]
                        + C2
                        * (var.f[DISP][mol][ZDIR][H1] + var.f[DISP][mol][ZDIR][H2]);
            }
            tempa = var.f[DISP][mol][XDIR][O] - var.f[DISP][mol][XDIR][H1];
            tempb = var.f[DISP][mol][YDIR][O] - var.f[DISP][mol][YDIR][H1];
            tempc = var.f[DISP][mol][ZDIR][O] - var.f[DISP][mol][ZDIR][H1];
            r1 = tempa * tempa + tempb * tempb + tempc * tempc;

            tempa = var.f[DISP][mol][XDIR][O] - var.f[DISP][mol][XDIR][H2];
            tempb = var.f[DISP][mol][YDIR][O] - var.f[DISP][mol][YDIR][H2];
            tempc = var.f[DISP][mol][ZDIR][O] - var.f[DISP][mol][ZDIR][H2];
            r2 = tempa * tempa + tempb * tempb + tempc * tempc;

            rx = ((var.f[DISP][mol][XDIR][O] - var.f[DISP][mol][XDIR][H1]) * (var.f[DISP][mol][XDIR][O] - var.f[DISP][mol][XDIR][H2]))
                    + ((var.f[DISP][mol][YDIR][O] - var.f[DISP][mol][YDIR][H1]) * (var.f[DISP][mol][YDIR][O] - var.f[DISP][mol][YDIR][H2]))
                    + ((var.f[DISP][mol][ZDIR][O] - var.f[DISP][mol][ZDIR][H1]) * (var.f[DISP][mol][ZDIR][O] - var.f[DISP][mol][ZDIR][H2]));

            r1 = Math.sqrt(r1);
            r2 = Math.sqrt(r2);
            cos = rx / (r1 * r2);
            dt = (Math.acos(cos) - ANGLE) * ROH;
            dr1 = r1 - ROH;
            dr2 = r2 - ROH;
            dr1s = dr1 * dr1;
            dr2s = dr2 * dr2;
            drp = dr1 + dr2;
            dts = dt * dt;
            lpota += (FC11 * (dr1s + dr2s) + FC33 * dts)
                    * 0.5
                    + FC12
                    * dr1
                    * dr2
                    + FC13
                    * drp
                    * dt
                    + (FC111 * (dr1s * dr1 + dr2s * dr2) + FC333 * dts * dt
                            + FC112 * drp * dr1 * dr2 + FC113 * (dr1s + dr2s)
                            * dt + FC123 * dr1 * dr2 * dt + FC133 * drp * dts)
                    * ROHI;
            lpota += (FC1111 * (dr1s * dr1s + dr2s * dr2s) + FC3333 * dts * dts
                    + FC1112 * (dr1s + dr2s) * dr1 * dr2 + FC1122 * dr1s * dr2s
                    + FC1113 * (dr1s * dr1 + dr2s * dr2) * dt + FC1123 * drp
                    * dr1 * dr2 * dt + FC1133 * (dr1s + dr2s) * dts + FC1233
                    * dr1 * dr2 * dts + FC1333 * drp * dts * dt)
                    * ROHI2;

            if (WaterMaster.VERBOSE) {
                System.out.println("Mol = " + mol);
                System.out.println("    rx = " + (float) rx);
                System.out.println("    r1 = " + (float) r1);
                System.out.println("    r2 = " + (float) r2);
                System.out.println("    cos = " + (float) cos);
                System.out.println("    dt = " + (float) dt);
                System.out.println("    dr1 = " + (float) dr1);
                System.out.println("    dr2 = " + (float) dr2);
                System.out.println("    dr1s = " + (float) dr1s);
                System.out.println("    dr2s = " + (float) dr2s);
                System.out.println("    drp = " + (float) drp);
                System.out.println("    lpota = " + (float) lpota);
            }
        }

        /*  compute inter-molecular potential energy */
        lpotr = 0.0;
        lptrf = 0.0;
        half_mol = nmol / 2;
        for (int mol = 0; mol < nrmols; mol++) {
            realMol = mol + startMol;
            compLast = realMol + half_mol;
            if (nmol % 2 == 0
                    && ((mol % 2 == 0 && (mol < half_mol)) || ((mol % 2 == 1) && mol > half_mol)))
                compLast -= 1;

            if (mol == (nrmols - 1)) {
                offset = 0;
                cpu = 0;
                ub = ubNmol(allPos, cpu);
            } else {
                offset = mol + 1;
                cpu = -1;
                ub = nrmols;
            }

            for (int icomp = realMol + 1; icomp <= compLast; icomp++) {
                comp = icomp;
                if (comp > (nmol - 1))
                    comp = comp % nmol; //is dit nog nodig?

                /*  compute some intermolecular distances */
                if (cpu == -1)
                    computeDistances(var.f[DISP][mol], var.f[DISP][offset], xl,
                            boxh, boxl);
                else
                    computeDistances(var.f[DISP][mol], allPos[cpu][offset], xl,
                            boxh, boxl);

                kc = 0;
                for (k = 0; k < 9; k++) {
                    rs[k] = xl[0][k] * xl[0][k] + xl[1][k] * xl[1][k]
                            + xl[2][k] * xl[2][k];
                    if (rs[k] > cut2)
                        kc = kc + 1;
                }
                if (kc != 9) {
                    for (k = 0; k < 9; k++) {
                        if (rs[k] <= cut2)
                            rl[k] = Math.sqrt(rs[k]);
                        else {
                            rl[k] = cutoff;
                            rs[k] = cut2;
                        }
                    }
                    lpotr += -QQ2 / rl[1] - QQ2 / rl[2] - QQ2 / rl[3] - QQ2
                            / rl[4] + QQ / rl[5] + QQ / rl[6] + QQ / rl[7] + QQ
                            / rl[8] + QQ4 / rl[0];
                    lptrf += -ref2
                            * rs[0]
                            - ref1
                            * ((rs[5] + rs[6] + rs[7] + rs[8]) * 0.5 - rs[1]
                                    - rs[2] - rs[3] - rs[4]);
                    if (kc <= 0) {
                        for (k = 9; k < 14; k++) {
                            rl[k] = Math.sqrt(xl[0][k] * xl[0][k] + xl[1][k]
                                    * xl[1][k] + xl[2][k] * xl[2][k]);
                        }

                        lpotr += A1
                                * Math.exp(-B1 * rl[9])
                                + A2
                                * (Math.exp(-B2 * rl[5])
                                        + Math.exp(-B2 * rl[6])
                                        + Math.exp(-B2 * rl[7]) + Math.exp(-B2
                                        * rl[8]))
                                + A3
                                * (Math.exp(-B3 * rl[10])
                                        + Math.exp(-B3 * rl[11])
                                        + Math.exp(-B3 * rl[12]) + Math.exp(-B3
                                        * rl[13]))
                                - A4
                                * (Math.exp(-B4 * rl[10])
                                        + Math.exp(-B4 * rl[11])
                                        + Math.exp(-B4 * rl[12]) + Math.exp(-B4
                                        * rl[13]));
                    }
                }
                if (offset == (ub - 1)) {
                    //step to next processor
                    if (icomp < compLast) {
                        offset = 0;
                        cpu += 1;
                        ub = ubNmol(allPos, cpu);
                    }
                } else {
                    offset += 1;
                }
            }

        }

        /* update shared sums from computed  private sums */
        pota[0] += lpota;
        potr[0] += lpotr;
        ptrf[0] += lptrf;
    }

}
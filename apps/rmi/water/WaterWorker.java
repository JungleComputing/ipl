/* $Id$ */


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import ibis.util.PoolInfo;

public class WaterWorker extends UnicastRemoteObject implements
        WaterWorkerInterface, ConstInterface {

    MoleculeEnsemble var;

    double[] tlc, pcc;

    int id, nmol, startMol, nrmols, ncpus, rest;

    double tstep, fpot, fkin, tkin, avgt, ten;

    int norder, nstep, nprint, nsave;

    double[] total;

    double[] potA, potR, potRF;

    double[] vir;

    double[][][][] allPositions;

    double[][][][] allVal;

    ForcesEnergy fe;

    PredCorr pe;

    WaterMasterInterface master;

    WaterWorkerInterface[] workers;

    String myName, masterName;

    WaterWorker(String myName, String masterName, int ncpus) throws Exception {

        int i = 0;
        boolean done = false;
        this.myName = myName;
        this.masterName = masterName;
        this.ncpus = ncpus;
        master = (WaterMasterInterface) RMI_init.lookup("//" + masterName
                + "/WaterMaster");

        id = master.logon(myName, this);

        //wait for others
        master.sync();

        System.out.println("I am worker " + id);

        workers = master.getWorkers();

        // workers[id] = this;

        //get my job
        Job job = master.getJob(id);

        this.var = job.var;

        this.startMol = job.startMol;
        this.id = id;
        this.pcc = job.pcc;
        this.tlc = job.tlc;
        this.nstep = job.nstep;
        this.tstep = job.tstep;
        this.norder = job.norder;
        this.nprint = job.nprint;
        this.nmol = job.nmol;
        this.fpot = job.fpot;
        this.fkin = job.fkin;
        this.nrmols = job.nrmols;
        this.nsave = job.nsave;
        this.master = master;
        this.rest = job.rest;

        vir = new double[1];
        fe = new ForcesEnergy(job.nmol, job.boxh, job.boxl, job.cut2, job.ref1,
                job.fhm, job.fom, job.cutoff, HMAS, OMAS);
        pe = new PredCorr(job.norder, job.boxh, job.boxl);

        avgt = 0.0;
        ten = 0.0;
        int quotient = nmol / ncpus;
        int cpu = id;

        allPositions = new double[nrBufs()][][][];
        allVal = new double[nrBufs()][][][];
        for (i = 0; i < nrBufs(); i++) {
            cpu++;
            if (cpu == ncpus)
                cpu = 0;
            if (cpu < rest) {
                allPositions[i] = new double[quotient + 1][NDIR][NATOMS];
                allVal[i] = new double[quotient + 1][NDIR][NATOMS];

            } else {
                allPositions[i] = new double[quotient][NDIR][NATOMS];
                allVal[i] = new double[quotient][NDIR][NATOMS];
            }
            for (int j = 0; j < allPositions.length; j++) {
                for (int k = 0; k < NDIR; k++) {
                    for (int l = 0; l < NATOMS; l++) {
                        allPositions[i][j][k][l] = 0.0;
                        allVal[i][j][k][l] = 0.0;
                    }
                }
            }
        }
    }

    public int ub(double[][][][] x, int index) {
        return x[index].length;
    }

    public synchronized void incAll(int dest, double[][][] all, int size)
            throws RemoteException {

        for (int mol = 0; mol < size; mol++) {
            for (int dir = XDIR; dir <= ZDIR; dir++) {
                for (int atom = H1; atom <= H2; atom++) {
                    var.f[dest][mol][dir][atom] += all[mol][dir][atom];
                }

            }
        }
    }

    public void incAllForceAcc(int dest, double[][][][] allVal)
            throws Exception {
        int cpu = id;

        for (int count = 0; count < nrBufs(); count++) {
            cpu++;
            if (cpu == ncpus)
                cpu = 0;
            workers[cpu].incAll(dest, allVal[count], ub(allVal, count));
        }
    }

    private void printVar() {
        for (int ordr = 0; ordr < MAXODR; ordr++) {
            System.out.println("    dest = " + ordr + ":");
            for (int j = 0; j < var.f[ordr].length; j++) {
                System.out.println("Molecule " + j + ":");
                for (int k = 0; k < var.f[ordr][j].length; k++) {
                    System.out.println("        dir = " + k + ":");
                    System.out.println("            "
                            + (float) var.f[ordr][j][k][0] + ",  "
                            + (float) var.f[ordr][j][k][1] + ", "
                            + (float) var.f[ordr][j][k][2]);
                }
            }
        }
    }

    private void sync(int no) throws Exception {
        // long start;

        // System.out.println("Starting sync " + no + ", time = " + currentTimeNanos());
        // start = System.currentTimeMillis();
        master.sync();
        // System.out.println("Done sync " + no + ": " + (System.currentTimeMillis() - start) + " ms.");
    }

    public void doInterf(int dest, double[] vir) throws Exception {

        int cpu = id;
        int quotient = 0;
        double tmp = 0.0;

        for (int i = 0; i < allPositions.length; i++) {
            for (int j = 0; j < allPositions[i].length; j++) {
                for (int k = 0; k < allPositions[i][j].length; k++) {
                    for (int l = 0; l < allPositions[i][j][k].length; l++) {
                        allPositions[i][j][k][l] = 0;
                        allVal[i][j][k][l] = 0;
                    }
                }
            }
        }
        getAllPositions();

        sync(0);

        // System.out.println("vars before fe.interf:");
        // printVar();

        fe.interf(var, allPositions, allVal, dest, vir, startMol, nrmols, nmol);

        // System.out.println("vars after fe.interf:");
        // printVar();
        sync(1);
        tmp = getAllVir();
        incAllForceAcc(dest, allVal);

        sync(2);
        vir[0] += tmp;
        fe.multiplyForces(var, dest, nrmols);
        // System.out.println("vars after fe.multiplyForces:");
        // printVar();
    }

    public double getPotA() throws RemoteException {
        return potA[0];
    }

    public double getPotR() throws RemoteException {
        return potR[0];
    }

    public double getPotRF() throws RemoteException {
        return potRF[0];
    }

    public double getTen() throws RemoteException {
        return ten;
    }

    public double getAvgt() throws RemoteException {
        return avgt;
    }

    public void doPoteng(double ttmv, double tvir, int iteration)
            throws Exception {
        double xtt = 0.0;
        double lpotA = 0.00;
        double lpotR = 0.00;
        double lpotRF = 0.00;
        double xvir = 0.00;
        double lavgt = 0.00;
        double lten = 0.00;
        int cpu = id;

        potA = new double[1];
        potR = new double[1];
        potRF = new double[1];
        potA[0] = 0.0;
        potR[0] = 0.0;
        potRF[0] = 0.0;

        for (int i = 0; i < allPositions.length; i++) {
            for (int j = 0; j < allPositions[i].length; j++) {
                for (int k = 0; k < allPositions[i][j].length; k++) {
                    for (int l = 0; l < allPositions[i][j][k].length; l++) {
                        allPositions[i][j][k][l] = 0;
                    }
                }
            }
        }
        getAllPositions();
        fe.poteng(allPositions, potA, potR, potRF, var, startMol, nrmols);
        sync(3);

        //get all potA en potR en poRF?
        double tA, tR, tRF;
        tA = potA[0];
        tR = potR[0];
        tRF = potRF[0];
        // System.out.println("tA = " + (float) tA);
        // System.out.println("tR = " + (float) tR);
        // System.out.println("tRF = " + (float) tRF);
        for (int i = 0; i < ncpus; i++) {
            if (i != id) {
                tA += workers[i].getPotA();
                tR += workers[i].getPotR();
                tRF += workers[i].getPotRF();
            }
        }
        // System.out.println("tA = " + (float) tA);
        // System.out.println("tR = " + (float) tR);
        // System.out.println("tRF = " + (float) tRF);
        /* modify computed sums */
        lpotA = tA * fpot;
        lpotR = tR * fpot;
        lpotRF = tRF * fpot;
        // System.out.println("lpotA = " + (float) lpotA);
        // System.out.println("lpotR = " + (float) lpotR);
        // System.out.println("lpotRF = " + (float) lpotRF);

        // System.out.println("tvir = " + (float) tvir);
        // System.out.println("fpot = " + (float) fpot);
        // System.out.println("ttmv = " + (float) ttmv);
        /* compute some values to print */
        xvir = tvir * fpot * 0.50 / ttmv;
        avgt = tkin * fkin * TEMP * 2.00 / (3.00 * ttmv);
        ten = (total[0] + total[1] + total[2]) * fkin;
        xtt = lpotA + lpotR + lpotRF + ten;
        sync(4);
        // System.out.println("xvir = " + (float) xvir);
        // System.out.println("avgt = " + (float) avgt);
        // System.out.println("ten = " + (float) ten);
        // System.out.println("xtt = " + (float) xtt);
        if (id == 0) {
            for (int i = 0; i < ncpus; i++) {
                if (i != id) {
                    lten += workers[i].getTen();
                    lavgt += workers[i].getAvgt();
                }
            }
        }
        // System.out.println("lten = " + (float) lten);
        // System.out.println("lavgt = " + (float) lavgt);
        sync(5);
        ten += lten;
        avgt += lavgt;
        xtt += lten;
        // System.out.println("ten = " + (float) ten);
        // System.out.println("avgt = " + (float) avgt);
        // System.out.println("xtt = " + (float) xtt);
        if (((iteration % nprint) == 0) && (id == 0)) {
            System.out.println();
            System.out.println(iteration + "  " + ten + "  " + lpotA + "  "
                    + lpotR);
            System.out.println(lpotRF + "   " + xtt + "   " + avgt + "   "
                    + xvir);
        }

    }

    public void mdmain(PoolInfo info) throws Exception {

        long start, end;
        long start1, end1;
        double tvir = 0.00;
        double ttmv = 0.00;
        double sum3 = 0.0;
        tkin = 0.00;

        total = new double[NDIR];
        /*.....START MOLECULAR DYNAMIC LOOP */

        if (nsave > 0) /* not true for input decks provided */
            System.out.println("COLLECTING X AND V DATA AT EVERY " + nsave
                    + " TIME STEPS");

        /* MOLECULAR DYNAMICS LOOP OVER ALL TIME-STEPS */

        start = System.currentTimeMillis();
        for (int i = 1; i <= nstep; i++) {
            start1 = System.currentTimeMillis();
            ttmv = ttmv + 1.0d;
            vir[0] = 0.0;
            for (int dir = XDIR; dir <= ZDIR; dir++) {
                total[dir] = 0.0;
            }

            // System.out.println("vars before pe.predic:");
            // printVar();
            pe.predic(tlc, var, nrmols);
            // System.out.println("vars before fe.intraf:");
            // printVar();
            fe.intraf(var, vir, nrmols);
            sync(6);
            doInterf(FORCES, vir);
            pe.correc(pcc, norder + 1, var, nrmols);
            // System.out.println("vars after pe.correc:");
            // printVar();
            pe.bndry(var, nrmols);
            // System.out.println("vars after pe.bndry:");
            // printVar();
            fe.kineti(total, var, nrmols);
            // System.out.println("vars after fe.kinety:");
            // printVar();
            sync(7);

            tkin = tkin + total[0] + total[1] + total[2];
            tvir = tvir - vir[0];

            // System.out.println("tkin = " + (float) tkin);
            // System.out.println("tvir = " + (float) tvir);
            /*  check if potential energy is to be computed, and if
             printing and/or saving is to be done, this time step.
             Note that potential energy is computed once every NPRINT
             time-steps */

            if (((i % nprint) == 0) || ((nsave > 0) && ((i % nsave) == 0))) {
                // printVar();
                doPoteng(ttmv, tvir, i);
            }
            sync(8);
            end1 = System.currentTimeMillis();
            if (id == 0)
                System.out
                        .println(" iteration took " + (end1 - start1) + " ms");

        }
        end = System.currentTimeMillis();
        if (info.rank() == 0) {
            info.printTime("Water_" + nmol, end - start);
        }
        if (id == 0) {
            System.out.println("Water " + (end - start) + " ms");
        }
        // System.exit(1);
    }

    public double[][][] getPositions() throws RemoteException {
        double[][][] all;

        all = new double[nrmols][NDIR][];

        for (int i = 0; i < nrmols; i++) {
            for (int j = 0; j < NDIR; j++) {
                all[i][j] = var.f[DISP][i][j];
            }
        }
        return all;
    }

    public int nrBufs() {
        return ((nmol / 2) + (nmol / ncpus - 1)) / (nmol / ncpus);
    }

    public void getAllPositions() throws Exception {
        int last, rest, cpu, count;

        last = nrBufs();
        rest = nmol % ncpus;
        count = 0;
        for (int i = id + 1; i <= id + last; i++) {
            cpu = i;
            if (cpu > (ncpus - 1))
                cpu -= ncpus;
            allPositions[count] = workers[cpu].getPositions();
            count += 1;
        }
    }

    public double getVir() throws RemoteException {
        return vir[0];
    }

    public double getAllVir() throws Exception {
        double temp = 0.0;

        for (int i = 0; i < ncpus; i++) {
            if (i != id) {
                temp += workers[i].getVir();

            }
        }
        return temp;
    }

    public int getId() throws Exception {
        return id;
    }

    public void start(PoolInfo info) throws Exception {
        double tvir, sum, sum4;
        double tmp = 0.0;

        sum = 0.0;
        master.sync1();
        vir[0] = 0.0;
        // System.out.println("vars before fe.intraf:");
        // printVar();
        System.out.println("fe = " + fe);
        fe.intraf(var, vir, nrmols);

        //master.sync();
        //tmp = getAllVir();
        //if(id == 0){
        //    vir[0] += tmp;
        //    System.out.println("---->" + vir[0]);
        //}
        //master.sync();
        vir[0] = 0.0;
        doInterf(ACC, vir);

        //if(id == 0)
        //    System.out.println("---->" + vir[0]);
        mdmain(info);
    }

}
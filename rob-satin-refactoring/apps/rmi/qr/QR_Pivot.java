/* $Id$ */


import java.util.Random;

import ibis.util.PoolInfo;

class QR_Pivot {

    static final int PIVOT_ELT_ELTS = 5;

    static final double FLT_EPSILON = 1.19209290E-07;

    static final double UNBALANCE_FAC = 0.02; /* Allow load imbalace 1.02 */

    static boolean pos_bound = false;

    static int col_surplus = 0;

    static int max_unbalance = -1;

    static int seed_offset = 0;

    static double bound = 2000.0;

    static double pivot_tolerance = 1.2;

    Random random;

    double[][] A;

    double[] rdiag;

    int m, n;

    int ncpus, cpu;

    int my_cols;

    QR_PIVOT_IX_T[] perm;

    i_Data data;

    i_Reduce reduce;

    BroadcastObject broadcast;

    PoolInfo info;

    QR_Pivot(PoolInfo info, int N, i_Data data, i_Reduce reduce,
            BroadcastObject broadcast) {

        double scale;
        double offset;

        this.info = info;
        this.data = data;
        this.reduce = reduce;
        this.broadcast = broadcast;

        m = n = N;

        cpu = info.rank();
        ncpus = info.size();

        my_cols = (n + ncpus - cpu - 1) / ncpus;

        //		System.out.println(cpu + " mycols = " + my_cols);

        if (bound != 0.0 && pos_bound) {
            bound = -bound;
        }

        perm = new QR_PIVOT_IX_T[min(m, n)];

        for (int i = 0; i < min(m, n); i++) {
            perm[i] = new QR_PIVOT_IX_T();
        }

        rdiag = new double[my_cols];
        A = new double[my_cols][m]; // Note: this is a packed matrix.

        /* v oid random_fill() { */

        if (bound == 0.0) {
            scale = 1.0;
            offset = 0.0;
        } else {
            scale = bound / (1.0 * Integer.MAX_VALUE);
            if (bound > 0.0) { /* Scale around zero */
                offset = -bound / 2.0;
            } else { /* Scale above zero */
                offset = 0;
            }
        }

        for (int j = 0; j < my_cols; j++) {

            // System.out.println(cpu + " translated col " + j + " to col " + col);

            random = new Random(j * ncpus + cpu + seed_offset);

            for (int i = 0; i < m; i++) {
                A[j][i] = Math.abs(random.nextInt()) * scale + offset;
            }
        }
    }

    int min(int m, int n) {
        return m < n ? m : n;
    }

    double eucl_norm2(int col, int from) {

        double sum = 0.0;

        for (int i = from; i < m; i++) {
            sum += A[col][i] * A[col][i];
        }

        return sum;
    }

    void householder_vector(int col, int iter, double[] v, double norm) {

        if (norm == 0.0) {
            return;
        }

        norm = 1.0 / norm;

        for (int i = iter; i < m; i++) {
            v[i] = A[col][i] * norm;
        }

        v[iter] += 1.0;
    }

    void factor_my_columns(int pivot, int first_col, int[] local_perm,
            int iter, double[] v) {

        int mycol;
        double tau;
        double old_norm = 0.0;
        double new_norm;

        if (pivot == cpu) {
            first_col++;
        }

        for (int j = first_col; j < my_cols; j++) {

            tau = 0.0;

            if (local_perm == null) {
                mycol = j;
            } else {
                mycol = local_perm[j];
            }

            for (int i = iter; i < m; i++) {
                tau += v[i] * A[mycol][i];
            }

            tau /= v[iter];

            for (int i = iter; i < m; i++) {
                A[mycol][i] -= tau * v[i];
            }
        }
    }

    void qrfac() throws Exception {

        int bla = 45;

        int mycol = 0;
        int col;
        int first_col;
        int pivot_owner;
        int swap;
        int unbalance = 2;

        int[] local_perm = new int[my_cols];
        ;

        double[] v = new double[m + 1];
        double[] subcol_norm2 = new double[my_cols];
        double[] safe_subcol_norm2 = new double[my_cols];

        PivotElt local_pivot = new PivotElt();
        PivotElt pivot = new PivotElt();

        int[] work_done = new int[ncpus];

        double ajnorm = 0.0;
        long time;

        /* Init stuff needed for pivoting */

        for (int i = 0; i < my_cols; i++) {
            subcol_norm2[i] = eucl_norm2(i, 0);
            safe_subcol_norm2[i] = subcol_norm2[i];
            local_perm[i] = i;
        }

        /* start the computation */

        col = 0;
        first_col = 0;

        //		RuntimeSystem.barrier();

        System.out.println(ncpus + " started.");

        time = System.currentTimeMillis();

        for (int iter = 0; iter < min(m, n); iter++) {

            /* Do pivoting */

            if (cpu == 0 && (iter % 10 == 0)) {
                System.out.println("Iter " + iter + " time so far = "
                        + (System.currentTimeMillis() - time));
            }

            if (iter > 0) {
                /* Update norms to 1 shorter columns */
                for (int i = first_col; i < my_cols; i++) {
                    // This code is an inline version of update_subcol_norm2 

                    /*		    update_subcol_norm2(&subcol_norm2[local_perm[i]],
                     &safe_subcol_norm2[local_perm[i]],
                     local_perm[i], iter, A, m);
                     static void
                     update_subcol_norm2(double *subcol_norm2, double *safe_subcol_norm2,
                     int col, int iter, double *A, int m)
                     */

                    double old_norm = subcol_norm2[local_perm[i]];
                    double calc_norm = eucl_norm2(local_perm[i], iter - 1);
                    int mytmpcol = local_perm[i];
                    double diag_iter2 = A[mytmpcol][iter - 1];
                    diag_iter2 *= diag_iter2;

                    subcol_norm2[local_perm[i]] -= diag_iter2;

                    if (subcol_norm2[local_perm[i]] < 400 * FLT_EPSILON
                            * safe_subcol_norm2[local_perm[i]]) {
                        // System.out.println("Bingo col " + local_perm[i]+ " iter " +iter);
                        subcol_norm2[local_perm[i]] = eucl_norm2(local_perm[i],
                                iter);
                        safe_subcol_norm2[local_perm[i]] = subcol_norm2[local_perm[i]];
                    }
                    calc_norm = eucl_norm2(local_perm[i], iter);

                    // End of inlined code
                }
            }

            /* First find local maximum norm */
            /* 0 entry works even if n % ncpus != 0 */
            local_pivot.index = -1;
            local_pivot.norm = 0.0;

            for (int i = first_col; i < my_cols; i++) {
                if (subcol_norm2[local_perm[i]] > local_pivot.norm) {
                    local_pivot.norm = subcol_norm2[local_perm[i]];
                    local_pivot.index = i;
                }
            }

            /* Translate to global column number */
            local_pivot.index = cpu + local_pivot.index * ncpus;
            local_pivot.cols = first_col;
            local_pivot.max_cols = iter / ncpus + 1 + unbalance;
            local_pivot.max_over_max_cols = local_pivot.norm;

            if (ncpus > 1) {
                pivot = reduce.reduce(local_pivot);
            } else {
                pivot = local_pivot;
            }

            if (pivot_tolerance * pivot.max_over_max_cols < pivot.norm) {
                if (cpu == 0) {
                    System.err.println("Need load balancing phase iter " + iter
                            + " norm " + pivot.norm + " max(cols)norm "
                            + pivot.max_over_max_cols);
                }
            }

            pivot_owner = pivot.index % ncpus;
            perm[iter].owner = pivot_owner;

            /*
             if (cpu == 0) { 
             work_done[pivot_owner]++;

             StringBuffer temp = new StringBuffer();

             temp.append(iter);

             for (int i=0;i<ncpus;i++) {
             temp.append(" ");					
             temp.append(work_done[i]);
             }

             System.err.print(temp + "\r");
             }
             */

            /* I own the pivot column */
            if (pivot_owner == cpu) {

                /* Calculate the Householder vector */
                col = pivot.index / ncpus;

                swap = local_perm[col];
                local_perm[col] = local_perm[first_col];
                local_perm[first_col] = swap;
                col = swap;
                perm[iter].col = col;

                if (A[col][iter] < 0.0) {
                    ajnorm = -Math.sqrt(subcol_norm2[col]);
                } else {
                    ajnorm = Math.sqrt(subcol_norm2[col]);
                }

                if (ajnorm == 0.0) {
                    System.err.println("Singular matrix : column " + iter
                            + "norm = 0");
                }

                householder_vector(col, iter, v, ajnorm);

                if (ncpus > 1) {
                    broadcast.send(iter, v, cpu);
                }
            } else {
                v = (double[]) broadcast.receive(iter);
            }

            /* Broadcast the Householder vector */

            // Should be broadcast of v[iter] till v[m-1], but this can't be done in CJ. So an additional copy is needed twice
            //			double tmp[] = null;
            //			if (me == pivot_owner) {
            //				tmp = new double[m-iter];
            //				for (int j = iter ; j < m ; j++)
            //					tmp[j-iter] = v[j];
            //			}
            //			tmp = (double[])broadcast(group, tmp, pivot_owner);
            //			if (me != pivot_owner)
            //				for (int j = iter; j < m ; j++)
            //					v[j] = tmp[j-iter];

            /* Use v to factor my columns */
            factor_my_columns(pivot_owner, first_col, local_perm, iter, v);

            if (pivot_owner == cpu) {
                /* Record v in subdiag column */
                for (int i = iter; i < m; i++) {
                    A[col][i] = v[i];
                }
                rdiag[col] = -ajnorm;
                first_col++;
            }
        }

        time = System.currentTimeMillis() - time;

        data.put(cpu, time);

        System.out.println(cpu + " done " + time);

        if (cpu != 0) {
            System.exit(0);
        }
    }
}

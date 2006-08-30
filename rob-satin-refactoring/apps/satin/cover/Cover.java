/* $Id$ */

public final class Cover extends ibis.satin.SatinObject implements
        CoverInterface, java.io.Serializable {
    static final int THRESHOLD = 15;

    int seq_do_try(int i, int N, int no_skills, byte[][] skills, int cover,
            byte[] covered, byte[] act, byte[] opt, int opt_elems) {
        int k, act_elems = 0;
        int opt_elems2;
        byte[] opt2;

        if (no_skills == cover) {
            for (k = 0; k < N; k++) {
                if (act[k] == 1)
                    act_elems++;
            }
            if (act_elems < opt_elems) {
                System.arraycopy(act, 0, opt, 0, N);
                opt_elems = act_elems;
            }
            return opt_elems;
        }

        if (i == N)
            return opt_elems;

        opt_elems2 = opt_elems;
        opt2 = (byte[]) opt.clone();

        // recursive call without the current element 
        opt_elems2 = seq_do_try(i + 1, N, no_skills, skills, cover, covered,
                act, opt2, opt_elems2);

        // recursive call with the current element 
        act[i] = 1;
        for (k = 0; k < no_skills; k++) {
            if (skills[i][k] == 1) {
                if (covered[k]++ == 0)
                    cover++;
            }
        }
        opt_elems = seq_do_try(i + 1, N, no_skills, skills, cover, covered,
                act, opt, opt_elems);

        // undo the setting of the current element
        act[i] = 0;
        for (k = 0; k < no_skills; k++)
            if (skills[i][k] == 1) {
                if (--covered[k] == 0) {
                    cover--;
                }
            }

        // take best result
        if (opt_elems2 < opt_elems) {
            opt_elems = opt_elems2;
            System.arraycopy(opt2, 0, opt, 0, N);
        }

        return opt_elems;
    }

    public Return spawn_try_it(int i, int N, int no_skills, byte[][] skills,
            int cover, byte[] covered, byte[] act, byte[] opt, int opt_elems) {
        return try_it(i, N, no_skills, skills, cover, covered, act, opt,
                opt_elems);
    }

    public Return try_it(int i, int N, int no_skills, byte[][] skills,
            int cover, byte[] covered, byte[] act, byte[] opt, int opt_elems) {
        int k, act_elems = 0;
        int opt_elems2;
        byte[] opt2;

        Return ret = new Return();
        ret.opt = opt;
        ret.opt_elems = opt_elems;

        if (no_skills == cover) {
            for (k = 0; k < N; k++) {
                if (act[k] == 1)
                    act_elems++;
            }
            if (act_elems < opt_elems) {
                //				System.arraycopy(act, 0, ret.opt, 0, N);
                ret.opt_elems = act_elems;
                ret.opt = (byte[]) act.clone();
            }
            return ret;
        }

        if (i == N)
            return ret;

        opt_elems2 = opt_elems;
        opt2 = (byte[]) opt.clone();

        /* recursive call without the current element */
        Return ret2;

        if (i >= THRESHOLD) {
            ret2 = new Return();
            ret2.opt_elems = seq_do_try(i + 1, N, no_skills, skills, cover,
                    covered, act, opt2, opt_elems2);
            ret2.opt = opt2;

            //			ret2 = try_it(i+1, N, no_skills, skills, cover, covered, act, opt2, opt_elems2);

            /* recursive call with the current element */
            /* must copy all OBJECTS we modify! */
            byte[] act_copy = (byte[]) act.clone();
            byte[] covered_copy = (byte[]) covered.clone();

            act_copy[i] = 1;
            for (k = 0; k < no_skills; k++) {
                if (skills[i][k] == 1) {
                    if (covered_copy[k]++ == 0) {
                        cover++;
                    }
                }
            }

            ret.opt_elems = seq_do_try(i + 1, N, no_skills, skills, cover,
                    covered_copy, act_copy, ret.opt, ret.opt_elems);

            if (ret2.opt_elems < ret.opt_elems) {
                return ret2;
            }

            return ret;
        }

        byte[] act_copy = (byte[]) act.clone();
        byte[] covered_copy = (byte[]) covered.clone();

        ret2 = spawn_try_it(i + 1, N, no_skills, skills, cover, covered, act,
                opt2, opt_elems2);

        /* recursive call with the current element */
        /* must use copies of all OBJECTS we modify! */

        act_copy[i] = 1;
        for (k = 0; k < no_skills; k++) {
            if (skills[i][k] == 1) {
                if (covered_copy[k]++ == 0) {
                    cover++;
                }
            }
        }

        ret = spawn_try_it(i + 1, N, no_skills, skills, cover, covered_copy,
                act_copy, ret.opt, ret.opt_elems);
        sync();

        if (ret2.opt_elems < ret.opt_elems) {
            return ret2;
        }

        return ret;
    }

    public static void main(String[] args) {
        int no_skills = 0, no_elems;
        int opt_elms;
        OrcaRandom rand = new OrcaRandom();
        Cover cover = new Cover();

        if (args.length == 0) {
            no_skills = 40;
        } else if (args.length == 1) {
            no_skills = Integer.parseInt(args[0]) * 2;
        } else {
            System.out.println("Usage: cover <size>");
            System.exit(1);
        }

        no_elems = no_skills / 2;

        byte[][] skills = new byte[no_elems][no_skills];

        for (int i = 0; i < no_elems; i++) {
            for (int j = 0; j < no_skills; j++) {
                byte c = (byte) ((rand.nextInt() % 1000) > 600 ? 1 : 0);
                skills[i][j] = c;
            }
        }

        byte[] act = new byte[no_skills];
        byte[] opt = new byte[no_skills];
        byte[] covered = new byte[no_skills];

        int opt_elems = no_elems + 1;

        /* output: opt and opt_elems */
        Return ret;
        System.out.println("cover (" + no_skills + " " + no_elems
                + ") started ");
        long start = System.currentTimeMillis();
        ret = cover.spawn_try_it(0, no_elems, no_skills, skills, 0, covered,
                act, opt, opt_elems);
        cover.sync();
        long end = System.currentTimeMillis();
        double time = (end - start) / 1000.0;

        System.out.println("application time cover (" + no_skills + ","
                + no_elems + ") took " + time + " s");
        System.out.print("application result cover (" + no_skills + ","
                + no_elems + ") = ");

        if (ret.opt_elems == no_elems + 1) {
            System.out.println("A cover does not exist!");
        }

        String result = "";
        for (int i = 0; i < no_elems; i++) {
            if (ret.opt[i] == 1) {
                result = result + "Element " + i;
                for (int j = 0; j < no_skills; j++) {
                    result = result + (skills[i][j] == 1 ? "* " : "_ ");
                }
            }
        }
        System.out.println(result);

        if (args.length == 0) {
            if (!result
                    .equals("Element 0* _ * * * _ _ _ * _ * _ _ * * _ _ * _ _ * * * _ _ _ _ _ _ _ _ _ * * _ _ _ _ * _ Element 2_ * _ _ _ _ * _ _ _ _ _ _ * _ _ _ _ _ * _ * _ * _ _ _ _ * * _ * _ _ _ * * _ _ * Element 5_ * _ _ _ _ _ * _ * _ * * _ _ * * * _ _ _ _ * * _ * * _ _ * * _ * * * _ _ * _ _ Element 11_ * * * * * _ _ * _ * _ * * _ * _ * * * * * _ _ * _ _ * * _ _ _ _ _ * * _ * _ _ ")) {
                System.out.println("Test failed!");
                System.exit(1);
            } else {
                System.out.println("Test succeeded!");
            }
        }
    }
}
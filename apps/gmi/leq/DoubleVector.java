/* $Id$ */


import ibis.gmi.GroupMember;

class DoubleVector extends GroupMember implements LEQGroup {

    double[] update;

    double residue;

    Update upd;

    boolean stop = false;

    int size;

    int cpus;

    LEQGroup g;

    int set = 0;

    public DoubleVector(int cpus, int size) {
        super();
        //data = new double[size];
        this.size = size;
        this.cpus = cpus;
        upd = new Update();
        upd.update = new double[size];
        upd.stop = false;
    }

    public void groupInit() {
    }

    public void init(LEQGroup g) {
        this.g = g;
    }

    public double[] value() {
        //System.out.println("valsue returns " + upd.update);
        return upd.update;

        /*
         while (data == null) { 
         try { 				
         wait();
         } catch (Exception e) { 
         // ignore
         }
         }
         double [] temp = data;
         data = null;		
         notifyAll();
         return temp;
         */
    }

    public double subscr(int i) {
        return upd.update[i];
    }

    public boolean done() {
        if (cpus > 1) {
            get(upd);
        }
        return upd.stop;
        /*

         while (data == null) { 
         try { 				
         wait();
         } catch (Exception e) { 
         // ignore
         }
         }
         return stop;
         */
    }

    public synchronized void get(Update d) {
        while (set == 0) {
            try {
                wait();
            } catch (Exception e) {
            }
        }
        set = 0;
        upd.update = update;
        upd.stop = stop;
        notifyAll();
    }

    public synchronized void set(double[] update, double residue) {
        //		System.out.println("set gets " + update + " " + residue);
        while (set != 0) {
            try {
                wait();
            } catch (Exception e) {
            }
        }
        set = 1;
        this.update = update;
        this.residue = residue;
        this.stop = (residue < Main.BOUND);
        notifyAll();
    }

    public void do_set(double[] update, double residue) {

        if (cpus > 1) {
            g.set(update, residue);
        } else {
            System.arraycopy(update, 0, upd.update, 0, size);
            upd.stop = (residue < Main.BOUND);
        }

        /*
         while (data != null) { 
         try { 				
         wait();
         } catch (Exception e) { 
         // ignore
         }
         }
         data = update;
         stop = (residue < Main.BOUND);
         notifyAll();
         */
    }
}


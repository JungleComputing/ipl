/* $Id$ */

/*
 * Created on 12.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the operation: logical and (MPJ.LAND).
 * Only available for these datatypes: MPJ.SHORT, MPJ.BOOLEAN, MPJ.INT and MPJ.LONG.
 */
public class OpLand extends Op {
    OpLand(boolean commute) throws MPJException {
        super(commute);
    }

    public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException {

        if (datatype == MPJ.SHORT) {
            if (((short[])invec).length != ((short[])inoutvec).length) {
                return;
            }
            for (int i=0; i< count; i++) {

                short o1 = ((short[])invec)[i+inoffset];
                short o2 = ((short[])inoutvec)[i+outoffset];

                if ((o1 != 0) && (o2 != 0)) {
                    ((short[])inoutvec)[i+outoffset] = 1;
                }
                else {
                    ((short[])inoutvec)[i+outoffset] = 0;
                }


            }
            return;
        }
        else if (datatype == MPJ.BOOLEAN) {
            if (((boolean[])invec).length != ((boolean[])inoutvec).length) {
                return;
            }
            for (int i=0; i< count; i++) {

                boolean o1 = ((boolean[])invec)[i+inoffset];
                boolean o2 = ((boolean[])inoutvec)[i+outoffset];

                ((boolean[])inoutvec)[i+outoffset] = o1 && o2;
            }
            return;
        }
        else if (datatype == MPJ.INT)  {
            if (((int[])invec).length != ((int[])inoutvec).length) {
                return;
            }
            for (int i=0; i< count; i++) {

                int o1 = ((int[])invec)[i+inoffset];
                int o2 = ((int[])inoutvec)[i+outoffset];

                if ((o1 != 0) && (o2 != 0)) {
                    ((int[])inoutvec)[i+outoffset] = 1;
                }
                else {
                    ((int[])inoutvec)[i+outoffset] = 0;
                }
            }
            return;
        }
        else if (datatype == MPJ.LONG) {
            if (((long[])invec).length != ((long[])inoutvec).length) {
                return;
            }
            for (int i=0; i< count; i++) {

                long o1 = ((long[])invec)[i+inoffset];
                long o2 = ((long[])inoutvec)[i+outoffset];

                if ((o1 != 0) && (o2 != 0)) {
                    ((long[])inoutvec)[i+outoffset] = 1;
                }
                else {
                    ((long[])inoutvec)[i+outoffset] = 0;
                }
            }
            return;
        }

        throw new MPJException("Operation does not support this Datatype");


    }
}

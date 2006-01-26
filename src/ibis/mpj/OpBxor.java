/* $Id$ */

/*
 * Created on 12.02.2005
 */
package ibis.mpj;

/**
 * Implementation of the operation: binary xor (MPJ.BXOR).
 * Only available for these datatypes: MPJ.BYTE, MPJ.SHORT, MPJ.BOOLEAN, MPJ.INT and MPJ.LONG.
 */
public class OpBxor extends Op {
    OpBxor(boolean commute) throws MPJException {
        super(commute);
    }

    public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException {
        if (datatype == MPJ.BYTE) {
            if (((byte[])invec).length != ((byte[])inoutvec).length) {
                return;
            }
            for (int i=0; i<count; i++) {

                byte o1 = ((byte[])invec)[i+inoffset];
                byte o2 = ((byte[])inoutvec)[i+outoffset];

                ((byte[])inoutvec)[i+outoffset] = (byte)(o1 ^ o2);
            }
            return;
        }
        else if (datatype == MPJ.SHORT) {
            if (((short[])invec).length != ((short[])inoutvec).length) {
                return;
            }
            for (int i=0; i< count; i++) {

                short o1 = ((short[])invec)[i+inoffset];
                short o2 = ((short[])inoutvec)[i+outoffset];

                ((short[])inoutvec)[i+outoffset] = (short)(o1 ^ o2);
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

                ((boolean[])inoutvec)[i+outoffset] = o1 ^ o2;
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

                ((int[])inoutvec)[i+outoffset] = o1 ^ o2;
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

                ((long[])inoutvec)[i+outoffset] = o1 ^ o2;
            }
            return;
        }

        throw new MPJException("Operation does not support this Datatype");

    }
}

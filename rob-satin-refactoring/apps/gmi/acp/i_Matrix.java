/* $Id$ */


import ibis.gmi.*;

interface i_Matrix extends GroupInterface {
    public void change(int x, int[] list_change, int poz_change);
}
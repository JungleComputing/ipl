import ibis.gmi.GroupInterface;

interface i_Reduce extends GroupInterface { 
    public void reduce_it(PivotElt elt);
}

import ibis.group.GroupInterface;

interface i_Reduce extends GroupInterface { 
	public void reduce_it(PivotElt elt);
}

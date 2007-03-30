/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;

import ibis.ipl.IbisIdentifier;

import java.util.Vector;

/**
 * Organisation of groups within each communicator.
 */
public class Group {
    protected Vector<IbisIdentifier> table;

    public Group() {
        this.table = new Vector<IbisIdentifier>();
    }

    protected void addId(IbisIdentifier id) {
        this.table.add(this.table.size(), id);		
    }

    protected IbisIdentifier getId(int rank) {
        return this.table.elementAt(rank);
    }


    /**
     * Size of group.
     * Result value is MPJ.UNDEFINED if the group is not initialized. 
     * @return number of processes in the group
     * @throws MPJException
     */
    public int size() throws MPJException {
        if (table != null) {
            return table.size();
        }
        return MPJ.UNDEFINED;
     }



    /**
     * Rank of this process in group.
     * Result value is MPJ.UNDEFINED if this process is not a member of the group.
     * @return rank of the calling process in the group
     * @throws MPJException
     */
    public int rank() throws MPJException {

        IbisIdentifier id = MPJ.getMyId();
        if (this.table != null) { 
            for (int i = 0; i < this.table.size(); i++) {
                IbisIdentifier a = this.table.elementAt(i); 
                if (a.equals(id)) {
                    return(i);
                }
            }
        }

        return(MPJ.UNDEFINED);
    }



    /**
     * Translate ranks within another group to ranks within this group.
     * Result elements are MPJ.UNDEFINED where no correspondence exists.
     * @param group1 another group
     * @param ranks1 array of valid ranks in group1
     * @return array of corresponding ranks in this group
     * @throws MPJException
     */
    public int[] translateRanks(Group group1, int[] ranks1) throws MPJException {
        if(ranks1 == null) {
            return(null);
        }

        int[] transRanks = new int[ranks1.length];

        if ((this == null) || (group1 == null)) {
            for (int i = 0; i < ranks1.length; i++) {
                transRanks[i] = MPJ.UNDEFINED;
            }
            return(transRanks);
        }

        for (int i = 0; i < ranks1.length; i++) {
            transRanks[i] = MPJ.UNDEFINED;
            if ((ranks1[i] < 0) || (ranks1[i] > this.table.size())) {
                transRanks[i] = MPJ.UNDEFINED;
            }
            else {
                IbisIdentifier a = group1.table.elementAt(ranks1[i]); 
                for (int j = 0; j < this.table.size(); j++) {
                    IbisIdentifier b = this.table.elementAt(j);
                    if (a.equals(b)) {

                        transRanks[i] = j;
                        break;
                    }
                }
            }
        }

        return(transRanks);
    }


    /**
     * Compare two groups.
     * MPJ.IDENT results if the group members and group order are exactly the same in both groups.
     * MPJ.SIMILAR results if the group members are the same, but the order is different.
     * MPJ.UNEQUAL results otherwise.
     * 
     * @param group1 the group to compare with
     * @return MPJ.IDENT, MPJ.SIMILAR or MPJ.UNEQUAL
     * @throws MPJException
     */
    public static int compare(Group group1, Group group2) throws MPJException {
        int result = MPJ.IDENT;

        if ((group1 == null) && (group2 == null)) {
            return(MPJ.IDENT);
        }
        else if ((group1 == null) || (group2 == null)  || (group1.size() != group2.size())) {
            return(MPJ.UNEQUAL);
        }
        else {

            for (int i = 0; i < group2.size(); i++) {
                IbisIdentifier a = group2.table.elementAt(i);
                IbisIdentifier b = group1.table.elementAt(i);
                if (!(a.equals(b))) {
                    result = MPJ.SIMILAR;
                }

                if (result == MPJ.SIMILAR) {
                    boolean test = false;

                    for (int j = 0; j < group1.size(); j++) {
                        IbisIdentifier c = group1.table.elementAt(j);
                        if (a.equals(c)) {
                            test = true;
                            break;
                        }		
                    }

                    if (!test) {
                        result = MPJ.UNEQUAL;
                        break;
                    }
                }
            }


        }
        return(result);
    }



    /**
     * Set union of two groups
     * @param group1 the group to set union with
     * @return union group
     * @throws MPJException
     */
    public static Group union(Group group1, Group group2) throws MPJException {
        if ((group1 == null) && (group1 == null)) {
            return (null);
        }
        else if (group1 == null) {
            Group uGroup = new Group();
            uGroup.table = new Vector<IbisIdentifier>(group2.table);

            return(uGroup);
        }
        else if (group2 == null) {
            Group uGroup = new Group();
            uGroup.table = new Vector<IbisIdentifier>(group1.table);

            return(uGroup);
        }
        else {
            Group uGroup = new Group();
            uGroup.table = new Vector<IbisIdentifier>(group1.table);

            for (int i = 0; i < group2.size(); i++) {
                boolean check = false;
                IbisIdentifier a = group2.table.elementAt(i);
                for (int j = 0; j < group1.size();  j++) {
                    IbisIdentifier b = group1.table.elementAt(j);

                    if (a.equals(b)) {
                        check = true;
                        break;
                    }
                }
                if (!check) {
                    uGroup.addId(a);
                }
            }
            return(uGroup);
        }


    }



    /**
     * Set intersection of two groups.
     * 
     * @param group1 the group  to set intersection with
     * @return intersection group
     * @throws MPJException
     */
    public static Group intersection(Group group1, Group group2) throws MPJException {
        if ((group1 == null) || (group2 == null)) {
            return (null);
        }
 
        Group iGroup = new Group();
        
        
        for (int i = 0; i < group2.size(); i++) {
        	boolean check = false;
        	IbisIdentifier a = group2.table.elementAt(i);
        	for (int j = 0; j < group1.size();  j++) {
        		IbisIdentifier b = group1.table.elementAt(j);
        		
        		if (a.equals(b)) {
        			check = true;
        			break;
        		}
        	}
        	if (check) {
        		iGroup.addId(a);
        	}
        }
        return(iGroup);
        

    }


    /**
     * Result contains all elements of this group that are not in group1.
     * 
     * @param group1 the group to set difference with 
     * @return difference group
     * @throws MPJException
     */
    public static Group difference(Group group1, Group group2) throws MPJException {
        if (group1 == null) {
            return(null);
        }
        else if (group2 == null) {
            Group dGroup = new Group();
            dGroup.table = new Vector<IbisIdentifier>(group1.table);
            return(dGroup);
        }
        else
        {
            Group dGroup = new Group();

            for (int i = 0; i < group1.size(); i++) {
                boolean check = false;
                IbisIdentifier a = group1.table.elementAt(i);

                for (int j = 0; j < group2.size();j ++) {
                    IbisIdentifier b = group2.table.elementAt(j);
                    if (a.equals(b)) {
                        check = true;
                        break;
                    }
                }

                if (!check) {
                    dGroup.addId(a);
                }

            }
            return (dGroup);	
        }


    }



    /**
     * Create a subset group including specified processes.
     * @param ranks ranks from this group appear in new group
     * @return new group
     * @throws MPJException
     */
    public Group incl(int[] ranks) throws MPJException {
        if (ranks == null) {
            return(null);			
        }
 
        Group iGroup = new Group();
        for (int i = 0; i < ranks.length; i++) {
        	if ((ranks[i] >= 0) && (ranks[i] < this.size())) {
        		IbisIdentifier a = this.table.elementAt(ranks[i]);
        		iGroup.addId(a);
        	}
        }
        return (iGroup);
        
    }
    


    /**
     * Create a subset group excluding specified processes.
     * 
     * @param ranks ranks from this group not to appear in new group
     * @return new group
     * @throws MPJException
     */
    public Group excl(int[] ranks) throws MPJException {
        Group eGroup = new Group();
        eGroup.table = new Vector<IbisIdentifier>(this.table);
    	if (ranks == null) {
            return(eGroup);			
    	}
    	for (int i = 0; i < ranks.length; i++) {
    		if ((ranks[i] >= 0) && (ranks[i] < this.size())) {
    			eGroup.table.removeElementAt(ranks[i]);
    		}
    	}
    	
    	if (eGroup.table.isEmpty()) {
    		return (null);
    	}
    	
    	return(eGroup);
    	
    	
    }



    /**
     * Create a subset group including processes specified by strided intervals of ranks.
     * The triplets are of the form (first rank, last rank, stride) indicating ranks in this group
     * to be included in the new group. The size of the first dimension of ranges is the number of
     * triplets. The size of the second dimension is 3.
     * 
     * @param ranks array of integer triplets
     * @return new group
     * @throws MPJException
     */
    public Group rangeIncl(int[][] ranks) throws MPJException {
        if ((ranks == null) || (ranks.length == 0)) {
            return(null);
        }
 
        Group iGroup = new Group();
        
        for (int i=0; i < ranks.length; i++) {
        	
        	
        	int j = ranks[i][0];
        	
        	if(ranks[i][2] > 0) {
        		
        		while(j <= ranks[i][1]) {
        			
        			if ((j >= 0) && (j < this.size())) {
        				iGroup.addId(this.table.elementAt(j));
        				
        			}
        			j += ranks[i][2]; 
        		}
        	}
        	else {
        		while(j >= ranks[i][1]) {
        			
        			if ((j >= 0) && (j < this.size())) {
        				iGroup.addId(this.table.elementAt(j));
        				
        			}
        			j += ranks[i][2]; 
        		}
        		
        	}
        }

        if (iGroup.table.isEmpty()) {
        	return(null);
        }
        
        return (iGroup);
        
    }
    




    /**
     * Create a subset group of excluding processes specified by strided intervals of ranks.
     * Triplet array is defined as for rangeIncl, the ranges indicating ranks in this group to be excluded
     * from the new group
     * @param ranks array of integer triplets
     * @return new group
     * @throws MPJException
     */
    public Group rangeExcl(int[][] ranks) throws MPJException {
        if ((ranks == null) || (ranks.length == 0)) {
            Group eGroup = new Group();
            eGroup.table = new Vector<IbisIdentifier>(this.table);
            return(eGroup);			
        }
 
        Group eGroup = new Group();
        Vector<Integer> exc = new Vector<Integer>();
        
        for (int i = 0; i < ranks.length; i++) {
        	
        	int min, max, step;
        	
        	if (ranks[i][2] < 0) {
        		min = ranks[i][1];
        		max = ranks[i][0]; 
        		step = Math.abs(ranks[i][2]);	
        	}
        	else {
        		min = ranks[i][0];
        		max = ranks[i][1]; 
        		step = ranks[i][2];	
        	}
        	
        	
        	int j = min;
        	
        	while (j <= max) {
        		exc.add(new Integer(j));
        		
        		j += step;
        	}
        	

            for (int k = 0; k < this.size(); k++) {
                boolean exclude = false;
                for (Integer in : exc) {
                    if (k == in.intValue()) {
                        exclude = true;
                        break;
                    }
                }

                if(!exclude) {
                    eGroup.addId(this.table.elementAt(k));
                }
            }
        }
        if (eGroup.table.isEmpty()) {
        	return(null);
        }
      	return (eGroup);
              
    }



    public void finalize() throws MPJException {
    	// nothing here
    }

}

public class SlaveSort {
    
    static final int INT_START = 0;
    static final int INT_END = 30;
    static final int MAX_RADIX = 4096;

    int myNum, ncpus, my_Nr_Of_Keys, myCpu;
    ProcArg proc;
    Tree tree;
    Parts sh_My_Keys;
    int[] key_To, key_From;
    int key_Start, key_Stop;
    String[] slaves;
    Stats stats;
    RadixMasterInterface master;
    TreeInterface[] trees;
    PartsInterface[] parts;
    
    SlaveSort(String myName, String masterName, int cpu)throws Exception{
    	
	    int sleep = 100;

	myCpu = cpu;
	master  = (RadixMasterInterface) RMI_init.lookup("//" + masterName + "/RadixMaster");
	myNum = master.logon(myName);	

	System.out.println("Got master, myNum = " + myNum);
	

	//wait for others
    	master.sync2();	

    	//get my job
    	Job job = master.get_Job(myNum);
	
	this.proc = new ProcArg(job.proc);
	this.ncpus = job.ncpus;
	this.slaves = job.slaves;

	sh_My_Keys = new Parts(job.part);
	my_Nr_Of_Keys = sh_My_Keys.part.length;
	key_From = sh_My_Keys.part;
	key_To = new int[sh_My_Keys.part.length];
	tree = new Tree(2*ncpus, proc.radix);
	stats = new Stats();
		
	//aanmelden bij master: mijn tree
	//en mijn part
	trees = master.getTrees((TreeInterface) tree, myNum);
	parts = master.getParts((PartsInterface) sh_My_Keys, myNum);
	trees[myNum] = tree;
	parts[myNum] = sh_My_Keys;
    }
    
    public void start()throws Exception{
    	int[] key_Density, rank_Me_Mynum, rank_Ff_Mynum;
	long start, end, timeStart, timeStop;
	int shift_Bits = proc.log2_Radix;    	
	
    	key_Density = new int[proc.radix];
    	rank_Me_Mynum = new int[proc.radix];
    	rank_Ff_Mynum = new int[proc.radix];

	if (ncpus > 1) master.sync();
	timeStart = System.currentTimeMillis();
	start = timeStart;
    	for(int shift = INT_START; shift < INT_END; shift +=shift_Bits){
	    if (ncpus > 1) master.sync(); 	
	    keys_Rank(rank_Me_Mynum, shift);
	    end = System.currentTimeMillis();
	    stats.histogramTime += end - start;
	    start = end;
	    key_Density[0] = 0;
	    for(int n = 1; n <proc.radix ; n++){
		key_Density[n] = key_Density[n-1] + rank_Me_Mynum[n - 1];
	    }
	    keys_Sort(key_Density, shift);
	    end = System.currentTimeMillis();
	    stats.sortTime += end - start;
	    start = end;
	    if (ncpus > 1) master.sync(); 
	    merge_Ranks(key_Density, rank_Me_Mynum, rank_Ff_Mynum, shift);
	    end = System.currentTimeMillis();
	    stats.mergeTime += end - start;
	    start = end;

	    if (ncpus > 1) master.sync(); 
	    move_Keys(rank_Me_Mynum, rank_Ff_Mynum, shift);
	    end = System.currentTimeMillis();
	    stats.permuteTime += end - start;
	    start = end;
	}
	timeStop = System.currentTimeMillis();
	stats.totalTime = timeStop - timeStart;
	master.putStats(myNum, stats);
	if (ncpus > 1) master.sync(); 
	check();
	master.sync2();
	master.sync2();
    }
   
    public void keys_Rank(int[] rank_Me_Mynum, int shift){

    	for(int k = 0; k < proc.radix; k++){
	    rank_Me_Mynum[k] = 0;
    	}

    	//determine frequency table
	for(int l = 0; l < my_Nr_Of_Keys; l++){
	    int x = ((key_From[l] >> shift) & (proc.radix - 1));
	    rank_Me_Mynum[x]++;
	}
    }
    
    public void keys_Sort(int[] density, int shift){
	int this_Key, tmp;

	for(int i = 0; i < my_Nr_Of_Keys; i++){
	    this_Key = ((key_From[i] >> shift) & (proc.radix - 1));
	    tmp = density[this_Key];
	    key_To[tmp] = key_From[i];
	    density[this_Key]++;
	}
    }
	
    public void merge_Ranks(int[] key_Density, int[] rank_Me_Mynum, int[] rank_Ff_Mynum, int m)throws Exception{
	//shared tree
	Prefix_Node myNode;

	//optellen
	tree_Reduce_Sum(key_Density, rank_Me_Mynum);
	if (ncpus > 1) master.sync(); 		
		
	//broadcast
	myNode = tree_Broadcast();
	tree_Scan_Sum(rank_Ff_Mynum);
	
	//compute absolute positions
	//pak de density op plek mynode
	//tel op bij rankff
	for(int i = 1; i < proc.radix; i++){
	    rank_Ff_Mynum[i] += myNode.densities[i-1];
	}
	
    }
    
    public void tree_Reduce_Sum(int[] key_Density, int[] rank_Me_Mynum)throws Exception{
	//shared tree
	int level, offset, base, index;
	Prefix_Node r, l, n;
	TreeInterface temp;

	n = tree.tree[myNum];

	//stop mijn rank en density in de tree
	tree.putSet(myNum, key_Density, rank_Me_Mynum);

	//update de tree bij een ieder
	for(int i = 0; i < ncpus; i++){
	    if (i != myNum){
		trees[i].putSet(myNum, key_Density, rank_Me_Mynum);
	    }
	}
	//build sum reduce tree
	if (ncpus > 1) master.sync();
	offset = myNum;
	level = ncpus >> 1;
	base = ncpus;
	
	if((myNum & 1) == 0){
	    tree.clearPauze(base + (offset >> 1));
	    for(int i = 0; i < ncpus ; i++){
		if (i != myNum){
		    trees[i].clearPauze(base + (offset >> 1));
		}
	    }
	}
	if (ncpus > 1) master.sync();
	index = myNum;
	while((offset & 1) != 0){
	    offset >>= 1;
	    r = n;
	    l = tree.tree[index - 1];
	    index = base + offset;
	    n = tree.tree[index];
	    tree.waitPauze(index);
	
	    if(offset != (level -1)){
		for(int i = 0; i < proc.radix; i++){
		    n.ranks[i] = r.ranks[i] + l.ranks[i];
		    n.densities[i] = r.densities[i] + l.densities[i];
		}
		for(int i = 0; i < ncpus; i++){
		    if(i != myNum){
			trees[i].putSet(index, n.densities, n.ranks);
		    }
				
		}
	    }else{
		for(int i = 0; i < proc.radix; i++){
		    n.densities[i] = r.densities[i] + l.densities[i];
		}
		for(int i = 0; i < ncpus; i++){
		    if(i != myNum){
			trees[i].putDensity(index, n.densities);
		    }		
		}
	    }
	    base += level;
	    level >>= 1;
	    if((offset & 1) ==0){
		tree.clearPauze(base + (offset >> 1));
	    }
	    for(int i = 0; i < ncpus; i++){
		if (i != myNum){
		    trees[i].clearPauze(base + (offset >> 1));
		}
	    }
	}
    }
    
    synchronized void t_Node_Put_Set(int place, int[] key_Density, int[] rank_Me_Mynum) throws Exception{
	tree.putSet(place, key_Density, rank_Me_Mynum);
    }
    
    synchronized void t_Node_Put_Dens(int place, int[] key_Density)throws Exception{
	tree.putDensity(place, key_Density);
    }	
    
    synchronized void signal_Done(int place)throws Exception{
	tree.clearPauze(place);
    }
    
    public Prefix_Node tree_Broadcast()throws Exception{
	Prefix_Node myNode, theirNode;
	int offset, level, base, my_Node_Index;
	TreeInterface temp;

	if(myNum != (ncpus - 1)){
	    offset = myNum;
	    level = ncpus;
	    base = 0;
	    while((offset & 1) != 0){
		offset >>= 1;
		base += level;
		level >>= 1;
	    }
	    my_Node_Index = base + offset;
	    myNode = tree.tree[base + offset];
		    
	    offset >>= 1;
	    base += level;
	    level >>=1;
	    while((offset & 1) != 0){
		offset >>= 1;
		base+= level;
		level >>=1;
	    }
	    theirNode = tree.tree[base + offset];
		    
	    tree.waitPauze(my_Node_Index);
		    
	    for(int i = 0; i < proc.radix; i++){
		myNode.densities[i] = theirNode.densities[i];
	    }
	    for(int i = 0; i < ncpus; i++){
		if(i != myNum){
		    trees[i].putDensity(my_Node_Index, myNode.densities);			}
	    }    	
	}else{
	    my_Node_Index = (2 * ncpus ) - 2;
	    myNode = tree.tree[my_Node_Index];
	}
		
	offset = myNum;
	level = ncpus;
	base = 0;
	while((offset & 1) != 0){
	    tree.clearPauze(base + (offset - 1));
	    for(int i = 0; i < ncpus; i++){
		if (i != myNum){
		    trees[i].clearPauze(base + (offset - 1));
		}
	    }
	    offset >>= 1;
	    base += level;
	    level >>= 1;
	}
	return myNode;
    }	
    
    
    public void tree_Scan_Sum(int[] rank_Ff_Mynum){
	int offset, level, base;
	Prefix_Node l;

	offset = myNum;
	level = ncpus;
	base = 0;
	for(int i = 0; i < proc.radix; i++){
	    rank_Ff_Mynum[i] = 0;
	}
	while(offset != 0){
	    if((offset & 1) != 0){
		//add ranks of node(s) to your left
		l = tree.tree[base + offset - 1];
		for(int i = 0; i < proc.radix; i++){
		    rank_Ff_Mynum[i] += l.ranks[i];
		}
	    }
	    base += level;
	    level >>= 1;
	    offset >>= 1;
	}
		
    }

    public void move_Keys(int[] rank_Me_Mynum, int[] rank_Ff_Mynum, int shift)throws Exception{
	int from_Start, from_End, cnt;
	int[] digit_Count, digit_Offset, procKeys;
	int maxProcKeys, k_Limit, digit, k2;

	int k = 0;
	digit_Count = new int[MAX_RADIX];
	digit_Offset = new int[MAX_RADIX];
	
	for(int j = 0; j <  MAX_RADIX; j++){
	    digit_Offset[j] = 0;
	    digit_Count[j] = 0;
	}
	
	k_Limit = k + my_Nr_Of_Keys;
	
	for(int procs = 0; procs < ncpus; procs++){
	    k2 = k;
	
	    while(k < k_Limit){
		digit = ((key_To[k] >> shift) & (proc.radix - 1));
		from_Start = rank_Ff_Mynum[digit];
				
		if(from_Start >= proc.key_Partition[procs+1]){
		    break;
		}
		
		digit_Count[digit] = rank_Me_Mynum[digit];
				
		from_End = from_Start + digit_Count[digit];
		if(from_End >= proc.key_Partition[procs+1]){
		    from_End = proc.key_Partition[procs+1];
		    digit_Count[digit] = from_End - from_Start;
		}
		
		from_Start -= proc.key_Partition[procs];
		digit_Offset[digit] = from_Start;
		
		rank_Me_Mynum[digit] -= digit_Count[digit];
		k += digit_Count[digit];
		rank_Ff_Mynum[digit] += digit_Count[digit];
	    }
	
	    if(k > k2){
		maxProcKeys = k - k2 + 2 * proc.radix + 1;
		procKeys = new int[maxProcKeys];
		cnt = 0;
		for(int i = 0; i < proc.radix; i++){
		    if(digit_Count[i] > 0){
			procKeys[cnt++] = digit_Count[i];
			procKeys[cnt++] = digit_Offset[i];
			for(int j = 0; j < digit_Count[i]; j++){
			    procKeys[cnt + j] = key_To[k2 + j];
			}
			//memcpy?
			cnt += digit_Count[i];
			k2 += digit_Count[i];
		    }
		}
		if(k2 == k){
		    //System.out.println(" k2 == k");
		}
		procKeys[cnt++] = 0;
		if(cnt <= maxProcKeys){
			    	//System.out.println(" cnt <= maxProcKeys");
		}
		keys_Move(procs, procKeys, cnt);
		for(int m = 0; m < proc.radix; m++){
		    digit_Count[m] = 0;
		}
	    }
	}
    }
    
    public void keys_Move(int procs, int[] procKeys, int cnt)throws Exception{
	int offset, cnt2;
	int l = 0;
	if (procs != myNum) {
	    parts[procs].put(procKeys);
	} else {
	    while((cnt2 = procKeys[l++]) > 0){
		offset = procKeys[l++];
		for(int j = 0; j < cnt2; j++){
		    sh_My_Keys.part[offset + j] = procKeys[l + j];
		}
		l+=cnt2;
	    }
	}
    }	
    
    public void check(){
	int amount = 0;
	
	amount = proc.key_Partition[myNum+1] - proc.key_Partition[myNum] ;
	for(int j = 0; j < amount - 1; j++){
	    if(sh_My_Keys.part[j] > sh_My_Keys.part[j+1]){
		System.out.println("keys not sorted, amount = " + amount + ", j = " + j);
		System.exit(1);
	    }
	}
    }
    
}



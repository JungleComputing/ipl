import java.lang.Math;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class WaterMaster extends UnicastRemoteObject 
    implements WaterMasterInterface, Runnable, ConstInterface {
	
    public static final boolean VERBOSE = false;
    public static final boolean USE_VM = false;

    int ncpus, workersAssigned;
    MoleculeEnsemble var; 
    double[] pcc, tlc;
    int nmol, nstep, norder, nprint, natmo, nsave;
    double tstep, boxh, boxl, ref1, cutoff, cut2;
    double fhm, fom, fkin, fpot;
    Barrier bar, bar1;
    Input in;
    int[] index;
    WaterWorkerInterface workers_i[];
	
    WaterMaster(int ncpus, String randomFile) throws Exception{
    	
        in = new Input(randomFile);

	this.ncpus = ncpus;
	workersAssigned = 0;
	pcc = new double[7];
	tlc = new double[100];
	workers_i = new WaterWorkerInterface[ncpus];
	bar = new Barrier(ncpus);
	bar1 = new Barrier(ncpus + 1);

	//bind master to registry
	//Must be after creating workers_i.
	RMI_init.bind("WaterMaster", this);

	readVariables();
	
	var = new MoleculeEnsemble(nmol);
	init();

	System.out.println("OK, boarded the data, now create the workers");

	partition();
    }

    public void partition(){
	int molsPerPart = nmol / ncpus;
	int rest = nmol % ncpus;
	int startMol = 0;
	int count;
	double sum = 0.0;

	index = new int[ncpus+1];
	count = rest;
	for(int i = 0; i < ncpus; i++){
	    index[i] = startMol;
	    if(rest == 0)
		startMol += molsPerPart;
	    else{
		startMol += molsPerPart + 1;
		rest -= 1;
	    }			
	}
	index[ncpus] = nmol;
    }

    public void init(){
	double sum = 0.0;

	computePccConstants(norder+1);
		
	System.out.println("TEMPERATURE                = "+ TEMP+" K");
	System.out.println("DENSITY                    = "+ RHO+" G/CC");
	System.out.println("NUMBER OF MOLECULES        = "+ nmol);
	System.out.println("TIME STEP                  = "+tstep+" SEC\n");
	System.out.println("ORDER USED TO SOLVE F=MA   = "+norder);
	System.out.println("NO. OF TIME STEPS          = "+nstep);
	System.out.println("FREQUENCY OF DATA SAVING   = "+nsave);
		
	setUpSystemConstants();
		
	System.out.println("SPHERICAL CUTOFF RADIUS    = "+cutoff+" ANGSTROM");
	
	/* initialization routine; also reads displacements and
	   sets up random velocities*/
	
	initia();
    }

    //
    // THIS ROUTINE INITIALIZES POSITIONS IN A CUBE AND RANDOMIZES 
    // VELOCITY OF EACH ATOM.
    // Always generate a regular lattice.
    public void initia() {

	double xMin = 0;
	double yMin = 0;
	double zMin = 0;
	double[] xMas = new double[4], xt = new double[4], yt = new double[4];
	double[] min;
	double z;
	double sux, suy, suz, sumx, sumy, sumz, fac;
	int mol = 0;
	int atom = 0;
	double val;
		
	double ns = Math.pow((double) nmol, 1.0/3.0) - 0.00001;
	double xs = boxl/ns;
	double zero = xs * 0.50;
	double wcos = ROH * Math.cos(ANGLE * 0.5);
	double wsin = ROH * Math.sin(ANGLE * 0.5);	
		
	xMas[1] = Math.sqrt(OMAS * HMAS); //in orca: omas
	xMas[0] = HMAS;
	xMas[2] = HMAS;
		
	/* .....ASSIGN POSITIONS */
	/* do not use input file for displacements; use a
	   regular lattice; */
	    
	System.out.println("***** NEW RUN STARTING FROM REGULAR LATTICE *****");
	System.out.println(ns+"  "+xs+"  "+wcos+"  "+wsin);
		
	xt[2] = zero;
	mol = 0;
	for(int i = 0; i < ns; i+=1) {
	    xt[1] = xt[2]+wcos;
	    xt[3] = xt[1];
	    yt[2] = zero;
	    for(int j = 0; j < ns; j+=1) {
		yt[1] = yt[2] + wsin;
		yt[3] = yt[2] - wsin;
		z = zero;
		for(int k = 0; k < ns; k++) {
		    for(atom = 0; atom < NATOMS; atom +=1) {
			var.f[DISP][mol][XDIR][atom] = xt[atom+1];
			var.f[DISP][mol][YDIR][atom] = yt[atom+1];
			var.f[DISP][mol][ZDIR][atom] = z;
		    }
		    mol += 1;
		    z=z+xs;
		}
		yt[2]=yt[2]+xs;
	    }
	    xt[2]=xt[2]+xs;
	}

	if(nmol != mol) {
	    System.out.println("Lattice init error: total mol "+mol+" != "+nmol);
	}
    
	/* ASSIGN RANDOM MOMENTA */
	System.out.println("** ASSIGN RANDOM MOMENTA **");
	sumx = 0.0;
	sumy = 0.0;
	sumz = 0.0;
		
	sux = in.readDouble();

	/*   read pseudo-random numbers from input file random.in */
	for(mol = 0; mol < nmol; mol++) {
	    for(atom = 0; atom < NATOMS; atom++) {
		var.f[VEL][mol][XDIR][atom] = in.readDouble();
		var.f[VEL][mol][YDIR][atom] = in.readDouble();
		var.f[VEL][mol][ZDIR][atom] = in.readDouble();

		sumx = sumx + var.f[VEL][mol][XDIR][atom];
		sumy = sumy + var.f[VEL][mol][YDIR][atom];
		sumz = sumz + var.f[VEL][mol][ZDIR][atom];
		// System.out.println("var[" + mol + "].getF(VEL, XDIR, " + atom + ") = " + (float) var[mol].getF(VEL, XDIR, atom));
		// System.out.println("var[" + mol + "].getF(VEL, YDIR, " + atom + ") = " + (float) var[mol].getF(VEL, YDIR, atom));
		// System.out.println("var[" + mol + "].getF(VEL, ZDIR, " + atom + ") = " + (float) var[mol].getF(VEL, ZDIR, atom));
	    } 
	}

	/* find average momenta per atom */
	sumx = sumx/(NATOMS * nmol);
	sumy = sumy/(NATOMS * nmol);
	sumz = sumz/(NATOMS * nmol);
		
	// System.out.println("sumx = " + (float) sumx);
	// System.out.println("sumy = " + (float) sumy);
	// System.out.println("sumz = " + (float) sumz);

	/*  find normalization factor so that <k.e.>=KT/2  */
	sux = 0.0;
	suy = 0.0;
	suz = 0.0;
	for(mol = 0; mol < nmol; mol++) {
	    sux = sux + (Math.pow((var.f[VEL][mol][XDIR][H1] - sumx),2.0)
			 + Math.pow((var.f[VEL][mol][XDIR][H2] - sumx),2.0))/HMAS
		+ Math.pow((var.f[VEL][mol][XDIR][O]  - sumx),2.0)/OMAS;
		
	    suy = suy + (Math.pow((var.f[VEL][mol][YDIR][H1] - sumy),2.0)
			 + Math.pow((var.f[VEL][mol][YDIR][H2] - sumy),2.0))/HMAS
		+ Math.pow((var.f[VEL][mol][YDIR][O]  - sumy),2.0)/OMAS;
		
	    suz = suz + (Math.pow((var.f[VEL][mol][ZDIR][H1] - sumz),2.0)
			 + Math.pow((var.f[VEL][mol][ZDIR][H2] - sumz),2.0))/HMAS
		+ Math.pow((var.f[VEL][mol][ZDIR][O] - sumz),2.0)/OMAS;
	}
	fac = BOLTZ * TEMP * natmo/UNITM * 
	    Math.pow((UNITT*tstep/UNITL),2.0);

	// System.out.println("fac = " + (float) fac);

	sux = Math.sqrt(fac/sux);
	suy = Math.sqrt(fac/suy);
	suz = Math.sqrt(fac/suz);

	// System.out.println("sux = " + (float) sux);
	// System.out.println("suy = " + (float) suy);
	// System.out.println("suz = " + (float) suz);
		
	/* normalize individual velocities so that there are no bulk
	   momenta  */
	xMas[1] = OMAS;
	for (mol = 0; mol < nmol; mol++) {
	    for (atom = 0; atom < NATOMS; atom++) {
		var.f[VEL][mol][XDIR][atom] = ( var.f[VEL][mol][XDIR][atom] -
						sumx) * sux/xMas[atom];
		var.f[VEL][mol][YDIR][atom] = ( var.f[VEL][mol][YDIR][atom] -
						sumy) * suy/xMas[atom];
		var.f[VEL][mol][ZDIR][atom] = ( var.f[VEL][mol][ZDIR][atom] -
						sumz) * suz/xMas[atom];
	    } 
	} 
    } 

    public void computePccConstants(int n){
	/*  calculate the coefficients of taylor series expansion */
	/*     for F(X), F"(X), F""(X), ...... (with DELTAT**n/n] included) */
	/*     in C(1,1),..... C(1,2),..... C(1,3),....... */
		
	int nn;
	double tn, tk;
		
	tlc[1] = 1.0D;
	for (int n1 = 2; n1 <= n; n1++) {  
	    nn = n1 - 1;
	    tn = (double)nn;
	    tlc[n1] = 1.0D;
	    tk = 1.0D;
	    for (int k1=2; k1<=n1; k1++) { 
		tlc[(k1-1)*n+nn] = tlc[(k1-2)* n + nn + 1] * tn/tk;
		nn = nn-1;
		tn = tn - 1.0D;
		tk = tk + 1.0D;
	    }
	}
		
	/* predictor-corrector constants for 2nd order differential equation */
	pcc[ACC] = 1.0D;
	int n1 = n-1;
	switch(n1) {
	case 1:
	case 2:
	    System.out.println("***** ERROR: THE ORDER HAS TO BE GREATER THAN 2 ****");
	    break;
	case 3:
	    pcc[DISP] = 1.0D/6.00;
	    pcc[VEL] = 5.0D/6.00;
	    pcc[DER_3] = 1.0D/3.00;
	    break;
	case 4:
	    pcc[DISP] = (double) 19.00/120.00;
	    pcc[VEL] = (double) 3.00/4.00;
	    pcc[DER_3] = 1.0D/2.00;
	    pcc[DER_4] = 1.0D/12.00;
	    break;
	case 5:
	    pcc[DISP] = (double) 3.00/20.00;
	    pcc[VEL] = (double) 251.00/360.00;
	    pcc[DER_3] = (double) 11.00/18.00;
	    pcc[DER_4] = 1.0D/6.00;
	    pcc[DER_5] = 1.0D/60.00;
	    break;
	case 6:
	    pcc[DISP] = (double) 863.00/6048.00;
	    pcc[VEL] = (double) 665.00/1008.00;
	    pcc[DER_3] = (double) 25.00/36.00;
	    pcc[DER_4] = (double) 35.00/144.00;
	    pcc[DER_5] = 1.0D/24.00;
	    pcc[DER_6] = 1.0D/360.00;
	    break;
	case 7:
	    pcc[DISP] = (double) 275.00/2016.00;
	    pcc[VEL] = (double) 19087.00/30240.00;
	    pcc[DER_3] = (double) 137.00/180.00;
	    pcc[DER_4] = 5.0D/16.00;
	    pcc[DER_5] = (double) 17.00/240.00;
	    pcc[DER_6] = 1.0D/120.00;
	    pcc[FORCES] = 1.0D/2520.00;
	    break;
	default:
	    break;
	}
    }           
	    
	
    public void setUpSystemConstants(){
										  	
	tstep = tstep / UNITT;        /* time between steps */
	natmo = NATOMS * nmol;        /* total number of atoms in system */

	fpot = UNITM * Math.pow((UNITL / UNITT),2.0) / (BOLTZ * TEMP * natmo);	      
	fkin = fpot * 0.50 / (tstep * tstep);
	boxl = Math.pow( (nmol*WTMOL*UNITM/RHO),(1.00/3.00));  /* computed length of the cubical "box".Note 
								  that box size is computed as being large 
								  enough to handle the input
								  number of water molecules */
		                
	boxl = boxl/UNITL;    /* normalized length of computational box */
	boxh = boxl*0.50; /* half the box length, used in 
			     computing cutoff radius */

	cutoff = Math.max(boxh,CUTOFFDEFAULT); /* sqrt of cutoff radius; max of boxh 
						  and default (= 0); i.e. CUTOFF
						  radius is set to half the normalized
						  box length */

	ref1 = -QQ / (cutoff*cutoff*cutoff);
	cut2 = cutoff*cutoff;       /* square of cutoff radius,  used 
				       to actually decide whether an 
				       interaction should be computed in 
				       INTERF and POTENG */

	fhm = (tstep * tstep * 0.50)/HMAS;
	fom = (tstep * tstep * 0.50)/OMAS;
    }   

    public synchronized int logon(String workerName, WaterWorkerInterface i)
      throws RemoteException {
	System.out.println("logon: " + workersAssigned);
	workers_i[workersAssigned] = i;
        workersAssigned++;
	
        return workersAssigned -1;
    }

    public WaterWorkerInterface[] getWorkers() throws RemoteException {
	return workers_i;
    }

    public void sync() throws RemoteException {
	bar.sync();
    }
    
    public void sync1() throws RemoteException {
	bar1.sync();
    }
    
    public synchronized Job getJob(int id) throws RemoteException {
	Job job;
	MoleculeEnsemble tmpVar;

	tmpVar = copy(index[id], index[id+1]);
	int rest = nmol % ncpus; 
	job = new Job(tmpVar, boxh, boxl, tlc, pcc, nmol, index[id], 
		      index[id+1] - index[id], tstep, cut2, ref1, fhm, fom, cutoff, fpot, fkin,
		      norder, nstep, nprint, nsave, rest);
	return job;
    }
	
    public void readVariables(){
			
	tstep = in.readDouble();
	nmol = in.readInt();
	nstep = in.readInt();
	norder = in.readInt();
	nsave = in.readInt();
	nprint = in.readInt();
			
	if(nmol > MAX_MOLS){
	    System.out.println("Error: Too many molecules");
	    System.exit(1);
	}
    }

    public MoleculeEnsemble copy(int begin, int end){
	MoleculeEnsemble tmp;
	double sum = 0;

	tmp = new MoleculeEnsemble(end - begin);
	for(int i = 0; i < end - begin; i++){
	    for (int ordr = 0; ordr < MAXODR; ordr++) {
		tmp.f[ordr][i] = MoleculeEnsemble.deepCopy(var.f[ordr][begin + i]);
	    }
	}
	
	return tmp;
    }
	
    public void run(){
	bar1.sync();
	System.out.println("Unbinding WaterMaster ...");
	workers_i = null;
	try {
	    RMI_init.unbind("WaterMaster");
	} catch (Exception e)
	{
	    System.out.println("Got exception");
	}
    }	
}

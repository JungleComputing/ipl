import java.io.*;

public class Job implements Serializable{

    MoleculeEnsemble var;
    double boxh, boxl;  
    double[] tlc, pcc; 
    int nmol, startMol, nrmols; 
    double tstep, cut2, ref1, fhm, fom, cutoff;
    double fpot, fkin;
    int norder, nstep, nprint, nsave, rest;
    Barrier bar, bar1;

    Job(MoleculeEnsemble var, double boxh, double boxl, double[] tlc, double[] pcc,
	int nmol, int startMol, int nrmols, double tstep, double cut2, double ref1, double fhm, 
	double fom, double cutoff, double fpot, double fkin, int norder, int nstep, int nprint, 
	int nsave, int rest){
	
	this.var = var;
	this.boxh = boxh;
	this.boxl = boxl;
	this.tlc = tlc;
	this.pcc = pcc;
	this.nmol = nmol;
	this.startMol = startMol;
	this.nrmols = nrmols;
	this.tstep = tstep;
	this.cut2 = cut2;
	this.ref1 = ref1;
	this.fhm = fhm;
	this.fom = fom;
	this.cutoff = cutoff;
	this.fpot = fpot;
	this.fkin = fkin;
	this.norder = norder;
	this.nstep = nstep;
	this.nprint = nprint;
	this.nsave = nsave;
	this.rest = rest;
	this.bar = bar;
	this.bar1 = bar1;
    }
    
}


public class PredCorr implements ConstInterface {

    int norder;
    double boxh, boxl;
	
    PredCorr(int norder, double boxh, double boxl){

	this.norder = norder;
	this.boxh = boxh;
	this.boxl = boxl;
    }
	

    // predicts new values for displacement and its five
    // derivatives 
    public void predic(double[] c, MoleculeEnsemble var, int nrmols) {
	/*   this routine calculates predicted F(X), F'(X), F''(X), ... */

	int jiz;
	int  ji;
	double s;

	jiz = 2;
	/* .....loop over F(X), F'(X), F''(X), ..... */
	for (int func = 0; func < norder; func++) {
	    for (int mol = 0; mol < nrmols; mol++)
		for (int dir = 0; dir < NDIR; dir++)
		    for (int atom = 0; atom < NATOMS; atom++ ) {
			ji = jiz;
			/* sum over Taylor Series */
			s = 0.0;
			for (int l = func; l < norder; l++) {
			    s += c[ji] * var.f[l+1][mol][dir][atom];
			    ji++;
			} 
			var.f[func][mol][dir][atom] += s;
		    } 
	    jiz += norder + 1;
	} 
    } 

    public void correc(double[] pcc, int nor1, MoleculeEnsemble var, int nrmols) {
	/* corrects the predicted values, based on forces
	   etc. computed in the interim */
	/* the predictor-corrector constants */
	/* NORDER + 1 = 7 for a sixth-order method) */
	
	/*
	  .....this routine calculates corrected F(X), F'(X), F"(X), ....
	  from corrected F(X) = predicted F(X) + PCC(1)*(FR-SD)
	  where SD is predicted accl. F"(X) and FR is computed 
	  accl. (force/mass) at predicted position
	*/
	
	double y;
	for (int mol = 0; mol < nrmols; mol++) {
	    for (int dir = 0; dir < NDIR; dir++) {
		for (int atom = 0; atom < NATOMS; atom++) {
		    y = var.f[FORCES][mol][dir][atom] - var.f[ACC][mol][dir][atom];
		    for (int func = 0; func < nor1; func++) 
			var.f[func][mol][dir][atom] += pcc[func] * y;   
		} 		
	    } 
	} 
    } 

    // this routine puts the molecules back inside the box if
    // they are  out
    public void bndry(MoleculeEnsemble var, int nrmols){

	for (int mol = 0; mol < nrmols; mol++) {
	    for (int dir = XDIR; dir <= ZDIR; dir++ ) {
		/* if the oxygen atom is out of the box */
		if (var.f[DISP][mol][dir][O] > boxl) {
		    /* move all three atoms back in the box */
		    var.f[DISP][mol][dir][H1] -= boxl;
		    var.f[DISP][mol][dir][O]  -= boxl;
		    var.f[DISP][mol][dir][H2] -= boxl;
		}
		else if (var.f[DISP][mol][dir][O] < 0.00) {
		    var.f[DISP][mol][dir][H1] += boxl;
		    var.f[DISP][mol][dir][O]  += boxl;
		    var.f[DISP][mol][dir][H2] += boxl;
		} 
	    } 
	}
    }

}

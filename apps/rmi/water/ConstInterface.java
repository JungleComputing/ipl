public interface ConstInterface{
	
    public static final int MAXODR = 8; //was 7
    public static final int NATOMS = 3;
    public static final int NDIR = 3;
    public static final int NVALS = NATOMS * NDIR;

    public static final int H1 = 0;
    public static final int O  = 1;
    public static final int H2 = 2;
    public static final int XDIR = 0;
    public static final int YDIR = 1;
    public static final int ZDIR = 2;
    public static final int DISP = 0;
    public static final int VEL = 1;
    public static final int ACC = 2;
    public static final int DER_3 = 3;
    public static final int DER_4 = 4;
    public static final int DER_5 = 5;
    public static final int DER_6 = 6;
    public static final int FORCES = 7;

    public static final double UNITT = 1.0e-15;
    public static final double UNITL = 1.0e-8;
    public static final double UNITM = 1.6605655e-24;
    public static final double BOLTZ = 1.380662e-16;
    
    public static final double TEMP = 298.0D;
    public static final double RHO = 0.9980D;

    public static final double OMAS = 15.99945; 
    public static final double HMAS = 1.007825;
    public static final double WTMOL = OMAS + 2.0D * HMAS;
    public static final double  QQ = 0.07152158;
    public static final double CUTOFFDEFAULT = 0.0D;
    public static final int MAX_MOLS = 4096;
    public static final double ROH = 0.9572;
    public static final double ROHI = 1.0D/ROH;
    public static final double ROHI2 = ROHI*ROHI;
    public static final double ANGLE = 1.824218;
    public static final double ZERO = 0.0D;
}

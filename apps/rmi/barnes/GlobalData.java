import java.io.Serializable;
import java.io.PrintWriter;
import java.io.FileOutputStream;

strictfp
public class GlobalData implements Cloneable, Serializable {

    final static boolean DEBUG = false;

  public Body gdBodies[];

  public double gdDoubleEpsilon = 0.0000001;
  public double gdStartTime  = 0.0;
  public double gdEndTime    = 10.0;
  public double gdDt         = 0.025;
  public double gdDtHalf     = gdDt/2;
  public double gdTheta      = 2.0;
  public double gdThetaSq    = gdTheta * gdTheta;
  public double gdBodyRange  = 30.0;
  public double gdSoft       = 0.0000025;
  public double gdSoftSQ     = 0.00000000000625;
  public double gdDimSlack   = 0.00001; 
  public int gdIterations    = (int)(((gdEndTime - gdStartTime)/ gdDt ) + 1.1);
  public int gdNumBodies     = 300;
  public int gdNumEssentialBodies;
  public int gdTotNumBodies  = 300;
  public int gdMaxBodies     = (int)((double)gdNumBodies * 1.2);
  public int gdMaxLocalBodies;
  public int gdMaxTempBodies;
  public int gdMaxTempCentersOfMass;
  public int gdMaxBodiesPerNode = 3;
  public double gdExtraBodySpaceFactor = 2;

  public int gdMedianBits    = 28;
  public int gdMedianShift   = 7;
  public int gdMedianRadix   = 128;

  public boolean gdSerialize   = false;
  public boolean gdTrimArrays  = false;
  public boolean gdThreads = false;
  public int gdGCInterval = 0;

  public int gdMyProc        = 1;
  public int gdNumProcs      = 1;
  public int gdMaxProcs      = 128;
  public int gdLogProcs      = 1;
  public int gdMaxLogProcs   = 7;
  public int gdFirstLoc      = 1;
  public double gdMaxLoad       = 0.01;

  public int gdIteration;
  public RandomNumber gdRand;
  public Plummer gdPlummer;

  public ProcessorImpl Proc;

  // Options

  public final boolean gdPrintTime = true;
  public final boolean gdPrintStats = true;
  public final boolean gdPrintBodies = false;

  public boolean gdComputeAccelerationsDirect = false;

  private transient PrintWriter Out;
  private transient FileOutputStream fOut;

  void Initialize() {

    Out = null;
    fOut = null;

    for (gdLogProcs=0; ((gdNumProcs-1)>>gdLogProcs)>0; gdLogProcs++);

    gdFirstLoc = (1<<gdLogProcs) - 1;
 
    gdMaxBodies = (int)((double)gdTotNumBodies*gdExtraBodySpaceFactor);
    gdMaxLocalBodies = (int)((double)(gdTotNumBodies/gdNumProcs)*gdExtraBodySpaceFactor); 

    gdMaxTempBodies = Math.max( gdTotNumBodies/10, 500 );
    gdMaxTempCentersOfMass = Math.max( gdTotNumBodies/10, 500 );
  }

  void InitializeBodies() {

    gdBodies = new Body[ gdMaxBodies ];

    // Scale ?

    double scale = Math.pow( gdTotNumBodies / 16384.0 , -0.25 );

    gdDt *= scale;
    gdEndTime *= scale;
    gdTheta *= 1.0/scale;

    debugStr("theta: " + gdTheta );

    gdDtHalf     = gdDt/2;
    gdThetaSq    = gdTheta * gdTheta;
    gdSoftSQ     = gdSoft * gdSoft;
    gdIterations = (int)(((gdEndTime - gdStartTime)/ gdDt ) + 1.1);

    debugStr( "scale: " + scale + "theta: " + gdTheta +
			  ", theta sq: " + gdThetaSq );

    debugStr("Parameters: " + gdTotNumBodies + " bodies, " +
			 gdDt + " dt, " + gdSoft + " eps, " + 
			 (2.0/gdTheta) + " tol, " + gdNumProcs + " procs, " + 
			 gdDtHalf + " dthalf, " +
			 gdMaxLocalBodies + " Max LocalBodies ");

  }

  public GlobalData GenerateClone() {
    
    try {
      return (GlobalData)clone();
    }
    catch( Exception e ) {
      return this;
    }
  }

  void OpenOutputFile( String OutputFile ) {
	  /*
    try {
      fOut[gdMyProc] = new FileOutputStream( OutputFile );
      Out[gdMyProc] = new PrintWriter( fOut[gdMyProc] );
    } catch (IOException e ) {
      System.out.println( "Ronald is stout: " + e );  
    }
	  */
  }


  void CloseOutputFile() {

	  /*    try {
      Out[gdMyProc].close();
      Out[gdMyProc] = null;
      fOut[gdMyProc].close();
      fOut[gdMyProc] = null;
    } catch (IOException e ) {
      System.out.println( "Ronald is stout: " + e );  
    }
	  */
  }


  void debugStr( String s ) {
	  /*    
    if (Out[gdMyProc]!=null) {
      Out[gdMyProc].println( s );
      Out[gdMyProc].flush(); 
    }
	  */
      if (DEBUG) {
	  System.out.println( gdMyProc + ": " + s );
      }
  }

  GlobalData() {

    gdRand = new RandomNumber();
    gdPlummer = new Plummer( gdRand );
  }


}

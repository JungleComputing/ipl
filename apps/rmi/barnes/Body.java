import java.lang.*;
import java.io.*;

strictfp public final class Body implements Serializable, Cloneable {

  public static int InstanceCount = 0;

  public int bNumber;
  public vec3 bPos;
  public vec3 bVel;
  public vec3 bAcc;
  public vec3 bOldAcc;
  public double bMass; 
  public int bWeight; 

  void Initialize() {

    bPos = new vec3();
    bVel = new vec3();
    bAcc = new vec3();
    bOldAcc = new vec3();
    bMass = 1.0;
    bNumber = 0;

    InstanceCount++;
  }


  Body() {

    Initialize();
  }

/*
  Body( double r ) {

    Initialize();

    bPos.x = (Math.random()-0.5)*r*2;
    bPos.y = (Math.random()-0.5)*r*2;
    bPos.z = (Math.random()-0.5)*r*2;

    bVel.x = (Math.random()-0.5)*r*2;
    bVel.y = (Math.random()-0.5)*r*2;
    bVel.z = (Math.random()-0.5)*r*2;
  }
*/
}

import java.lang.*;
import java.io.*;

public final class SendBody implements Serializable, Cloneable {

  public vec3 bPos;
  public double bMass; 

  SendBody() {
	bPos = new vec3();
  }
}

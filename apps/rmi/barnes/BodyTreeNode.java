import java.lang.*;

strictfp class BodyTreeNode {

  // Instance counter;

  public static int InstanceCount = 0;

  // Debugging stuff;

  public static boolean  btnDebug;

  private static final String depthStr[] = {

    "", 
    ".",
    "..",
    "...",
    "....",
    ".....",
    "......",
    ".......",
    "........",
    ".........",
    "..........",
    "...........",
    "............",
    ".............",
    "..............",
    "...............",
    "................",
    ".................",
    "..................",
    "...................",
    "....................",

  };

  // Variables

  protected GlobalData btnGd;
  protected double btnCenter_x;
  protected double btnCenter_y;
  protected double btnCenter_z;
  protected double	btnHalfSize;
  private int 	btnBodyIndex[];
  private int 	btnBodyCount;
  private int        btnChildrenCount;
  private BodyTreeNode btnChildren[];
  private double	btnTotalMass;
  public double		btnCenterOfMass_x;
  public double		btnCenterOfMass_y;
  public double		btnCenterOfMass_z;

  private int       btnDepth;
  private double btnMaxTheta;

  public boolean btnCenterOfMassValid;
  public boolean btnCenterOfMassReceived;

  protected boolean 	btnIsLeaf;

  // Constructors and support functions

  public Body getBody( int index ) {
    return btnGd.gdBodies[ btnBodyIndex[ index ] ];
  }
  public int getBodyIndex( int index ) {
    return btnBodyIndex[ index ];
  }

  public BodyTreeNode getChild( int index ) {
    return btnChildren[ index ];
  }


  public double getMaxTheta() {
    return btnMaxTheta;
  }

  public int getNumBodies() {
    return btnBodyCount;
  }

  public double getSize() {
    return btnHalfSize * 2;
  }

  public double getMass() {
    return btnTotalMass;
  }

  public boolean isLeaf() {
    return btnIsLeaf;
  }

  protected void Initialize( GlobalData g, double center_x, double center_y, double center_z, double HalfSize, int depth ) {

    btnGd        = g;
    btnCenter_x  = center_x;
    btnCenter_y  = center_y;
    btnCenter_z  = center_z;

    btnHalfSize  = HalfSize;
    btnBodyCount = 0;
    btnIsLeaf    = true;
    btnBodyIndex = new int[ btnGd.gdMaxBodiesPerNode ];
    btnChildren  = null; 
    btnCenterOfMass_x = 0;
    btnCenterOfMass_y = 0;
    btnCenterOfMass_z = 0;
    btnChildrenCount = 0; 
    btnDepth = depth;
    btnMaxTheta  = btnGd.gdThetaSq * HalfSize * HalfSize;

    btnCenterOfMassValid = false;
    btnCenterOfMassReceived = false;

    InstanceCount++;
  }


  public BodyTreeNode( GlobalData g ) {
    Initialize( g, 0, 0, 0, g.gdBodyRange, 0 );
  }


  public BodyTreeNode( GlobalData g, double center_x, double center_y, double center_z, double HalfSize, int depth ) {
    Initialize( g, center_x, center_y, center_z, HalfSize, depth );
  }


  int dumpTree( int level, int maxlevel ) {

    int i, bodies = btnBodyCount;

    if (!btnIsLeaf) {
      for (i=0;i<8;i++)
        if (btnChildren[i]!=null)
           bodies += btnChildren[i].dumpTree( level+1, maxlevel );
    }

    if (level<=maxlevel) {

	String str = "l" + level +
		", center (" + btnCenter_x + 
		"," + btnCenter_y + 
		"," + btnCenter_z + 
		"), size " + btnHalfSize +
		", cofm (" + btnCenterOfMass_x + 
		"," + btnCenterOfMass_y + 
		"," + btnCenterOfMass_z + 
		"), leaf " + (btnIsLeaf ? 1 : 0) + 
		", bodies " + bodies + ": ";

	if (btnIsLeaf)
	  for (int j=0; j<btnBodyCount; j++ )
	    str += btnGd.gdBodies[ btnBodyIndex[j] ].bNumber + " ";

	btnGd.debugStr( str );
   }

    return bodies;
  }



  // Tree management routines

  private void createChild( int i ) {

    double newSize	= btnHalfSize / 2.0;

    switch( i ) {

      case 7:
    	btnChildren[7] = new BodyTreeNode( btnGd, btnCenter_x + newSize,
	  btnCenter_y + newSize, btnCenter_z + newSize, newSize, btnDepth+1 );
	break;
      case 6:
        btnChildren[6] = new BodyTreeNode( btnGd, btnCenter_x - newSize,
	  btnCenter_y + newSize, btnCenter_z + newSize, newSize, btnDepth+1 );
        break;
      case 5:
        btnChildren[5] = new BodyTreeNode( btnGd, btnCenter_x + newSize,
	  btnCenter_y - newSize, btnCenter_z + newSize, newSize, btnDepth+1 );
        break;
      case 4: 
        btnChildren[4] = new BodyTreeNode( btnGd, btnCenter_x - newSize,
	  btnCenter_y - newSize, btnCenter_z + newSize, newSize, btnDepth+1 );
        break;
      case 3:
        btnChildren[3] = new BodyTreeNode( btnGd, btnCenter_x + newSize,
	  btnCenter_y + newSize, btnCenter_z - newSize, newSize, btnDepth+1 );
	break;
      case 2:
        btnChildren[2] = new BodyTreeNode( btnGd, btnCenter_x - newSize,
	  btnCenter_y + newSize, btnCenter_z - newSize, newSize, btnDepth+1 );
        break;
      case 1:
        btnChildren[1] = new BodyTreeNode( btnGd, btnCenter_x + newSize,
	  btnCenter_y - newSize, btnCenter_z - newSize, newSize, btnDepth+1 );
        break;
      case 0:
        btnChildren[0] = new BodyTreeNode( btnGd, btnCenter_x - newSize,
	  btnCenter_y - newSize, btnCenter_z - newSize, newSize, btnDepth+1 );
        break;
    }
    btnChildrenCount++;
  }


  private BodyTreeNode findContainingChild( int bodyIndex ) {

    int index = 0;
    double dx, dy, dz;
    
    dx = btnGd.gdBodies[ bodyIndex ].bPos.x - btnCenter_x;
    dy = btnGd.gdBodies[ bodyIndex ].bPos.y - btnCenter_y;
    dz = btnGd.gdBodies[ bodyIndex ].bPos.z - btnCenter_z;

    if (dx>=0) index += 1;
    if (dy>=0) index += 2;
    if (dz>=0) index += 4;

    if ( Math.abs(dx)>btnHalfSize || Math.abs(dy)>btnHalfSize || Math.abs(dz)>btnHalfSize ) {

      debugFatalStr( "Error! Tree inconsistency detected, while trying to distribute body: " + bodyIndex );
      debugFatalStr( "Cell Center: [ " + btnCenter_x + ", " +
        btnCenter_y + ", " +
        btnCenter_z + " ]"  );
      debugFatalStr( "Cell Size:   " + btnHalfSize );
      debugFatalStr( "Body:        [ " + btnGd.gdBodies[ bodyIndex ].bPos.x + ", " +
        btnGd.gdBodies[ bodyIndex ].bPos.y + ", " +
        btnGd.gdBodies[ bodyIndex ].bPos.z + " ]"  );

    } else {

      if (btnChildren[index] == null)
        createChild( index );

      return btnChildren[index];	
    }

    return null; // generates a null pointer exception if the tree is inconsistent
  }


  private BodyTreeNode findContainingChild( CenterOfMass c ) {

    int index = 0;
    double dx, dy, dz;
    
    dx = c.cofmCenter_x - btnCenter_x;
    dy = c.cofmCenter_y - btnCenter_y;
    dz = c.cofmCenter_z - btnCenter_z;

    if (dx>=0) index += 1;
    if (dy>=0) index += 2;
    if (dz>=0) index += 4;

    if ( Math.abs(dx)>btnHalfSize || Math.abs(dy)>btnHalfSize || Math.abs(dz)>btnHalfSize ) {

      debugFatalStr( "Error! Tree inconsistency detected, while trying to add a center of mass! ");
      debugFatalStr( "Cell Center: [ " + btnCenter_x + ", " +
        btnCenter_y + ", " +
        btnCenter_z + " ]"  );      
      debugFatalStr( "Cell Size:   " + btnHalfSize + " dx=" + dx + " dy=" + dy + " dz=" + dz);
      new Exception().printStackTrace();

    } else {

      if (btnChildren[index] == null)
        createChild( index );

      return btnChildren[index];	
    }

    return null; // generates a null pointer exception if the tree is inconsistent
  }



  private void Split() {

    int 	i, body;
    // Create children

    btnIsLeaf	= false;

    debugStr( "Splitting node, bodies: " + btnBodyCount );

    btnChildren = new BodyTreeNode[8];
    
    for ( i=0; i<8; i++ )
      btnChildren[i] = null;

    // And redistribute all bodies

    for ( i=0; i<btnBodyCount; i++ ) {
 
      body = btnBodyIndex[i];

      findContainingChild( body ).addBody( body );
    }

    btnBodyCount = 0;

    btnChildrenCount = 0;
  }

 
  public void addBody( int bodyIndex ) {

    btnCenterOfMassValid = false;

    if (btnIsLeaf) {

      if (btnBodyCount<btnGd.gdMaxBodiesPerNode) {

        btnBodyIndex[ btnBodyCount++ ] = bodyIndex;
	
        return;

      } else {
	
        Split();
      }

    } 

    findContainingChild( bodyIndex ).addBody( bodyIndex );
  }


  void addCenterOfMass( CenterOfMass c ) {

    // indicate that the center of mass has changed

    btnCenterOfMassValid = false;

    if (c.cofmCenter_x == btnCenter_x &&
      c.cofmCenter_y == btnCenter_y &&
      c.cofmCenter_z == btnCenter_z ) {

      // this is the right node, add the center of mass
	    /*
      btnGd.debugStr("adding COFM: (" + btnCenter_x + "," + 
		  btnCenter_y + "," + 
		  btnCenter_z + "), " + c.cofmMass );
	    */
      if (btnCenterOfMassReceived) {

        btnTotalMass += c.cofmMass;
        
        btnCenterOfMass_x += c.cofmCenterOfMass_x * c.cofmMass;
        btnCenterOfMass_y += c.cofmCenterOfMass_y * c.cofmMass;
        btnCenterOfMass_z += c.cofmCenterOfMass_z * c.cofmMass;

      } else {

        btnTotalMass = c.cofmMass;
        
        btnCenterOfMass_x = c.cofmCenterOfMass_x * c.cofmMass;
        btnCenterOfMass_y = c.cofmCenterOfMass_y * c.cofmMass;
        btnCenterOfMass_z = c.cofmCenterOfMass_z * c.cofmMass;
     
        btnCenterOfMassReceived = true;
      }
      /*
      btnGd.debugStr("updating COFM, now: (" + btnCenterOfMass_x + "," + 
		  btnCenterOfMass_y + "," + 
		  btnCenterOfMass_z + "), " + c.cofmMass );
      */
      return;
    }

    if (btnIsLeaf) {
      Split();
    }

    findContainingChild( c ).addCenterOfMass( c );
  }


  // Tree integrity check and support routines

  private boolean check( vec3 center, double size, vec3 pos ) {

    double maxx = center.x + size;
    double minx = center.x - size;
    double maxy = center.y + size;
    double miny = center.y - size;
    double maxz = center.z + size;
    double minz = center.z - size;

    if (pos.x>maxx) return false; 
    if (pos.x<minx) return false; 
    if (pos.y>maxy) return false; 
    if (pos.y<miny) return false; 
    if (pos.z>maxz) return false; 
    if (pos.z<minz) return false; 

    return true;
  }

 /*
  public int sanityCheck() {

    int SaneChildren = 0, i, InsaneBodies;

    if (btnIsLeaf) {
 
      // check all contained bodies to see if they really fit into this cell

      InsaneBodies = 0;

      for (i=0;i<btnBodyCount;i++) { 

	 int bi = btnBodyIndex[i];
    
         if (!check( btnCenter, btnHalfSize, btnGd.gdBodies[bi].bPos)) {
           InsaneBodies++;
           debugStr( "Body: x: " + btnGd.gdBodies[bi].bPos.x + ", y: " +  
             btnGd.gdBodies[bi].bPos.y + ", z: " +  btnGd.gdBodies[bi].bPos.z );
         }
      }

      if (InsaneBodies>0) {
         debugStr( "^^^ (" + InsaneBodies + "/" + btnBodyCount + ") outside cell bounds, center: " +
            "x: " + btnCenter.x + ", y: " + btnCenter.y + ", z: " + btnCenter.z + ", size: " + btnHalfSize );
         SaneChildren = 1;
      }
    }
    else {

      // this is an internal node, check all children.

      for (i=0;i<8;i++) {
        if (btnChildren[i]!=null) {
           SaneChildren += btnChildren[i].sanityCheck();
        }
      }
    }
    return SaneChildren;
  }
 */

  public int countBodies() {

    int i, bodies = 0;

    if (btnIsLeaf) {
      return btnBodyCount;
    }
    else {

      for (i=0;i<8;i++) {
        if (btnChildren[i]!=null)
          bodies += btnChildren[i].countBodies();
      }
    }
    return bodies;
  }

  public void Iterate( int Depth ) {

    int i;

    for (i=0;i<Depth;i++) {
      System.out.print(".");
    }

    if (btnIsLeaf) {
      debugStr( "LeafNode, Bodies: " + btnBodyCount + " of " + btnGd.gdMaxBodiesPerNode + ".");
    }
    else {
      debugStr( "Internal Node, Children: " + btnChildrenCount + " of 8" );

      for (i=0;i<8;i++) {
        if (btnChildren[i]!=null)
          btnChildren[i].Iterate( (Depth+1) );
      }
    }
  }

/*
  private String getDepthStr() {

    return depthStr[ btnDepth ];
  }
*/

  protected void debugStr( String s ) {

    if (btnDebug)
      btnGd.debugStr( s );
  }


  protected void debugFatalStr( String s ) {

      btnGd.debugStr( s );
  }

/*
  public static void setDebugMode( boolean debug ) {

    btnDebug = debug;
  }
*/

  public void ComputeCenterOfMass() {

    int i, bi;
    double recip;

    if (!btnCenterOfMassValid) {

	    //      btnGd.debugStr("entering, cofm not valid!");

      btnCenterOfMassValid = true;

      if (!btnCenterOfMassReceived) {

        btnTotalMass = 0.0;

        btnCenterOfMass_x = 0;
        btnCenterOfMass_y = 0;
        btnCenterOfMass_z = 0;
      }

      if (btnIsLeaf) {

        Body b;

        // Compute Center of mass of all bodies in this cell.

        for (i=0;i<btnBodyCount;i++) {

          b = btnGd.gdBodies[ btnBodyIndex[i] ];

          btnCenterOfMass_x += b.bPos.x * b.bMass;
          btnCenterOfMass_y += b.bPos.y * b.bMass;
          btnCenterOfMass_z += b.bPos.z * b.bMass;
  
          btnTotalMass += b.bMass;
        }

        // the following may generate an exception!

        recip = 1.0/btnTotalMass;

        btnCenterOfMass_x *= recip;
        btnCenterOfMass_y *= recip;
        btnCenterOfMass_z *= recip;

	//	btnGd.debugStr("leaf COFM: " + btnCenterOfMass_x + ", " + btnCenterOfMass_y + ", " + btnCenterOfMass_z + " " + btnTotalMass + " " + btnBodyCount );

      } else {

        BodyTreeNode b;

        // Compute the center of mass of all children if it is an internal node

        btnCenterOfMass_x = 0;
        btnCenterOfMass_y = 0;
        btnCenterOfMass_z = 0;

        btnTotalMass = 0;

        for (i=0;i<8;i++) {

          b = btnChildren[i];

          if (b!=null) {

             b.ComputeCenterOfMass();

             btnCenterOfMass_x += b.btnCenterOfMass_x * b.btnTotalMass;
             btnCenterOfMass_y += b.btnCenterOfMass_y * b.btnTotalMass;
             btnCenterOfMass_z += b.btnCenterOfMass_z * b.btnTotalMass;

             btnTotalMass += b.btnTotalMass;
          }
        }

        recip = 1.0/btnTotalMass;

        btnCenterOfMass_x *= recip;
        btnCenterOfMass_y *= recip;
        btnCenterOfMass_z *= recip;

	//	btnGd.debugStr("internal COFM: " + btnCenterOfMass_x + ", " + btnCenterOfMass_y + ", " + btnCenterOfMass_z + " " + btnTotalMass );
      }
    }
  }

  void Barnes( Body b ) {

    double   Diff_x, Diff_y, Diff_z;
    Body   b2;
    double Dist, DistSq, Factor;
    int    i;

    //Diff = new vec3();

    // Compute the acceleration this node acts on the body

    Diff_x = btnCenterOfMass_x - b.bPos.x;
    Diff_y = btnCenterOfMass_y - b.bPos.y;
    Diff_z = btnCenterOfMass_z - b.bPos.z;

    DistSq = Diff_x * Diff_x + Diff_y * Diff_y + Diff_z * Diff_z;
    /*
    btnGd.debugStr( b.bNumber + ": cofm: " + btnCenterOfMass_x +
		    "," + btnCenterOfMass_y + 
		    "," + btnCenterOfMass_z );
    */
    //    btnGd.debugStr( b.bNumber + ": barnesing, DistSq: " + DistSq + ", maxTheta: " + btnMaxTheta );

    if (DistSq >= btnMaxTheta) {

      // The distance was large enough to use the treenode instead of iterating all bodies.  

      DistSq += btnGd.gdSoftSQ;
      Dist = Math.sqrt( DistSq );
      Factor = btnTotalMass / (DistSq * Dist);

      b.bAcc.x += Diff_x * Factor;
      b.bAcc.y += Diff_y * Factor;
      b.bAcc.z += Diff_z * Factor;

      b.bWeight++;

    } else {

      // The distance was too small.  

      if (btnIsLeaf) {

        // compute accelerations on the body due to all bodies in this node.

        for ( i=0; i<btnBodyCount; i++ ) {

          b2 = btnGd.gdBodies[ btnBodyIndex[i] ];

          Diff_x = b2.bPos.x - b.bPos.x;
          Diff_y = b2.bPos.y - b.bPos.y;
          Diff_z = b2.bPos.z - b.bPos.z;

          DistSq = Diff_x * Diff_x + Diff_y * Diff_y + Diff_z * Diff_z + btnGd.gdSoftSQ;
          Dist = Math.sqrt( DistSq );
          Factor = b2.bMass / (DistSq * Dist);

          b.bAcc.x += Diff_x * Factor;
          b.bAcc.y += Diff_y * Factor;
          b.bAcc.z += Diff_z * Factor;

          b.bWeight++;
        }

      } else {

        for (i=0;i<8;i++)
          if (btnChildren[i]!=null)
            btnChildren[i].Barnes( b );
      }

    }

  }

  int ComputeLeafAccelerationsBarnes( BodyTreeNode root ) {

    int i, Weight = 0;
    Body b;

    // Iterate the tree for each body

    for ( i=0; i<btnBodyCount; i++ ) {

      b = btnGd.gdBodies[ btnBodyIndex[i] ];

      // check if it is not an essential body

      if (b.bNumber==-1) continue;

      b.bWeight = 0;
      b.bAcc.x = 0;
      b.bAcc.y = 0;
      b.bAcc.z = 0; 

      root.Barnes( b );
      /*
      btnGd.debugStr("computed accels for body " + b.bNumber + ", weight " +  b.bWeight + ", (" + b.bAcc.x + "," + b.bAcc.y + "," + b.bAcc.z + ")" );
      */
      Weight += b.bWeight;
    }

    return Weight;
  }


  int ComputeAccelerationsBarnes( BodyTreeNode root ) {

    int i, interactions = 0;

    if (btnIsLeaf) {

      interactions = ComputeLeafAccelerationsBarnes( root );
    }
    else {

      for (i=0;i<8;i++) {
        if (btnChildren[i]!=null)
          interactions += btnChildren[i].ComputeAccelerationsBarnes( root );
      }
    }
    return interactions;
  }

}






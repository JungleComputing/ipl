import java.lang.*;
// import java.io.*;
import ibis.rmi.*;

strictfp class OrbTreeNode {

  vec3 lower, upper;
  int first, last;
  int children[];

  OrbTreeNode() {

    lower = new vec3();
    upper = new vec3();

    first = last = 0;
    
    children = new int[2];
    
    children[0] = 0;
    children[1] = 0;
  }

}


public class OrbTree {

  MinMax lMinMax = new MinMax(), gMinMax;
  GlobalData g;
  OrbTreeNode Orb[];
  int pos[], wei[], weights[], index[], count[], load[];

  OrbTree( GlobalData g ) {

    this.g = g;

    Orb = new OrbTreeNode[ 2 * g.gdNumProcs - 1 ];

    for (int i=0; i<( 2 * g.gdNumProcs - 1); i++ ) {
      Orb[i] = new OrbTreeNode();
    }
    
    load = new int[ 2 * g.gdMaxProcs ];

    pos     = new int[ g.gdMaxBodies ];
    wei     = new int[ g.gdMaxBodies ];
    weights = new int[ g.gdMaxBodies ];
    index   = new int[ g.gdMaxBodies ];
    count   = new int[ g.gdMedianRadix ];
  }


  int determineLoadBalanceLevel() {

    int i,sum = 0;

    for ( i=0; i<g.gdNumBodies; i++ ) {
      sum += g.gdBodies[i].bWeight;
    }

    //    g.debugStr("local sum: " + sum );

    for ( i=0; i<g.gdNumProcs; i++ ) {
      load[ g.gdFirstLoc + i ] = sum;
    }

    g.Proc.TotalExchangeInt( load, g.gdFirstLoc );

    sum = 0;

    for ( i=0; i<g.gdNumProcs; i++ ) {
      sum += load[ g.gdFirstLoc + i ];
    }

    //    g.debugStr("total sum: " + sum );

    return checkLoad( 0, sum, g.gdLogProcs );
  }


  int checkLoad( int parent, int sum, int level ) {

    int lev0, lev1;

    if (parent<g.gdFirstLoc)
      load[ parent ] = 0;

    // first child

    if (Orb[parent].children[0]>0) {
      lev0 = checkLoad( Orb[parent].children[0], sum, level-1 );
      load[ parent ] += load[ Orb[ parent ].children[0] ];
    } else {
      lev0 = 0;
    }

    // second child

    if (Orb[parent].children[1]>0) {
      lev1 = checkLoad( Orb[parent].children[1], sum, level-1 );
      load[ parent ] += load[ Orb[ parent ].children[1] ];
    } else {
      lev1 = 0;
    }

    /*
    g.debugStr("parent: " + parent + ", level: " + level + ", lev0: " + lev0 + ", lev1: " + lev1 );
    g.debugStr("  load parent: " + load[ parent ] );
    g.debugStr("  condition: " + ((int)((1.0+g.gdMaxLoad)*(double)(sum*(Orb[parent].last - Orb[parent].first + 1)) / (double)(g.Proc.ProcessorCount))) );
    */

    if (load[ parent ] > (int)((1.0+g.gdMaxLoad)*(double)(sum*(Orb[parent].last - 
      Orb[parent].first + 1)) / (double)(g.Proc.ProcessorCount)))
      return (level + 1);
    else
      return (Math.max( lev0, lev1 ));
    
  }

 
  void Update() {

    UpdateBounds();
    UpdateBodies();
  }


  void UpdateBounds() {

    // Compute the global tree bounds

    ComputeLocalBounds();

    try {

      gMinMax = g.Proc.setMinMax( lMinMax );
    
    } catch (RemoteException e) {
       g.Proc.HandleException( e, "MinMax Exchange" );
    }
    /* 
    g.debugStr( "local bounds: min [ " + lMinMax.min.x + ", " + lMinMax.min.y + ", " + lMinMax.min.z + " ]" );
    g.debugStr( "local bounds: max [ " + lMinMax.max.x + ", " + lMinMax.max.y + ", " + lMinMax.max.z + " ]" );

    g.debugStr( "global bounds: min [ " + gMinMax.min.x + ", " + gMinMax.min.y + ", " + gMinMax.min.z + " ]" );
    g.debugStr( "global bounds: max [ " + gMinMax.max.x + ", " + gMinMax.max.y + ", " + gMinMax.max.z + " ]" );
    */
    // Adjust the new global outer bounds in the Orb tree

    AdjustBounds();
  }

    public vec3 getGlobalMin() {
	return gMinMax.min;
    }

    public vec3 getGlobalMax() {
	return gMinMax.max;
    }

    public vec3 getLocalMin() {
	return lMinMax.min;
    }

    public vec3 getLocalMax() {
	return lMinMax.max;
    }
  
  void AdjustBounds() {

    double dx, dy, dz, side;

    // Compute new boundaries

    dx = gMinMax.max.x - gMinMax.min.x;
    dy = gMinMax.max.y - gMinMax.min.y;
    dz = gMinMax.max.z - gMinMax.min.z;

    side = Math.max( Math.max( dx, dy ), dz ); 

    //    g.debugStr("side: " + side );

    gMinMax.max.x = gMinMax.min.x + (1.0 + g.gdDimSlack) * side;
    gMinMax.max.y = gMinMax.min.y + (1.0 + g.gdDimSlack) * side;
    gMinMax.max.z = gMinMax.min.z + (1.0 + g.gdDimSlack) * side;

    gMinMax.min.x = gMinMax.min.x - g.gdDimSlack * side;
    gMinMax.min.y = gMinMax.min.y - g.gdDimSlack * side;
    gMinMax.min.z = gMinMax.min.z - g.gdDimSlack * side;
    /*
    g.debugStr( "orb min: " + gMinMax.min.x + ", " + gMinMax.min.y + ", " + gMinMax.min.z );
    g.debugStr( "orb max: " + gMinMax.max.x + ", " + gMinMax.max.y + ", " + gMinMax.max.z );
*/
    if (g.gdIteration==0) {

      Orb[0].lower.x = gMinMax.min.x;
      Orb[0].lower.y = gMinMax.min.y;
      Orb[0].lower.z = gMinMax.min.z;

      Orb[0].upper.x = gMinMax.max.x;
      Orb[0].upper.y = gMinMax.max.y;
      Orb[0].upper.z = gMinMax.max.z;

      Orb[0].first = 0;
      Orb[0].last = g.gdNumProcs - 1;
      
    } else {

      // Adjust all sides sharing a side with the old Orb root to the new
      // sizes

      for (int i = g.gdFirstLoc + g.gdNumProcs - 1; i>=0; i-- ) {
	      /*
	      if (Orb[i].upper.x == Orb[0].upper.x) {
		      Orb[i].upper.x = gMinMax.max.x;
		      g.debugStr("adjusting ux for orbnode " + i + " to " + gMinMax.max.x );
	      }
	      if (Orb[i].upper.y == Orb[0].upper.y) {
		      Orb[i].upper.y = gMinMax.max.y;
		      g.debugStr("adjusting uy for orbnode " + i + " to " + gMinMax.max.y);
	      }
	      if (Orb[i].upper.z == Orb[0].upper.z) {
		      Orb[i].upper.z = gMinMax.max.z;
		      g.debugStr("adjusting uz for orbnode " + i + " to " + gMinMax.max.z);
	      }

 	      if (Orb[i].lower.x == Orb[0].lower.x) {
		      Orb[i].lower.x = gMinMax.min.x;
		      g.debugStr("adjusting lx for orbnode " + i + " to " + gMinMax.min.x);
	      }
	      if (Orb[i].lower.y == Orb[0].lower.y) {
		      Orb[i].lower.y = gMinMax.min.y;
		      g.debugStr("adjusting ly for orbnode " + i + " to " + gMinMax.min.y );
	      }
	      if (Orb[i].lower.z == Orb[0].lower.z) {
		      Orb[i].lower.z = gMinMax.min.z;
		      g.debugStr("adjusting lz for orbnode " + i + " to " + gMinMax.min.z );
	      }

*/
        if (Orb[i].upper.x == Orb[0].upper.x) 
          Orb[i].upper.x = gMinMax.max.x;
        if (Orb[i].upper.y == Orb[0].upper.y) 
          Orb[i].upper.y = gMinMax.max.y;
        if (Orb[i].upper.z == Orb[0].upper.z) 
          Orb[i].upper.z = gMinMax.max.z;

        if (Orb[i].lower.x == Orb[0].lower.x) 
          Orb[i].lower.x = gMinMax.min.x;
        if (Orb[i].lower.y == Orb[0].lower.y) 
          Orb[i].lower.y = gMinMax.min.y;
        if (Orb[i].lower.z == Orb[0].lower.z) 
          Orb[i].lower.z = gMinMax.min.z;

      }

    }

  }


  void UpdateBodies() {

    vec3 min, max, pos;

    int i,j;

    if ((g.gdIteration>0) && (g.gdNumProcs>1)) {

	    //      g.debugStr("UpdateBodies: starting exchange");

      // Find a new region for bodies who are not in the current orb
      // region anymore

      min = Orb[ g.gdFirstLoc + g.gdMyProc ].lower;
      max = Orb[ g.gdFirstLoc + g.gdMyProc ].upper;

      for ( i=0; i<g.gdNumBodies; i++ ) {

        pos = g.gdBodies[i].bPos;
      
        if (( pos.x < min.x ) || ( pos.x >= max.x ) ||
          ( pos.y < min.y ) || ( pos.y >= max.y ) ||
          ( pos.z < min.z ) || ( pos.z >= max.z )) {
        
          // Find a new destination for this body.

          for ( j=0; j<g.gdFirstLoc; ) {

            j = Orb[j].children[0];
            
            if ( pos.x >= Orb[j].upper.x || pos.y >= Orb[j].upper.y || pos.z >= Orb[j].upper.z )
              j++;
          }
	  
	  //	  g.debugStr("sending body " + g.gdBodies[i].bNumber + " to " + (j-g.gdFirstLoc ) );

          g.Proc.setExchangeDestination( i, j - g.gdFirstLoc);

        } else {

          g.Proc.setExchangeDestination( i, g.gdMyProc );
        }

      }

      try {
        g.Proc.ExchangeBodies();
      } catch (RemoteException e) {
        g.Proc.HandleException( e, "UpdateBodies Exchange" );
      }
    } 
    
    //    g.debugStr("UpdateBodies: leaving");

  }

  void ComputeLocalBounds() {

    int i;

    lMinMax.min.x = Double.MAX_VALUE;
    lMinMax.min.y = Double.MAX_VALUE;
    lMinMax.min.z = Double.MAX_VALUE;

    lMinMax.max.x = -Double.MAX_VALUE;
    lMinMax.max.y = -Double.MAX_VALUE;
    lMinMax.max.z = -Double.MAX_VALUE;

    for ( i=0; i<g.gdNumBodies; i++ ) {

      lMinMax.min.x = Math.min( lMinMax.min.x, g.gdBodies[i].bPos.x ); 
      lMinMax.min.y = Math.min( lMinMax.min.y, g.gdBodies[i].bPos.y ); 
      lMinMax.min.z = Math.min( lMinMax.min.z, g.gdBodies[i].bPos.z ); 

      lMinMax.max.x = Math.max( lMinMax.max.x, g.gdBodies[i].bPos.x ); 
      lMinMax.max.y = Math.max( lMinMax.max.y, g.gdBodies[i].bPos.y ); 
      lMinMax.max.z = Math.max( lMinMax.max.z, g.gdBodies[i].bPos.z ); 

    }
  }


  int ComputeDim( vec3 t ) {

    return ((t.x+g.gdDoubleEpsilon) > t.y) ?
	    (((t.x+g.gdDoubleEpsilon) > t.z) ? 0:2) :
	    (((t.y+g.gdDoubleEpsilon) > t.z) ? 1:2); 
  }


  int findMedian( int pos[], int wei[], int medWei, int rootLoc ) {

    int procs = Orb[rootLoc].last - Orb[rootLoc].first + 1, 
      firstProc = Orb[rootLoc].first,
      loWei = 0,
      median = 0,
      actBodies = g.gdNumBodies,
      offset, size, subset[],
      shift, firstRad, locRad, partMed, i, j, k; // count is now private for this object

    //    g.debugStr( "findMedian called, rootLoc: " + rootLoc );

    //    for (i=0; i<100; i++) 
    //    g.debugStr( "pos[" + i + "]: " + pos[i] );

    for ( shift = g.gdMedianBits - g.gdMedianShift; shift>=0;  shift -= g.gdMedianShift ) {

	//      g.debugStr( "findMedian shift: " + shift + ", totSize: " + g.gdMedianRadix + ", actBodies: " + actBodies );

      // count total weight of each radix

      for (i=0; i<g.gdMedianRadix; i++) 
        count[i] = 0;

      for (i=actBodies-1; i>=0; i--)
        count[ (pos[i]>>shift) & (g.gdMedianRadix-1) ] += wei[i];

      // compute prefix sums of the count array

      for (i=1; i<g.gdMedianRadix; i++) 
        count[i] += count[i-1];

      //for (i=0; i<g.gdMedianRadix; i++) 
      // g.debugStr( "count[" + i + "]: " + count[i] );

      // Exchange the array
      // For efficiency reasons, we transmit the whole array, as well as an offset and size,
      // because the array is relatively small.

      g.Proc.resetExchangeIntArray();

      for (i= (g.Proc.myProc - firstProc + 1) % procs; i!=g.Proc.myProc - firstProc; i = (i+1) % procs ) {

        firstRad = i * (g.gdMedianRadix / procs) + Math.min( i, g.gdMedianRadix % procs );
         
        locRad = g.gdMedianRadix / procs + ((i < g.gdMedianRadix % procs ) ? 1 : 0);
	
        g.Proc.setExchangeIntArrayDest( firstProc + i, firstRad, locRad ); 

	//	g.debugStr("sending to " + (firstProc + i) + ": first: " + firstRad + ", size: " + locRad );
      }

      g.Proc.ExchangeIntArray( count );
   
      // Add all subsets to the local subset

      firstRad = (g.Proc.myProc - firstProc) * (g.gdMedianRadix / procs) + 
        Math.min( g.Proc.myProc - firstProc, g.gdMedianRadix % procs );
         
      locRad = g.gdMedianRadix / procs + (((g.Proc.myProc - firstProc) < g.gdMedianRadix % procs ) ? 1 : 0);

      //      g.debugStr("firstRad: " + firstRad + ", locRad: " + locRad );

      //      g.debugStr("count[ firstRad ] 1: " + count[ firstRad ] );

      for (i=0; i<g.Proc.ProcessorCount; i++ ) {

        subset = g.Proc.getExchangeIntArray( i );
  
        if (subset!=null) {
          offset = g.Proc.getExchangeIntArrayOffset( i );
          size = g.Proc.getExchangeIntArraySize( i );
          for (j=0; j<size; j++)
            count[ firstRad + j ] += subset[ offset + j ];
        }      
      }
      
      // broadcast local subset to all processors in the subtree

      g.Proc.resetExchangeIntArray();

      count[ firstRad ] = ( count[ firstRad ] << g.gdMaxLogProcs ) + g.Proc.myProc;

      //      g.debugStr("count[ firstRad ] 2: " + count[ firstRad ] );

      for (i= (g.Proc.myProc - firstProc + 1) % procs; i!=g.Proc.myProc - firstProc; i = (i+1) % procs ) {
        g.Proc.setExchangeIntArrayDest( firstProc + i, firstRad, locRad ); 
	//	g.debugStr("sending to " + (firstProc + i) + ": first: " + firstRad + ", size: " + locRad );

      }

      g.Proc.ExchangeIntArray( count );
      
      count[ firstRad ] = ( count[ firstRad ] >> g.gdMaxLogProcs );

      //      g.debugStr("count[ firstRad ] 3: " + count[ firstRad ] );

      //      for (i=0; i<g.gdMedianRadix; i++) 
      //        g.debugStr( "count1 [" + i + "]: " + count[i] );

      for (i=0; i<g.Proc.ProcessorCount; i++ ) {

        subset = g.Proc.getExchangeIntArray( i );
  
        if (subset!=null) {

          offset = g.Proc.getExchangeIntArrayOffset( i );

          j = subset[ offset ] & (g.gdMaxProcs - 1);

          subset[ offset ] = subset [ offset ] >> g.gdMaxLogProcs;

          firstRad = j - firstProc * ( g.gdMedianRadix / procs ) +
            Math.min( j - firstProc, g.gdMedianRadix % procs );

          locRad = g.gdMedianRadix / procs + (( j - firstProc < g.gdMedianRadix % procs ) ? 1 : 0);

	  //	  g.debugStr("first: " + firstRad + ", size: " + locRad + ", offset: " + offset );

          // operator precedence?

          for (k=0; k<locRad; k++)
	      //            count[ firstRad + k ] = subset[ offset + k ];
             count[ offset + k ] = subset[ offset + k ];
        }      
      }

      //      for (i=0; i<g.gdMedianRadix; i++) 
      //        g.debugStr( "count2 [" + i + "]: " + count[i] );

      
      for (i=0; i< g.gdMedianRadix && count[i] + loWei  <= medWei; i++ );

      //      g.debugStr("i: " + i + " lowei: " + loWei + ", medWei: " + medWei );

      if ((i>0) && (count[i-1] + loWei == medWei)) {

        for (j=g.gdMedianRadix-1; (j>=0) && (count[j] + loWei >= medWei ); j-- );
 
        partMed = (i==j+2) ? i : i - ((i-j)>>1);

        actBodies = 0;

	//	g.debugStr("count found, partMed: " + partMed );

      } else {

        partMed = i;

        for (i=0, j=0; i<actBodies; i++) {

          if (((pos[i]>>shift) & (g.gdMedianRadix-1)) == partMed) {
            pos[j] = pos[i];
            wei[j++] = wei[i];
          }

        }
        actBodies = j;

	//	g.debugStr("count not found, partMed: " + partMed );
      }

      median |= (partMed<<shift);

      if (partMed>0)
        loWei += count[ partMed - 1 ];
    }

    //    g.debugStr("returning median: " + median );

    return median;
  }


  void LoadBalance( int level ) {

    int i, j, k, rootLoc, dim, procs, firstProc, halfProcs, totWeight, median;
    double normFac, minPos, newMedian;

    vec3 t = new vec3();
    OrbTreeNode root;

    //    g.debugStr( "entering loadbalance, level " + level );

    for ( ; level>0; level-- ) {

      // g.debugStr( "doing level " + level );

      // Find the rootnode of the orb subtree

      for ( i=g.gdLogProcs, rootLoc=0; i>level; i-- ) {

        rootLoc = Orb[ rootLoc ].children[0];
 
        if (g.Proc.myProc > Orb[rootLoc].last) 
          rootLoc++;
      }

      //      g.debugStr("level: " + level + ", rootLoc: " + rootLoc );

      root = Orb[rootLoc];

      // Find the number of procs and first processor in the subtree, and
      // determine the longest dimension in the subtree.

      procs = root.last - root.first + 1;
      halfProcs = ((procs - 1)>>1) + 1;
      firstProc = root.first;
      
      t.x = root.upper.x - root.lower.x;
      t.y = root.upper.y - root.lower.y;
      t.z = root.upper.z - root.lower.z;

      //      dim = ( t.x > t.y ) ? ( ( t.x > t.z ) ? 0 : 2 ) : ( ( t.x > t.z ) ? 1 : 2 );
      dim = ComputeDim( t );
      /*
      g.debugStr("rootloc upper: " + root.upper.x + ", " + root.upper.y + ", " + root.upper.z );
      g.debugStr("rootloc lower: " + root.lower.x + ", " + root.lower.y + ", " + root.lower.z );
      g.debugStr("rootloc     t: " + t.x + ", " + t.y + ", " + t.z );

      g.debugStr("level: " + level + ", procs: " + procs + ", halfProcs: " + halfProcs + ", firstProc: " + firstProc + ", dim: " + dim );
      */

      // Copy positions and weights to an array

      normFac = (double)((1<<g.gdMedianBits) - 1)/t.element( dim );
      minPos = root.lower.element( dim );

      //      g.debugStr("normfac: " + normFac + ", minPos: " + minPos );
  
      totWeight = 0;

      for ( i=0; i<g.gdNumBodies; i++ ) {
        pos[i] = (int)((g.gdBodies[i].bPos.element( dim ) - minPos) * normFac );
        wei[i] = g.gdBodies[i].bWeight;
        totWeight += g.gdBodies[i].bWeight;
      }

      // Compute the sum of the weights in the subtree      

      for ( i=0; i<g.gdNumProcs; i++ ) {
        weights[i] = ((i>=firstProc) && (i<(firstProc+procs))) ? totWeight : -1;
      }

      //      g.debugStr("level: " + level + ", bodies: " + g.gdNumBodies + ", totalWeight: " + totWeight );
  
      g.Proc.TotalExchangeInt( weights );

      totWeight = 0;

      for ( i=firstProc; i<(firstProc + procs); i++ ) {
        totWeight += weights[i];
      }      

      //      g.debugStr("level: " + level + ", bodies: " + g.gdNumBodies + ", totalWeight: " + totWeight );

      // find weighted median for all processors in the subtree

      median = findMedian( pos, wei, totWeight * halfProcs / procs, rootLoc );

      newMedian = (procs!=1) ? (double) (median) / normFac + minPos : root.upper.element( dim );

      //      g.debugStr("newmedian:" + newMedian );

      if (g.Proc.myProc < firstProc + halfProcs ) {

        // This processor is on the lower side of the hyperplane. Mark bodies that have
        // to be send to the upper side.

        for ( j=0, i=g.gdNumBodies-1; i>=0; i-- ) {

          if (g.gdBodies[i].bPos.element( dim ) >= newMedian)
            index[i] = firstProc + halfProcs + (j++) % (procs-halfProcs);
          else
            index[i] = g.Proc.myProc;
        }
              
      } else {

        // This processor is on the upper side of the hyperplane. Mark bodies that have
        // to be send to the lower side.

        for ( j=0, i=g.gdNumBodies-1; i>=0; i-- ) {

          if (g.gdBodies[i].bPos.element( dim ) < newMedian)
            index[i] = firstProc + (j++) % halfProcs;
          else
            index[i] = g.Proc.myProc;
        }

      }
      /*
    for (i=0; i<g.gdNumBodies; i++) {

      // dump where they are going...

      if (index[i] != g.Proc.myProc) 
	g.debugStr("sending body " + i + " to proc " + index[i] );

    }
      */

      //      g.debugStr("pre exchange");
      try {
        g.Proc.ExchangeBodies( index );
      } catch (RemoteException e) {}

      //      g.debugStr("post exchange");
      for ( i=0; i<g.gdNumProcs; i++ )
        weights[i] = (g.Proc.myProc == firstProc) ? median : -1;

      //      for ( i=0; i<g.gdNumProcs; i++ ) 
      //	  g.debugStr( "1.proc " + i + ", weight: " + weights[i] );

      g.Proc.TotalExchangeInt( weights );

      //      for ( i=0; i<g.gdNumProcs; i++ ) 
      //	  g.debugStr( "2.proc " + i + ", weight: " + weights[i] );

      k = (1<<(g.gdLogProcs - level + 1 )) - 1;

      for ( j=0, i=0; j < (1<<(g.gdLogProcs - level)); j++, i++ ) {

        while (weights[i]==-1) i++;

	//        g.debugStr("i: " + i );

        rootLoc = (1<<(g.gdLogProcs - level)) + j - 1;
 
	//        g.debugStr("rootLoc: " + rootLoc );        

        root = Orb[rootLoc];

        procs = root.last - root.first + 1;

        t.x = root.upper.x - root.lower.x;
        t.y = root.upper.y - root.lower.y;
        t.z = root.upper.z - root.lower.z;

	//    dim = ( t.x > t.y ) ? ( ( t.x > t.z ) ? 0 : 2 ) : ( ( t.x > t.z ) ? 1 : 2 );
      dim = ComputeDim( t );

	//        g.debugStr("level: " + level + ", procs: " + procs + ", dim: " + dim );

        normFac = (double)((1<<g.gdMedianBits) - 1)/t.element( dim );
        minPos = root.lower.element( dim );
        newMedian = (double) (weights[i]) / normFac + minPos ;
        
	//        g.debugStr("normfac: " + normFac + ", minPos: " + minPos + ", newMedian: " + newMedian );

        // Fill in the boundaries for the first child

        Orb[k].upper.x = root.upper.x;
        Orb[k].upper.y = root.upper.y;
        Orb[k].upper.z = root.upper.z;

        Orb[k].lower.x = root.lower.x;
        Orb[k].lower.y = root.lower.y;
        Orb[k].lower.z = root.lower.z;

        Orb[k].first = root.first;
        Orb[k].last = root.first + ((procs - 1)>>1);

        root.children[0] = k;
        root.children[1] = 0;

        k++;

        // Fill in the boundaries for second child

        if (procs>1) {
    
          // Fix the first child

          Orb[k-1].upper.setElement( dim, newMedian );
  
          // Do the second child

          Orb[k].upper.x = root.upper.x;
          Orb[k].upper.y = root.upper.y;
          Orb[k].upper.z = root.upper.z;

          Orb[k].lower.x = root.lower.x;
          Orb[k].lower.y = root.lower.y;
          Orb[k].lower.z = root.lower.z;

          Orb[k].first = root.first + ((procs - 1)>>1) + 1;
          Orb[k].last  = root.last;

          Orb[k].lower.setElement( dim, newMedian );

          root.children[1] = k;

          k++;
        }

      }

    }

  }

  int sCOFM, sBody;
  int tsCOFM, tsBody;

  void ExchangeEssentialTree( BodyTree b ) {

    int essBodyCount, essCenterOfMassCount, totBodyCount = 0, totCenterOfMassCount = 0;
    Body essBodyRecv[];
    CenterOfMass essCenterOfMassRecv[];

    g.Proc.resetExchangeEssential();

    //    g.debugStr("Entering ExchangeEssential");

    // transmit the local essential trees to all procs...

    tsCOFM=0;
    tsBody = 0;

    for ( int i=(g.Proc.myProc+1)%g.Proc.ProcessorCount; i != g.Proc.myProc; i = (i+1)%g.Proc.ProcessorCount ) {
     sCOFM=0;
     sBody = 0;
     iCount = 0;
     sendEssential( b, i, false );
     tsCOFM +=sCOFM;
     tsBody += sBody;
     //     g.debugStr("dest " + i + ": COFM " + sCOFM + ", Bodies " + sBody + ", icount: " + iCount );
    
    }

    //   g.debugStr("total sent: COFM " + tsCOFM + ", Bodies " + tsBody );

    g.Proc.exchangeEssential();

    //    g.debugStr("Constructing global essential tree");

    // Add all essential bodies to the tree...

    g.gdNumEssentialBodies = g.gdNumBodies;
    
    for (int j=0;j<g.Proc.ProcessorCount;j++) {

      if (j!=g.Proc.myProc) {

        essBodyCount = g.Proc.getEssentialBodyCount( j );
        essBodyRecv = g.Proc.getEssentialBodies( j );

        // Add the bodies to the global array and insert them into the tree

        for ( int i=0; i<essBodyCount; i++ ) {
          g.gdBodies[ g.gdNumEssentialBodies ] = essBodyRecv[i];
	  g.gdBodies[ g.gdNumEssentialBodies ].bNumber = -1; // indicate an essential body
          b.addBody(  g.gdNumEssentialBodies++ );
        }
 
        // Add all center of masses to the tree

        essCenterOfMassCount = g.Proc.getEssentialCenterOfMassCount( j );
        essCenterOfMassRecv = g.Proc.getEssentialCenterOfMass( j );

        for ( int i=0; i<essCenterOfMassCount; i++ ) {
          b.addCenterOfMass( essCenterOfMassRecv[i] );
        }

	totBodyCount += essBodyCount;
	totCenterOfMassCount += essCenterOfMassCount;
      }
    }

    //    g.debugStr( "Received " + totBodyCount + " bodies, and " + totCenterOfMassCount +
    //      " centers of mass " );
    //    g.debugStr("Constructing global essential tree finished");

  }

  int iCount;

  void sendEssential( BodyTreeNode btn, int dest, boolean inside ) {

    OrbTreeNode destOrb, myOrb;
    double lx, ly, lz, ux, uy, uz, distSq = 0.0, t0x, t0y, t0z, t1x, t1y, t1z;

    destOrb = Orb[ g.gdFirstLoc + dest ];

    ux = uy = uz = 0.0;
    lx = ly = lz = 0.0;
    /*
    g.debugStr("sendEssential, center (" + btn.btnCenter_x + 
	       "," + btn.btnCenter_y + 
 	       "," + btn.btnCenter_z + 
	       "), dest " + dest + ", inside " + (inside ? "true" : "false") );
*/
    if (!inside) {

      // if parent not competely inside local region, check if this node is

      lx = btn.btnCenter_x - btn.btnHalfSize;
      ly = btn.btnCenter_y - btn.btnHalfSize;
      lz = btn.btnCenter_z - btn.btnHalfSize;

      ux = btn.btnCenter_x + btn.btnHalfSize;
      uy = btn.btnCenter_y + btn.btnHalfSize;
      uz = btn.btnCenter_z + btn.btnHalfSize;
        
      /*      g.debugStr("  lower (" + lx + "," + ly + "," + lz + ")\n" +
		 "  upper (" + ux + "," + uy + "," + uz + ")" );
      */
      myOrb = Orb[ (g.gdFirstLoc + g.Proc.myProc) ];
      /*
      g.debugStr("  lower (" + myOrb.lower.x + "," + myOrb.lower.y + "," + myOrb.lower.z + ")\n" +
		 "  upper (" + myOrb.upper.x + "," + myOrb.upper.y + "," + myOrb.upper.z + ")" );
      */
      //      g.debugStr("  myOrb: " +  (g.gdFirstLoc + g.Proc.myProc) );
      /*    
      g.debugStr("eval l0: " + ((lx >= myOrb.lower.x)?"true":"false"));
      g.debugStr("eval l1: " + ((ly >= myOrb.lower.y)?"true":"false"));
      g.debugStr("eval l2: " + ((lz >= myOrb.lower.z)?"true":"false"));
      g.debugStr("eval u0: " + ((ux < myOrb.upper.x)?"true":"false"));
      g.debugStr("eval u1: " + ((uy < myOrb.upper.y)?"true":"false"));
      g.debugStr("eval u2: " + ((uz < myOrb.upper.z)?"true":"false"));
      */
      /*
      if ((((lx - myOrb.lower.x) >= -g.gdDoubleEpsilon) &&
	   ((lx - myOrb.lower.x) < g.gdDoubleEpsilon)) ||
	  (((ly - myOrb.lower.y) >= -g.gdDoubleEpsilon) &&
	   ((ly - myOrb.lower.y) < g.gdDoubleEpsilon)) ||
	  (((lz - myOrb.lower.z) >= -g.gdDoubleEpsilon) &&
	   ((lz - myOrb.lower.z) < g.gdDoubleEpsilon)) ||
	  (((ux - myOrb.upper.x) < g.gdDoubleEpsilon) &&
	   ((ux - myOrb.upper.x) >= -g.gdDoubleEpsilon)) ||
	  (((uy - myOrb.upper.y) < g.gdDoubleEpsilon) &&
	   ((uy - myOrb.upper.y) >= -g.gdDoubleEpsilon)) ||
	  (((uz - myOrb.upper.z) < g.gdDoubleEpsilon)) &&
	  ((uz - myOrb.upper.z) >= -g.gdDoubleEpsilon)) {
	     	g.debugStr(" epsilon drama " );
	              g.debugStr("  lower (" + lx + "," + ly + "," + lz + ")\n" +
	 "  upper (" + ux + "," + uy + "," + uz + ")" );
     
        g.debugStr("  lower (" + myOrb.lower.x + "," + myOrb.lower.y + "," + myOrb.lower.z + ")\n" +
		 "  upper (" + myOrb.upper.x + "," + myOrb.upper.y + "," + myOrb.upper.z + ")" );

	      inside = true;
      }
      */

      if (((lx - myOrb.lower.x) >= -g.gdDoubleEpsilon) &&
	  ((ly - myOrb.lower.y) >= -g.gdDoubleEpsilon) &&
	  ((lz - myOrb.lower.z) >= -g.gdDoubleEpsilon) &&
	  ((ux - myOrb.upper.x) < g.gdDoubleEpsilon) &&
	  ((uy - myOrb.upper.y) < g.gdDoubleEpsilon) &&
	  ((uz - myOrb.upper.z) < g.gdDoubleEpsilon))
	      {
		      inside = true;
		      iCount++;
		      /*
		      g.debugStr( iCount + ", inside true!");
		      g.debugStr("  lower (" + lx + "," + ly + "," + lz + ")\n" +
				 "  upper (" + ux + "," + uy + "," + uz + ")" );
		      g.debugStr("  lower (" + myOrb.lower.x + "," + myOrb.lower.y + "," + myOrb.lower.z + ")\n" +
				 "  upper (" + myOrb.upper.x + "," + myOrb.upper.y + "," + myOrb.upper.z + ")" );
		      
	      } else {
		      g.debugStr( "0, inside false!");
		      g.debugStr("  lower (" + lx + "," + ly + "," + lz + ")\n" +
				 "  upper (" + ux + "," + uy + "," + uz + ")" );
		      g.debugStr("  lower (" + myOrb.lower.x + "," + myOrb.lower.y + "," + myOrb.lower.z + ")\n" +
				 "  upper (" + myOrb.upper.x + "," + myOrb.upper.y + "," + myOrb.upper.z + ")" );
		      */

	      }

/*
      if ((lx >= myOrb.lower.x) && (ly >= myOrb.lower.y) && (lz >= myOrb.lower.z) &&
        (ux < myOrb.upper.x) && (uy < myOrb.upper.y) && (uz < myOrb.upper.z))
        inside = true;
      */
    }
      
    //    g.debugStr("sendEssential, inside " + (inside ? "true" : "false") );

    if (inside) {

      t0x = destOrb.lower.x - btn.btnCenterOfMass_x;
      t0y = destOrb.lower.y - btn.btnCenterOfMass_y;
      t0z = destOrb.lower.z - btn.btnCenterOfMass_z;

      t1x = btn.btnCenterOfMass_x - destOrb.upper.x;
      t1y = btn.btnCenterOfMass_y - destOrb.upper.y;
      t1z = btn.btnCenterOfMass_z - destOrb.upper.z;

    } else {

      t0x = destOrb.lower.x - ux;
      t0y = destOrb.lower.y - uy;
      t0z = destOrb.lower.z - uz;

      t1x = lx - destOrb.upper.x;
      t1y = ly - destOrb.upper.y;
      t1z = lz - destOrb.upper.z;

    }

    //    g.debugStr("t0x: " + t0x + ", t0y: " + t0y + ", t0z: " + t0z );
    //    g.debugStr("t1x: " + t1x + ", t1y: " + t1y + ", t1z: " + t1z );

    distSq = 0.0;

    // Get the square of the distance

    if (t0x>0)
      distSq += t0x * t0x;
    else
      if (t1x>0)
        distSq += t1x * t1x;

    if (t0y>0)
      distSq += t0y * t0y;
    else
      if (t1y>0)
        distSq += t1y * t1y;

    if (t0z>0)
      distSq += t0z * t0z;
    else
      if (t1z>0)
        distSq += t1z * t1z;

    //    g.debugStr("distSQ: " + distSq + ", maxTheta: " + btn.getMaxTheta() ); 

    if (distSq >= btn.getMaxTheta() && (!btn.isLeaf() || btn.getNumBodies()>0)) {

      // The node is far enough away; send the center of mass.

	//      g.debugStr("sendEssential, sending COFM; distSQ: " + distSq ); 

      CenterOfMass c = new CenterOfMass( btn.getMass(), btn.btnCenter_x, btn.btnCenter_y, btn.btnCenter_z,
        btn.btnCenterOfMass_x, btn.btnCenterOfMass_y, btn.btnCenterOfMass_z );

        sCOFM++;
        g.Proc.sendEssentialCenterOfMass( dest, c );

    } else {

      // The node is too close, process the elements in this tree

      if (btn.isLeaf()) {

        // The node is a leaf, send all bodies
	  //       	 g.debugStr("sendEssential, sending " + btn.getNumBodies() + " Bodies; distSQ: " + distSq ); 

        for ( int i=0; i<btn.getNumBodies(); i++ ) {
	  sBody++;
	  //	  g.debugStr("sendEssential, dest " + dest + ", sending body " + btn.getBodyIndex( i)  );
         g.Proc.sendEssentialBody( dest, btn.getBody( i ) );
        }

      } else {

        // The node is internal, process all children

	  //               g.debugStr("in sendEssential, sending subTree; distSQ: " + distSq ); 

        for ( int i=0; i<8; i++ ) {
	    if ( btn.getChild( i ) != null ) {
		//              g.debugStr("in sendEssential, sending Child: " + i ); 

              sendEssential( btn.getChild( i ), dest, inside );
	    }
	}
      }

    }

  }

}

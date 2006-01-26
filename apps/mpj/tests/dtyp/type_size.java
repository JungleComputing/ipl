/* $Id$ */

/*
 * File         : type_size.java
 * Author       : Sung-Hoon Ko
 * Created      : Fri Jul 10 15:07:58 1998
 * Revision     : $Revision$
 * Updated      : $Date$
 * Copyright: Northeast Parallel Architectures Center
 *            at Syracuse University 1998
 */
/* Ported to MPJ:
   Markus Bornemann
   Vrije Universiteit Amsterdam Department of Computer Science
   25/5/2005
*/

import ibis.mpj.*;

class type_size {
  static public void test() throws MPJException {
    System.out.println();
    System.out.println("MPJ.BYTE.size()    = "+ MPJ.BYTE.size());
    System.out.println("MPJ.CHAR.size()    = "+ MPJ.CHAR.size());
    System.out.println("MPJ.SHORT.size()   = "+ MPJ.SHORT.size());
    System.out.println("MPJ.BOOLEAN.size() = "+ MPJ.BOOLEAN.size());
    System.out.println("MPJ.INT.size()     = "+ MPJ.INT.size());
    System.out.println("MPJ.LONG.size()    = "+ MPJ.LONG.size());
    System.out.println("MPJ.FLOAT.size()   = "+ MPJ.FLOAT.size());
    System.out.println("MPJ.DOUBLE.size()  = "+ MPJ.DOUBLE.size());
    System.out.println();
  
  }

  static public void main(String[] args) throws MPJException {   
    MPJ.init(args);   
    
    test();
     
    MPJ.finish();


  }
}

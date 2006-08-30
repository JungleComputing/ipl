/* $Id$ */

import ibis.mpj.*;

public class complexAdd extends Op {

  complexAdd(boolean commute) throws MPJException {
    super(commute);
  }  
  
  public void call(Object invec, int inoffset, Object inoutvec, int outoffset, int count, Datatype datatype) throws MPJException {
    Object [] in_array = (Object[])invec;
    Object [] out_array = (Object[])inoutvec;

    for (int i = 0; i <count; i++) {
    }
    
    for (int i = 0; i < count; i++){
      complexNum ocd = (complexNum)out_array[outoffset + i];
      complexNum icd = (complexNum)in_array[inoffset + i];
      
      ocd.realPart += icd.realPart;
      ocd.imaginPart += icd.imaginPart;
    }
  }
}

/*
 * Created on 23.01.2005
 */
package ibis.mpj;

import java.util.*;

/**
 * The queue which holds all received objects, which were not expected.
 */
public class MPJObjectQueue {

	private Vector queue;
	private boolean lock;

	protected MPJObjectQueue() {
		this.queue = new Vector();
		this.lock = false;
	}
	
	
	
	protected synchronized void addObject(MPJObject obj) {
		queue.add(obj);
	}
	
	protected synchronized MPJObject getObject(int contextId, int tag) {
		MPJObject obj = null;
		int i = 0;
		boolean found = false;
		
		for (Enumeration el = queue.elements(); el.hasMoreElements();) {
			obj = (MPJObject)el.nextElement();
			
			if (((obj.getTag() == tag) || 
				((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
				 (obj.getContextId() == contextId)) {
				
				found = true;
				
				
				break;
			}
		    i++;
		}
		
		if (found) {
			
			queue.remove(i);
			
		}
		else {
			obj = null;
		}
		
		return obj;
	}
	
	protected synchronized Status probe(int contextId, int tag) throws MPJException{
		MPJObject obj = null;
		Status status = null;
		
		boolean found = false;
		//System.out.println("checking ObjectQueue.");
		
		for (Enumeration el = queue.elements(); el.hasMoreElements();) {
			obj = (MPJObject)el.nextElement();
			
			if (((obj.getTag() == tag) || 
					((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
					(obj.getContextId() == contextId)) {
				
				found = true;
				break;
			}
		}
		
		if (found) {
			status = new Status();
			if (obj.getObjectData() != null) {
				status.setTag(obj.getTag());
				
				if(obj.getObjectData() instanceof byte[]) {
					status.setCount(((byte[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof char[]) {
					status.setCount(((char[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof short[]) {
					status.setCount(((short[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof boolean[]) {
					status.setCount(((boolean[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof int[]) {
					status.setCount(((int[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof long[]) {
					status.setCount(((long[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof float[]) {
					status.setCount(((float[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else if(obj.getObjectData() instanceof double[]) {
					status.setCount(((double[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				else {
					status.setCount(((Object[])obj.getObjectData()).length);
					status.setSize(status.getCount(null));
					return(status);
				} 
				
				
			}
		}
		return(status);
	}
	
	
	protected synchronized void lock() {
		this.lock = true;
	}
	
	protected boolean isLocked() {
		return(this.lock);
	}
	
	protected synchronized void release() {
		this.lock = false;
	}
	
	
}

/* $Id$ */

package org.apache.bcel;

/** 
 * RepositoryObserver is an interface that must be implemented by all
 * Repository users that want to be informed which classes are added to
 * the Repository.
 */

public interface RepositoryObserver {
    /** Notify that a class is added to the Repository.
     */
    public void notify(String class_name);
}

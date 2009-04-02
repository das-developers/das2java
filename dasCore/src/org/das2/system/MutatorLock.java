/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.system;

/**
 * see DasAxis.getMutatorLock, DasDevicePosition.getMutatorLock
 * TODO: This needs work, because there is no way to query if the lock was
 * successful.
 * 
 * @author jbf
 */
public interface MutatorLock {

    public void lock();

    public void unlock();
}

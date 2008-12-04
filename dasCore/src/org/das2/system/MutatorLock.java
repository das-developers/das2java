/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.system;

/**
 * see DasAxis.getMutatorLock, DasDevicePosition.getMutatorLock
 * 
 * @author jbf
 */
public interface MutatorLock {

    public void lock();

    public void unlock();
}

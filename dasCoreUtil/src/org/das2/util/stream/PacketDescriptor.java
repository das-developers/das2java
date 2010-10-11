/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.stream;

/**
 *
 * @author jbf
 */
public class PacketDescriptor {
    private int contentLength=-1;
    private StreamEntity[] children;
    private int childCount= 0;
    private boolean leaf= true;
}

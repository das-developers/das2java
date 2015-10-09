/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

/**
 * Anchor Type
 * @author jbf
 */
public enum AnchorType {
    CANVAS, // anchored by row and column
    PLOT,   // anchored by datum ranges
    DATA,   // anchored to the data (not implemented, same as PLOT for now).
}

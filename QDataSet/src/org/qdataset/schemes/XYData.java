/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes;

/**
 * Simplest, least abstract 
 * @author jbf
 */
public interface XYData {
    int size();
    double getX(int i);
    double getY(int i);
}

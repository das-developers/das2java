/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes;

/**
 *
 * @author jbf
 */
public interface UncertaintyProvider {
   double getUncertPlus(int i);
   double getUncertMinus(int i);  
}

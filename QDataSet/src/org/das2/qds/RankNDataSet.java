/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * RankNDataSet is a dataset that can only be accessed by slicing to
 * lower dimensionality.
 * @author jbf
 */
public interface RankNDataSet {
    QDataSet slice( int dim );
    int rank();
}

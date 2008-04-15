/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.demos;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetIterator;
import org.virbo.dataset.QDataSet;

/**
 *
 * @author jbf
 */
public class DemoDataSetIterator {
    
    public static void main( String[] args ) {
        DataSetIterator it;
        QDataSet ds;
        
       /* QDataSet rank2= DDataSet.createRank2(3,4);
        
        it= DataSetIterator.create(rank2);
        while ( it.hasNext() ) {
            double d= it.next();
            System.err.println( ""+d+ " "+it.getIndex(0)+" "+it.getIndex(1) );
        }
        QDataSet rank3= DDataSet.createRank3(3,4,5);
        it= DataSetIterator.create(rank3);
        while ( it.hasNext() ) {
            double d= it.next();
            System.err.println( ""+d+ " "+it.getIndex(0)+" "+it.getIndex(1)+" "+it.getIndex(2) );
        } 
       
        System.err.println( "zero length first index:");
        ds= DDataSet.createRank3(0,4,5);
        it= DataSetIterator.create(ds);
        while ( it.hasNext() ) {
            double d= it.next();
            System.err.println( ""+d+ " "+it.getIndex(0)+" "+it.getIndex(1)+" "+it.getIndex(2) );
        }
        */
        System.err.println( "zero length second index:");
        ds= DDataSet.createRank3(3,0,5);
        it= DataSetIterator.create(ds);
        while ( it.hasNext() ) {
            double d= it.next();
            System.err.println( ""+d+ " "+it.getIndex(0)+" "+it.getIndex(1)+" "+it.getIndex(2) );
        }        
    }
}

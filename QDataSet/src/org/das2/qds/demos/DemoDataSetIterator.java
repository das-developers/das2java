/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.demos;

import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.OldDataSetIterator;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;

/**
 *
 * @author jbf
 */
public class DemoDataSetIterator {
    
    public static void main( String[] args ) {
        OldDataSetIterator it;
        QubeDataSetIterator it2; // replaces OldDataSetIterator
        
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
        it= OldDataSetIterator.create(ds);
        it2= new QubeDataSetIterator( ds );
        
        while ( it2.hasNext() ) {
            //double d= it.next();
            it2.next();
            //System.err.print( ""+d+ " "+it.getIndex(0)+" "+it.getIndex(1)+" "+it.getIndex(2) + "   " );
            System.err.println( " "+it2.index(0)+" "+it2.index(1)+" "+it2.index(2) );
        }        

        System.err.println( "zero length second index, better example:");
        // this dataset has eleven elements in three inner datasets.  The
        // second inner dataset is empty.
        JoinDataSet join= new JoinDataSet(2);
        join.join( DDataSet.createRank1(5) );
        join.join( DDataSet.createRank1(0) );
        join.join( DDataSet.createRank1(6) );
        
        it2= new QubeDataSetIterator( join );
        
        int count= 0;
        while ( it2.hasNext() ) {
            //double d= it.next();
            it2.next();
            count++;
            if ( count==5 ) {
                System.err.println("bug in carry");
            }
            System.err.println( " "+ count+": "+it2.index(0)+" "+it2.index(1) );
        }        
        System.err.println( "count="+count );
                
        
    }
}

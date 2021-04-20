/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.OldDataSetIterator;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;

/**
 *
 * @author jbf
 */
public class ModeChangesDemo {
    public static void main(String[] args ) {
        rank2();
        //rank3();
        
        QDataSet nonQube= rankN(3);
        OldDataSetIterator it = OldDataSetIterator.create(nonQube);
        QubeDataSetIterator it2= new QubeDataSetIterator(nonQube);
        while (it.hasNext()) {
            it.next();
            it2.next();
            System.err.println( it2 );
        }
        
    }

    private static void rank2() {
        QDataSet qube1 = DDataSet.createRank1( 4);
        QDataSet qube2 = DDataSet.createRank1( 5);
        QDataSet qube3 = DDataSet.createRank1( 3);
        JoinDataSet nonQube = new JoinDataSet(2);
        nonQube.join(qube1);
        nonQube.join(qube2);
        nonQube.join(qube3);

        OldDataSetIterator it = OldDataSetIterator.create(nonQube);
        QubeDataSetIterator it2= new QubeDataSetIterator(nonQube);
        
        while (it.hasNext()) {
            it.next();
            it2.next();
            System.err.println(it.getIndex(0) + "," + it.getIndex(1)+ "   " +it2.index(0) + "," + it2.index(1) );
        }
                
    }
    
    private static void rank3() {
        QDataSet qube1 = DDataSet.createRank2(10, 4);
        QDataSet qube2 = DDataSet.createRank2(7, 5);
        QDataSet qube3 = DDataSet.createRank2(9, 3);
        JoinDataSet nonQube = new JoinDataSet(3);
        nonQube.join(qube1);
        nonQube.join(qube2);
        nonQube.join(qube3);

        OldDataSetIterator it = OldDataSetIterator.create(nonQube);
        QubeDataSetIterator it2= new QubeDataSetIterator(nonQube);
        
        int count= 0;
        while (it.hasNext()) {
            it.next();
            count++;
            System.err.print(it.getIndex(0) + "," + it.getIndex(1) + "," + it.getIndex(2) + "   ");
            System.err.println(it.getIndex(0) + "," + it.getIndex(1) + "," + it.getIndex(2));
        }
        System.err.println("count="+count);
    }
    
    private static QDataSet rankN( int rank ) {
        if ( rank==1 ) {
            return DDataSet.createRank1( (int)( Math.random()*5+1) );
        } else {
            JoinDataSet ds= new JoinDataSet(rank);
            int size= (int)( Math.random()*5+1);
            for ( int i=1; i<size; i++ ) {
                ds.join( rankN(rank-1) );
            }
            return ds;
        }
    }
}

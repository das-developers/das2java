/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Random;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.das2.qds.DDataSet;
import org.das2.qds.FDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;
import org.das2.qstream.SimpleStreamFormatter;
import org.das2.qstream.StreamException;

/**
 *
 * @author jbf
 */
public class SimpleStreamFormatterTest {

    public static void main(String[] args) throws ParseException, StreamException, FileNotFoundException, IOException {
        //test6();
        //test7();
        testBundle();
    }

    private static QDataSet test1() throws ParseException, StreamException, IOException {
        QDataSet ds = Ops.timegen("2003-09-09", "1 " + Units.days, 11);
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test1.qds"), true );
        return ds;
    }

    private static QDataSet test1_5() throws ParseException, StreamException, IOException {
        QDataSet ds= Ops.labelsDataset(new String[]{"B-GSM,X", "B-GSM,Y", "B-GSM,Z"});
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test1_5.qds"), true );
        return ds;
    }
    
    private static QDataSet test2() throws ParseException, StreamException, IOException  {
        MutablePropertyDataSet tags = (MutablePropertyDataSet) Ops.timegen("2003-09-09", "1 " + Units.days, 11);
        tags.putProperty( QDataSet.NAME, "time");
        MutablePropertyDataSet ds = (MutablePropertyDataSet) Ops.findgen(11, 3);
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME,"B_GSM");
        
        MutablePropertyDataSet labels= (MutablePropertyDataSet) Ops.labelsDataset(new String[]{"B-GSM-X", "B-GSM-Y", "B-GSM-Z"});
        labels.putProperty(QDataSet.NAME, "dimLabels");
        ds.putProperty(QDataSet.DEPEND_1,labels );
        
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test2.qds"), true );

        return ds;
    }
    
    
    private static QDataSet test3() throws ParseException, StreamException, IOException  {
        MutablePropertyDataSet tags = (MutablePropertyDataSet) Ops.timegen("2003-09-09", "13.86 " + Units.seconds, 11 );
        tags.putProperty( QDataSet.NAME, "time");
        
        MutablePropertyDataSet ds = (MutablePropertyDataSet) Ops.multiply( Ops.pow( Ops.replicate(1e5,11,3), Ops.randu(11,3) ), Ops.randu( 11, 3) );
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME,"B_GSM");
        
        MutablePropertyDataSet mode = (MutablePropertyDataSet) Ops.floor( Ops.multiply( Ops.randu( 11 ), Ops.replicate(4,11) ) );
        EnumerationUnits u= new EnumerationUnits("quality");
        u.createDatum( 0, "Good" );
        u.createDatum( 1, "Better" );
        u.createDatum( 2, "Best" );
        u.createDatum( 3, "Perfect" );
        mode.putProperty( QDataSet.UNITS, u );
        mode.putProperty( QDataSet.DEPEND_0, tags );
        mode.putProperty( QDataSet.NAME, "quality" );
        
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME,"B_GSM");
        
        
        MutablePropertyDataSet labels= (MutablePropertyDataSet) Ops.labelsDataset(new String[]{"B-GSM-X", "B-GSM-Y", "B-GSM-Z"});
        labels.putProperty(QDataSet.NAME, "dimLabels");
        ds.putProperty(QDataSet.DEPEND_1,labels );
        
        ds.putProperty( QDataSet.PLANE_0, mode );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test3.qds"), true );

        return ds;
    }
    
    private static QDataSet test4_rank3() throws ParseException, StreamException, IOException {
        DDataSet ds= (DDataSet) Ops.dindgen( 3, 4, 5 );
        ds.putValue( 1, 2, 3, 0.05 );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test4_rank3.qds"), true );
        return ds;
    }
    
    private static QDataSet test0_rank2()  throws ParseException, StreamException, IOException {
        DDataSet ds= (DDataSet) Ops.dindgen( 3, 4 );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test0_rank2.qds"), true );
        return ds;
    }
    
    private static QDataSet test5() throws StreamException, IOException {
        DDataSet ds= (DDataSet) Ops.dindgen( 5 );
        SimpleStreamFormatter format = new SimpleStreamFormatter();
        format.format( ds, new FileOutputStream("test5.qds"), true );
        return ds;
    }
    
    /**
     * "city skyline" dataset with mode changes.
     * @return
     * @throws org.das2.stream.StreamException
     * @throws java.io.IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     */
    private static QDataSet test6() throws StreamException, IOException {
        QDataSet result= null;
        result= Ops.join( result, Ops.dindgen( 5 ) );
        result= Ops.join( result, Ops.dindgen( 5 ) );
        result= Ops.join( result, Ops.dindgen( 5 ) );
        result= Ops.join( result, Ops.dindgen( 4 ) );
        result= Ops.join( result, Ops.dindgen( 4 ) );
        result= Ops.join( result, Ops.dindgen( 4 ) );
        result= Ops.join( result, Ops.dindgen( 4 ) );
        SimpleStreamFormatter format = new SimpleStreamFormatter();
        format.format( result, new FileOutputStream("test6.qds"), true );
        return result;
    }

    /**
     * test performance of formatting  200K records with 15 planes.
     * @return
     */
    private static QDataSet test7() throws StreamException, IOException {
        int nrec= 190000;
        
        long t0= System.currentTimeMillis();
        
        
        FDataSet result= FDataSet.createRank1( nrec );
        funData( result, 9.2, 0.01, 0, false );
        DDataSet dep0= DDataSet.createRank1( nrec );
        funData( dep0, 10000, 0.01, 0, true );
        result.putProperty( QDataSet.DEPEND_0, dep0 );
        for ( int i=0; i<13; i++ ) {
            FDataSet planeds= FDataSet.createRank1( nrec );
            funData( planeds, Math.random()*100, Math.random()*10, 0, false );
            planeds.putProperty( QDataSet.NAME, "myplane_"+i );
            result.putProperty( "PLANE_"+i, planeds );
        }
        System.err.println( "generated data in  "+ ( System.currentTimeMillis()-t0) );
        
        t0= System.currentTimeMillis();
        System.err.println( "formatting... " );
        
        SimpleStreamFormatter format = new SimpleStreamFormatter();
        format.format( result, new FileOutputStream("test7.qds"), false );
        
        System.err.println( "time: "+ ( System.currentTimeMillis()-t0) );
        
        return result;
    }

    private static void testBundle() throws StreamException, FileNotFoundException, IOException {
        QDataSet ds= BundleBinsDemo.demo1();
        SimpleStreamFormatter format = new SimpleStreamFormatter();
        format.format( ds, new FileOutputStream("testBundle.qds"), true );

    }

    private static void funData( WritableDataSet ds, double start, double res, int seed, boolean mono ) {
        Random rand= new Random(seed);
        if ( !mono ) {
            QubeDataSetIterator it= new QubeDataSetIterator(ds);
            while ( it.hasNext() ) {
                it.next();
                it.putValue( ds, start );
                start+= res * ( rand.nextDouble() - 0.5 );
            }
        } else {
            QubeDataSetIterator it= new QubeDataSetIterator(ds);
            while ( it.hasNext() ) {
                it.next();
                it.putValue( ds, start );
                start+= res;
            }            
        }
    }
}

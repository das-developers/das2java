/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.EnumerationUnits;
import org.das2.datum.Units;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;
import org.virbo.qstream.SimpleStreamFormatter;
import org.virbo.qstream.StreamException;

/**
 *
 * @author jbf
 */
public class SimpleStreamFormatterTest {

    public static void main(String[] args) throws ParseException, StreamException, FileNotFoundException, IOException, ParserConfigurationException {
        test6();
    }

    private static QDataSet test1() throws ParseException, StreamException, IOException, ParserConfigurationException {
        QDataSet ds = Ops.timegen("2003-09-09", "1 " + Units.days, 11);
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test1.qds"), true );
        return ds;
    }

    private static QDataSet test1_5() throws ParseException, StreamException, IOException, ParserConfigurationException {
        QDataSet ds= Ops.labels(new String[]{"B-GSM,X", "B-GSM,Y", "B-GSM,Z"});
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test1_5.qds"), true );
        return ds;
    }
    
    private static QDataSet test2() throws ParseException, StreamException, IOException, ParserConfigurationException  {
        MutablePropertyDataSet tags = (MutablePropertyDataSet) Ops.timegen("2003-09-09", "1 " + Units.days, 11);
        tags.putProperty( QDataSet.NAME, "time");
        MutablePropertyDataSet ds = (MutablePropertyDataSet) Ops.findgen(11, 3);
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME,"B_GSM");
        
        MutablePropertyDataSet labels= (MutablePropertyDataSet) Ops.labels(new String[]{"B-GSM-X", "B-GSM-Y", "B-GSM-Z"});
        labels.putProperty(QDataSet.NAME, "dimLabels");
        ds.putProperty(QDataSet.DEPEND_1,labels );
        
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test2.qds"), true );

        return ds;
    }
    
    
    private static QDataSet test3() throws ParseException, StreamException, IOException, ParserConfigurationException  {
        MutablePropertyDataSet tags = (MutablePropertyDataSet) Ops.timegen("2003-09-09", "13.86 " + Units.seconds, 11 );
        tags.putProperty( QDataSet.NAME, "time");
        
        MutablePropertyDataSet ds = (MutablePropertyDataSet) Ops.multiply( Ops.pow( Ops.replicate(1e5,11,3), Ops.rand(11,3) ), Ops.rand( 11, 3) );
        ds.putProperty(QDataSet.DEPEND_0, tags);
        ds.putProperty(QDataSet.NAME,"B_GSM");
        
        MutablePropertyDataSet mode = (MutablePropertyDataSet) Ops.floor( Ops.multiply( Ops.rand( 11 ), Ops.replicate(4,11) ) );
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
        
        
        MutablePropertyDataSet labels= (MutablePropertyDataSet) Ops.labels(new String[]{"B-GSM-X", "B-GSM-Y", "B-GSM-Z"});
        labels.putProperty(QDataSet.NAME, "dimLabels");
        ds.putProperty(QDataSet.DEPEND_1,labels );
        
        ds.putProperty( QDataSet.PLANE_0, mode );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test3.qds"), true );

        return ds;
    }
    
    private static QDataSet test4_rank3() throws ParseException, StreamException, IOException, ParserConfigurationException {
        DDataSet ds= (DDataSet) Ops.dindgen( 3, 4, 5 );
        ds.putValue( 1, 2, 3, 0.05 );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test4_rank3.qds"), true );
        return ds;
    }
    
    private static QDataSet test0_rank2()  throws ParseException, StreamException, IOException, ParserConfigurationException {
        DDataSet ds= (DDataSet) Ops.dindgen( 3, 4 );
        SimpleStreamFormatter format = new SimpleStreamFormatter();

        format.format( ds, new FileOutputStream("test0_rank2.qds"), true );
        return ds;
    }
    
    private static QDataSet test5() throws StreamException, IOException, ParserConfigurationException {
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
    private static QDataSet test6() throws StreamException, IOException, ParserConfigurationException {
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

}

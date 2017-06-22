/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.demos;

import org.das2.qds.AbstractDataSet;
import org.das2.qds.QDataSet;

/**
 * old das2 ripples dataset, but supports rank 1 and 2.
 * See Ops.ripples.
 * @author jbf
 */
public class RipplesDataSet extends AbstractDataSet {

    double x1,y1,p1;
    double x2,y2,p2;
    int xlen, ylen;
    int rank;
    double fill= -1e31;

    public RipplesDataSet( ) {
        this( 2, 3, 1, 13, 15, 2, 30, 30 );
    }

    public RipplesDataSet( int len0 ) {
        this( len0/10., len0/10., len0/20., len0/2., len0/2., len0/10., len0, len0 );
        this.rank=1;
    }

    public RipplesDataSet( int len0, int len1 ) {
        this( len0/10., len1/10., len1/20., len0/2., len1/2., len1/10., len0, len1 );
    }

    public RipplesDataSet( double x1, double y1, double p1, double x2, double y2, double p2, int xlength, int ylength ) {
        this.x1= x1;
        this.y1= y1;
        this.p1= p1;
        this.x2= x2;
        this.y2= y2;
        this.p2= p2;
        this.xlen= xlength;
        this.ylen= ylength;
        this.rank= 2;
        putProperty(QDataSet.QUBE,Boolean.TRUE);
        putProperty(QDataSet.FILL_VALUE,fill);
    }
    
    @Override
    public int rank() {
        return rank;
    }

    @Override
    public int length() {
        return xlen;
    }

    @Override
    public int length(int i) {
        return ylen;
    }

    @Override
    public double value(int i) {
        return value( i, xlen*130/400 );
    }

    @Override
    public double value(int i0, int i1) {
        double x= i0;
        double y= i1;
        if (12.<x && x<14.) {
            return fill;
        } else {
            double rad1= Math.sqrt((x-x1)*(x-x1)+(y-y1)*(y-y1));
            double exp1= Math.exp(-rad1/p1)*Math.cos(Math.PI*rad1/p1);
            double rad2= Math.sqrt((x-x2)*(x-x2)+(y-y2)*(y-y2));
            double exp2= Math.exp(-rad2/p2)*Math.cos(Math.PI*rad2/p2);
            double z= (exp1+exp2);
            //double z= (exp2);
            return z;
        }
    }


}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.demos;

import java.text.ParseException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import org.das2.qds.math.PoissonDistribution;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.util.DataSetBuilder;

/**
 * Model of plasma distribution function for given density, temperature, speed.
 * A java.util.Random object is passed in so that the data may be reproducible
 * (by using a given starting seed).
 * @author jbf
 */
public class PlasmaModel {

    private static class PlasmaModelSpec {

        double nc = 1.2; // core density 1/cm^3
        //double wcperp = 8000. * 1e5; // core thermal velocity cm/s
        double wcparl = 8000. * 1e5;
        double mass = 9.11e-28;
        boolean isotropic = true;
        double geomFactor = 1000e-40;

        PoissonDistribution poissonDistribution;
        
        public PlasmaModelSpec() {
            poissonDistribution= new PoissonDistribution();
        }

        public void setDensity(Datum density) {
            this.nc = density.doubleValue(Units.pcm3);
        }

        public Datum getDensity() {
            return Units.pcm3.createDatum(nc);
        }

        public double f(double energy, Units units) {
            if (units != Units.eV) {
                throw new IllegalArgumentException("units must be in eV");
            }
            if (!isotropic) {
                throw new IllegalArgumentException("distribution is not isotropic, need theta,phi");
            }
            double v = Math.sqrt(2 * energy * 1.6e-19 * 1e7 / mass);
            double logfc = Math.log10(nc / (Math.pow(Math.PI, (3. / 2.)) * wcparl * wcparl)) - 3 * Math.pow(v / wcparl, 2);
            return Math.pow(10,logfc);
        }

        public double fcounts(double energy, Units units, Random random) {
            if (units != Units.eV) {
                throw new IllegalArgumentException("units must be in eV");
            }
            double fcount = 2. * (energy / mass) * (energy / mass) * geomFactor * f(energy, units);
            return fcount;
        }

        public int counts(double energy, Units units, Random random) {
            if (units != Units.eV) {
                throw new IllegalArgumentException("units must be in eV");
            }
            double fcount = 2. * (energy / mass) * (energy / mass) * geomFactor * f(energy, units);
            return poissonDistribution.poisson(fcount, random);
        }
    }

    /**
     * return a rank 2 dataset with time as DEPEND_0 and energy as DEPEND_1.
     * @return 
     */
    public QDataSet getRank2( ) {

        try {
            PlasmaModelSpec model = new PlasmaModelSpec();
            Random random = new Random(5330);
            //System.err.println("First Random: "+random.nextDouble() ); //TODO: look into bug where this always causes hang in autoplot-test148
            //System.err.println("Java version: "+System.getProperty("java.version"));
            Units xunits = Units.us2000;
            Datum start = Units.us2000.parse("2000-017T00:00");
            Datum end = Units.us2000.parse("2000-018T00:00");
            double xTagWidth = Units.seconds.convertDoubleTo( Units.microseconds, 13.8 );
            double x = start.doubleValue(xunits);
            DataSetBuilder builder = new DataSetBuilder(2,1000,20);
            DataSetBuilder xx= new DataSetBuilder(1,1000);
            boolean ylog = false;
            DatumVector[] yTags = new DatumVector[1];
            Random s = new Random(234567); // repeatable random sequence

            double n= 2.0;
            //int irec=0;
            while (x < end.doubleValue(xunits)) {
                int whichYTags = s.nextInt(yTags.length);
                int nj;
                if (yTags[whichYTags] == null) {
                    nj = whichYTags * 10 + 20;
                    double[] yy = new double[nj];
                    for (int j = 0; j < nj; j++) {
                        if (ylog) {
                            yy[j] = (nj / 300) + j * 0.05; // findbugs okay ICAST_IDIV_CAST_TO_DOUBLE
                            yy[j] = Math.pow(10,yy[j]);
                        } else {
                            yy[j] = (nj / 3) + j * 1.2;
                        }
                    }
                    yTags[whichYTags] = DatumVector.newDatumVector(yy, Units.dimensionless);
                } else {
                    nj = yTags[whichYTags].getLength();
                }
                double[] zz = new double[nj];
                int ncol = s.nextInt(4) + 1;
                DatumVector ydv = yTags[whichYTags];
                for (int icol = 0; icol < ncol; icol++) {
                    double d=  random.nextDouble();
                    //if ( icol==0 && irec<150 ) {
                    //    System.err.println( String.format( "%d %d %5.4f", irun, irec, d ) ); //TODO: see above use at line 80.
                    //}
                    n= n * Math.pow(10, ( d-0.5 )/100 );
                    model.setDensity( Units.pcm3.createDatum(n) );
                    for (int j = 0; j < nj; j++) {
                        zz[j] = model.counts( ydv.get(j).doubleValue(Units.dimensionless), Units.eV, random );
                        builder.putValue( -1, j, zz[j] );
                    }
                    xx.putValue( -1, x );
                    x += xTagWidth;
                    //irec++;
                }
                builder.nextRecord();
                xx.nextRecord();
            }
            xx.putProperty( QDataSet.UNITS, Units.us2000 );
            builder.putProperty( QDataSet.DEPEND_0, xx.getDataSet() );
            DDataSet yy= DDataSet.wrap(yTags[0].toDoubleArray(Units.dimensionless));
            yy.putProperty(QDataSet.UNITS, Units.eV);
            builder.putProperty( QDataSet.DEPEND_1, yy );
            //System.err.println("Last Random: "+random.nextDouble() ); //TODO: see above use at line 80.
            return builder.getDataSet();
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
       
    }
//
//    public static Runnable getRunnable(final int irun) {
//        return new Runnable() {
//            @Override
//            public void run() {
//                QDataSet ds= new PlasmaModel().getRank2(irun);
//                System.err.println(ds);
//            }
//        };
//    }
//    public static void main(String[] args) {
//        for ( int i=0; i<4; i++ ) {
//            new Thread( getRunnable(i) ).start();
//        }
//    }
}

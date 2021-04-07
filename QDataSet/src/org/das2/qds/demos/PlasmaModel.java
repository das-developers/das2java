/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds.demos;

import java.text.ParseException;
import java.util.Random;
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

        private double nc = 1.2; // core density 1/cm^3
        
        private double wcperp = 8000. * 1e5; // core thermal velocity cm/s
        private double wcparl = 8000. * 1e5;
        
        //static final double mass = 511000; // MeV //9.11e-28;  // gram
        //static final double kappa = 8.617333262145e-5;  // eV / K
        private static final double mass = 9.11e-28;  // gram
        
        private boolean isotropic = true;
        private double geomFactor = 1000e-40;

        PoissonDistribution poissonDistribution;
        
        public PlasmaModel() {
            poissonDistribution= new PoissonDistribution();
        }

        /**
         * set the model density
         * @param density 
         */
        public void setDensity(Datum density) {
            this.nc = density.doubleValue(Units.pcm3);
        }

        /**
         * get the model density
         * @return 
         */
        public Datum getDensity() {
            return Units.pcm3.createDatum(nc);
        }

        /**
         * set the parallel speed
         * @param wcparl 
         */
        public void setWcparl(Datum wcparl) {
            this.wcparl = wcparl.doubleValue(Units.cmps);
        }

        /**
         * get the parallel speed.
         * @return 
         */
        public Datum getWcParl() {
            return Units.cmps.createDatum(this.wcparl);
        }

        /**
         * set the perpendicular speed
         * @param wcperp perpendicular speed
         */
        public void setWcPerp(Datum wcperp) {
            this.wcperp= wcperp.doubleValue(Units.cmps);
        }
        
        /**
         * get the perpendicular speed
         * @return the perpendicular speed
         */
        public Datum getWcPerp() {
            return Units.cmps.createDatum(this.wcperp);
        }

        /**
         * set the detector geometry factor
         * @param geom the detector geometry factor
         */
        public void setGeomFactor( Datum geom ) {
            this.geomFactor= geom.doubleValue(Units.dimensionless);
        }
        
        /**
         * get the detector geometry factor
         * @return the detector geometry factor
         */
        public Datum getGeomFactor( ) {
            return Units.dimensionless.createDatum(this.geomFactor);
        }
        
        /**
         * return f at the given energy
         * @param energy
         * @return 
         */
        public double f( Datum energy ) {
            double en= energy.doubleValue( Units.eV );
            if ( wcperp!=wcparl ) {
                throw new IllegalArgumentException("distribution is not isotropic, need pitch angle");
            }
            double v = Math.sqrt(2 * en * 1.6e-19 * 1e7 / mass);
            double logfc = Math.log10(nc / (Math.pow(Math.PI, (3. / 2.)) * wcparl * wcparl)) - 3 * Math.pow(v / wcparl, 2);
            return Math.pow(10,logfc);
        }

        public double f( Datum energy, Datum pitchAngle ) {
            double v = Math.sqrt(2 * energy.doubleValue(Units.eV) * 1.6e-19 * 1e7 / mass);
            double a = pitchAngle.doubleValue( Units.radians );
            double vparl= Math.cos(a) * v;
            double vperp= Math.sin(a) * v;
            
            //double kappa= 8.617333262145e-5;
            //double mass_2K= mass / ( 2 * kappa ); 
            //double f= nc * ( mass_2K / ( Math.PI * wcperp ) ) 
            //        * Math.sqrt( mass_2K / ( Math.PI * wcperp ) ) 
            //        * Math.exp( - ( mass_2K * Math.pow( vperp, 2 ) / wcperp ) )
            //        * Math.exp( - ( mass_2K * Math.pow( vparl, 2 ) / wcparl ) );
            //return f;
            double logfc = Math.log10(nc / (Math.pow(Math.PI, (3. / 2.)) * wcparl * wcperp)) 
                    - Math.pow(vparl / wcparl, 2) - 2*Math.pow(vperp / wcperp, 2);
            return Math.pow(10,logfc);
        }
        
        /**
         * return the counts at this energy, assuming an isotropic distribution.  No
         * Poisson noise is added to the output.
         * @param energy
         * @return 
         */
        public double fcounts( Datum energy) {
            double en= energy.doubleValue(Units.eV);
            double fcount = 2. * (en / mass) * (en / mass) * geomFactor * f(energy);
            return fcount;
        }

        /**
         * return the counts at this energy, assuming an isotropic distribution, and
         * Poisson noise is added to the result.
         * @param energy in eV
         * @param random source of random numbers.
         * @return 
         */
        public int counts( Datum energy, Random random) {
            double en= energy.doubleValue(Units.eV);
            double fcount = 2. * (en / mass) * (en / mass) * geomFactor * f(energy);
            return poissonDistribution.poisson(fcount, random);
        }
        
        /**
         * return the counts at this energy and pitch angle, without Poisson noise added to the result.
         * @param energy
         * @param pitch
         * @return the floating point count rate.
         */
        public double fcounts( Datum energy, Datum pitch ) {
            double f= f(energy, pitch);
            double fcount = f * 2. * Math.pow( energy.doubleValue( Units.eV) / mass, 2 ) * geomFactor;
            return fcount;
        }

        /**
         * return the counts at this energy and pitch angle, with Poisson noise added to the result.
         * @param energy
         * @param pitch
         * @param random random number source
         * @return the count rate
         */
        public int counts(Datum energy, Datum pitch, Random random) {
            double fcount = fcounts( energy, pitch );
            return poissonDistribution.poisson(fcount, random);
        }

    /**
     * return a rank 2 dataset with time as DEPEND_0 and energy as DEPEND_1.
     * @return 
     */
    public QDataSet getRank2( ) {

        try {
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
                    setDensity( Units.pcm3.createDatum(n) );
                    for (int j = 0; j < nj; j++) {
                        zz[j] = counts( ydv.get(j), random );
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
       
    /**
     * return a rank 2 dataset with time as DEPEND_0 and energy as DEPEND_1.
     * @return 
     */
    public QDataSet getRank3( ) {

        try {
            Random random = new Random(5330);
            //System.err.println("First Random: "+random.nextDouble() ); //TODO: look into bug where this always causes hang in autoplot-test148
            //System.err.println("Java version: "+System.getProperty("java.version"));
            Datum start = Units.us2000.parse("2000-017T00:00");
            Datum end = Units.us2000.parse("2000-018T00:00");
            Datum xTagWidth = Units.seconds.createDatum(13.8);
            Datum t = start;
            DataSetBuilder builder = new DataSetBuilder(3,1000,20,18);
            DataSetBuilder xx= new DataSetBuilder(1,1000);
            boolean ylog = false;
            DatumVector[] energyTags = new DatumVector[1];
            DatumVector[] alphaTags = new DatumVector[1];
            Random s = new Random(234567); // repeatable random sequence

            double n= 2.0;
            //int irec=0;
            while ( t.lt(end) ) {
                int whichTags = s.nextInt(energyTags.length);
                int ne;
                if (energyTags[whichTags] == null) {
                    ne = whichTags * 10 + 20;
                    double[] en = new double[ne];
                    for (int j = 0; j < ne; j++) {
                        if (ylog) {
                            en[j] = (ne / 300) + j * 0.05; // findbugs okay ICAST_IDIV_CAST_TO_DOUBLE
                            en[j] = Math.pow(10,en[j]);
                        } else {
                            en[j] = (ne / 3) + j * 1.2;
                        }
                    }
                    energyTags[whichTags] = DatumVector.newDatumVector(en, Units.eV);
                } else {
                    ne = energyTags[whichTags].getLength();
                }
                int na;
                if (alphaTags[whichTags] == null) {
                    na = 18;
                    double[] yy = new double[na];
                    for (int j = 0; j < na; j++) {
                        yy[j]= Math.PI * ( 0.5 + j ) / na ;
                    }
                    alphaTags[whichTags] = DatumVector.newDatumVector(yy, Units.radians );
                } else {
                    na = alphaTags[whichTags].getLength();
                }
                
                int ncol = s.nextInt(4) + 1;
                DatumVector ydv = energyTags[whichTags];
                DatumVector adv = alphaTags[whichTags];
                for (int icol = 0; icol < ncol; icol++) {
                    double d=  random.nextDouble();
                    //if ( icol==0 && irec<150 ) {
                    //    System.err.println( String.format( "%d %d %5.4f", irun, irec, d ) ); //TODO: see above use at line 80.
                    //}
                    n= n * Math.pow(10, ( d-0.5 )/100 );
                    setDensity( Units.pcm3.createDatum(n) );
                    for (int j = 0; j < ne; j++) {
                        for ( int k=0; k<na; k++ ) {
                            double zz = counts( ydv.get(j), adv.get(k), random );
                            builder.putValue( -1, j, k, zz );
                        }
                        
                    }
                    xx.putValue( -1, t );
                    t= t.add( xTagWidth );
                    //irec++;
                }
                builder.nextRecord();
                xx.nextRecord();
            }
            xx.putProperty( QDataSet.UNITS, Units.us2000 );
            builder.putProperty( QDataSet.DEPEND_0, xx.getDataSet() );
            DDataSet yy= DDataSet.wrap(energyTags[0].toDoubleArray(Units.eV));
            yy.putProperty(QDataSet.UNITS, Units.eV);
            DDataSet aa= DDataSet.wrap(alphaTags[0].toDoubleArray(Units.radians));
            builder.putProperty( QDataSet.DEPEND_1, yy );
            builder.putProperty( QDataSet.DEPEND_2, aa );
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

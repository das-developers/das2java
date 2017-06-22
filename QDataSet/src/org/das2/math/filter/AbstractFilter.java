/**
 * Taken from GPL code from within JavaSpeechToolkit 
 * http://code.google.com/p/jstk/source/browse/trunk/jstk/src/de/fau/cs/jstk/sampled/filters/Butterworth.java?r=176
 */
/*
        Copyright (c) 2011
                Speech Group at Informatik 5, Univ. Erlangen-Nuremberg, GERMANY
                Korbinian Riedhammer

        This file is part of the Java Speech Toolkit (JSTK).

        The JSTK is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        The JSTK is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with the JSTK. If not, see <http://www.gnu.org/licenses/>.
*/

package org.das2.math.filter;

import java.util.Arrays;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.FlattenWaveformDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * Java Speech Toolkit code modified to use QDataSets.
 * @author jbf
 */
public class AbstractFilter {
    QDataSet source;
    
    AbstractFilter( QDataSet source ) {
        this.source= source;
    }
    
    /**
     * return the units of the timetags of the dataset, as an offset.
     * @return the units of the timetags of the dataset, as an offset.
     */
    protected Units getTimeOffsetUnits() {
        QDataSet timeDomainTags= (QDataSet) source.property(QDataSet.DEPEND_0);
        Units u= SemanticOps.getUnits(timeDomainTags);
        return u.getOffsetUnits();
    }
    
    /**
     * return the sample rate in the given units.
     * @param source the signal, containing DEPEND_0 timetags.
     * @param units the target units.
     * @return the sample rate in the given units. (E.g. Units.hertz)
     */
    protected double getSampleRate( QDataSet source, Units units ) {
        QDataSet timeDomainTags= (QDataSet) source.property(QDataSet.DEPEND_0);
        Units u= getTimeOffsetUnits();
        u= UnitsUtil.getInverseUnit(u);
        return u.convertDoubleTo( units,  1 / ( timeDomainTags.value(1) - timeDomainTags.value(0) ) );
    }
    
        /** i/o pointer for input signal ringbuffer */
        private int px;
       
        /** i/o pointer for output signal ringbuffer */
        private int py;
       
        /** input signal ringbuffer */
        private double [] xv;
       
        /** output signal ringbuffer */
        private double [] yv;
       
        /** filter coefficients A (applied to prevously filtered signal) */
        private double [] a;
       
        /** filter coefficients B (applied to input signal */
        private double [] b;

        
        /**
         * Set the filter coefficients stored in Matlab style. If a[0] not equal 1,
         * the filter coefficients are normalized by a[0].
         *
         * @param b
         * @param a
         */
        public void setCoefficients(double [] b, double [] a) {
                this.b = Arrays.copyOf(b,b.length);
                this.a = Arrays.copyOf(a,a.length);
               
                xv = new double [ this.b.length + 1 ];
                yv = new double [ this.a.length + 1 ];
               
                if (this.a[0] == 1.) {
                        for (int i = 1; i < this.a.length; ++i)
                                this.a[i] /= this.a[0];
                        for (int i = 0; i < this.b.length; ++i)
                                this.b[i] /= this.a[0];
                }
        }
       
        public QDataSet filter() {
            if ( source.rank()==2 ) {
                source= new FlattenWaveformDataSet(source);
            }

            ArrayDataSet buf= ArrayDataSet.copy(source);
            
                // apply the filter
                for (int i = 0; i < source.length(); ++i) {
                        // get the new sample
                        xv[px] = source.value(i);
                       
                        // compute the output
                        buf.putValue(i, b[0] * xv[px]);
                        for (int j = 1; j < b.length; ++j)
                                buf.putValue( i, buf.value(i) + ( b[j] * xv[(px - j + xv.length) % xv.length ] ) );
                       
                        for (int j = 1; j < a.length; ++j)
                                buf.putValue( i, buf.value(i) - ( a[j] * yv[(py - j + yv.length) % yv.length] ) );
                       
                        // save the result
                        yv[py] = buf.value(i);
                       
                        // increment the index of the ring buffer
                        px = (px + 1) % xv.length;
                        py = (py + 1) % yv.length;
                }
                
                return buf;
        }

       
    @Override
        public String toString() {
                return "b = " + Arrays.toString(b) + " a = " + Arrays.toString(a);
        }
    
}

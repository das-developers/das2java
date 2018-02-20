package org.das2.qds.buffer;

import java.nio.ByteBuffer;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.FloatReadAccess;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;

public class FloatDataSet extends BufferDataSet implements WritableDataSet {

    public FloatDataSet( int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back ) {
        super( rank, reclen, recoffs, len0, len1, len2, len3, FLOAT, back );
    }

    public double value() {
        return back.getFloat(offset());
    }

    public double value(int i0) {
        return back.getFloat(offset(i0));
    }

    public double value(int i0, int i1) {
        return back.getFloat(offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return back.getFloat(offset(i0, i1, i2));
    }
    
    public double value(int i0, int i1, int i2, int i3) {
        return back.getFloat(offset(i0, i1, i2,i3));
    }

    /**
     * check for fill as well, since often numerical noise will corrupt 
     * the fill values.
     * @param name the property name
     * @param value the property value
     */
    @Override
    public void putProperty(String name, Object value) {
        if ( name.equals(QDataSet.UNITS) ) {
            if ( UnitsUtil.isTimeLocation( (Units)value ) ) {
                logger.warning("floats are being used to store times, which typically lacks sufficient resolution to represent data.");
            }
        }
        super.putProperty(name, value);
        //if ( name.equals(QDataSet.FILL_VALUE) ) checkFill(); // because of rounding errors
    }
    
    public void putValue(double d) {
        ensureWritable();
        back.putFloat( offset(), (float)d );
    }

    public void putValue(int i0, double d) {
        ensureWritable();
        back.putFloat( offset(i0), (float)d );
    }

    public void putValue(int i0, int i1, double d) {
        ensureWritable();
        back.putFloat( offset(i0, i1), (float)d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ensureWritable();
        back.putFloat( offset(i0, i1, i2), (float)d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ensureWritable();
        back.putFloat( offset(i0, i1, i2, i3), (float)d );
    }

    /**
     * Clients should use this instead of casting the class to the 
     * capability class.
     * @param <T>
     * @param clazz the class, such as WritableDataSet.class
     * @return null or the capability if exists, such as WritableDataSet
     */
    @Override
    public <T> T capability(Class<T> clazz) {
        if ( clazz==WritableDataSet.class ) {
            if ( isImmutable() ) {
                return null;
            } else {
                return (T) this;
            }
        } else if ( clazz==FloatReadAccess.class ) {
            return clazz.cast( new FloatDataSet.FloatDataSetFloatReadAccess() );
        } else {
            return super.capability(clazz);
        }
    }
    
    public class FloatDataSetFloatReadAccess implements FloatReadAccess {

        @Override
        public float fvalue() {
            return back.getFloat( offset() );
        }

        @Override
        public float fvalue(int i0) {
            return back.getFloat( offset(i0) );
        }

        @Override
        public float fvalue(int i0, int i1) {
            return back.getFloat( offset(i0, i1) );
        }

        @Override
        public float fvalue(int i0, int i1, int i2) {
            return back.getFloat( offset(i0, i1, i2) );
        }

        @Override
        public float fvalue(int i0, int i1, int i2, int i3) {
            return back.getFloat( offset(i0, i1, i2, i3) );
        }
        
    }
    
}

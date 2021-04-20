
package org.das2.qds;

/**
 * reverses the order of the elements of the dataset.  If there is a DEPEND_0,
 * high-rank DEPEND properties, or BUNDLE_0, they are reversed as well.
 * @author jbf
 */
public class ReverseDataSet extends AbstractDataSet {

    QDataSet source;
    int len;
    int lastIndex;

    public ReverseDataSet( QDataSet source ) {
        this.source= source;
        this.len= source.length();
        this.lastIndex= len-1;
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public double value() {
        return source.value(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double value(int i) {
        return source.value( lastIndex-i );
    }

    @Override
    public double value(int i0, int i1) {
        return source.value( lastIndex-i0,i1 );
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return source.value( lastIndex-i0,i1,i2 );
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return source.value( lastIndex-i0,i1,i2,i3 );
    }
    
    @Override
    public int rank() {
        return source.rank();
    }

    @Override
    public Object property(String name) {
        if ( name.equals(QDataSet.DEPEND_0) ) {
            QDataSet dep0= (QDataSet) source.property(QDataSet.DEPEND_0);
            if ( dep0!=null ) {
                return new ReverseDataSet(dep0);
            } else {
                return null;
            }
        } else if ( name.startsWith("DEPEND_") ) {
            QDataSet dep= (QDataSet)source.property(name);
            if ( dep!=null && dep.rank()>1 ) {
                return new ReverseDataSet(dep);
            } else {
                return dep;
            }
        } else if ( name.startsWith("BUNDLE_0" ) ) {
            QDataSet bds= (QDataSet)source.property(name);
            if ( bds!=null ) {
                return new ReverseDataSet(bds);
            } else {
                return null;
            }
        } else {
            if ( this.properties.containsKey(name) ) {
                return this.properties.get(name);
            } else {
                return source.property(name);
            }
        }
    }

    @Override
    public Object property(String name, int i) {
        return super.property(name, len-1-i);
    }
    
    


}

package org.das2.qds;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.util.LoggerManager;
import org.das2.qds.ops.Ops;

/**
 * Extracts a subset of the source dataset by using a rank 1 subset of indeces on each index.
 * @author jbf
 */
public class SubsetDataSet extends AbstractDataSet {

    private static final Logger logger= LoggerManager.getLogger("qdataset");

    QDataSet source;

    QDataSet[] sorts;
    int[] lens;

    boolean nonQube=false;

    private static List<Integer> indgen( int start, int stop, int stride ) {
        ArrayList<Integer> result= new ArrayList<>();
        for ( int i=start; i<stop; i+=stride ) {
            result.add(i);
        }
        return result;
    } 
    
    /**
     * parse the string spec into a list of indices.  The spec is a 
     * comma-delineated list containing any combination of:<ul>
     * <li>index, with negative indices relative to the end.
     * <li>start:stop, with stop exclusive.
     * <li>start:stop:stride, incrementing stride elements, including negative.
     * <li>start-stopInclusive, where the trailing index is also included.
     * </ul>
     * If the spec starts with ~, then these indices are removed. For example:<ul>
     * <li>~5, remove the 5th index.
     * <li>~15:20, remove the 5 indices starting at 15.
     * <li>~-1, remove the last index.
     * </ul>
     * Note if invert is present, then the indices cannot be reversed.
     * @param spec
     * @param dimlen, the amount added to negative indices.
     * @return the list of integers.
     * @throws ParseException 
     */
    public static int[] parseIndices( String spec, int dimlen ) throws ParseException {
        Pattern p1= Pattern.compile("(\\-?\\d+)\\-(\\-?\\d+)");
        Pattern p2= Pattern.compile("(\\-?\\d+)?\\:(\\-?\\d+)?(\\:(\\-?\\d+)?)?");
        
        boolean invert= spec.length()>1 && spec.charAt(0)=='~';
        if ( invert ) {
            spec= spec.substring(1);
        }
        
        String[] ss= spec.split(",");
        
        List<Integer> result= new ArrayList<>();
        
        int charPos= 0;
        for (String s : ss) {
            Matcher m = p2.matcher(s);
            if (m.matches()) {
                int start= m.group(1)==null ? 0 : Integer.parseInt(m.group(1));
                if ( start<0 ) start+= dimlen;
                int stop= m.group(2)==null ? dimlen : Integer.parseInt(m.group(2));
                if ( stop<0 ) stop+= dimlen;
                int stride= m.group(4)==null ? 1 : Integer.parseInt(m.group(4));
                List<Integer> ii= indgen( start, stop, stride );
                result.addAll( ii );
            } else {
                m = p1.matcher(s);
                if (m.matches()) {
                    int start= m.group(1)==null ? 0 : Integer.parseInt(m.group(1));
                    if ( start<0 ) start+= dimlen;
                    int stop= m.group(2)==null ? dimlen : Integer.parseInt(m.group(2));
                    if ( stop<0 ) stop+= dimlen;
                    List<Integer> ii;
                    if ( start>stop ) {
                        ii= indgen( stop, start+1, 1 );
                    } else {
                        ii= indgen( start, stop+1, 1 );
                    }
                    result.addAll( ii );
                } else {
                    try {
                        int ii = Integer.parseInt(s);
                        if ( ii<0 ) ii+=dimlen;
                        result.add(ii);
                    } catch ( NumberFormatException ex ) {
                        throw new ParseException("unable to parse: "+s,charPos);
                    }
                }
            }
            charPos+= 1+s.length();
        }
        int[] iresult;
        if ( invert ) {
            iresult= new int[dimlen-result.size()];
            Collections.sort(result);
            int resultIndex= 0;
            int outputIndex= 0;
            int ii= result.get(resultIndex);
            for ( int i=0; i<dimlen; i++ ) {
                if ( i==ii ) {
                    resultIndex++;
                    if ( resultIndex==result.size() ) {
                        ii= Integer.MAX_VALUE;
                    } else {
                        ii= result.get(resultIndex);
                    }
                } else {
                    iresult[outputIndex]= i;
                    outputIndex++;
                }
            }
        } else {
            iresult= new int[result.size()];
            for ( int i=0; i<result.size(); i++ ) {
                iresult[i]= result.get(i);
            }
        }
        return iresult;
    }
    
    /**
     * create a subSetDataSet for the source, which is read for applyIndex calls
     * which reduce each index.
     * @param source 
     */
    public SubsetDataSet( QDataSet source ) {
        this.source= source;
        sorts= new QDataSet[ source.rank() ];
        lens= new int[ source.rank() ];
        if ( !DataSetUtil.isQube(source) ) {
            nonQube= true;
        } else { // flatten the qube immediately, because we are seeing this with FFTPower output.
            QDataSet dep1= (QDataSet) source.slice(0).property(QDataSet.DEPEND_0);
            putProperty( QDataSet.DEPEND_1, dep1 );
        }
        int[] lenss= DataSetUtil.qubeDims(source);
        if ( nonQube ) {
            lens[0]= source.length();
            sorts[0]= new IndexGenDataSet(lens[0]);
            for ( int i=1; i<source.rank(); i++ ) {
                lens[i]= Integer.MAX_VALUE;
                sorts[i]= new IndexGenDataSet(lens[i]);
            }
        } else {
            for ( int i=0; i<lenss.length; i++ ) {
                lens[i]= lenss[i];
                sorts[i]= new IndexGenDataSet(lenss[i]);
            }
        }
    }

    /**
     * apply the subset indexes to a given dimension.  For example,
     * if a=[10,20,30,40] then applyIndex( 0, [1,2] ) would result in [20,30].
     * @param idim
     * @param idx the rank 1 index list, for example from where on a rank 1 dataset.
     */
    public void applyIndex( int idim, QDataSet idx ) {
        if ( nonQube && idim>0 ) throw new IllegalArgumentException("unable to applyIndex on non-qube source dataset");
        if ( idx.rank()==1 ) {
            QDataSet max= Ops.reduceMax( idx, 0 );
            if ( max.value()>=lens[idim] ) {
                logger.log(Level.WARNING, "idx dataset contains maximum that is out-of-bounds: {0}", max);
            }
        }
        sorts[idim]= idx;
        lens[idim]= idx.length();
        if ( idx.rank()>1 ) {
            throw new IllegalArgumentException("indexes must be rank 1");
        }
        QDataSet dep= (QDataSet)property( "DEPEND_"+idim );
        if ( dep==null ) {
            dep= (QDataSet) source.property( "DEPEND_"+idim );
        }
        if ( dep!=null ) {
            SubsetDataSet dim= new SubsetDataSet( dep );
            switch (dim.rank()) {
                case 1:
                    dim.applyIndex(0,idx);
                    break;
                case 2:
                    dim.applyIndex(1,idx);
                    break;
                case 3:
                    dim.applyIndex(2,idx);
                    break;
                default:
                    throw new IllegalArgumentException("DEPEND_"+idim+" must be rank 1 or rank 2");
            }
            putProperty("DEPEND_"+idim,dim);
        }
        for ( int i=idim+1; i<source.rank(); i++ ) { // rfe670: high-rank DEPEND_2
            dep= (QDataSet)property("DEPEND_"+i);
            if ( dep!=null && dep.rank()>=2 ) {
                SubsetDataSet dim= new SubsetDataSet( dep );
                dim.applyIndex(idim,idx);
                putProperty("DEPEND_"+i,dim);
            }
        }
        if ( idim==0 ) { // DEPEND_1-4 can be rank 2, where the 0th dimension corresponds to DEPEND_0.
            for ( int i=1; i<QDataSet.MAX_RANK; i++ ) {
                QDataSet depi= (QDataSet)source.property("DEPEND_"+i); // note this is not a qube...
                if ( depi!=null && depi.rank()>1 ) {
                    SubsetDataSet dim= new SubsetDataSet( depi );
                    dim.applyIndex( 0, idx );
                    putProperty("DEPEND_"+i, dim );
                }
            }
        }
        
        // support subset to get a subset of bundled datasets.
        QDataSet bundle= (QDataSet)property( "BUNDLE_"+idim );
        if ( bundle==null ) {
            bundle= (QDataSet) source.property( "BUNDLE"+idim );
        }
        if ( bundle!=null ) {
            SubsetDataSet b= new SubsetDataSet(bundle);
            b.applyIndex( 0, idx );
            putProperty( "BUNDLE_"+idim, b );
        }
    }

    @Override
    public int rank() {
        return source.rank();
    }

    @Override
    public int length() {
        return lens[0];
    }

    @Override
    public int length(int i) {
        return nonQube ? source.length(i) : lens[1];
    }

    @Override
    public int length(int i, int j) {
        return nonQube ? source.length(i,j) : lens[2];
    }

    @Override
    public int length(int i, int j, int k) {
        return nonQube ? source.length(i,j,k) : lens[3];
    }

    @Override
    public double value() {
        return source.value();
    }

    @Override
    public double value(int i) {
        return source.value((int)sorts[0].value(i));
    }

    @Override
    public double value(int i0, int i1) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1),(int)sorts[2].value(i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return source.value((int)sorts[0].value(i0),(int)sorts[1].value(i1),(int)sorts[2].value(i2),(int)sorts[3].value(i3));
    }

    @Override
    public Object property(String name, int i) {
        Object v= properties.get(name);
        return v!=null ? v : source.property(name, ((int)sorts[0].value(i)) );
    }

    @Override
    public Object property(String name) {
        Object v= properties.get(name);
        return v!=null ? v : source.property(name);
    }

}

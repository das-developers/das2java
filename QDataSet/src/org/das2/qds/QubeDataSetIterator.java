/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.qds.ops.Ops;

/**
 * DataSetIterator implementation that can be used for all dataset (not just qubes).  
 * Originally this only worked for QDataSets that were qubes, or datasets that 
 * had the same dataset geometry for each slice.  At some point this was 
 * modified to work with any dataset but the name remains.
 * 
 * DataSetIterators are intended to work with multiple datasets at once.  For example,
 * if we want to add the data from two datasets together, we would create one 
 * iterator that would be used to access both datasets.  One dataset is provided
 * to the constructor, but any dataset of the same geometry can be passed to the
 * getValue method.
 * 
 * TODO: This does not work for Rank 0 datasets.  See
 * sftp://klunk.physics.uiowa.edu/home/jbf/project/autoplot/script/demos/jeremy/qubeDataSetIteratorForNonQubes.jy
 * 
 * @author jbf
 */
public final class QubeDataSetIterator implements DataSetIterator {

    private static final Logger logger= LoggerManager.getLogger("qdataset.iterator");
    
    /**
     * DimensionIterator iterates over an index.  For example, using 
     * Jython for brevity:
     *<blockquote><pre><small>{@code
     * ds= zeros(15,4,2)
     * ds[:,:,:] has itertors that count of 0,...,14; 0,...,3; and 0,1
     * ds[3:15,:,:]  uses a StartStopStepIterator to count off 3,4,5,...,14
     * ds[3,:,:]  uses a SingletonIterator
     * i1= [0,1,2,3]
     * i2= [0,0,1,1]
     * i3= [0,1,0,1]
     * ds[i1,i2,i3]  # uses IndexListIterator
     *}</small></pre></blockquote>
     */
    public interface DimensionIterator {

        /**
         * true if there are more indeces in the iteration
         * @return true if there are more indeces in the iteration
         */
        boolean hasNext();

        /**
         * return the next index of the iteration
         * @return the next index of the iteration
         */
        int nextIndex();

        /**
         * return the current index.
         * @return the current index.
         */
        int index();

        /**
         * return the length of the iteration.
         * @return the length of the iteration.
         */
        int length();
    }

    /**
     * DimensionIteratorFactory creates DimensionIterators
     */
    public interface DimensionIteratorFactory {
        DimensionIterator newIterator(int len);
    }

    /**
     * Iterator for counting off indeces.  (3:15:2 in ds[3:15:2,:])
     */    
    public static class StartStopStepIterator implements DimensionIterator {

        int start;
        int stop;
        int step;
        int index;
        boolean all; // just for toString

        public StartStopStepIterator(int start, int stop, int step, boolean all) {
            this.start = start;
            this.stop = stop;
            this.step = step;
            this.index = start - step;
            this.all = all;
        }

        @Override
        public boolean hasNext() {
            return index + step < stop;
        }

        @Override
        public int nextIndex() {
            index += step;
            return index;
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int length() {
            int remainder= (stop - start) % step;
            return (stop - start) / step + ( remainder>0 ? 1 :0 );
        }

        @Override
        public String toString() {
            return all ? ":" : "" + start + ":" + stop + (step == 1 ? "" : ":" + step);
        }
    }

    /**
     * generates iterator for counting off indeces.  (3:15:2 in ds[3:15:2,:])
     */
    public static class StartStopStepIteratorFactory implements DimensionIteratorFactory {

        Number start;
        Number stop;
        Number step;

        /**
         * create the factory which will create iterators.
         * @param start the start index.  
         * @param stop the stop index, exclusive.  null (or None) is used to indicate the end of non-qube datasets.
         * @param step the step size, null means just use 1.
         */
        public StartStopStepIteratorFactory(Number start, Number stop, Number step) {
            this.start = start;
            this.stop = stop;
            this.step = step;
        }

        @Override
        public DimensionIterator newIterator(int length) {
            int start1 = start == null ? 0 : start.intValue();
            int stop1 = stop == null ? length : stop.intValue();
            int step1 = step == null ? 1 : step.intValue();
            if (start1 < 0) {
                start1 = length + start1;
            }
            if (stop1 < 0) {
                stop1 = length + stop1;
            }

            return new StartStopStepIterator(start1, stop1, step1, start == null && stop == null && step == null);
        }
    }

    /**
     * Iterator that goes through a list of indeces.
     */
    public static class IndexListIterator implements DimensionIterator {

        QDataSet ds;
        int index;

        public IndexListIterator(QDataSet ds) {
            if ( ds.rank()==0 ) {
                ds= Ops.join(null,ds);
            }
            this.ds = ds;
            if ( ds.rank()!=1 ) {
                throw new IllegalArgumentException("list of indeces dataset must be rank 1");
            }
            this.index = -1;
        }

        @Override
        public boolean hasNext() {
            return index < ds.length()-1;
        }

        @Override
        public int nextIndex() {
            index++;
            return (int) ds.value(index);
        }

        @Override
        public int index() {
            return (int) ds.value(index);
        }

        @Override
        public int length() {
            return ds.length();
        }

        @Override
        public String toString() {
            String dstr= ds.toString();
            dstr= dstr.replace("(dimensionless)", "");
            return "[" + dstr  + " @ " +index + "]";
        }
    }

    /**
     * return the current line in the Jython script as &lt;filename&gt;:&lt;linenum&gt;
     * or ??? if this cannot be done.  Note calls to this will collect a stack
     * trace and will affect performance.
     * @return the current line or ???
     * @see JythonOps#currentLine()
     */
    public static String currentJythonLine() {
        StackTraceElement[] sts= new Exception().getStackTrace();
        int i= 0;
        while ( i<sts.length ) {
            if ( sts[i].getClassName().startsWith("org.python.pycode") ) {
                return sts[i].getFileName()+":"+ sts[i].getLineNumber();
            }
            i=i+1;
        }
        return "???";
    }

    /**
     * Factory for iterator that goes through a list of indeces.
     */
    public static class IndexListIteratorFactory implements DimensionIteratorFactory {

        QDataSet ds;

        public IndexListIteratorFactory(QDataSet ds) {
            if ( ds.rank()==0 ) {
                ds= Ops.join(null,ds);
            }
            this.ds = ds;
            if ( ds.rank()!=1 ) {
                throw new IllegalArgumentException("list of indeces dataset must be rank 1");
            }
            // check the first hundred indeces to see if they are non-integer.
            for ( int i=0; i<Math.min( ds.length(), 100 ); i++ ) {
                double v= ds.value(i);
                if ( v!=Math.floor(v) ) {
                    String jythonLine= currentJythonLine();
                    if ( jythonLine.equals("???") ) {
                        logger.warning( "indices should be integers, which might indicate there's a bug" );
                    } else {
                        logger.log(Level.WARNING, "indices should be integers, which might indicate there''s a bug at {0}", jythonLine);
                    }
                    break;
                }
            }
        }

        @Override
        public DimensionIterator newIterator(int length) {
            ArrayDataSet list= ArrayDataSet.copy(ds);
            for ( int i=0; i<list.length(); i++ ) {
                if ( list.value(i)<0 ) list.putValue(i,list.value(i)+length);
            }
            return new IndexListIterator(list);
        }
        
        public QDataSet getList() {
            return this.ds;
        }
        
        /**
         * return the maximum possible index, plus one, or -1 if this is not declared.  
         * This will be the VALID_MAX of ds.  This is to support rfe737, where the
         * "where" command can keep track of the size of the data it tested.
         * For example, for where(findgen(10).lt(5)) would return "10" here, because
         * findgen(10) has 10 elements.
         * @return -1 or 
         */
        private int getMaxIndexExclusive() {
            Number validMax= (Number)ds.property(QDataSet.VALID_MAX);
            if ( validMax!=null ) {
                return validMax.intValue();
            } else {
                return -1;
            }
        }
    }

    /**
     * Iterator for a single index.
     */
    public static class SingletonIterator implements DimensionIterator {

        int index;
        boolean hasNext = true;

        public SingletonIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public int nextIndex() {
            hasNext = false;
            return index;
        }

        @Override
        public int index() {
            return index;
        }

        @Override
        public int length() {
            return 1;
        }

        @Override
        public String toString() {
            return "" + index;
        }
    }

    /**
     * Factory for iterator for a single index.
     */
    public static class SingletonIteratorFactory implements DimensionIteratorFactory {

        int index;

        public SingletonIteratorFactory(int index) {
            this.index = index;
        }

        @Override
        public DimensionIterator newIterator(int length) {
            if ( index<0 ) {
                return new SingletonIterator(length+index);
            } else {
                return new SingletonIterator(index);
            }
            
        }
    }
    
    private DimensionIterator[] it;
    private DimensionIteratorFactory[] fit;
    private boolean isAllIndexLists;

    private int rank;
    private int[] qube;
    private QDataSet ds;
    private boolean allnext = true;  // we'll have to do a borrow to get started.
    
    private ProgressMonitor monitor;

    /**
     * dataset iterator to help in implementing the complex indexing
     * types of python.  Each of the dimensions is set to iterate over all
     * indeces (e.g ds[:,:,:])
     * 
     * Client codes should create a new iterator, set the index iterator factories, then iterate.
     * @param ds the dataset we will iterate over.
     */
    public QubeDataSetIterator( QDataSet ds ) {
        logger.log(Level.FINE, "new dataset iterator for {0}", ds);
        List<String> problems= new ArrayList<>();
        if ( ! DataSetUtil.validate(ds,problems) ) {
            throw new IllegalArgumentException("data doesn't validate: "+problems );
        }
        it= new DimensionIterator[ ds.rank() ];
        fit= new DimensionIteratorFactory[ ds.rank() ];
        
        if ( DataSetUtil.isQube(ds) ) {
            this.qube = DataSetUtil.qubeDims(ds);
            this.ds = ds;
        } else {
            this.ds = ds;
        }
        this.rank = ds.rank();
        for (int i = 0; i < ds.rank(); i++) {
            fit[i] = new StartStopStepIteratorFactory(0, null, 1);
        }
        initialize();
    }

    /**
     * internal constructor.  Note we have to have both constructors, which must
     * be kept in sync, because of initialize().
     * @param ds
     * @param fits
     */
    private QubeDataSetIterator(QDataSet ds, DimensionIteratorFactory[] fits) {
        if (Boolean.TRUE.equals(ds.property(QDataSet.QUBE))) {
            this.qube = DataSetUtil.qubeDims(ds);
            this.ds = ds;
        } else {
            this.ds = ds;
        }
        this.rank = ds.rank();
        this.ds = ds;
        this.fit = fits;
        it= new DimensionIterator[ fits.length ];
        initialize();
    }

    /**
     * convenient method for monitoring long processes, this allows
     * clients to let the iterator manage the monitor.  setTaskSize, 
     * started, setTaskProgress, and finished are called automatically.  
     * The monitor will reflect the zeroth index.
     * @param mon the monitor, or null.
     */
    public void setMonitor( ProgressMonitor mon ) {
        this.monitor= mon;
        this.monitor.setTaskSize( this.rank==0 ? 1 : this.dimLength(0) );
        this.monitor.started();
    }

    /**
     * return an iterator for the slice of a dataset (on the 0th index).  
     * This is introduced to improve performance by reducing the number of 
     * bounds checks, etc from the general case.  Note slice is a native
     * operation for most datasets now, so this is probably obsolete.
     * @param ds the dataset.
     * @param sliceIndex the index of the slice.
     * @return an iterator for the slice.
     * @see org.das2.qds.QDataSet#slice(int) 
     */
    public static QubeDataSetIterator sliceIterator( QDataSet ds, int sliceIndex ) {
        QubeDataSetIterator result;
        if (ds.rank() == 0) {
            throw new IllegalArgumentException("can't slice rank 0");
        } else if (ds.rank() == 1) {
            throw new IllegalArgumentException("can't slice rank 1");
        } else if (ds.rank() == 2) {
            result = new QubeDataSetIterator(ds, 
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                    new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1)});
        } else if (ds.rank() == 3) {
            result = new QubeDataSetIterator(ds,
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                        new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1),
                        new StartStopStepIteratorFactory(0, null, 1),});
        } else if (ds.rank() == 4) {
            result = new QubeDataSetIterator( ds,
                    new DimensionIteratorFactory[]{new SingletonIteratorFactory(sliceIndex),
                        new StartStopStepIteratorFactory(0, ds.length(sliceIndex), 1),
                        new StartStopStepIteratorFactory(0, null, 1),
                        new StartStopStepIteratorFactory(0, null, 1), } );
        } else {
            throw new IllegalArgumentException("rank limit");
        }
        return result;
    }

    /**
     * reinitializes the iterator.
     * @param dim the dimension index (0,...,rank-1)
     * @param fit the iterator factory to use for the index.
     */
    public void setIndexIteratorFactory(int dim, DimensionIteratorFactory fit) {
        if ( dim >= this.rank ) {
            throw new IllegalArgumentException( String.format( "rank limit: rank %d dataset %s has no index %d", ds.rank(), ds, dim ) );
        }
        if ( fit instanceof IndexListIteratorFactory ) {
            IndexListIteratorFactory ffit= (IndexListIteratorFactory)fit;
            int max= ffit.getMaxIndexExclusive();
            if ( max>-1 ) {
                if ( this.qube!=null ) {
                    if ( this.qube[dim]!=max ) {
                        String jythonLine= currentJythonLine();
                        if ( jythonLine.equals("???") ) {
                            logger.log(Level.WARNING, "rfe737: index list appears to be for dimensions of length {0} (see VALID_MAX) but is indexing dimension length {1}, which may indicate there''s a bug.", new Object[]{max, this.qube[dim]});
                        } else {
                            logger.log(Level.WARNING, "rfe737: index list appears to be for dimensions of length {0} (see VALID_MAX) but is indexing dimension length {1}, which may indicate there''s a bug at {2}.", new Object[]{max, this.qube[dim], jythonLine});
                        }
                    }
                }
            }
        }
        this.fit[dim] = fit;
        initialize();
    }

    /**
     * now that the factories are configured, initialize the iterator to
     * begin iterating.
     */
    private void initialize() {
        boolean allLi= true;
        for (int i = 0; i < rank; i++) {
            int dimLength= dimLength(i);
            it[i] = fit[i].newIterator(dimLength);
            if ( !( it[i] instanceof IndexListIterator ) ) allLi= false;
        }
        this.isAllIndexLists= allLi;
    }

    /**
     * return the length of the dimension.  This is introduced with the intention
     * that this can support non-qube datasets as well.
     * @param idim the dimension index (0,...,rank-1)
     * @return the length of the dimension.
     */
    private int dimLength(int idim) {
        if (qube != null) {
            return qube[idim];
        } else {
            int result = 0;
            switch (idim) {
                case 0:
                    result = ds.length();
                    break;
                case 1:
                    result = ds.length(Math.max(0, index(0)));
                    break;
                case 2:
                    result = ds.length(Math.max(0, index(0)), Math.max(0, index(1)));
                    break;
                case 3:
                    result = ds.length(Math.max(0, index(0)), Math.max(0, index(1)), Math.max(0, index(2)));
                    break;
                default:
                    throw new IllegalArgumentException("dimension not supported: " + idim);
            }
            return result;
        }
    }

    /**
     * true if there are more values.
     * @return true if there are more values.
     */
    @Override
    public boolean hasNext() {
        if (rank == 0) {
            if ( this.allnext ) {
                return true;
            } else {
                if ( monitor!=null ) monitor.finished();
                return false;
            }
        } else {
            if ( it[0].length()==0 ) {
                if ( monitor!=null ) monitor.finished();
                return false;
            } // check for empty datasets.
            if ( qube!=null ) {
                for ( int i=1; i<rank; i++ ) {
                    if ( it[i].length()==0 ) {
                        if ( monitor!=null ) monitor.finished();
                        return false;
                    } // this is true only for Qubes.
                }
            }
            int i = rank - 1;
            if (it[i].hasNext()) {
                return true;
            } else {
                if (i > 0) {
                    for (int j = i - 1; j >= 0; j--) {
                        if (it[j].hasNext()) {
                            return true;
                        }
                    }
                }
            }
            if ( monitor!=null ) monitor.finished();
            return false;
        }
    }

    /**
     * advance to the next index.  getValue(ds) will return the value of the
     * dataset at this index.
     * @see #getValue(org.das2.qds.QDataSet) 
     */
    @Override
    public void next() {

        if (this.allnext) {
            for (int i = 0; i < (rank - 1); i++) {
                if ( !( isAllIndexLists && ( it[i] instanceof IndexListIterator ) ) ) {
                    it[i].nextIndex();
                    if ( i==0 && monitor!=null ) monitor.setTaskProgress(it[0].index());
                }
            }
            allnext = false;
            if (rank == 0) {
                return;
            }
        }

        // implement borrow logic
        int i = rank - 1;
        if (it[i].hasNext()) {
            it[i].nextIndex();
            if ( it[i] instanceof IndexListIterator ) { // all index lists need to be incremented together.
                for ( int k=0; k<i; k++ ) {
                    if ( isAllIndexLists && ( it[k] instanceof IndexListIterator ) ) {
                        if ( k==0 && monitor!=null ) monitor.setTaskProgress(it[0].index());
                        it[k].nextIndex();
                    }
                }
            }
        } else {
            if (i > 0) {
                for (int j = i - 1; j >= 0; j--) {
                    if (it[j].hasNext()) {
                        it[j].nextIndex();
                        if ( j==0 && monitor!=null ) {
                            monitor.setTaskProgress(it[0].index());
                        }
                        if ( it[j] instanceof IndexListIterator ) { // all index lists need to be incremented together.
                            for ( int k=0; k<j; k++ ) {
                                if ( isAllIndexLists && ( it[k] instanceof IndexListIterator ) ) {
                                    it[k].nextIndex();
                                }
                            }
                        }
                        for (int k = j + 1; k <= i; k++) {
                            it[k] = fit[k].newIterator(dimLength(k));
                            it[k].nextIndex();
                        }
                        break;
                    }
                }
            } else {
                throw new IllegalArgumentException("no more elements");
            }
        }
    }

    @Override
    public int index(int dim) {
        return it[dim].index();
    }

    @Override
    public int length(int dim) {
        return it[dim].length();
    }

    @Override
    public int rank() {
        int result= rank;
        for (DimensionIterator it1 : it) {
            if (it1 instanceof QubeDataSetIterator.SingletonIterator) {
                result--;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        if (rank == 0) {
            return "Iter hasNext=" + hasNext();
        } else {
            StringBuilder its = new StringBuilder( it[0].toString() );
            StringBuilder ats = new StringBuilder( "" + it[0].index() );
            for (int i = 1; i < rank; i++) {
                its.append(",").append(it[i]);
                ats.append(",").append(it[i].index());
            }
            return "Iter [" + its + "] @ [" + ats + "] ";
        }
    }

    /**
     * return a dataset that will have the same geometry at the
     * dataset implied by each dimension iterator.  This is
     * introduced to encapsulate this dangerous code to here where it could
     * be done correctly.  Right now this assumes QUBES.
     *
     * Do not pass the result of this into the putValue of this iterator,
     * the result should have its own iterator.
     *
     * 20101006 jbf: copy dimensional metadata DEPEND_0, etc where possible
     * 20120718 jbf: bugfix with ds[7,16,4,:]. Support BINS.
     * @return empty dataset with structural properties set.
     */
    @Override
    public DDataSet createEmptyDs() {
        List<Integer> qqube = new ArrayList<>();
        List<Integer> dimMap= new ArrayList<>();  //e.g. [0,1,2,3]
        for (int i = 0; i < this.it.length; i++) {
            if ( this.it[i]==null ) {
                continue;
            }
            boolean reform=  this.it[i] instanceof SingletonIterator;
            if (!reform) {
                qqube.add( this.it[i].length() );
                dimMap.add( i );
            }
        }
        
        int[] qube1;
        if (isAllIndexLists) {  // a=[1,2,3]; b=[2,3,1]; Z[a,b] is rank 1
            int len= this.it[0].length();
            for ( int i=1; i<this.it.length; i++ ) {
                if ( this.it[i].length()!=len ) {
                    throw new IllegalArgumentException("all index lists must have the same length.  index 0 has "+len+ " elements and index "+i+ " has "+this.it[i].length()+" elements");
                }
            }
            qube1=  new int[] { this.it[0].length() };
        } else {
            qube1 = new int[qqube.size()];
            for (int i = 0; i < qqube.size(); i++) {
                qube1[i] = qqube.get(i);
            }
        }
        DDataSet result = DDataSet.create(qube1);

        // move the structural properties like DEPEND_i and BUNDLE_i.  idim refers to the source, i refers to the result.
        for ( int i=0; i<dimMap.size(); i++ ) {
            int idim= dimMap.get(i);
            QDataSet dep= (QDataSet) this.ds.property("DEPEND_"+idim);
            QDataSet bund= (QDataSet) this.ds.property("BUNDLE_"+idim);
            String bins= (String) this.ds.property("BINS_"+idim);
            if ( fit[idim] instanceof StartStopStepIteratorFactory ) {
                if ( dep!=null && dep.rank()==1 ) {
                    StartStopStepIterator sssi= (StartStopStepIterator)it[idim];
                    if ( sssi.step==1 && sssi.start==0 && sssi.stop==dep.length() ) {
                        result.putProperty( "DEPEND_"+i, dep );
                    } else if ( sssi.step==1 ) {
                        result.putProperty( "DEPEND_"+i, dep.trim( sssi.start, sssi.stop ) );
                    } else {
                        QDataSet depSlice= DataSetOps.trim( dep, sssi.start, sssi.stop, sssi.step );
                        result.putProperty( "DEPEND_"+i, depSlice );
                    }
                } else if ( dep!=null && dep.rank()==2 ) {
                    DimensionIteratorFactory[] dif= new DimensionIteratorFactory[2];
                    if ( it[0] instanceof StartStopStepIterator ) {
                        StartStopStepIterator sssi= (StartStopStepIterator)it[0];
                        dif[0]= new StartStopStepIteratorFactory( sssi.start, sssi.stop, sssi.step );
                    } else if ( it[0] instanceof IndexListIterator ) {
                        IndexListIterator ili= (IndexListIterator)it[0];
                        dif[0]= new IndexListIteratorFactory( ili.ds );
                    } else if ( it[0] instanceof SingletonIterator ) {
                        SingletonIterator ili= (SingletonIterator)it[0];
                        dif[0]= new SingletonIteratorFactory( ili.index );
                    }
                    dif[1]= fit[idim];
                    QubeDataSetIterator iter2= new QubeDataSetIterator(dep,dif);
                    DDataSet depNew= iter2.createEmptyDs();
                    QubeDataSetIterator itOut= new QubeDataSetIterator(depNew);
                    while ( iter2.hasNext() ) {
                        iter2.next();
                        itOut.next();
                        itOut.putValue( depNew, iter2.getValue(dep) );
                    }
                    result.putProperty( "DEPEND_"+i, depNew );
                }
                if ( bund!=null ) {
                    StartStopStepIterator sssi= (StartStopStepIterator)it[idim];
                    if ( sssi.step==1 && sssi.start==0 && sssi.stop==bund.length() ) {
                        result.putProperty( "BUNDLE_"+i, bund );
                    } else if ( sssi.step==1 ) {
                        result.putProperty( "BUNDLE_"+i, bund.trim( sssi.start, sssi.stop ) );
                    } else {
                        QDataSet bundSlice= DataSetOps.trim( bund, sssi.start, sssi.stop, sssi.step );
                        result.putProperty( "BUNDLE_"+i, bundSlice );
                    }
                }
                if ( bins!=null && it[idim].length()==bins.split(",").length ) { //TODO: verify this
                    result.putProperty( "BINS_"+i, bins );
                }
            } else if ( fit[idim] instanceof IndexListIteratorFactory && !isAllIndexLists ) {
                if ( dep!=null && dep.rank()==2 ) {
                    DimensionIteratorFactory[] dif= new DimensionIteratorFactory[2];
                    if ( it[0] instanceof StartStopStepIterator ) {
                        StartStopStepIterator sssi= (StartStopStepIterator)it[0];
                        dif[0]= new StartStopStepIteratorFactory( sssi.start, sssi.stop, sssi.step );
                    } else if ( it[0] instanceof IndexListIterator ) {
                        IndexListIterator ili= (IndexListIterator)it[0];
                        dif[0]= new IndexListIteratorFactory( ili.ds );
                    } else if ( it[0] instanceof SingletonIterator ) {
                        SingletonIterator ili= (SingletonIterator)it[0];
                        dif[0]= new SingletonIteratorFactory( ili.index );
                    }
                    dif[1]= fit[idim];
                    QubeDataSetIterator iter2= new QubeDataSetIterator(dep,dif);
                    DDataSet depNew= iter2.createEmptyDs();
                    QubeDataSetIterator itOut= new QubeDataSetIterator(depNew);
                    while ( iter2.hasNext() ) {
                        iter2.next();
                        itOut.next();
                        itOut.putValue( depNew, iter2.getValue(dep) );
                    }
                    result.putProperty( "DEPEND_"+i, depNew );
                } else if ( dep!=null ) {
                    IndexListIteratorFactory sssi= (IndexListIteratorFactory)fit[idim];
                    result.putProperty( "DEPEND_"+i, DataSetOps.applyIndex( dep, 0, sssi.getList(), false ) );
                }
                if ( bund!=null ) {
                    IndexListIteratorFactory sssi= (IndexListIteratorFactory)fit[idim];
                    result.putProperty( "BUNDLE_"+i, DataSetOps.applyIndex( bund, 0, sssi.getList(), false ) );
                }
            }
        }
        return result;

    }

    @Override
    public final double getValue(QDataSet ds) {
        switch (rank) {
            case 0:
                return ds.value();
            case 1:
                return ds.value(index(0));
            case 2:
                return ds.value(index(0), index(1));
            case 3:
                return ds.value(index(0), index(1), index(2));
            case 4:
                return ds.value(index(0), index(1), index(2), index(3) );
            default:
                throw new IllegalArgumentException("rank limit");
        }
    }

    @Override
    public final void putValue(WritableDataSet ds, double v) {
        switch (rank) {
            case 0:
                ds.putValue(v);
                return;
            case 1:
                ds.putValue(index(0), v);
                return;
            case 2:
                ds.putValue(index(0), index(1), v);
                return;
            case 3:
                ds.putValue(index(0), index(1), index(2), v);
                return;
            case 4:
                ds.putValue(index(0), index(1), index(2), index(3), v);
                return;
            default:
                throw new IllegalArgumentException("rank limit");
        }
    }
}

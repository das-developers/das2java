
package org.das2.qds.filters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.das2.util.LoggerManager;
import org.das2.util.TickleTimer;
import org.das2.util.WindowManager;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.qds.DataSetOps;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * Chain together a number of FilterEditorPanels to one long filter chain.  For example,
 * |slice1(0)|smooth(5) would add two of the FilterEditorPanel to control each
 * filter.  Additionally, this adds and removes filters from the chain.
 * 
 * @author jbf
 */
public final class FiltersChainPanel extends javax.swing.JPanel implements FilterEditorPanel {
    
    private QDataSet inputDs;
    private String currentFilter= null; // the currently implemented filter.
    private FilterEditorPanel currentFilterPanel=null;
    private boolean implicitUnbundle= false;
    private final TickleTimer timer;
    private final Timer recalculatingTimer; //AWT thread
    
    private static final Logger logger= LoggerManager.getLogger("qdataset.filters");
    private static final String CLASS_NAME = FiltersChainPanel.class.getName();
    
    private final Color backgroundColor;
    
    /**
     * Creates new form FiltersChainPanel
     */
    public FiltersChainPanel() {
        logger.entering( CLASS_NAME, "<init>" );
        initComponents();
        backgroundColor= this.getBackground();
        
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ));
        timer= new TickleTimer( 50, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateImmediately();
            }
        });
        recalculatingTimer= new Timer( 100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                indicateRecalculating();
            }
        }); 
        recalculatingTimer.setRepeats(false);
        
        setFilter("");
    }

    List<FilterEditorPanel> editors= new LinkedList();
    
    /**
     * these are the results after each filter. 
     */
    List<QDataSet> results= new LinkedList();
    
    /**
     * these are the filters used to get to each result.
     */
    List<String> resultFilters= new LinkedList();
    
    /**
     * true means the filter is recalculating, and the GUI will be updated later.
     */
    List<Boolean> recalculating= new LinkedList();
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
        add(scrollPane);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * this should return a delegate editor for the given filter.  This component
     * will be configured as specified in f.  
     * 
     * Note that two filters that start with the same command (e.g. slice0) must
     * use the same editor!
     * 
     * @param f the filter, which may or may not start with a pipe.
     * @return the filter.
     */
    private FilterEditorPanel getEditorFor(String f, FilterEditorPanel recyclable) {
        logger.entering( CLASS_NAME, "getEditorFor", f );
        if ( !f.startsWith("|") ) f= "|"+f;
        if ( !f.contains("(")&& !f.endsWith(")") ) {
            f= f+"()";
        }
        String srecyclable= recyclable==null ? null : recyclable.getFilter();
        int i= f.indexOf("(");
        if ( i==-1 ) i=f.length();
        if ( srecyclable!=null && srecyclable.startsWith(f.substring(0,i)) ) {
            assert recyclable!=null;
            logger.log(Level.FINE, "recycling to provide {0}", f);
            if ( !srecyclable.equals(f) ) {
                recyclable.setFilter(f);
            }
            return recyclable;
        }
        
        logger.log( Level.FINE, "creating new editor panel for {0}", f );
        
        FilterEditorPanel result;
        if ( f.matches("\\|add\\((.*)\\)") ) {
            result= new AddFilterEditorPanel();
        } else if ( f.matches("\\|subtract\\((.*)\\)") ) {
            result= new SubtractFilterEditorPanel();
        } else if ( f.matches("\\|butterworth\\((\\d),(\\S+),(\\S+)\\)") ) {
            result= new ButterworthFilterEditorPanel();
        } else if ( f.matches("\\|butterworth\\((\\d),(\\S+),(\\S+),(\\S+)\\)") ) {
            result= new ButterworthFilterEditorPanel();
        } else if ( f.matches("\\|collapse(\\d)\\(\\)") ) {
            result= new CollapseFilterEditorPanel();
        } else if ( f.matches("\\|contour\\((.*)\\)") ) {
            result= new ContourFilterEditorPanel();
        } else if ( f.matches("\\|detrend\\((.*)\\)") ) {
            result= new DetrendFilterEditorPanel();
        } else if ( f.matches("\\|divide\\((.*)\\)") ) {
            result= new DivideFilterEditorPanel();
        } else if ( f.matches("\\|fftPower\\((\\d+),(\\d),'?(\\S+)'?\\)") ) {
            result= new FftPowerFilterEditorPanel();
        } else if ( f.matches("\\|hanning\\((.*)\\)") ) {
            result= new HanningFilterEditorPanel();
        } else if ( f.matches("\\|medianFilter\\((.*)\\)") ) {
            result= new MedianFilterEditorPanel();
        } else if ( f.matches("\\|multiply\\((.*)\\)") ) {
            result= new MultiplyFilterEditorPanel();
        } else if ( f.matches("\\|reducex\\('?(\\d+)\\s?(\\S+)'?\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new ReducexFilterEditorPanel();
        } else if ( f.matches( SetDepend0CadenceFilterEditorPanel.PROP_REGEX ) ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetDepend0CadenceFilterEditorPanel();
        } else if ( f.matches("\\|setDepend0Units\\('(\\S+)'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetDepend0UnitsFilterEditorPanel();
        } else if ( f.matches("\\|setUnits\\('(\\S+)'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetUnitsFilterEditorPanel();
        } else if ( f.matches("\\|slice(\\d)\\((\\d+)\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            if ( false && recyclable instanceof SliceFilterEditorPanel ) {
                recyclable.setFilter(f);
                return recyclable;
            } else {
                result= new SliceFilterEditorPanel();
            }
        } else if ( f.matches("\\|slice(\\d)\\(\\'(\\S+)\\'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            if ( recyclable instanceof SliceFilterEditorPanel ) {
                recyclable.setFilter(f);
                return recyclable;
            } else { 
                result= new SliceFilterEditorPanel();
            }
        } else if ( f.matches("\\|cos\\(\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|sin\\(\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|total(\\d)\\(()\\)") ) {
            result= new TotalFilterEditorPanel();
        } else if ( f.matches("\\|slices\\((.*)\\)") ) { 
            result= new SlicesFilterEditorPanel();
        } else if ( f.matches("\\|smooth\\(\\d+\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SmoothFilterEditorPanel();
        } else if ( f.matches("\\|smoothfit\\(\\d+\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SmoothFilterEditorPanel();
        } else if ( f.matches("\\|histogram\\(\\)") ) { 
            result= new HistogramFilterEditorPanel();
        } else if ( f.matches("\\|histogram\\((\\S+),(\\S+),(\\S+)\\)") ) { 
            result= new HistogramFilterEditorPanel();            
        } else if ( f.matches(Histogram2dFilterEditorPanel.PROP_REGEX) ) {
            result= new Histogram2dFilterEditorPanel();            
        } else if ( f.matches( UnbundleFilterEditorPanel.PROP_REGEX ) ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new UnbundleFilterEditorPanel();
        } else if ( f.matches("\\|dbAboveBackgroundDim1\\((\\S+)\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new dbAboveBackgroundDim1FilterEditorPanel();
        } else if ( f.matches("\\|transpose\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|toDegrees\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|toRadians\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|valid\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|extent\\(\\)") ) {
            result= new NoArgFilterEditorPanel();            
        } else if ( f.matches("\\|diff\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|sqrt\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|flattenWaveform\\(.*\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|pow\\(.*\\)") ) {
            result= new PowFilterEditorPanel();
        } else if ( f.matches("\\|getProperty\\((.*)\\)") ) {
            result= new GetPropertyEditorPanel();
        } else if ( f.matches("\\|putProperty\\((.*)\\)") ) {
            result= new PutPropertyFilterEditorPanel();
        } else if ( f.matches("\\|setValidRange\\((.*)\\)") ) {
            result= new SingleArgumentEditorPanel( "setValidRange", "Valid Range", "The limits of valid data (inclusive)", new String[] { "", "-1e31 to 1e31", "0 to 100" } );
        } else if ( f.matches("\\|setFillValue\\((.*)\\)") ) {
            result= new SingleArgumentEditorPanel( "setFillValue", "Fill Value", "Numerical value marking invalid data", new String[] { "", "-1e31", "0", "-1" } );
        } else if ( f.matches("\\|putProperty\\((.*)\\)") ) {
            result= new PutPropertyFilterEditorPanel();
        } else if ( f.matches( TrimFilterEditorPanel.PROP_REGEX)){
            result= new TrimFilterEditorPanel();
        } else if ( f.matches( TrimFilterEditorPanel.PROP_TRIMI_REGEX)){
            result= new TrimFilterEditorPanel();
        } else {
            result= new AnyFilterEditorPanel();
        }
        result.setFilter(f);
        String tooltip= TooltipKeeper.getInstance().getTooltipFor(f);
        if ( tooltip!=null ) {
            result.getPanel().setToolTipText(tooltip);
        }
        return result;
    }
    
    @Override
    public String getFilter() {
        
        logger.entering( CLASS_NAME, "getFilter" );
        
        final StringBuilder b= new StringBuilder();
        
        Runnable run= new Runnable() {
            @Override
            public void run() {
                List<FilterEditorPanel> leditors= new ArrayList(editors);
                int ifilter= 0;
                for ( FilterEditorPanel p: leditors ) {
                    if ( ifilter==0 && p instanceof UnbundleFilterEditorPanel && implicitUnbundle ) {
                        b.append( ((UnbundleFilterEditorPanel)p).getComponent() );
                    } else {
                        b.append(p.getFilter());
                    }
                }
            }
        };
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InterruptedException | InvocationTargetException ex) {
                Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return b.toString();
    }

    private void deleteFilter( int fi ) {
        FilterEditorPanel p= editors.remove(fi);
        removeFocusListeners(p.getPanel());
        setFilter( getFilter() );
        resetFilterInput();
        updateSoon( getFilter() );
    }
    
    /**
     * force the filters to reset based on the new dataset.
     */
    private void resetFilterInput() {
        QDataSet inputDs1= this.inputDs;
        setInput(null);
        setInput(inputDs1);        
    }
    
    /**
     * find the filterEditorPanel, or null.
     * @param c null or the current component.
     * @return null or the FilterEditorPanel
     */
    private FilterEditorPanel getFilterEditorPanelParent( Component c ) {
        // go through parents to get FilterEditorPanel and do focus based on this.
        while ( c!=null && !( c instanceof FilterEditorPanel ) ) {
            c= c.getParent();
        }
        if ( c==null ) {
            return null;
        }
        return (FilterEditorPanel)c;
    }
    
    private final FocusListener lostFocusListener= new FocusListener() {

        @Override
        public void focusGained(FocusEvent e) {
            logger.log(Level.FINE, "focusGained {0}", e.getComponent().getName() );
            currentFilterPanel= getFilterEditorPanelParent(e.getComponent());
        }

        @Override
        public void focusLost(FocusEvent e) {
            logger.log(Level.FINE, "focusLost {0}", e.getComponent().getName() );
            FilterEditorPanel c= getFilterEditorPanelParent( e.getComponent() );
            FilterEditorPanel n= getFilterEditorPanelParent( e.getOppositeComponent() );
            if ( c==null ) return;
            if ( c!=currentFilterPanel || c!=n ) {
            //if ( !getFilter().equals(currentFilter) ) {
                updateSoon( null );
                currentFilterPanel= (FilterEditorPanel)c;
            } else {
                logger.log( Level.FINER, "... already up to date");
            }
        }
        
    };
    
    
    private final ActionListener requestUpdateListener= new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.log(Level.FINE, "requestUpdateFrom {0}", e.getSource());
            updateSoon( null );
        }
    };
    
    
    private final ChangeListener requestChangeListener= new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateSoon( null );
        }
    };
        
    private void addFilterNew( int idx ) {
        AddFilterDialog afd= new AddFilterDialog();
        int r= WindowManager.showConfirmDialog( this, afd, "Add Operation", JOptionPane.OK_CANCEL_OPTION );
        //int r= JOptionPane.showConfirmDialog( this, afd, "Add Operation", JOptionPane.OK_CANCEL_OPTION );
        if ( r==JOptionPane.OK_OPTION ) {
            String ss= afd.getValue();
            FilterEditorPanel filter1= getEditorFor(ss, null);
            filter1.getPanel().addFocusListener( lostFocusListener );
            addFocusListeners( filter1.getPanel() );
            editors.add( idx, filter1 );
            String filter= getFilter();
            setFilter( filter );
            resetFilterInput();
            updateSoon( filter );
        }
    }
     
    private void addFocusListeners( JPanel p ) {
        for ( Component c: p.getComponents() ) {
            c.addFocusListener(lostFocusListener);
            if ( c instanceof JTextField ) {
                ((JTextField)c).addActionListener(requestUpdateListener);
            } else if ( c instanceof JComboBox ) {
                ((JComboBox)c).addActionListener(requestUpdateListener);
            } else if ( c instanceof JSpinner ) {
                ((JSpinner)c).addChangeListener(requestChangeListener);
            } else if ( c instanceof AbstractButton ) {
                ((AbstractButton)c).addActionListener(requestUpdateListener);
            } else if ( c instanceof JPanel ) {
                addFocusListeners((JPanel)c); //TODO: consider not until the JPanel focus is blurred.
            }
        }
    }
    
    private void removeFocusListeners( JPanel p ) { 
        for ( Component c: p.getComponents() ) {
            c.removeFocusListener(lostFocusListener);
            if ( c instanceof JTextField ) {
                ((JTextField)c).removeActionListener(requestUpdateListener);
            } else if ( c instanceof JComboBox ) {
                ((JComboBox)c).removeActionListener(requestUpdateListener);
            } else if ( c instanceof JSpinner ) {
                ((JSpinner)c).removeChangeListener(requestChangeListener);
            } else if ( c instanceof AbstractButton ) {
                ((AbstractButton)c).removeChangeListener(requestChangeListener);
            } else if ( c instanceof JPanel ) {
                addFocusListeners((JPanel)c);
            }
        }
    }
    
    /**
     * turn off and on the add and subtract buttons.
     */
    private boolean addSubtractButtons = true;

    public static final String PROP_ADDSUBTRACTBUTTONS = "addSubtractButtons";

    public boolean isAddSubtractButtons() {
        return addSubtractButtons;
    }

    public void setAddSubtractButtons(boolean addSubtractButtons) {
        boolean oldAddSubtractButtons = this.addSubtractButtons;
        this.addSubtractButtons = addSubtractButtons;
        
        String filter= getFilter();
        setFilter( null );
        setFilter( filter );
        
        firePropertyChange(PROP_ADDSUBTRACTBUTTONS, oldAddSubtractButtons, addSubtractButtons);
    }

    /**
     * return the panel with the add and remove icons.
     * @param fi the position 
     * @return one panel  ( +  panel GUI  - )
     */
    private JPanel onePanel( final int fi ) {
        logger.entering( CLASS_NAME, "onePanel", fi );
        final JPanel sub= new JPanel( new BorderLayout() );

        String sfilter= fi==-1 ? "" : editors.get(fi).getFilter();
        JPanel pp= fi==-1 ? null : editors.get(fi).getPanel();
        
        if ( pp!=null ) {
            addFocusListeners( pp );
        }
        
        Dimension limit= new Dimension(24,24);
        
        if ( addSubtractButtons ) {
            JButton subAdd= new JButton("");
            subAdd.setIcon( new ImageIcon( FiltersChainPanel.class.getResource("/resources/add.png") ) );
            subAdd.setMaximumSize( limit );
            subAdd.setPreferredSize( limit );

            if ( fi>=0 ) {
                subAdd.setToolTipText( "insert new filter before "+ sfilter );
                subAdd.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        org.das2.util.LoggerManager.logGuiEvent(e);                    
                        //if ( true ) {
                            addFilterNew(fi);
                        //} else {
                        //    addFilter(fi);
                        //}
                    }
                } );
            } else {
               subAdd.setToolTipText( "insert new filter" );        
               subAdd.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        org.das2.util.LoggerManager.logGuiEvent(e);
                        //if ( true ) {
                            addFilterNew(editors.size());
                        //} else {
                        //    addFilter(editors.size());
                        //}
                    }
                } );
            }

            sub.add( subAdd, BorderLayout.WEST );

            if ( fi>=0 ) {
                JButton subDelete= new JButton("");
                subDelete.setIcon( new ImageIcon( FiltersChainPanel.class.getResource("/resources/subtract.png") ) );
                subDelete.setMaximumSize( limit );
                subDelete.setPreferredSize( limit );
                subDelete.setToolTipText( "remove filter " + sfilter );
                subDelete.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        org.das2.util.LoggerManager.logGuiEvent(e);                                        
                        deleteFilter(fi);
                        Container parent= sub.getParent();
                        parent.remove(sub);
                        parent.validate();
                    }
                } );
                sub.add( subDelete, BorderLayout.EAST );
            }
        }

        if ( fi>=0 ) {
            sub.add( pp, BorderLayout.CENTER );

        } else {
            if ( addSubtractButtons ) {
                final JLabel tf= new JLabel();
                tf.setText("<html><i>&nbsp;(click to add)</i></html>");
                sub.add( tf, BorderLayout.CENTER );
            }

        }

        Dimension maximumSize = sub.getPreferredSize();
        maximumSize.width = Integer.MAX_VALUE;
        sub.setMaximumSize(maximumSize);

        return sub;
    }
    
    /**
     * set the filter, and rebuild the GUI.  Note this should be called from the 
     * event thread.  TODO: This means that filters are happening on the event thread,
     * which is going to lead to problems.
     * @param filter the filter for the block, such as "|slice1(2)"
     */
    @Override
    public void setFilter(String filter) {
        logger.entering( CLASS_NAME, "setFilter", filter );

        String oldFilter= getFilter();
        
        if ( currentFilter!=null && currentFilter.equals(filter) ) {
            logger.fine("filter unchanged, so we don't need to do anything, right?");
            return;
        }
        
        //if ( this.getName()!=null && this.getName().startsWith("post") ) {
        //    
        //} else {
        //    System.err.println("here "+this.getName());
        //}
        
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.warning("must be called from event thread"); 
        }
        
        if ( filter==null ) filter= ""; // autoplot-test100 Automatic GUI testing hits this, presumably intermediate state.
         
        //if ( filter.equals( this.getFilter() ) ) { // the problem is that bindings will call this without setInput.
        //    logger.finer("no need to update...");
        //    return;
        //}

        // will contain an empty string when there is no initial unbundle, 
        String[] ss= filter.split("\\|",-2);
        
        List<FilterEditorPanel> recycle= new ArrayList(editors);
        for ( int i= editors.size(); i<ss.length; i++ ) {
            recycle.add(null);
        }
        
        for ( FilterEditorPanel p: editors ) {
            removeFocusListeners( p.getPanel() );
        }
        editors.clear();
        recalculating.clear();
        
        JPanel content= new JPanel();
        this.setPreferredSize( new Dimension( 500, 300 ) );

        BoxLayout lo= new BoxLayout( content, BoxLayout.Y_AXIS );
        content.setLayout( lo );

        int scroll0= scrollPane.getVerticalScrollBar().getValue();
        
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        scrollPane.getVerticalScrollBar().setUnitIncrement( scrollPane.getFont().getSize() );
                
        for ( int i=0; i<ss.length; i++ ) {
            ss[i]= ss[i].trim();
        }
        
        int i=0;

        if ( ss[0].length()>0 ) {
            FilterEditorPanel p = getEditorFor("|unbundle("+ss[0]+")", recycle.get(0) );
            editors.add(p);
            recalculating.add(Boolean.FALSE);
            JPanel ll= onePanel(i);
            content.add( ll );
            i++;
            content.add( new JLabel( "--------" ) );
            implicitUnbundle= true;
        } else {
            implicitUnbundle= false;
        }
        ss= Arrays.copyOfRange( ss, 1, ss.length );
        
//        while ( resultFilters.size()<ss.length ) { // TODO: s.length()==0
//            resultFilters.add("");
//            results.add(null);
//        }
//        
//        while( resultFilters.size()>ss.length ) {
//            resultFilters.remove(ss.length);
//            results.remove(ss.length);
//        }
//        
//        boolean dirty= false; // keep track of which results we can recycle.
        for (String s : ss) {
            if ( s.length()>0 ) {
//                if ( !resultFilters.get(i).equals(s) ) {
//                    dirty= true;
//                }
//                if ( dirty ) results.set(i,null);
//                
                FilterEditorPanel p = getEditorFor(s, recycle.get(editors.size()) );
                editors.add(p);
                recalculating.add(Boolean.FALSE);
                JPanel ll= onePanel(i);
                content.add( ll );
                i++;
                if ( i<ss.length ) {
                    content.add( new JLabel( "--------" ) );
                }
            }
        }

        JPanel add= onePanel(-1);
        content.add( add );

        content.add(Box.createVerticalGlue());
        
        scrollPane.setViewportView(content);
        scrollPane.getVerticalScrollBar().setValue(scroll0);
        
        this.currentFilter= filter;
        
        //content.revalidate();
        this.revalidate();
        
        firePropertyChange( PROP_FILTER, oldFilter, filter );
        
    }

    private void updateImmediately() {
        Runnable run= new Runnable() {
            @Override
            public void run() {
                String f= getFilter();
                String oldCurrentFilter= currentFilter;
                setFilter( f );
                if ( inputDs!=null ) setInput( inputDs );
                if ( oldCurrentFilter.equals(f) ) {
                    logger.fine("does not change.");
                } else {
                    firePropertyChange( PROP_FILTER, oldCurrentFilter, f );  
                }
            }
        };
        try {
            SwingUtilities.invokeAndWait(run);
        } catch (InterruptedException | InvocationTargetException ex) {
            Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void indicateRecalculating() {
        for ( int i=0; i<recalculating.size(); i++ ) {
            boolean recalc= recalculating.get(i);
            if ( recalc ) {
                editors.get(i).getPanel().setBackground(Color.GRAY);
            } else {
                editors.get(i).getPanel().setBackground(backgroundColor);
            }
        }
    }
    
    /**
     * set the input dataset for each filter.
     */
    private void updateSoon( final String filter) {
        logger.entering( CLASS_NAME, "updateSoon", filter);
        //this.inputDs= null;
        timer.tickle(filter); 
    }
    
    
    /**
     * This should not be called on the event thread.
     * @param ds the input dataset
     * @param filter
     * @param index 
     */
    private void setInput( QDataSet ds, String filter, List<FilterEditorPanel> leditors ) {
        
        if ( SwingUtilities.isEventDispatchThread() ) {
            logger.warning("must NOT be called from event thread");
        }
        
        String[] ss= filter.split("\\|");
        int i=0;
        int iss= 0;
        for (String s : ss) {
            s= s.trim();
            iss++;
            if ( s.length()>0 ) {
                final FilterEditorPanel p = leditors.get(i);
                if ( ds!=null ) {
                    final int fi= i;
                    final QDataSet fds= ds;
                    Runnable run= new Runnable() {
                        @Override
                        public void run() {
                            p.setInput(fds);
                            if ( recalculating.size()>fi ) { // transitional state
                                recalculating.set(fi,Boolean.FALSE);
                                indicateRecalculating();
                            }
                        }
                    };
                    SwingUtilities.invokeLater(run);
                    //run.run();
                    
                    if ( iss<ss.length ) {
                        try {
                            if ( iss==1 && implicitUnbundle ) {
                                ds= DataSetOps.sprocess( "|unbundle("+s+")", ds, new NullProgressMonitor() );
                            } else {
                                ds= DataSetOps.sprocess( "|"+s, ds, new NullProgressMonitor() );
                            }
                            //resultFilters.set( i, s );
                            //results.set( i, ds );

                        } catch ( Exception ex ) {
                            //p.getPanel().setBackground(Color.RED);
                            p.getPanel().setToolTipText(ex.getMessage());
                            ds= null;
                        }
                    }
                    PropertyChangeListener[] pcls= p.getPanel().getPropertyChangeListeners();
                    for ( PropertyChangeListener pcl: pcls ) {
                        p.getPanel().removePropertyChangeListener( pcl );
                    }
                    p.getPanel().addPropertyChangeListener("filter",new PropertyChangeListener() {
                        @Override
                        public void propertyChange(PropertyChangeEvent evt) {
                            updateSoon( (String)evt.getNewValue() );
                        }
                    });
                }
                i=i+1;
            }
        }
        this.repaint();
        
    }
    
    
    public void resetInput( final QDataSet ds ) {
        
        logger.entering( CLASS_NAME, "resetInput", ds );
        
        this.inputDs= ds;
        
        final String filter= getFilter();
        logger.log(Level.FINE, "filter: {0}", filter);
        
        for ( int i=0; i<recalculating.size(); i++ ) {
            recalculating.set(i,Boolean.TRUE);
        }
        recalculatingTimer.restart();
        
        final List<FilterEditorPanel> leditors= new ArrayList(editors);
        
        Runnable run= new Runnable() { 
            @Override
            public void run() {
                logger.entering( CLASS_NAME, "resetInput", ds );
                setInput( ds, filter, leditors );
                logger.exiting( CLASS_NAME, "resetInput", ds );
            }
        };
        
        new Thread( run, "resetInput" ).start();
        logger.exiting( CLASS_NAME, "resetInput", ds );
    }
    
    /**
     * the filter must be set before this is called.  This will set 
     * droplist labels, etc. 
     * @param ds the dataset, or null.
     */
    @Override
    public void setInput( final QDataSet ds ) {
        logger.entering( CLASS_NAME, "setInput", ds );
        
        if ( this.inputDs==ds ) {
            logger.fine("already set input...");
            return;
        } 
        
        resetInput(ds);
        logger.exiting( CLASS_NAME, "setInput", ds );
    }

    /**
     * return true if the filter appears that it will be valid, and 
     * false when it clearly won't be.
     * @param filter
     * @return 
     */
    public boolean validateFilter(String filter) {
        String[] ss= filter.split("\\|");
        int i=0;
        final List<FilterEditorPanel> leditors= new ArrayList(editors);
        
        QDataSet ds= inputDs;
        for (String s : ss) {
            s= s.trim();
            if ( s.length()>0 ) {
                if ( i<leditors.size() )  {
                    final FilterEditorPanel p = leditors.get(i);
                    if ( p.validateFilter("|"+s,ds) ) {
                        ds=null;
                    } else {
                        return false;
                    }
                }
                i=i+1;
            }
        }
        return true;
    }    
     
    @Override
    public boolean validateFilter(String filter, QDataSet in) {
        this.inputDs= in;
        return validateFilter(filter);
    }
    
    @Override
    public JPanel getPanel() {
        logger.entering( CLASS_NAME, "getPanel" );        
        return this;
    }
    
    /**
     * dataset for testing.
     * @param s name of dataset, including: rank1TimeSeries, qube
     * @see #main(java.lang.String[]) 
     * @return dataset for testing.
     */
    protected static QDataSet getExampleDataSet( String s ) {
        try {
            switch (s) {
                case "rank1TimeSeries":
                    return Ops.ripplesTimeSeries(20);
                case "qube":
                    MutablePropertyDataSet ds= (MutablePropertyDataSet) Ops.ripples(300,30,20);
                    MutablePropertyDataSet dds;
                    dds= (MutablePropertyDataSet) Ops.timegen("2000-01-01T00:00", "60s", 300 );
                    dds.putProperty( QDataSet.NAME, "Epoch" );
                    ds.putProperty( QDataSet.DEPEND_0, dds );
                    dds= (MutablePropertyDataSet) Ops.findgen(30);
                    dds.putProperty( QDataSet.NAME, "index30" );
                    ds.putProperty( QDataSet.DEPEND_1, dds );
                    dds= (MutablePropertyDataSet) Ops.findgen(20);
                    dds.putProperty( QDataSet.NAME, "index20" );
                    ds.putProperty( QDataSet.DEPEND_2, dds );
                    return ds;
                default:
                    return Ops.ripplesVectorTimeSeries(30);
            }
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void main( String[] args ) throws Exception {
        logger.setLevel(Level.ALL);
        Handler h= new ConsoleHandler();
        h.setLevel(Level.ALL);
        
        logger.addHandler( h );
        
        final FiltersChainPanel ff= new FiltersChainPanel();

        //QDataSet ds= getDataSet( "rank1TimeSeries" );
        final QDataSet ds= getExampleDataSet( "qube" );
        
        ff.setFilter("|slice0(2)|cos()|collapse1()|butterworth(2,500,750,True)"); //butterworth(2,500,550,True)");
        //ff.setFilter("|butterworth(2,500,550,True)"
        //ff.setFilter("|unbundle('bx1')");
        //ff.setFilter("|setDepend0Cadence(50s)");
        ff.setInput(ds);
        
        final JTextField tf= new JTextField();
        tf.setText(ff.getFilter());
        
        tf.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ff.setFilter(tf.getText());
                ff.setInput(ds);
            }
        });
        
        ff.addPropertyChangeListener("filter", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                tf.setText(ff.getFilter());
            }
        });
        
        JPanel p= new JPanel( new BorderLayout() );
        p.add( ff );
        p.add( tf, BorderLayout.NORTH );
        
        JButton b= new JButton("reset data");
        p.add( b, BorderLayout.SOUTH );
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ff.setInput(null);
                ff.setInput(ds);
            }
        });
        
        JDialog d= new JDialog();
        d.setContentPane( p );
        d.setResizable(true);
        d.setModal(true);
        d.pack();
        d.setSize(640,480);
        d.setVisible(true);
        System.err.println(ff.getFilter());
    }
}

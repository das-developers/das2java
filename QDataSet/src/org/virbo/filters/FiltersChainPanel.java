/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.filters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.das2.util.LoggerManager;
import org.das2.util.TickleTimer;
import org.das2.util.monitor.NullProgressMonitor;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 * Chain together a number of FilterEditorPanels to one long filter chain.  For example,
 * |slice1(0)|smooth(5) would add two of the FilterEditorPanel to control each
 * filter.  Additionally, this adds and removes filters from the chain.
 * 
 * @author jbf
 */
public final class FiltersChainPanel extends javax.swing.JPanel implements FilterEditorPanel {
    
    private QDataSet inputDs;
    //private String currentFilter= null;
    private boolean implicitUnbundle= false;
    TickleTimer timer;

    private static final Logger logger= LoggerManager.getLogger("apdss.filters");
    private static final String CLASS_NAME = FiltersChainPanel.class.getName();
    
    /**
     * the current 
     */
    public static final String PROP_FILTER= "filter";
    
    /**
     * Creates new form FiltersChainPanel
     */
    public FiltersChainPanel() {
        logger.entering( CLASS_NAME, "<init>" );
        initComponents();
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ));
        timer= new TickleTimer( 50, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateImmediately();
            }
        });
        setFilter("");
    }

    List<FilterEditorPanel> editors= new LinkedList();

    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

    /**
     * this should return a delegate editor for the given filter.  This component
     * will be configured as specified in f.
     * @param f the filter, which may or may not start with a pipe.
     * @return the filter.
     */
    private FilterEditorPanel getEditorFor(String f, FilterEditorPanel recyclable) {
        logger.entering( CLASS_NAME, "getEditorFor", f );
        if ( !f.startsWith("|") ) f= "|"+f;
        
        String srecyclable= recyclable==null ? null : recyclable.getFilter();
        int i= f.indexOf("(");
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
        } else if ( f.matches("\\|butterworth\\((\\d),(\\d+),(\\w+)\\)") ) {
            result= new ButterworthFilterEditorPanel();
        } else if ( f.matches("\\|butterworth\\((\\d),(\\d+),(\\d+),(\\w+)\\)") ) {
            result= new ButterworthFilterEditorPanel();
        } else if ( f.matches("\\|collapse(\\d)\\(\\)") ) {
            result= new CollapseFilterEditorPanel();
        } else if ( f.matches("\\|contour\\((.*)\\)") ) {
            result= new ContourFilterEditorPanel();
        } else if ( f.matches("\\|cos\\(\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|detrend\\((.*)\\)") ) {
            result= new DetrendFilterEditorPanel();
        } else if ( f.matches("\\|divide\\((.*)\\)") ) {
            result= new DivideFilterEditorPanel();
        } else if ( f.matches("\\|fftPower\\((\\d+),(\\d),'?(\\w+)'?\\)") ) {
            result= new FftPowerFilterEditorPanel();
        } else if ( f.matches("\\|hanning\\((.*)\\)") ) {
            result= new HanningFilterEditorPanel();
        } else if ( f.matches("\\|median\\((.*)\\)") ) {
            result= new MedianFilterEditorPanel();
        } else if ( f.matches("\\|multiply\\((.*)\\)") ) {
            result= new MultiplyFilterEditorPanel();
        } else if ( f.matches("\\|reducex\\('?(\\d+)\\s(\\w+)'?\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new ReducexFilterEditorPanel();
        } else if ( f.matches( SetDepend0CadenceFilterEditorPanel.PROP_REGEX ) ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetDepend0CadenceFilterEditorPanel();
        } else if ( f.matches("\\|setDepend0Units\\('(\\w+)'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetDepend0UnitsFilterEditorPanel();
        } else if ( f.matches("\\|setUnits\\('(\\w+)'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SetUnitsFilterEditorPanel();
        } else if ( f.matches("\\|slice(\\d)\\((\\d+)\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SliceFilterEditorPanel();
        } else if ( f.matches("\\|smooth\\(\\d+\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SmoothFilterEditorPanel();
        } else if ( f.matches("\\|smoothfit\\(\\d+\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SmoothFilterEditorPanel();
        } else if ( f.matches("\\|histogram\\(\\)") ) { 
            result= new HistogramFilterEditorPanel();
        } else if ( f.matches("\\|histogram\\((\\d),(\\d+),(\\d+)\\)") ) { 
            result= new HistogramFilterEditorPanel();            
        } else if ( f.matches( UnbundleFilterEditorPanel.PROP_REGEX ) ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new UnbundleFilterEditorPanel();
        } else if ( f.matches("\\|dbAboveBackgroundDim1\\((\\d+)\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new dbAboveBackgroundDim1FilterEditorPanel();
        } else {
            result= new AnyFilterEditorPanel();
        }
        result.setFilter(f);
        return result;
    }
    
    @Override
    public synchronized String getFilter() {
        logger.entering( CLASS_NAME, "getFilter" );
        StringBuilder b= new StringBuilder();
        int ifilter= 0;
        for ( FilterEditorPanel p: editors ) {
            if ( ifilter==0 && p instanceof UnbundleFilterEditorPanel && implicitUnbundle ) {
                b.append( ((UnbundleFilterEditorPanel)p).getComponent() );
            } else {
                b.append(p.getFilter());
            }
        }
        return b.toString();
    }

    private void deleteFilter( int fi ) {
        FilterEditorPanel p= editors.remove(fi);
        removeFocusListeners(p.getPanel());
        setFilter( getFilter() );
        QDataSet inputDs1= inputDs;
        this.inputDs= null;
        updateSoon(inputDs1, getFilter() );
    }
    
    private final FocusListener lostFocusListener= new FocusListener() {

        @Override
        public void focusGained(FocusEvent e) {
            logger.log(Level.FINE, "focusGained {0}", e.getComponent());
        }

        @Override
        public void focusLost(FocusEvent e) {
            logger.log(Level.FINE, "focusLost {0}", e.getComponent());
            updateSoon(inputDs, null );
        }
        
    };
    
    
    private final ActionListener requestUpdateListener= new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            updateSoon(inputDs, null );
        }
    };
    
    
    private final ChangeListener requestChangeListener= new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateSoon(inputDs, null );
        }
    };
        
    private void addFilter( int idx ) {
        JPanel optionsPanel= new JPanel();

        optionsPanel.setLayout( new BoxLayout(optionsPanel,BoxLayout.Y_AXIS) );

        ButtonGroup group= new ButtonGroup();

        // CAUTION: ") " is used to delimit the annotation from the command.
        String[] opts= new String[] {
        "abs() return the absolute value of the data.",
        "accum() running sum of the rank 1 data. (opposite of diff).",
        "add(1.) add a scalar",
        "butterworth(2,500,550,True) Butterworth notch filter",
        "contour(0,1,10) convert to contours at 0 1 and 10",
        "collapse0() average over the zeroth dimension to reduce the dimensionality. (See total)",
        "collapse1() average over the first dimension to reduce the dimensionality.",
        "cos() cos of the data in radians. (No units check)",
        "dbAboveBackgroundDim1(10) show data as decibels above the 10% level",
        "detrend(5) remove boxcar average from the rank 1 data. (See smooth)",
        "diff() finite differences between adjacent elements in the rank 1 data.",
        "divide(2.) divide by a scalar",
        "exp10() plot pow(10,ds)",
        "fftPower(128) plot power spectrum by breaking waveform data in windows of length size.",
        "fftPower(128,2,'Hanning') power spectrum with sliding window (1=no overlap,2=50% 4=75%).",
        "flatten() flatten a rank 2 dataset. The result is a n,3 dataset of [x,y,z]. (opposite of grid)",
        "grid() grid the rank2 buckshot but gridded data into a rank 2 table.",
        "hanning(128) run a hanning window before taking fft.",
        "histogram() perform an \"auto\" histogram of the data that automatically sets bins. ",
        "logHistogram() perform the auto histogram in the log space.",
        "log10() take the base-10 log of the data." ,
        "magnitude() calculate the magnitude of the vectors ",
        "medianFilter(5) boxcar median filter.",
        "multiply(2) multiply by a scalar ",
        "negate() flip the sign on the data.",
        "setUnits('nT') reset the units to the new units",
        "setDepend0Units('nT') reset the units to the new units",
        "setDepend0Cadence('50s') reset the cadence to 50 seconds",
        "sin() sin of the data in radians. (No units check)",
        "slice0(0) slice the data on the zeroth dimension (often time) at the given index.",
        "slice1(0) slice the data on the first dimension at the given index.",
        "slices(':',2,3) slice the data on the first and second dimensions, leaving the zeroth alone.",
        "smooth(5) boxcar average over the rank 1 data.  (See detrend)",
        "reducex('1 hr') reduce data to 1 hr intervals",
        "toDegrees() convert the data to degrees. (No units check)",
        "toRadians() convert the data to radians. (No units check) ",
        "total1() total over the first dimension to reduce the dimensionality. (See collapse0)",
        "transpose() transpose the rank 2 dataset.",
        "unbundle('Bx') unbundle a component ",
        "valid() replace data with 1 where valid, 0 where invalid",
        };

        for ( String opt : opts ) {
            JRadioButton cb = new JRadioButton(opt);
            group.add(cb);
            optionsPanel.add(cb);
        }

        JScrollPane p= new JScrollPane(optionsPanel);
        Dimension d= java.awt.Toolkit.getDefaultToolkit().getScreenSize();

        Dimension v= new Dimension( 700, Math.min( 700, d.height-100 ) );
        p.setMaximumSize(v);
        p.setPreferredSize(v);
        
        p.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        p.getVerticalScrollBar().setUnitIncrement(optionsPanel.getFont().getSize());
       int r= JOptionPane.showConfirmDialog( this, p, "Add Filter", JOptionPane.OK_CANCEL_OPTION );
       if ( r==JOptionPane.OK_OPTION ) {
           String ss=null;
           Enumeration<AbstractButton> ee= group.getElements();
           while ( ee.hasMoreElements() ) {
               AbstractButton b= ee.nextElement();
               if ( b.isSelected() ) {
                   String s= b.getText();
                   int ii= s.indexOf(") ");
                   ss= s.substring(0,ii+1);
               }
           }
           if ( ss!=null ) {
               FilterEditorPanel filter1= getEditorFor(ss, null);
               filter1.getPanel().addFocusListener( lostFocusListener );
               addFocusListeners( filter1.getPanel() );
               editors.add( idx, filter1 );
               String filter= getFilter();
               setFilter( filter );
               QDataSet inputDs1= this.inputDs;
               setInput(null);
               setInput(inputDs1);
               updateSoon(inputDs1, filter );
           }
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
     * return the panel with the add and remove icons.
     * @param fi
     * @return 
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
        
        JButton subAdd= new JButton("");
        subAdd.setIcon( new ImageIcon( FiltersChainPanel.class.getResource("/resources/add.png") ) );
        subAdd.setMaximumSize( limit );
        subAdd.setPreferredSize( limit );

        if ( fi>=0 ) {
            subAdd.setToolTipText( "insert new filter before "+ sfilter );
            subAdd.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);                    
                    addFilter(fi);
                }
            } );
        } else {
           subAdd.setToolTipText( "insert new filter" );        
           subAdd.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);
                    addFilter(editors.size());
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
            subDelete.addActionListener( new ActionListener() {
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

        if ( fi>=0 ) {
            sub.add( pp, BorderLayout.CENTER );

        } else {
            final JLabel tf= new JLabel();
            tf.setText("<html><i>&nbsp;(click to add)</i></html>");
            sub.add( tf, BorderLayout.CENTER );

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
    public synchronized void setFilter(String filter) {
        logger.entering( CLASS_NAME, "setFilter", filter );
        
        if ( filter==null ) filter= ""; // autoplot-test100 Automatic GUI testing hits this, presumably intermediate state.
                
        //if ( filter.equals( this.getFilter() ) ) { // the problem is that bindings will call this without setInput.
        //    logger.finer("no need to update...");
        //    return;
        //}

        // will contain an empty string when there is no initial unbundle, 
        String[] ss= filter.split("\\|");
        
        List<FilterEditorPanel> recycle= new ArrayList(editors);
        for ( int i= editors.size(); i<ss.length; i++ ) {
            recycle.add(null);
        }
        
        for ( FilterEditorPanel p: editors ) {
            removeFocusListeners( p.getPanel() );
        }
        editors.clear();
        
        JPanel content= new JPanel();
        this.setPreferredSize( new Dimension( 300, 300 ) );

        BoxLayout lo= new BoxLayout( content, BoxLayout.Y_AXIS );
        content.setLayout( lo );

        JScrollPane pane= new JScrollPane( content );
        pane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        pane.getVerticalScrollBar().setUnitIncrement( pane.getFont().getSize() );
        
        this.removeAll();
        
        for ( int i=0; i<ss.length; i++ ) {
            ss[i]= ss[i].trim();
        }
        
        int i=0;

        if ( ss[0].length()>0 ) {
            FilterEditorPanel p = getEditorFor("|unbundle("+ss[0]+")", recycle.get(0) );
            editors.add(p);
            JPanel ll= onePanel(i);
            content.add( ll );
            i++;
            content.add( new JLabel( "--------" ) );
            implicitUnbundle= true;
        } else {
            implicitUnbundle= false;
        }
        ss= Arrays.copyOfRange( ss, 1, ss.length );
        
        for (String s : ss) {
            if ( s.length()>0 ) {
                FilterEditorPanel p = getEditorFor(s, recycle.get(editors.size()) );
                editors.add(p);
                JPanel ll= onePanel(i);
                content.add( ll );
                i++;
                content.add( new JLabel( "--------" ) );
            }
        }

        JPanel add= onePanel(-1);
        content.add( add );

        content.add(Box.createVerticalGlue());
           
        this.add( pane );
        
        //content.revalidate();
        this.revalidate();
    }

    private void updateImmediately() {
        Runnable run= new Runnable() {
            @Override
            public void run() {
                String f= getFilter();
                setFilter( f );
                if ( inputDs!=null ) setInput( inputDs );
                firePropertyChange( PROP_FILTER, null, f );
                //currentFilter= f;        
            }
        };
        try {
            SwingUtilities.invokeAndWait(run);
        } catch (InterruptedException ex) {
            Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * set the input dataset for each filter.
     * TODO: This does data processing on the event thread and will surely cause problems.
     */
    private void updateSoon( final QDataSet inputDs, final String filter) {
        //this.inputDs= null;
        timer.tickle(filter);
    }
    
    /**
     * the filter must be set before this is called.  This will set droplist labels, etc.
     * @param ds 
     */
    @Override
    public synchronized void setInput( QDataSet ds) {
        logger.entering( CLASS_NAME, "setInput", ds );
        
        if ( this.inputDs==ds ) {
            logger.fine("already set input...");
            return;
        } 
        
        this.inputDs= ds;
        
        String filter= getFilter();
        logger.log(Level.FINE, "filter: {0}", filter);
        
        String[] ss= filter.split("\\|");
        int i=0;
        int iss= 0;
        for (String s : ss) {
            s= s.trim();
            iss++;
            if ( s.length()>0 ) {
                FilterEditorPanel p = editors.get(i);
                if ( ds!=null ) {
                    
                    p.setInput(ds);
                    
                    if ( iss<ss.length ) {
                        try {
                            ds= DataSetOps.sprocess( "|"+s, ds, new NullProgressMonitor() );
                        } catch ( Exception ex ) {
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
                            updateSoon(inputDs, (String)evt.getNewValue() );
                        }
                    });
                }
                i=i+1;
            }
        }
        this.repaint();
        //this.revalidate();        
    }

    @Override
    public JPanel getPanel() {
        logger.entering( CLASS_NAME, "getPanel" );        
        return this;
    }
    
    /**
     * dataset for testing.
     * @see #main(java.lang.String[]) 
     * @return dataset for testing.
     */
    private static QDataSet getDataSet( String s ) {
        try {
            if ( s.equals("rank1TimeSeries" ) ) {
                return Ops.ripplesTimeSeries(20);
            } else if ( s.equals("qube" ) ) {
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
            } else {
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
        final QDataSet ds= getDataSet( "qube" );
        
        //ff.setFilter("|slice0(2)|cos()|collapse1()|butterworth(2,500,750,True)"); //butterworth(2,500,550,True)");
        //ff.setFilter("|butterworth(2,500,550,True)"
        //ff.setFilter("|unbundle('bx1')");
        ff.setFilter("|setDepend0Cadence(50s)");
        ff.setInput(ds);
        
        final JTextField tf= new JTextField();
        tf.setText(ff.getFilter());
        
        tf.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ff.setFilter(tf.getText());
                ff.setInput(ds);
            }
        });
        
        ff.addPropertyChangeListener( "filter", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                tf.setText(ff.getFilter());
            }
        });
        
        JPanel p= new JPanel( new BorderLayout() );
        p.add( ff );
        p.add( tf, BorderLayout.NORTH );
        
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

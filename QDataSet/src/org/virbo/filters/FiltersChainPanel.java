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
    private String currentFilter= null; // the currently implemented filter.
    private boolean implicitUnbundle= false;
    TickleTimer timer;

    private static final Logger logger= LoggerManager.getLogger("qdataset.filters");
    private static final String CLASS_NAME = FiltersChainPanel.class.getName();
    
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
        } else if ( f.matches("\\|butterworth\\((\\d),(\\S+),(\\S+)\\)") ) {
            result= new ButterworthFilterEditorPanel();
        } else if ( f.matches("\\|butterworth\\((\\d),(\\S+),(\\S+),(\\S+)\\)") ) {
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
            result= new SliceFilterEditorPanel();
        } else if ( f.matches("\\|slice(\\d)\\(\\'(\\S+)\\'\\)") ) { // TODO: FilterEditorPanel might choose to accept a filter.
            result= new SliceFilterEditorPanel();
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
        } else if ( f.matches("\\|diff\\(\\)") ) {
            result= new NoArgFilterEditorPanel();
        } else if ( f.matches("\\|getProperty\\((.*)\\)") ) {
            result= new GetPropertyEditorPanel();
        } else if ( f.matches("\\|putProperty\\((.*)\\)") ) {
            result= new PutPropertyFilterEditorPanel();
        } else {
            result= new AnyFilterEditorPanel();
        }
        result.setFilter(f);
        return result;
    }
    
    @Override
    public String getFilter() {
        
        logger.entering( CLASS_NAME, "getFilter" );
        
        final StringBuilder b= new StringBuilder();
        
        Runnable run= new Runnable() {
            @Override
            public void run() {
                int ifilter= 0;
                for ( FilterEditorPanel p: editors ) {
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
            } catch (InterruptedException ex) {
                Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(FiltersChainPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return b.toString();
    }

    private void deleteFilter( int fi ) {
        FilterEditorPanel p= editors.remove(fi);
        removeFocusListeners(p.getPanel());
        setFilter( getFilter() );
        updateSoon( inputDs, getFilter() );
    }
    
    private final FocusListener lostFocusListener= new FocusListener() {

        @Override
        public void focusGained(FocusEvent e) {
            logger.log(Level.FINE, "focusGained {0}", e.getComponent().getName() );
        }

        @Override
        public void focusLost(FocusEvent e) {
            logger.log(Level.FINE, "focusLost {0}", e.getComponent().getName() );
            if ( !getFilter().equals(currentFilter) ) {
                updateSoon(inputDs, null );
            } else {
                logger.log( Level.FINER, "... already up to date");
            }
        }
        
    };
    
    
    private final ActionListener requestUpdateListener= new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            logger.log(Level.FINE, "requestUpdateFrom {0}", e.getSource());
            updateSoon(inputDs, null );
        }
    };
    
    
    private final ChangeListener requestChangeListener= new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            updateSoon(inputDs, null );
        }
    };
        
    private void addFilterNew( int idx ) {
        AddFilterDialog afd= new AddFilterDialog();
        int r= JOptionPane.showConfirmDialog( this, afd, "Add Filter", JOptionPane.OK_CANCEL_OPTION );
        if ( r==JOptionPane.OK_OPTION ) {
            String ss= afd.getValue();
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
                    //if ( true ) {
                        addFilterNew(fi);
                    //} else {
                    //    addFilter(fi);
                    //}
                }
            } );
        } else {
           subAdd.setToolTipText( "insert new filter" );        
           subAdd.addActionListener( new ActionListener() {
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
    public void setFilter(String filter) {
        logger.entering( CLASS_NAME, "setFilter", filter );
        
        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.warning("must be called from event thread");
        }
        
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
        
        scrollPane.setViewportView(content);
        scrollPane.getVerticalScrollBar().setValue(scroll0);
        
        this.currentFilter= filter;
        
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
        logger.entering( CLASS_NAME, "updateSoon", filter);
        //this.inputDs= null;
        timer.tickle(filter); 
    }
    
    /**
     * the filter must be set before this is called.  This will set droplist labels, etc.
     * @param ds the dataset, or null.
     */
    @Override
    public void setInput( QDataSet ds) {
        logger.entering( CLASS_NAME, "setInput", ds );
        
        if ( this.inputDs==ds ) {
            logger.fine("already set input...");
            return;
        } 

        if ( !SwingUtilities.isEventDispatchThread() ) {
            logger.warning("not event thread");
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
     * @param s name of dataset, including: rank1TimeSeries, qube
     * @see #main(java.lang.String[]) 
     * @return dataset for testing.
     */
    protected static QDataSet getExampleDataSet( String s ) {
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
        final QDataSet ds= getExampleDataSet( "qube" );
        
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

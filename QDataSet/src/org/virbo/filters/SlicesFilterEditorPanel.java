/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.filters;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import org.virbo.dataset.QDataSet;
import static org.virbo.dataset.QDataSet.MAX_RANK;
import static org.virbo.filters.AbstractFilterEditorPanel.logger;

/**
 *
 * @author jbf
 */
public class SlicesFilterEditorPanel extends AbstractFilterEditorPanel implements FilterEditorPanel {
    
    static final long t0= System.currentTimeMillis();
    int[] qube= null;
    JSpinner[] spinners= new JSpinner[MAX_RANK];
    JCheckBox[] checkboxs= new JCheckBox[MAX_RANK];
    int rank= MAX_RANK;
    
    public SlicesFilterEditorPanel() {
        setFilter("|slices0(:)");
    }
    
    @Override
    public String getFilter() {
        logger.fine( "getFilter" );
        StringBuilder result= new StringBuilder("|slices(");
        for ( int i=0; i<rank; i++ ) {
            String sep= i==0 ? "" : ",";
            if ( checkboxs[i].isSelected() ) {
                result.append(sep).append(spinners[i].getValue());
            } else {
                result.append(sep).append("':'");
            }
        }
        result.append(")");
        return result.toString();
    }

    private JPanel getDimensionPanel( final int i, String s) throws NumberFormatException {
        JPanel p1= new JPanel(  );
        p1.setLayout( new BoxLayout( p1, BoxLayout.X_AXIS ) );
        //p1.setBorder( new TitledBorder("One Panel"));
        final JCheckBox cb1= new JCheckBox("Index"+i+":");
        cb1.setToolTipText("slice on this dimension");
        p1.add( cb1 );
        checkboxs[i]=cb1;
        cb1.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                spinners[i].setEnabled( cb1.isSelected() );
            }
        });
        p1.add( Box.createHorizontalStrut(14) );
        JSpinner sp1= new JSpinner();
        sp1.setMinimumSize(new Dimension(30,14));
        sp1.setPreferredSize(new Dimension(30,14));
        addMouseWheelListenerToSpinner( sp1 );
        spinners[i]= sp1;
        if ( s.startsWith("'") && s.endsWith("'") ) s= s.substring(1,s.length()-1);
        if ( s.equals(":") ) {
            sp1.setEnabled(false);
        } else {
            sp1.setValue(Integer.parseInt(s));
        }
        p1.add( sp1 );
        p1.add( Box.createGlue() );
        return p1;
    }

    public static void addMouseWheelListenerToSpinner( final JSpinner sliceIndexSpinner ) {
            
        sliceIndexSpinner.addMouseWheelListener( new MouseWheelListener() { 

            @Override
            public void mouseWheelMoved(MouseWheelEvent evt) {
                SpinnerNumberModel snm= ((SpinnerNumberModel)sliceIndexSpinner.getModel());
                int newIndex= snm.getNumber().intValue() - evt.getWheelRotation();
                if ( newIndex<0 ) newIndex= 0;
                Number nmax= (Number)snm.getMaximum();
                if ( nmax!=null ) {
                   int maxIndex= nmax.intValue();
                   if ( newIndex>maxIndex ) newIndex= maxIndex;
                }
                snm.setValue( newIndex );
            }
        } );        
    }
    
    @Override
    public final void setFilter(String filter) {
        logger.log(Level.FINE, "setFilter {0}", filter);
        
        Pattern p= Pattern.compile("\\|slices\\((.*)\\)");
        Matcher m= p.matcher(filter);
        if ( m.matches() ) {
            String arg= m.group(1);
            String[] ss= arg.split(",");
            rank= ss.length;
            this.removeAll();
            this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS )  );
            for ( int i=0; i<rank; i++ ) {
                add( getDimensionPanel(i, ss[i]) );
            }
        }
    }

    @Override
    public void setInput(QDataSet ds) {
        logger.log(Level.FINE, "setInput {0}", ds.toString() );
        String[] depNames1= FilterEditorPanelUtil.getDimensionNames(ds);
        this.rank= ds.rank();
        for ( int i=0; i<rank; i++ ) {
            QDataSet dep= (QDataSet) ds.property("DEPEND_"+i);
            if ( checkboxs[i]==null ) {
                add( getDimensionPanel( i,":") );
            }
            checkboxs[i].setText(depNames1[i]);
            checkboxs[i].setToolTipText("slice on "+depNames1[i]);
            int max;
            if ( dep==null ) {
                max= Integer.MAX_VALUE;
            } else {
                if ( dep.rank()==1 ) {
                    max= dep.length();
                } else if ( dep.rank()==2 ) { 
                    max= dep.length(0);
                } else {
                    max= Integer.MAX_VALUE;                    
                }
            }
            spinners[i].setModel( new SpinnerNumberModel( ((Integer)spinners[i].getValue()).intValue(), 0, max, 1 ) );
        }
    }
    

    public static void main( String[] args ) {
        FilterEditorPanel filter= new SlicesFilterEditorPanel();
        QDataSet ds= FiltersChainPanel.getExampleDataSet("qube");
        filter.setFilter("|slices(0,:)");
        filter.setInput(ds);
        JOptionPane.showMessageDialog( null, filter);
        System.err.println( filter.getFilter() );
    }
    
}

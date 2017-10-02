/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.components.propertyeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.das2.graph.ColorUtil;
import org.das2.util.DesktopColorChooserPanel;

/**
 *
 * @author jbf
 */
public class NamedColorChooserPanel extends AbstractColorChooserPanel {

    JList l;
    
    @Override
    public void updateChooser() {
        Color c= getColorFromModel();
        String s= ColorUtil.nameForColor(c);
        if ( !s.startsWith("#") ) {
            l.setSelectedValue( s, true );
        } else {
            l.clearSelection();
            l.repaint();
        }
    }

    @Override
    protected void buildChooser() {
        l= new JList();
        final Map<String,Color> colors= ColorUtil.getNamedColors();
        final DefaultListModel m= new DefaultListModel( );
        for ( String s: colors.keySet() ) {
            m.addElement(s);
        }
        l.setModel(m);
        ListCellRenderer r= new ListCellRenderer() {
            JLabel label= new JLabel();
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                label.setText(String.valueOf(value));
                Color cc=  colors.get((String)value);
                label.setIcon( DesktopColorChooserPanel.colorIcon( cc, 24, 16 ) );
                if( isSelected ) {
                    label.setBackground( list.getSelectionBackground() );
                    label.setForeground( list.getSelectionForeground() );
                    label.setOpaque(true);
                } else {
                    label.setBackground( list.getBackground() );
                    label.setForeground( list.getForeground() );
                    label.setOpaque(false);
                }
                return label;
            }
        };
        l.setCellRenderer(r);
        l.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        l.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String colorName= String.valueOf( l.getSelectedValue() );
                getColorSelectionModel().setSelectedColor(colors.get(colorName));
            }
        });
        
        this.setLayout(new BorderLayout());
        
        JScrollPane jsp= new JScrollPane(l,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        this.add(jsp,BorderLayout.CENTER);
    }

    @Override
    public String getDisplayName() {
        return "Named Colors";
    }

    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }
    
}

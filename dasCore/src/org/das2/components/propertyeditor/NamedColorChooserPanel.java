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

    @Override
    public void updateChooser() {
        
    }

    @Override
    protected void buildChooser() {
        JList l= new JList();
        final Map<String,Color> colors= ColorUtil.getNamedColors();
        final DefaultListModel m= new DefaultListModel( );
        for ( String s: colors.keySet() ) {
            m.addElement(s);
        }
        l.setModel(m);
        ListCellRenderer r= new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lss= new JLabel(value.toString());
                Color cc=  colors.get((String)value);
                lss.setIcon( DesktopColorChooserPanel.colorIcon( cc, 24, 16 ) );
                return lss;
            }
        };
        l.setCellRenderer(r);
        l.addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                String colorName= (String)m.getElementAt(e.getFirstIndex());
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

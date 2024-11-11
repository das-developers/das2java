
package org.das2.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ListSelectionEvent;

/**
 * Show list of 140 named web colors, like SaddleBrown and OliveDrab
 * @author jbf
 */
public class NamedColorChooserPanel extends AbstractColorChooserPanel {

    JList l;
    private boolean ignoreChanges=false;
    
    private static final Logger logger= LoggerManager.getLogger("das2.util");
    
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

    private static void updateFavorites(String[] ss, JList myFavoritesList ) {
        final DefaultListModel m2= new DefaultListModel( );
        for ( String s: ss ) {
            m2.addElement(s);
        }
        myFavoritesList.setModel(m2);
    }
    
    @Override
    protected void buildChooser() {
        l= new JList();

        final JList myFavoritesList= new JList();
        
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
                Color cc=  ColorUtil.decodeColor((String)value);
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
        l.addListSelectionListener((ListSelectionEvent e) -> {
            String colorName= (String)l.getSelectedValue();
            if ( colorName==null ) {
                return;
            }
            getColorSelectionModel().setSelectedColor( ColorUtil.decodeColor(colorName) );
            
            if ( !ignoreChanges ) {
                Preferences prefs= Preferences.userNodeForPackage(this.getClass());
                String ps = prefs.get("namedPalette", "black,white,DodgerBlue");
                List<String> ss= new LinkedList( Arrays.asList(ps.split(",")) );
                ss.remove(colorName);
                ss.add(colorName);
                while ( ss.remove("null") ) {
                    logger.finer("removed null which got into history");
                }
                while ( ss.size()>6 ) {
                    ss.remove(0);
                }
                prefs.put( "namedPalette", String.join(",",ss) );
                updateFavorites(ss.toArray(new String[ss.size()]),myFavoritesList);
            }
            
        });
        
        this.setLayout(new BorderLayout());
        
        String ps = Preferences.userNodeForPackage(this.getClass()).get("namedPalette", "black,white,DodgerBlue");
        
        String[] ss= ps.split(",");
        updateFavorites(ss,myFavoritesList);
        myFavoritesList.setCellRenderer(r);
        myFavoritesList.addListSelectionListener((ListSelectionEvent e) -> {
            if ( ignoreChanges ) return;
            String colorName= String.valueOf( myFavoritesList.getSelectedValue() );
            if ( colorName==null ) return;
            ignoreChanges= true;
            getColorSelectionModel().setSelectedColor( ColorUtil.decodeColor(colorName) );
            ignoreChanges= false;
        });
        JScrollPane jsp2= new JScrollPane(myFavoritesList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.add(jsp2,BorderLayout.EAST);
        
        JScrollPane jsp= new JScrollPane(l,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        this.add(jsp,BorderLayout.CENTER );
        
        JButton findClose= new JButton("Find Close Color");
        findClose.setAction( getFindCloseAction() );
        this.add(findClose, BorderLayout.SOUTH);
    }

    private Action getFindCloseAction() {
        return new AbstractAction("find close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Map<String,Color> colors= ColorUtil.getNamedColors();
                
                Color selected= getColorFromModel();
                float[] hsv= new float[3];
                Color.RGBtoHSB( selected.getRed(), selected.getGreen(), selected.getBlue() , hsv );
                
                double distance= Double.MAX_VALUE;
                String bestName= "";
                
                for ( Entry<String,Color> color: colors.entrySet() ) {
                    Color c= color.getValue();
                    float[] components= new float[3];
                    Color.RGBtoHSB( c.getRed(), c.getGreen(), c.getBlue() , components );
                    double d= Math.abs( components[0] - hsv[0] );
                    if ( d>0.5 ) d= 1-d;
                    double dv= Math.abs( components[2]- hsv[2] );
                    double ds= Math.abs( components[1]- hsv[1] );
                    d= d + dv + ds;
                    if ( d<distance ) {
                        distance= d;
                        bestName= color.getKey();
                    }
                }
                getColorSelectionModel().setSelectedColor( colors.get( bestName ) );
                
            }
        };
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

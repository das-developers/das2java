/* File: DasProperties.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

public class DasProperties extends Properties {
    
    // Contains the global user-configurable parameters that are
    // persistent between sessions.
    
    private static RenderingHints hints;
    private static ArrayList propertyOrder;
    private static Editor editor;
    private static JFrame jframe;
    
    private DasProperties() {
        super();
        hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        setDefaults();
        propertyOrder= new ArrayList();
        readPersistentProperties();
        setPropertyOrder();
    }
    
    private void setPropertyOrder() {
        propertyOrder.add(0,"username");
        propertyOrder.add(1,"password");
        propertyOrder.add(2,"debugLevel");
        for (Iterator i= this.keySet().iterator(); i.hasNext(); ) {
            String s= (String)i.next();
            if (!propertyOrder.contains(s)) {
                propertyOrder.add(s);
            }
        }
    }
    
    private void setDefaults() {
        setProperty("username","");
        setProperty("password","");
        setProperty("debugLevel","endUser");
        setProperty("defaultServer","http://www-pw.physics.uiowa.edu/das/dasServer");
    }
    
    public static RenderingHints getRenderingHints() {
        return instance.hints;
    }
    
    public static boolean isHeadless() {
        return "true".equals(System.getProperty("java.awt.headless"));
    }
    
    public static void setHeadless(boolean headless) {
        if ( headless ) {
            System.setProperty("java.awt.headless", "true");
        } else {
            System.setProperty("java.awt.headless", "false");
        }
    }
    
    public static DasProperties getInstance() {
        return instance;
    }
    
    private static class DasPropertiesTableModel extends AbstractTableModel {
        public int getColumnCount() { return 2;  }
        public int getRowCount() { return instance.size(); }
        public Object getValueAt(int row, int col) {
            String propertyName= (String)propertyOrder.get(row);
            String value;
            if (col==0) {
                value= propertyName;
            } else {
                value= instance.getProperty(propertyName);
                if (propertyName.equals("password")) {
                    value="";
                }
            }
            return value;
        }
        public void setValueAt(Object value, int row, int col) {
            String propertyName= (String)propertyOrder.get(row);
            if (propertyName.equals("password")) {
                if (!value.toString().equals("")) {
                    value= edu.uiowa.physics.pw.das.util.Crypt.crypt(value.toString());
                }
            } else if ( propertyName.equals("debugLevel") ) {
                edu.uiowa.physics.pw.das.util.DasDie.setDebugVerbosityLevel(value.toString());
            }
            instance.setProperty(propertyName,value.toString());
            editor.setDirty(true);
        }
        
        public boolean isCellEditable(int row, int col) { return (col==1); }
    }
    
    private static TableModel getTableModel() {
        return new DasPropertiesTableModel();
    }
    
    private static JTable getJTable() {
        return new JTable(getTableModel()) {
            {setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
                 public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                     String propertyName= (String)propertyOrder.get(row);
                     if (propertyName.equals("password") && column==1 ) {
                         //String stars= "**************************************************";
                         //int nstars= instance.getProperty("password").length();
                         //nstars= nstars > stars.length() ? stars.length() : nstars;
                         //stars= stars.substring(0,nstars);
                         return super.getTableCellRendererComponent( table, "* * * *", isSelected, hasFocus, row, column );
                     } else {
                         return super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
                     }
                 }
             });}
             public TableCellEditor getCellEditor(int row, int col) {
                 String propertyName= (String)propertyOrder.get(row);
                 if (propertyName.equals("password")) {
                     return new DefaultCellEditor(new JPasswordField());
                 } else if (propertyName.equals("debugLevel")) {
                     String[] data= {"endUser","dasDeveloper"};
                     return new DefaultCellEditor(new JComboBox(data));
                 } else {
                     return super.getCellEditor(row,col);
                 }
             }
        };
    }
    
    
    private static TableCellEditor getTableCellEditor() {
        return new DefaultCellEditor(new JTextField()) {
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                String propertyName= (String)propertyOrder.get(row);
                if (propertyName.equals("password")) {
                    return new JPasswordField();
                } else if (propertyName.equals("debugLevel")) {
                    String[] data= {"endUser","dasDeveloper"};
                    return new JList(data);
                } else {
                    return super.getTableCellEditorComponent(table,value,isSelected,row,column);
                }
            }
        };
    }
    
    public static class Editor extends JPanel implements ActionListener {
        JButton saveButton;
        
        Editor() {
            super();
            setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
            
            JTable jtable= getJTable();
            add(jtable);
            
            JPanel controlPanel= new JPanel();
            controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.X_AXIS));
            
            controlPanel.add(Box.createHorizontalGlue());
            
            JButton b;
            saveButton= new JButton("Save");
            saveButton.setActionCommand("Save");
            saveButton.addActionListener(this);
            saveButton.setToolTipText("save to $HOME/.das2rc");
            
            controlPanel.add(saveButton);
            
            b= new JButton("Dismiss");
            b.addActionListener(this);
            controlPanel.add(b);
            
            add( Box.createVerticalGlue() );
            
            add(controlPanel);
            
        }
        
        public void actionPerformed(ActionEvent e) {
            String command= e.getActionCommand();
            if (command.equals("Save")) {
                instance.writePersistentProperties();
                setDirty(false);
            } else if (command.equals("Dismiss")) {
                jframe.dispose();
            }
            
        }
        
        public void setDirty(boolean dirty){
            if (dirty) {
                saveButton.setText("Save*");
            } else {
                saveButton.setText("Save");
            }
        }
    }
    
    public static void showEditor() {
        jframe= new JFrame("Das Properities");
        editor= new Editor();
        
        jframe.setSize(400,300);
        jframe.setContentPane(editor);
        jframe.setVisible(true);
    }
    
    public static DasProperties instance = new DasProperties();
    
    public void readPersistentProperties() {
        
        try {
            String file= System.getProperty("user.home")+System.getProperty("file.separator")+".das2rc";
            File f= new File(file);
            
            if (f.canRead()) {
                try {
                    InputStream in= new FileInputStream(f);
                    load(in);
                    in.close();
                } catch (IOException e) {
                    edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
                }
            } else {
                try {
                    OutputStream out= new FileOutputStream(f);
                    store(out,"");
                    out.close();
                } catch (IOException e) {
                    edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
                }
            }
        } catch ( SecurityException ex ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.INFORM, "Unable to read persistent properties");
        }
    }
    
    public void writePersistentProperties() {
        
        String file= System.getProperty("user.home")+System.getProperty("file.separator")+".das2rc";
        File f= new File(file);
        
        if (f.canWrite()) {
            edu.uiowa.physics.pw.das.util.DasDie.println("Attempt to write .das2rc...");
            try {
                OutputStream out= new FileOutputStream(f);
                store(out,"");
                out.close();
            } catch (IOException e) {
                edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
            }
        } else {
            DasException e= new edu.uiowa.physics.pw.das.DasIOException("Can't write to file "+f);
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(e);
        }
    }
    
    public static void main(String[] args) {
        DasProperties.showEditor();
    }
}

/* File: DasAxisSelector.java
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

package edu.uiowa.physics.pw.das.components;

/**
 *
 * @author  jbf
 */
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.graph.DasAxis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class DasAxisSelector extends javax.swing.JPanel implements ActionListener {
    
    private DasAxis axis= null;
    
    JTextField idStart= null;
    JTextField idStop= null;
    
    /** Creates a new instance of DasTimeRangeSelector */
    private DasAxisSelector() {
        super();
        buildComponents();
    }
    
    private void buildComponents() {
        this.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        idStart= new JTextField("");
        idStart.setSize(9,1);
        idStart.addActionListener(this);
        idStart.setActionCommand("setMinimum");
        this.add(idStart);
        
        idStop= new JTextField("");
        idStop.setSize(9,1);
        idStop.addActionListener(this);
        idStop.setActionCommand("setMaximum");
        this.add(idStop);
        
    }
    
    public DasAxisSelector(DasAxis axis) {
        this();
        this.axis= axis;
        update();
    }
    
    public double getStartTime() {
        double s1= Double.valueOf(idStart.getText()).doubleValue();
        return s1;
    }
    
    public double getEndTime() {
        double s1= Double.valueOf(idStop.getText()).doubleValue();
        return s1;
    }
    
    private void update() {
        DecimalFormat df= new DecimalFormat();
        df.setMaximumFractionDigits(2);
        idStart.setText(df.format(axis.getDataMinimum()));
        idStop.setText(df.format(axis.getDataMaximum()));
        idStart.setText(""+axis.getDataMinimum());
        idStop.setText(""+axis.getDataMaximum());
    }
    
    public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
        String command= actionEvent.getActionCommand();
        update();
        if (command.equals("setMinimum")) {
            try {
                axis.setDataRange(Datum.create(Double.valueOf(idStart.getText()).doubleValue(),axis.getUnits()),
                axis.getDataMaximum());
            } catch (NumberFormatException e) {
                edu.uiowa.physics.pw.das.util.DasDie.println(e);
            }
        } else if (command.equals("setMaximum")) {
            try {
                axis.setDataRange(axis.getDataMinimum(),
                 Datum.create(Double.valueOf(idStop.getText()).doubleValue(), axis.getUnits() ));
            } catch (NumberFormatException e) {
                edu.uiowa.physics.pw.das.util.DasDie.println(e);
            }
        }
    }
    
}

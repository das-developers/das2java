/* File: MessageBox.java
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

package edu.uiowa.physics.pw.das.util;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 *
 * @author  eew
 */
public class MessageBox extends Dialog {
    
    public static final int OK = 1;
    public static final int CANCEL = 2;
    public static final int YES = 4;
    public static final int NO = 8;
    
    public static final int YES_NO = 12;
    public static final int YES_NO_CANCEL = 14;
    public static final int OK_CANCEL = 3;
    public static final int DEFAULT = 3;
    
    private int result;
    private int type;
    private Button yes; 
    private Button no; 
    private Button ok; 
    private Button cancel;
    
    /** Creates a new instance of MessageBox */
    private MessageBox(Frame owner) {
        super(owner);
    }
    
    private MessageBoxListener createListener()
    {
        return new MessageBoxListener();
    }
    
    public static int showModalMessage(Frame owner, int type, String title, String message)
    {
        return showModalMessage(owner, type, title, breakLines(message));
    }
    
    public static int showModalMessage(Frame owner, int type, String title, String[] message)
    {
        MessageBox mb = new MessageBox(owner);
        MessageBoxListener mbl = mb.createListener();
        Panel messagePanel, buttonPanel;
        
        if (type == 0) type = OK_CANCEL;
        mb.type = type;
        
        mb.setTitle(title);
        mb.setModal(true);
        mb.setLayout(new BorderLayout());
        mb.addWindowListener(mbl);
        
        messagePanel = new Panel(new GridLayout(0,1));
        for (int i = 0; i < message.length; i++)
        {
            messagePanel.add(new Label(message[i]));
        }
        
        mb.add(messagePanel, "Center");
        
        buttonPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));
        if ((type & OK) == OK)
        {
            mb.ok = new Button("Ok");
            mb.ok.addActionListener(mbl);
            buttonPanel.add(mb.ok);
        }
        if ((type & YES) == YES)
        {
            mb.yes = new Button("Yes");
            mb.yes.addActionListener(mbl);
            buttonPanel.add(mb.yes);
        }
        if ((type & NO) == NO)
        {
            mb.no = new Button("No");
            mb.no.addActionListener(mbl);
            buttonPanel.add(mb.no);            
        }
        if ((type & CANCEL) == CANCEL)
        {
            mb.cancel = new Button("Cancel");
            mb.cancel.addActionListener(mbl);
            buttonPanel.add(mb.cancel);
        }
        
        mb.add(messagePanel, "Center");
        mb.add(buttonPanel, "South");
        
        mb.pack();
        Dimension od = owner.getSize();
        Point op = owner.getLocation();
        Dimension md = mb.getSize();
        mb.setLocation(op.x + (od.width - md.width)/2, op.y + (od.height - md.height)/2);
        
        mb.show();
        
        return mb.result;
        
    }
    
    private static String[] breakLines(String s)
    {
        java.util.StringTokenizer st = new java.util.StringTokenizer(s, "\n", false);
        int lines = st.countTokens();
        String[] list = new String[lines];
        for (int i = 0; i < lines; i++)
            list[i] = st.nextToken();
        return list;
    }
    
    private class MessageBoxListener extends WindowAdapter implements ActionListener
    {
        public void windowClosing(WindowEvent e)
        {
            result = CANCEL;
            hide();
        }
        
        public void actionPerformed(ActionEvent e)
        {
            if (e.getSource() == ok)
            {
                result = OK;
            }
            else if (e.getSource() == cancel)
            {
                result = CANCEL;
            }
            else if (e.getSource() == yes)
            {
                result = YES;
            }
            else if (e.getSource() == no)
            {
                result = NO;
            }
            hide();
        }
    }
}

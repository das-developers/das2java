/* File: AccountManager.java
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
package edu.uiowa.physics.pw.das.client;

import edu.uiowa.physics.pw.das.client.DasServer;

import javax.swing.*;
import java.awt.*;

public class AccountManager extends JPanel {
    
    JLabel feedbackLabel;
    JTextField tfUser;
    
    JPasswordField tfPass;
    JPasswordField tfNewPass;
    JPasswordField tfConfirmPass;
    
    DasServer dasServer;
    Key key;
    
    public AccountManager(DasServer dasServer) {
        
        this.dasServer= dasServer;
        
        setLayout( new BoxLayout(this,BoxLayout.Y_AXIS));
        
        add(new JLabel(dasServer.getName(),JLabel.LEFT));
        add(new JLabel(dasServer.getLogo(),JLabel.LEFT));
        
        add(new JLabel("Changing Password"));
        
        add(new JLabel("Username: ",JLabel.LEFT));
        tfUser= new JTextField();
        add(tfUser);
        
        add(new JLabel("Password: ",JLabel.LEFT));
        tfPass= new JPasswordField();
        add(tfPass);
        
        add(new JLabel("New Password: ",JLabel.LEFT));
        tfNewPass= new JPasswordField();
        add(tfNewPass);
        
        add(new JLabel("Confirm Password: ",JLabel.LEFT));
        tfConfirmPass= new JPasswordField();
        add(tfConfirmPass);
        
        feedbackLabel= new JLabel("",JLabel.LEFT);
        add(feedbackLabel);
        
    }
    
    public void changePassword() {
        
        int okayCancel=JOptionPane.OK_OPTION;
        boolean success= false;
        
        while ( okayCancel==JOptionPane.OK_OPTION && success==false ) {
            okayCancel=
            JOptionPane.showConfirmDialog(null,this,"Account Manager",
            JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            
            if (okayCancel==JOptionPane.OK_OPTION) {                
                String pass= String.valueOf(tfPass.getPassword());                
                String newPass= String.valueOf(tfNewPass.getPassword());
                String confirmPass= String.valueOf(tfConfirmPass.getPassword());
                
                if (newPass.equals(confirmPass)) {
                    try {
                        dasServer.changePassword(tfUser.getText(),pass,newPass);
                        success= true;
                    } 
                    catch ( org.das2.DasException e ) {
                        feedbackLabel.setText(e.toString());                        
                    }                        
                    catch ( Exception e ) {
                        feedbackLabel.setText("Failed connect to server");
                    }
                } else {
                   feedbackLabel.setText("Passwords do not match");
                   feedbackLabel.setForeground(Color.red);
                }                
            }
        }        
    }
    
    public static void main( String[] args ) throws Exception {
        AccountManager a= new AccountManager(DasServer.create(new java.net.URL("http://www-pw.physics.uiowa.edu/das/dasServer")));
        a.changePassword();
        
    }
}


/* File: Authenticator.java
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

import edu.uiowa.physics.pw.das.DasProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.net.*;
import java.util.Properties;

public class Authenticator extends JPanel {
    
    JLabel feedbackLabel;
    JTextField tfUser;
    
    JPasswordField tfPass;
    
    DasServer dasServer;
    
    public Authenticator(DasServer dasServer) { 
        this( dasServer, "" );
    }
    
    public Authenticator(DasServer dasServer, String restrictedResourceLabel ) {

        this.dasServer= dasServer;
        
        setLayout( new BoxLayout(this,BoxLayout.Y_AXIS));
        
        add(new JLabel(dasServer.getName(),JLabel.LEFT));
        add(new JLabel(dasServer.getLogo(),JLabel.LEFT));
        
        if ( ! "".equals( restrictedResourceLabel ) ) {
            add( new JLabel( ""+restrictedResourceLabel ) );
        }
        
        add(new JLabel("Username: ",JLabel.LEFT));
        tfUser= new JTextField();
        Properties xxx=DasProperties.getInstance();
        if (!"".equals(DasProperties.getInstance().getProperty("username"))) {
            tfUser.setText(DasProperties.getInstance().getProperty("username"));
        }
        add(tfUser);
        
        add(new JLabel("Password: ",JLabel.LEFT));
        tfPass= new JPasswordField();
        if (!"".equals(DasProperties.getInstance().getProperty("password"))) {
            tfPass.setText(DasProperties.getInstance().getProperty("password"));
        }
        add(tfPass);
        
        feedbackLabel= new JLabel("",JLabel.LEFT);
        add(feedbackLabel);        
        
        String lockingKeyWarning= "";
        if ( Toolkit.getDefaultToolkit().getLockingKeyState( KeyEvent.VK_CAPS_LOCK ) ) {
            lockingKeyWarning+= ", CAPS LOCK is on";
        }

        if ( Toolkit.getDefaultToolkit().getLockingKeyState( KeyEvent.VK_NUM_LOCK ) ) {
            lockingKeyWarning+= ", NUM LOCK is on";
        }
        
        if ( !"".equals( lockingKeyWarning ) ) {
            feedbackLabel.setText(lockingKeyWarning.substring(2));
        }

    }
    
    public Key authenticate() {
        
        Key result=null;
        int okayCancel=JOptionPane.OK_OPTION;
                
        while ( okayCancel==JOptionPane.OK_OPTION && result==null ) {
            okayCancel=       
                JOptionPane.showConfirmDialog(null,this,"Authenticator",
                        JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        
            if (okayCancel==JOptionPane.OK_OPTION) {
                char[] ipass= tfPass.getPassword();
                String pass= String.valueOf(tfPass.getPassword());
                if (pass.equals(DasProperties.getInstance().getProperty("password"))) {
                    pass= "sendPropertyPassword";
                }
                
                String username= tfUser.getText().trim();
                try {
                    result= dasServer.authenticate(username,pass);
                    if (result==null) {
                        feedbackLabel.setText("Login incorrect");
                        feedbackLabel.setForeground(Color.red);
                    }
                } catch ( Exception e ) {
                    feedbackLabel.setText("Failed connect to server");
                }
                DasProperties.getInstance().setProperty("username",username);
                if (!pass.equals("sendPropertyPassword")) {
                    String cryptPass= edu.uiowa.physics.pw.das.util.Crypt.crypt(pass);
                    DasProperties.getInstance().setProperty("password",cryptPass);
                }
                DasProperties.getInstance().writePersistentProperties();
            }
        }
        
        return result;
    }
    
    public static void main( String[] args ) {
        try {
            Authenticator a= new Authenticator(DasServer.create(new URL("http://www-pw.physics.uiowa.edu/das-test/das2ServerEEW")));
            edu.uiowa.physics.pw.das.util.DasDie.println(a.authenticate());
        }
        catch (MalformedURLException mue) {
            mue.printStackTrace();
        }
        
    }
}


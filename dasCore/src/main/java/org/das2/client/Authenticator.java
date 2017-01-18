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
package org.das2.client;

import org.das2.DasApplication;
import org.das2.DasProperties;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.util.prefs.Preferences;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class Authenticator extends JPanel {
    
    JLabel feedbackLabel;
    JTextField tfUser;
    
    JPasswordField tfPass;
    
    DasServer dasServer;
    String resourceId;  // identifies the DasServer and the resource
    String resource;     
    
    final String KEY_AUTOLOGIN= "autoLogin";
    final String KEY_SAVECREDENTIALS= "saveCredentials";
    
    Preferences prefs= Preferences.userNodeForPackage( Authenticator.class );
    
    public Authenticator(DasServer dasServer) {
        this( dasServer, "" );
    }
    
    public Authenticator(DasServer dasServer, String restrictedResourceLabel ) {
        
        this.dasServer= dasServer;
        this.resourceId= String.valueOf(  dasServer.getURL() ) + "::" + restrictedResourceLabel;;
        this.resource= restrictedResourceLabel;
        
        setLayout( new BoxLayout(this,BoxLayout.Y_AXIS));
        
        add(new JLabel(dasServer.getName(),JLabel.LEFT));
        add(new JLabel(dasServer.getLogo(),JLabel.LEFT));
        
        if ( ! "".equals( restrictedResourceLabel ) ) {
            add( new JLabel( ""+restrictedResourceLabel ) );
        }
        
        add(new JLabel("Username: ",JLabel.LEFT));
        tfUser= new JTextField();
        add(tfUser);
        
        add(new JLabel("Password: ",JLabel.LEFT));
        tfPass= new JPasswordField();
        add(tfPass);
        
        JPanel prefsPanel= new JPanel();
        prefsPanel.setLayout( new BoxLayout( prefsPanel, BoxLayout.Y_AXIS ) );
        
        {
            final JCheckBox cb= new JCheckBox(  );
            cb.setSelected( prefs.getBoolean( KEY_SAVECREDENTIALS, true) );
            cb.setAction( new AbstractAction("save credentials" ) {
                public void actionPerformed( ActionEvent e ) {
                    prefs.putBoolean( KEY_SAVECREDENTIALS,cb.isSelected() );
                }
            } );
            prefsPanel.add( cb );
        }
        
        {
            final JCheckBox cb= new JCheckBox( );
            cb.setSelected( prefs.getBoolean(KEY_AUTOLOGIN,false) );
            cb.setAction( new AbstractAction( "allow automatic logins" ) {
                public void actionPerformed( ActionEvent e ) {
                    prefs.putBoolean( KEY_AUTOLOGIN,cb.isSelected() );
                }
            } );
            prefsPanel.add( cb );
        }
        
        add( prefsPanel );
        
        if ( prefs.getBoolean(KEY_SAVECREDENTIALS,true) ) {
            String username= prefs.get( resourceId+".username", DasProperties.getInstance().getProperty("username") );
            if (!"".equals(username)) tfUser.setText(username);
            String passwordCrypt= prefs.get( resourceId+".passwordCrypt", DasProperties.getInstance().getProperty("password") );
            if (!"".equals(passwordCrypt)) tfPass.setText("usePrefs");
        }
        
        feedbackLabel= new JLabel("",JLabel.LEFT);
        feedbackLabel.setForeground(Color.red);
        add(feedbackLabel);
        
        try {
            String lockingKeyWarning= "";
            if ( Toolkit.getDefaultToolkit().getLockingKeyState( KeyEvent.VK_CAPS_LOCK ) ) {
                lockingKeyWarning+= ", CAPS LOCK is on";
            }
                        
            if ( !"".equals( lockingKeyWarning ) ) {
                feedbackLabel.setText(lockingKeyWarning.substring(2));
            }
        } catch ( UnsupportedOperationException e ) {
            //  I sure hope they don't have caps lock on!
        }
        
    }
        
    public Key authenticate() {
        
        Key result=null;
        int okayCancel=JOptionPane.OK_OPTION;
        
        if ( prefs.getBoolean(KEY_AUTOLOGIN,false) ) {
            String username= prefs.get( resourceId+".username", DasProperties.getInstance().getProperty("username") );
            String passCrypt= prefs.get( resourceId+".passwordCrypt", DasProperties.getInstance().getProperty("password") );
            result= dasServer.authenticate(username,passCrypt);            
            if ( result!=null ) {                
                if ( checkGroup(result) ) {
                    return result;
                } else {
                    feedbackLabel.setText(username+" doesn't have access to "+resource);
                }
            } else {
                feedbackLabel.setText("stored credentials rejected by server");
            }
        }
        
        Component parent=DasApplication.getDefaultApplication().getMainFrame();
        
        while ( okayCancel==JOptionPane.OK_OPTION && result==null ) {
            okayCancel=
                    JOptionPane.showConfirmDialog(parent,this,"Authenticator",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            
            if (okayCancel==JOptionPane.OK_OPTION) {
                
                String username= tfUser.getText().trim();
                String password= String.valueOf( tfPass.getPassword() );
                
                String passCrypt= null;
                if ( password.equals("usePrefs") ) {
                    passCrypt= prefs.get( resourceId+".passwordCrypt", DasProperties.getInstance().getProperty("password") );
                } else {
                    passCrypt= org.das2.util.Crypt.crypt(password);
                }
                
                try {
                    result= dasServer.authenticate(username,passCrypt);
                    if (result==null) {
                        feedbackLabel.setText("Login incorrect");                        
                    } else {
                        if ( !checkGroup( result ) ) {
                            feedbackLabel.setText(username+" doesn't have access to "+resource );
                            result= null;
                        }
                        if (prefs.getBoolean(KEY_SAVECREDENTIALS,true) ) {
                            prefs.put( resourceId+".username", username );
                            prefs.put( resourceId+".passwordCrypt", passCrypt );
                            prefs.flush();
                        }
                    }
                } catch ( Exception e ) {
                    feedbackLabel.setText("Failed connect to server");                    
                }
            }
        }
        
        return result;
    }

    private boolean checkGroup(Key result) {
        if ( resource.equals("") ) {
            return true;
        } else {
            List groups= dasServer.groups(result);
            return ( groups.contains( resource ) );
        }
    }
    
}


/* File: Splash.java
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

package org.das2.util;

import java.util.logging.*;
import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 *
 * @author  jbf
 */
public class Splash extends JWindow {
    
    private static Splash instance=null;
    
    private Handler handler;
    private final JLabel messageLabel;
    
    public static String getVersion() {
        //DName: das_20030505_01_beta D
        String cvsTagName= "$Name$";
        String version;
        if (cvsTagName.length()<=9) {
            version="untagged_version";
        } else {
            version= cvsTagName.substring(6,cvsTagName.length()-2);
        }
        return version;
    }
    
    public Handler getLogHandler() {
        if ( handler==null ) {
            handler= createhandler();
        }
        return handler;
    }
    
    private Handler createhandler() {
        Handler result= new Handler() {
            @Override
            public void publish( LogRecord logRecord ) {                
                System.out.println( logRecord.getMessage() );
                messageLabel.setText(logRecord.getMessage() );
            }
            @Override
            public void flush() {}
            @Override
            public void close() {}
        };
        return result;
    }
    
    private static ImageIcon getSplashImage() {
        URL url= Splash.class.getResource("/images/dasSplash.gif");
        if ( url==null ) return null;
        return new ImageIcon(url);        
    }
    
    public static Splash getInstance() {
        if ( instance==null ) {
            instance= new Splash();
        }
        return instance;
    }
    
    public static void showSplash() {
        getInstance();
        instance.setVisible(true);
    }
         
    public static void hideSplash() {
        getInstance();
        instance.setVisible(false);
    }
    
    /** Creates a new instance of Splash */
    public Splash() {
        super();
        JPanel panel= new JPanel(new BorderLayout());
        panel.add(new JLabel(getSplashImage()),BorderLayout.CENTER);
        
        Box bottomPanel= Box.createHorizontalBox();
        
        messageLabel= new JLabel("");
        messageLabel.setMinimumSize( new Dimension( 200, 10 ) );
        bottomPanel.add( messageLabel );
        bottomPanel.add( Box.createHorizontalGlue() );
        bottomPanel.add( new JLabel("version "+getVersion()+"   ",JLabel.RIGHT) );
        
        panel.add( bottomPanel, BorderLayout.SOUTH );
        this.setContentPane(panel);
        this.pack();
        //this.setLocation(300,300);
        this.setLocationRelativeTo(null);
    }
    
    public static void main( String[] args ) {
        System.out.println("This is das2 version "+getVersion());
        Splash.showSplash();
        Logger.getLogger("").addHandler( Splash.getInstance().getLogHandler() );
        try {
            for ( int i=0; i<6; i++ ) {
                Thread.sleep(500);
                Logger.getLogger("").log(Level.INFO, "i={0}", i);
                //Splash.getInstance().messageLabel.setText( "ii-="+i );
            }
        } catch ( java.lang.InterruptedException e ) {}        
        Splash.hideSplash();        
    }
    
}

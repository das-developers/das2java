/* File: Splash.java
 * Copyright (C) 2002-2003 University of Iowa
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

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 *
 * @author  jbf
 */
public class Splash extends JWindow {
    
    public static Splash instance=null;
    
    public static String getVersion() {
        //DName: das_20030505_01_beta D
        String cvsTagName= "$Name$";
        String version;
        if (cvsTagName.length()==9) {
            version="untagged_version";
        } else {
            version= cvsTagName.substring(6,cvsTagName.length()-2);
        }
        return version;
    }
    
    private static ImageIcon getSplashImage() {
        URL url= Splash.class.getResource("/images/dasSplash.gif");
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
        panel.add(new JLabel("version "+getVersion()+"   ",JLabel.RIGHT),BorderLayout.SOUTH);
        this.setContentPane(panel);
        this.pack();
        //this.setLocation(300,300);
        this.setLocationRelativeTo(null);
    }
    
    public static void main( String[] args ) {
        System.out.println("This is das2 version "+getVersion());
        Splash.showSplash();
        try {
            Thread.sleep(3000);
        } catch ( java.lang.InterruptedException e ) {}        
        Splash.hideSplash();        
    }
    
}

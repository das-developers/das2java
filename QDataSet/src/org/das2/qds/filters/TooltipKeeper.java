/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.filters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import static org.das2.qds.filters.AddFilterDialog.build;

/**
 *
 * @author jbf
 */
public class TooltipKeeper {
    
    private static TooltipKeeper instance=null;
    
    public static TooltipKeeper getInstance() {
        if ( instance==null ) {
            instance= new TooltipKeeper();
        }
        return instance;
    }
    
    DefaultMutableTreeNode tree;
    
    private TooltipKeeper() {
        try (InputStream in = AddFilterDialog.class.getResourceAsStream("filters.xml")) {
            tree= build( in );
        } catch ( IOException ex ) {
            tree= null;
        }
    }
    
    private String getTooltipFor( String filter, DefaultMutableTreeNode root ) {
        Object o= root.getUserObject();
        if ( o instanceof AddFilterDialog.Bookmark ) {
            AddFilterDialog.Bookmark b= (AddFilterDialog.Bookmark)o;
            String t= b.filter;
            if ( t.length()>0 ) {
                int i= t.indexOf('(');
                if ( i==-1 ) {
                    i= t.length();
                }
                if ( t.substring(0,i).equals(filter) ) { 
                    return b.description;
                } 
            }
        }
        Enumeration children= root.children();
        while ( children.hasMoreElements() ) {
            String s= getTooltipFor( filter, (DefaultMutableTreeNode)children.nextElement() );
            if ( s!=null ) return s;
        }
        return null;
    }
    
    public String getTooltipFor( String filter ) {
        int i= filter.indexOf('(');
        filter= filter.substring(0,i);
        return getTooltipFor( filter, tree );
    }
}

/*
 * UserMessageCenter.java
 *
 * Created on March 28, 2006, 2:58 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.system;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author Jeremy
 */
public class UserMessageCenter {
    private static UserMessageCenter instance;
    public static UserMessageCenter getDefault() {
        if ( instance==null ) {
            instance= new UserMessageCenter();
        }
        return instance;
    }
    
    private UserMessageCenter() {
        createComponents();
    }
    
    HashMap sources= new HashMap();   //<message>
    
    /**
     * Notify the user of the message, coalescing redundant messages from the same
     * source, etc.
     */
    public void notifyUser( Object source, String message ) {
        notifyUser( source, new JLabel(message) );
    }
    
    public void notifyUser( Object source, JLabel message ) {
        HashMap sourceMessages= (HashMap)sources.get( source );
        if ( sourceMessages!=null ) {
            if ( sourceMessages.containsKey(message.getText() ) ) {
                return;
            }
        }
        if ( sourceMessages==null ) {
            sourceMessages= new HashMap();
            sources.put( source, sourceMessages );
        }
        sourceMessages.put( message.getText(), null );
                
        JPanel panel= new JPanel();
        panel.setLayout( new BorderLayout(  ) );
        panel.add( message, BorderLayout.CENTER );
        
        panel.add( new JButton( getNextAction() ), BorderLayout.SOUTH );
        pane.add( panel, tabCount );
        tabCount++;
        
        if ( tabCount>0 ) {
            frame.setVisible(true);
        }
        
    }
    
    private Action getNextAction() {
        return new AbstractAction( "Next >>" ) {
            public void actionPerformed(ActionEvent e) {
                next();
            }
        };
    }
    
    int tabCount;
    private void next() {
        int currentTab= pane.getSelectedIndex();
        if ( currentTab<(tabCount-1) ) {
            currentTab++;
            pane.setSelectedIndex(currentTab);
        }
    }
    
    private void prev() {
        int currentTab= pane.getSelectedIndex();
        if ( currentTab>0 ) {
            currentTab--;
            pane.setSelectedIndex(currentTab);
        }
    }
    
    private JTabbedPane pane;
    private JFrame frame;
    
    private void createComponents() {
        frame= new JFrame( "das2 messages" );
        frame.setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        pane= new JTabbedPane();
        pane.setOpaque(true);
        pane.setPreferredSize( new Dimension( 400, 300 ) );
        pane.setMinimumSize( pane.getPreferredSize() );
        frame.setContentPane( pane );
        frame.pack();
    }
    
    
    
}

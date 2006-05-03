/*
 * UserMessageCenter.java
 *
 * Created on March 28, 2006, 2:58 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.system;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;

/**
 *
 * @author Jeremy
 */
public class UserMessageCenter {

    private static UserMessageCenter instance;

    class MessageRecord {
        JButton nextButton;
        Component component;
    }

    List messageRecords;

    public static UserMessageCenter getDefault() {
        if ( instance==null ) {
            instance= new UserMessageCenter();
        }
        return instance;
    }

    private UserMessageCenter() {
        createComponents();
        messageRecords= new ArrayList();
    }

    HashMap sources= new HashMap();   //<message>

    /**
     * Notify the user of the message, coalescing redundant messages from the same
     * source, etc.
     */
    public void notifyUser( Object source, String message ) {
        JTextPane textComponent= new JTextPane();
        if ( message.startsWith("<html>" ) )   textComponent.setContentType("text/html");
        try {
            textComponent.getEditorKit().read( new StringReader(message), textComponent.getDocument(), 0 );
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        notifyUser( source, textComponent);
    }

    private void notifyUser( Object source, JTextPane message ) {
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

        JButton nextButton= new JButton( getNextAction() );
        panel.add( nextButton, BorderLayout.SOUTH );
        pane.add( panel, tabCount );
        tabCount++;

        if ( tabCount>0 ) {
            frame.setVisible(true);
        }

        MessageRecord record= new MessageRecord();
        record.component= panel;
        record.nextButton= nextButton;

        messageRecords.add( record );
        update();
    }

    private void update() {
        for ( int i=0; i<messageRecords.size(); i++ ) {
            MessageRecord record= (MessageRecord)messageRecords.get(i);
            record.nextButton.setEnabled( i<messageRecords.size()-1) ;
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

    /**
     * Notify user that an exception occured, presumably because they are capable of handling the exception.
     */
    public void notifyUser( Object source, Throwable e) {
        notifyUser( source, e.getMessage() );
    }



}

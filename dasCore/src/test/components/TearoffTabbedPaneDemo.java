/*
 * TearoffTabbedPaneDemo.java
 *
 * Created on February 1, 2006, 4:36 PM
 *
 *
 */

package test.components;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import org.das2.components.TearoffTabbedPane;
import java.awt.Font;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author Jeremy
 */
public class TearoffTabbedPaneDemo {
    private static JPanel getPanel( final TearoffTabbedPane pane, final int index ) {
        final JPanel panel= new JPanel();
        panel.setName( "tab"+index );
        panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
        JLabel label= new JLabel(" --"+index+"--");
        label.setFont( Font.decode( "HELVETICA" ).deriveFont(Font.BOLD,40.0f) );
        panel.add( label );

        JLabel sublabel= new JLabel( "name="+panel.getName()+" hash="+panel.hashCode() );
        sublabel.setFont( Font.decode( "HELVETICA" ).deriveFont(Font.ITALIC,8.0f) );
        panel.add( sublabel );

        JButton action= new JButton( new AbstractAction("Remove First Tab") {
            public void actionPerformed(ActionEvent e) {
                System.err.println("Button "+index);
                if ( pane.getTabCount()>1 ) {
                    pane.removeTabAt(1);
                    if ( pane.getTabCount()==1 ) ((JButton)e.getSource()).setEnabled(false);
                } else {
                    ((JButton)e.getSource()).setEnabled(false);
                }
            }
        });
        panel.add( action );

        JButton peek= new JButton( new AbstractAction("Peek (see stderr)") {
            public void actionPerformed(ActionEvent e) {
                pane.peek();
            }
        });
        panel.add( peek );

        return panel;
    }

    public static void main( String[] args ) {
        TearoffTabbedPane pane= new TearoffTabbedPane();
        pane.addTab( "firstTab", getPanel(pane,1) );
        pane.addTab( "secondTab", getPanel(pane,2) );
        pane.addTab( "thirdTab", getPanel(pane,3) );

        JFrame frame= new JFrame();
        frame.getContentPane().add( pane );
        pane.setPreferredSize( new Dimension(600,400) );
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    }
    
    
}


package org.das2.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Keep track of window positions.  Windows should be passed by this class
 * before they are realized, and the name property of the dialog will be 
 * used to look up the last size and position.  When the window has a parent,
 * the position is stored relative to the parent.  Finally when a window
 * is dismissed, this class should be called again so that the 
 * position is kept.
 *
 * @author jbf
 */
public class WindowManager {
    
    private static final Logger logger= LoggerManager.getLogger("das2.windowmanager");
    
    private static final WindowManager instance= new WindowManager();
    
    public static WindowManager getInstance() {
        return instance;
    }

    /**
     * TODO: this will show the icon.
     * @param parent
     * @param omessage
     * @param title
     * @param optionType
     * @param messageType
     * @param icon
     * @return 
     */
    public static int showConfirmDialog( Component parent, JPanel omessage, String title, int optionType, int messageType, Icon icon ) {
        return showConfirmDialog( parent, omessage, title, optionType );
    }
    
    /**
     * call this before the window.
     * @param window the window.
     */
    public void recallWindowSizePosition( Window window ) {
        Container parent= window.getParent();
        String name= window.getName(); 
        logger.log(Level.FINE, "looking up position for {0}", name);
        if ( name==null ) return;
        final Preferences prefs= Preferences.userNodeForPackage(WindowManager.class);
        int grab= 4 * window.getFont().getSize(); // pixels so mouse operator has something to grab
        Dimension screenSize= java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        if ( prefs.getInt( "window."+name+".screenwidth", 0 )==screenSize.width ) {
            int w= prefs.getInt( "window."+name+".width", -9999 );
            int h= prefs.getInt( "window."+name+".height", -9999 );
            if ( w>10 && h>10 && w<screenSize.width && h<screenSize.height ) {
                window.setSize( w, h );
            }   
            if ( parent!=null ) {
                int x= prefs.getInt( "window."+name+".rlocationx", -9999 );
                int y= prefs.getInt( "window."+name+".rlocationy", -9999 );        
                if ( x>-9999 && y>-9999 ) {
                    int newx= parent.getX()+x;
                    int newy= parent.getY()+y;
                    if ( newx<0 ) newx= 0;
                    if ( newy<0 ) newy= 0;
                    if ( newx>screenSize.width-grab ) newx= screenSize.width-grab;
                    if ( newy>screenSize.height-grab ) newy= screenSize.height-grab;
                    window.setLocation( newx, newy );
                }
            } else {
                int x= prefs.getInt( "window."+name+".locationx", -9999 );
                int y= prefs.getInt( "window."+name+".locationy", -9999 );
                if ( x>-9999 && y>-9999 ) {
                    int newx= x;
                    int newy= y;
                    if ( newx<0 ) newx= 0;
                    if ( newy<0 ) newy= 0;
                    if ( newx>screenSize.width-grab ) newx= screenSize.width-grab;
                    if ( newy>screenSize.height-grab ) newy= screenSize.height-h;
                    window.setLocation( newx, newy );
                }
            }
        }
    }
    
    /**
     * record the final position of the dialog.  This will store the 
     * position in the Java prefs manager for this class.
     * @param window the window
     */
    public void recordWindowSizePosition( Window window ) {
        int x= window.getLocation().x;
        int y= window.getLocation().y;
        int w= window.getWidth();
        int h= window.getHeight();
        
        Container c= window.getParent();
        String name= window.getName(); 
        
        logger.log(Level.FINE, "storing position for {0}", name);
        if ( name==null ) return;
        
        final Preferences prefs= Preferences.userNodeForPackage(WindowManager.class);
        logger.log( Level.FINE, "saving last location {0} {1} {2} {3}", new Object[]{x, y, h, w});
        // so that we know these settings are still valid.
        prefs.putInt( "window."+name+".screenwidth", java.awt.Toolkit.getDefaultToolkit().getScreenSize().width ); 
        if ( c!=null ) {
            prefs.putInt( "window."+name+".rlocationx", x-c.getX() );
            prefs.putInt( "window."+name+".rlocationy", y-c.getY() );
        } else {
            prefs.putInt( "window."+name+".locationx", x );
            prefs.putInt( "window."+name+".locationy", y );            
        }
        prefs.putInt( "window."+name+".width", w );
        prefs.putInt( "window."+name+".height", h );
        
    }

    /**
     * convenient method for showing dialog which is modal.
     * @param dia the dialog that is set to be modal.
     */
    public void showModalDialog( Dialog dia ) {
        if ( !dia.isModal() ) throw new IllegalArgumentException("dialog should be modal");
        WindowManager.getInstance().recallWindowSizePosition(dia);
        dia.setVisible(true);
        WindowManager.getInstance().recordWindowSizePosition(dia);        
    }
    
    /**
     * new okay/cancel dialog that is resizable and is made with a simple dialog.
     * @param parent the parent window or dialog.
     * @param omessage String or Component.
     * @param title the dialog title.  
     * @param optionType.  This must be OK_CANCEL_OPTION or YES_NO_CANCEL_OPTION
     * @return JOptionPane.OK_OPTION, JOptionPane.CANCEL_OPTION.
     */
    public static int showConfirmDialog( Component parent, Object omessage, final String title, int optionType ) {
        if ( optionType!=JOptionPane.OK_CANCEL_OPTION && optionType!=JOptionPane.YES_NO_CANCEL_OPTION ) {
            throw new IllegalArgumentException("must be OK_CANCEL_OPTION or YES_NO_CANCEL_OPTION");
        }
        final Component message;
        if ( !( omessage instanceof Component ) ) {
            message= new JLabel( omessage.toString() );
        } else {
            message= (Component)omessage;
        }
        final Window p= ( parent instanceof Window ) ? ((Window)parent) : SwingUtilities.getWindowAncestor(parent);
        final JDialog dia= new JDialog( p, Dialog.ModalityType.APPLICATION_MODAL );
        final String name;
        if ( title.startsWith("Run Script ") ) { //small kludge to hide user-created data from injection into user prefs.
            String hash= String.format( "%09d", title.substring(11).hashCode() );
            name= "RunScript"+hash;
        } else {
            name= title.replaceAll( "\\s","");
        }
        dia.setName(name);
        dia.setLayout( new BorderLayout() );
        final JPanel pc= new JPanel();
        final List<Integer> result= new ArrayList(1);
        result.add( JOptionPane.CANCEL_OPTION );
        BoxLayout b= new BoxLayout(pc,BoxLayout.X_AXIS);
        pc.setLayout( b );
        
        pc.add( Box.createGlue() );
        
        if ( optionType==JOptionPane.OK_CANCEL_OPTION ) {
            pc.add( new JButton( new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);
                    result.set( 0, JOptionPane.CANCEL_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            }) );
            pc.add( Box.createHorizontalStrut(7) );
            pc.add( new JButton( new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);        
                    result.set( 0, JOptionPane.OK_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            }) );
        } else if ( optionType==JOptionPane.YES_NO_CANCEL_OPTION ) {
            pc.add( new JButton( new AbstractAction("Yes") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);                        
                    result.set( 0, JOptionPane.YES_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            }) );
            pc.add( Box.createHorizontalStrut(7) );
            pc.add( new JButton( new AbstractAction("No") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);                        
                    result.set( 0, JOptionPane.NO_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            }) );
            pc.add( Box.createHorizontalStrut(7) );
            pc.add( new JButton( new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);        
                    result.set( 0, JOptionPane.CANCEL_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            }) );
        }
        
        //TODO: there are two dialog codes which could be made into one.
        dia.getRootPane().registerKeyboardAction( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                org.das2.util.LoggerManager.logGuiEvent(e);        
                result.set( 0, JOptionPane.CANCEL_OPTION );
                dia.setVisible(false);
                dia.dispose(); 
            }
        }, KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), JComponent.WHEN_IN_FOCUSED_WINDOW );       

        pc.add( Box.createHorizontalStrut(7) );
        
        Runnable run = new Runnable() {
            @Override
            public void run() {
                dia.setResizable(true);
                dia.add( (Component)message );
                dia.add( pc, BorderLayout.SOUTH );
                dia.setTitle(title);
                //dia.setMinimumSize( new Dimension(300,300) );
                dia.pack();
                dia.setLocationRelativeTo(p);
                WindowManager.getInstance().recallWindowSizePosition(dia);
                dia.setVisible(true);
                WindowManager.getInstance().recordWindowSizePosition(dia);
            }
        };
        if ( EventQueue.isDispatchThread() ) {
            run.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(run);
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return result.get(0);
    }

}

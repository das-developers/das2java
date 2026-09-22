
package org.das2.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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

    private static Preferences getPrefs() {
        return Preferences.userNodeForPackage(WindowManager.class);
    }
    
    /**
     * TODO: this will indicate the message type
     * @param parent
     * @param omessage
     * @param title
     * @param optionType
     * @param messageType
     * @return 
     */
    public static int showConfirmDialog( Component parent, Object omessage, String title, int optionType, int messageType ) {
        return showConfirmDialog( parent, omessage, title, optionType );
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
    public static int showConfirmDialog( Component parent, Object omessage, String title, int optionType, int messageType, Icon icon ) {
        return showConfirmDialog( parent, omessage, title, optionType );
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
    
    private boolean isOnScreen( Rectangle pos, int grab ) {
        GraphicsEnvironment ge=GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs=ge.getScreenDevices();
        for ( GraphicsDevice gd: gs ) {
            Rectangle bounds= gd.getDefaultConfiguration().getBounds();
            Rectangle intersect= pos.intersection(bounds);
            if ( intersect.height>grab && intersect.width>grab ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * returns the size of the screen when all the monitors are 
     * considered one large virtual display.
     * @return 
     */
    private Rectangle getVirtualScreenSize() {
        Rectangle all = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice gd : ge.getScreenDevices()) {
            Rectangle b = gd.getDefaultConfiguration().getBounds();
            all = all.union(b);
        }

        return all;        
    }
    
    Dimension lastFileChooserDimension= null;
    int lastFileChooserX= 0;
    int lastFileChooserY= 0;
    
    public void recallWindowSizePosition( JFileChooser window ) {
        String name= window.getName(); 
        if (name==null) name="fileChooser";
        logger.log(Level.FINE, "looking up position for {0}", name);
        final Preferences prefs= getPrefs();
        int grab= 4 * 12; // pixels so mouse operator has something to grab
        Rectangle screenSize= getVirtualScreenSize();
        Pattern p= Pattern.compile("(?<width>\\d+)x(?<height>\\d+)");
        String s= prefs.get( "window."+name+".screensize", "" );
        logger.log(Level.FINE, "found for window.{0}.screensize: {1} currentSize: {2}x{3}", new Object[]{name, s, screenSize.width, screenSize.height });
        Matcher m0= p.matcher(s);
        if ( m0.matches() && Integer.parseInt( m0.group("width") )==screenSize.width && Integer.parseInt( m0.group("height") )==screenSize.height ) {
            String wh= prefs.get("window."+name+".size", "" );
            logger.log(Level.FINER, "window.{0}.size={1}", new Object[]{name, wh});
            Matcher m= p.matcher(wh);
            int w= m.matches() ? Integer.parseInt( m.group("width") ) : -9999;
            int h= m.matches() ? Integer.parseInt( m.group("height") ) : -9999;
            if ( w>10 && h>10 && w<screenSize.width && h<screenSize.height ) {
                window.setSize( w, h );
                window.setPreferredSize( new Dimension( w, h ) );
                lastFileChooserDimension= window.getSize();
            }   

            String xy= prefs.get( "window."+name+".location", "" );
            logger.log(Level.FINER, "window.{0}.location={1}", new Object[]{name, xy});
            Pattern p2= Pattern.compile("(?<x>\\d+),(?<y>\\d+)");
            Matcher m2= p2.matcher(xy);
            int x= m2.matches() ? Integer.parseInt( m2.group("x") ) : -9999;
            int y= m2.matches() ? Integer.parseInt( m2.group("y") ) : -9999;
            if ( x!=0 && y!=0 && x>-9999 && y>-9999 ) {
                int newx= x;
                int newy= y;
                if ( newx<0 ) newx= 0;
                if ( newy<0 ) newy= 0;
                if ( newx>screenSize.width-grab ) newx= screenSize.width-grab;
                if ( newy>screenSize.height-grab ) newy= screenSize.height-h;
                if ( isOnScreen( new Rectangle(newx,newy,window.getWidth(),window.getWidth()), grab ) ) {
                    window.setLocation( newx, newy );
                }
            }
        }
        window.addComponentListener( new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lastFileChooserDimension= window.getSize();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                lastFileChooserDimension= window.getSize();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                lastFileChooserX= window.getX();
                lastFileChooserY= window.getY();
                System.err.println("x,y="+lastFileChooserX+","+lastFileChooserY);
            }
            
            
        } );
        SwingUtilities.invokeLater(() -> {  // Thanks, ChatGPT!
            Action a = window.getActionMap().get("viewTypeDetails");
            if (a != null) a.actionPerformed(null);
        });
    }
    
    /**
     * get the name used to store this window configuration.  If allocate is true,
     * then possibly create a new name if the old one is not found.
     * @param window
     * @param allocate
     * @return 
     */
    public String getRecallName( Window window, boolean allocate) {
        String name= window.getName(); 
        logger.log(Level.FINE, "looking up position for {0}", name);
        if ( name==null ) return null;
        final Preferences prefs= getPrefs();
        Rectangle d= getVirtualScreenSize();
        
        String screenSize= String.format("%dx%d",d.width,d.height);
        
        String result;        
        String s= prefs.get( "window."+name+".screensize", "" );
        if ( s.equals(screenSize) ) {
            result= name;
        } else {
            result= name+"."+screenSize;
        } 
        return result;
    } 
            
    public void recordWindowSizePosition( JFileChooser window ) {
        int x= lastFileChooserX;
        int y= lastFileChooserY;
        
        if ( lastFileChooserDimension==null ) return;
        
        int w= lastFileChooserDimension.width;
        int h= lastFileChooserDimension.height;
        
        Container c= window.getParent();
        String name= window.getName(); 
        if ( name==null ) name="fileChooser";
        logger.log(Level.FINE, "storing position for {0}", name);
        
        final Preferences prefs= getPrefs();
        logger.log( Level.FINE, "saving last location {0} {1} {2} {3}", new Object[]{x, y, h, w});
        // so that we know these settings are still valid.
        Rectangle d= getVirtualScreenSize();
        prefs.put( "window."+name+".screensize", String.format("%dx%d",d.width,d.height) );
        if ( c!=null ) {
            prefs.put( "window."+name+".rlocation", String.format( "%d,%d", x-c.getX(), y-c.getY() ) );
            prefs.put( "window."+name+".location", String.format( "%d,%d", x, y ) );
        } else {
            prefs.put( "window."+name+".rlocation", "0,0" );
            prefs.put( "window."+name+".location", String.format( "%d,%d", x, y ) );
        }
        prefs.put( "window."+name+".size", String.format( "%dx%d", w, h ) );
     
    }
    
    /**
     * call this before the window.
     * @param window the window.
     */
    public void recallWindowSizePosition( Window window ) {
        
        final Preferences prefs= getPrefs();
        Rectangle screenSize= getVirtualScreenSize();

        String name= getRecallName( window, false );
        if ( name==null ) return;
        
        logger.log(Level.FINE, "looking up position for {0}", name);

        Container parent= window.getParent();
        int grab= 4 * window.getFont().getSize(); // pixels so mouse operator has something to grab
        
        Pattern p= Pattern.compile("(?<width>\\d+)x(?<height>\\d+)");
        String s= prefs.get( "window."+name+".screensize", "" );
        logger.log(Level.FINE, "found for window.{0}.screensize: {1} currentSize: {2}x{3}", new Object[]{name, s, screenSize.width, screenSize.height });
        Matcher m0= p.matcher(s);
        if ( m0.matches() && Integer.parseInt( m0.group("width") )==screenSize.width && Integer.parseInt( m0.group("height") )==screenSize.height ) {
            String wh= prefs.get("window."+name+".size", "" );
            logger.log(Level.FINER, "window.{0}.size={1}", new Object[]{name, wh});
            Matcher m= p.matcher(wh);
            int w= m.matches() ? Integer.parseInt( m.group("width") ) : -9999;
            int h= m.matches() ? Integer.parseInt( m.group("height") ) : -9999;
            if ( w>10 && h>10 && w<screenSize.width && h<screenSize.height ) {
                window.setSize( w, h );
            }   
            if ( parent!=null ) {
                String rxy= prefs.get( "window."+name+".rlocation", "" );
                logger.log(Level.FINER, "window.{0}.rlocation={1}", new Object[]{name, rxy});
                Pattern p2= Pattern.compile("(?<x>\\-?\\d+),(?<y>\\-?\\d+)");
                Matcher m2= p2.matcher(rxy);
                int x= m2.matches() ? Integer.parseInt( m2.group("x") ) : -9999;
                int y= m2.matches() ? Integer.parseInt( m2.group("y") ) : -9999;
                if ( x>-9999 && y>-9999 ) {
                    int newx= parent.getX()+x;
                    int newy= parent.getY()+y;
                    if ( newx<0 ) newx= 0;
                    if ( newy<0 ) newy= 0;
                    if ( newx>screenSize.width-grab ) newx= screenSize.width-grab;
                    if ( newy>screenSize.height-grab ) newy= screenSize.height-grab;
                    if ( isOnScreen( new Rectangle(newx,newy,window.getWidth(),window.getWidth()), grab ) ) {
                        window.setLocation( newx, newy );
                    }
                }
            } else {
                String xy= prefs.get( "window."+name+".location", "" );
                logger.log(Level.FINER, "window.{0}.location={1}", new Object[]{name, xy});
                Pattern p2= Pattern.compile("(?<x>\\-?\\d+),(?<y>\\-?\\d+)");
                Matcher m2= p2.matcher(xy);
                int x= m2.matches() ? Integer.parseInt( m2.group("x") ) : -9999;
                int y= m2.matches() ? Integer.parseInt( m2.group("y") ) : -9999;
                if ( x>-9999 && y>-9999 ) {
                    int newx= x;
                    int newy= y;
                    if ( newx<0 ) newx= 0;
                    if ( newy<0 ) newy= 0;
                    if ( newx>screenSize.width-grab ) newx= screenSize.width-grab;
                    if ( newy>screenSize.height-grab ) newy= screenSize.height-h;
                    if ( isOnScreen( new Rectangle(newx,newy,window.getWidth(),window.getWidth()), grab ) ) {
                        window.setLocation( newx, newy );
                    }
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
        String name= getRecallName(window, true); 
        
        logger.log(Level.FINE, "storing position for {0}", name);
        if ( name==null ) return;
        
        final Preferences prefs= getPrefs();
        logger.log( Level.FINE, "saving last location {0} {1} {2} {3}", new Object[]{x, y, h, w});
        // so that we know these settings are still valid.
        Rectangle d= getVirtualScreenSize();
        prefs.put( "window."+name+".screensize", String.format("%dx%d",d.width,d.height) );
        if ( c!=null ) {
            prefs.put( "window."+name+".rlocation", String.format( "%d,%d", x-c.getX(), y-c.getY() ) );
            prefs.put( "window."+name+".location", String.format( "%d,%d", x, y ) );
        } else {
            prefs.put( "window."+name+".rlocation", "0,0" );
            prefs.put( "window."+name+".location", String.format( "%d,%d", x, y ) );
        }
        prefs.put( "window."+name+".size", String.format( "%dx%d", w, h ) );
        
    }

    /**
     * convenient method for showing dialog which is modal.
     * @param dia the dialog that is set to be modal.
     */
    public void showModalDialog( Dialog dia ) {
        if ( !dia.isModal() ) throw new IllegalArgumentException("dialog should be modal");
        WindowManager.getInstance().recallWindowSizePosition(dia);
        try { 
            dia.setVisible(true);
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, null, ex ); // TODO: bug2293: explore where this comes from.
        } 
        WindowManager.getInstance().recordWindowSizePosition(dia);        
    }
    
    public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
    public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
    public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    public static final int OK_OPTION = JOptionPane.OK_OPTION;
    public static final int NO_OPTION = JOptionPane.NO_OPTION;
    public static final int YES_OPTION = JOptionPane.YES_OPTION;
    
    /**
     * New okay/cancel dialog that is resizable and is made with a simple dialog.
     * @param parent the parent window or dialog, or null.
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
        final Window p;
        if ( parent!=null ) {
            p= ( parent instanceof Window ) ? ((Window)parent) : SwingUtilities.getWindowAncestor(parent); 
        } else {
            p= null;
        }
        final JDialog dia= new JDialog( p, Dialog.ModalityType.APPLICATION_MODAL );
        final String name;
        if ( title.startsWith("Run Script ") ) { //small kludge to hide user-created data from injection into user prefs. This is copied from Autoplot and is harmless.
            String hash= String.format( "%09d", title.substring(11).hashCode() );
            if ( hash.startsWith("-") ) hash= "0"+hash.substring(1);
            name= "RunScript-"+hash; 
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
            AbstractAction aa= new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);
                    result.set( 0, JOptionPane.CANCEL_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            };
            pc.add( new JButton( aa ) );
            pc.add( Box.createHorizontalStrut(7) );
            aa= new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    org.das2.util.LoggerManager.logGuiEvent(e);        
                    result.set( 0, JOptionPane.OK_OPTION );
                    dia.setVisible(false);
                    dia.dispose(); 
                }
            };
            pc.add( new JButton( aa ) );
            
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

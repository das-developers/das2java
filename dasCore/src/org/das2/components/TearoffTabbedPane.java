/*
 * TearoffTabbedPane.java
 *
 * Created on January 26, 2006, 7:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.das2.util.LoggerManager;
import test.components.TearoffTabbedPaneDemo;

/**
 * Like the Swing TabbedPane, but this allows the tabs to be 
 * removed to other windows or other TearoffTabbedPanes.
 * @author Jeremy
 */
public class TearoffTabbedPane extends JTabbedPane {

    int selectedTab;
    Point dragStart;
    Point dragOffset;
    JFrame draggingFrame;
    JPopupMenu tearOffMenu = new JPopupMenu();
    JPopupMenu dockMenu = new JPopupMenu();

    private TearoffTabbedPane parentPane; // non-null for babysitter panes.

    private TearoffTabbedPane rightPane = null;
    private TearoffTabbedPane dropDirty = null;

    private JFrame rightFrame = null;
    private ComponentListener rightFrameListener;
    private int rightOffset= 0;
    

    private final static Logger logger= Logger.getLogger( "das2.gui" );

    /**
     * size of top region that accepts drop
     */
    private static int TOP_DROP_MARGIN=200;

    LinkedHashMap<Component, TabDesc> tabs = new LinkedHashMap<>();
    int lastSelected; /* keep track of selected index before context menu */

    private static void copyInputMap(JFrame parent, JFrame babySitter) {
        Component c;
        JComponent parentc, babySitterC;

        c = parent.getContentPane();
        if (!(c instanceof JComponent)) {
            return;
        }
        parentc = (JComponent) c;

        c = babySitter.getContentPane();
        if (!(c instanceof JComponent)) {
            return;
        }
        babySitterC = (JComponent) c;

        InputMap m = parentc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (m == null) {
            return;
        }
        babySitterC.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, m);
        ActionMap am = parentc.getActionMap();
        if (am == null) {
            return;
        }
        babySitterC.setActionMap(am);
    }
    
    private boolean dropDecorate;

    /**
     * paint decorations to indicate this will accept a drop.
     * @param b 
     */
    private void setDropDecorate(boolean b) {
        this.dropDecorate= b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        try {
            super.paintComponent(g); //To change body of generated methods, choose Tools | Templates.
        } catch ( ClassCastException ex ) {
            ex.printStackTrace();
            System.err.println("See https://sourceforge.net/p/autoplot/bugs/1998/");
        }
        if ( dropDecorate ) {
            Graphics2D g2= (Graphics2D)g;
            int h= g.getFontMetrics().getHeight();
            g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
            Color c= this.getBackground();
            c= new Color( c.getRed(), c.getGreen(), c.getBlue(), 220 );
            g2.setColor( c );
            g2.fill( g.getClip() );
            g2.setColor( this.getForeground() );
            g2.drawString( "(dock)", h*3, h );
        }
    }

    private static class TabDesc {

        Icon icon;
        String title;
        String tip;
        int index;
        /** This is the JFrame that contains the child, another TearOffTabbedPane, or null. **/
        Container babysitter;

        TabDesc(String title, Icon icon, String tip, int index) {
            this.title = title;
            this.icon = icon;
            this.tip = tip;
            this.index = index;
            this.babysitter = null;
        }

        @Override
        public String toString() {
            if ( this.babysitter==null ) {
                return this.title + "@"+ this.index + ": (docked)";
            } else {
                return this.title + "@"+ this.index + ": "+ this.babysitter.getName();
            }
        }
    }

    /**
     * create a new TearoffTabbedPane
     */
    public TearoffTabbedPane() {
        this(null);
    }

    private TearoffTabbedPane(TearoffTabbedPane parent) {
        super();
        if (parent == null) {
            //TODO: need a way to remove mouse adapter when parent isn't JFrame
            MouseAdapter ma = new ParentMouseAdapter();
            addMouseListener(ma);
            addMouseMotionListener(getMouseMotionListener());
        } else {
            parentPane = parent;
            addMouseListener(getChildMouseAdapter());
            addMouseMotionListener(getChildMouseMotionListener());
//            Window w= SwingUtilities.getWindowAncestor(parent); // bug https://sourceforge.net/p/autoplot/bugs/1620/
//            if ( w!=null ) {
//                w.addWindowListener( new WindowAdapter() {
//                    @Override
//                    public void windowClosing(WindowEvent e) {
//                        for ( TabDesc td: tabs.values() ) {
//                            if ( td.babysitter!=null ) td.babysitter.setVisible(false);
//                        }
//                    }
//                });
//            }
        }
        super.addChangeListener( new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                try {
                    LoggerManager.logGuiEvent(e);
                } catch ( Exception ex ) {
                    
                }
            }
        });
    }

    /**
     * I needed a way to hide the mouseAdapter, since we can't do this automatically.  
     */
    public void hideMouseAdapter() {
        // https://sourceforge.net/tracker/?func=detail&aid=3377337&group_id=199733&atid=970682
        MouseListener[] mls= getMouseListeners();
        if ( mls.length>0 ) {
            MouseListener ml= mls[mls.length-1];
            removeMouseListener( ml );
        }
        MouseMotionListener[] mmls= getMouseMotionListeners();
        if ( getMouseMotionListeners().length>0 ) {
            MouseMotionListener ml= mmls[mmls.length-1];
            removeMouseMotionListener( ml );
        }

    }

    private MouseMotionListener getMouseMotionListener() {
        return new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedTab == -1) {
                    return;
                }
                if (dragStart == null) {
                    dragStart = e.getPoint();
                } else {
                    if (dragStart.distance(e.getPoint()) > 10) {
                        if (draggingFrame == null ) {
                            setSelectedIndex(selectedTab);
                            getComponentAt(selectedTab).setVisible(true);
                            dragOffset= getComponentAt(selectedTab).getLocationOnScreen();
                            Point ds= new Point(dragStart);
                            SwingUtilities.convertPointToScreen(ds, e.getComponent() );
                            int tabAndWindowHeight=40; // ubuntu, TODO: calculate
                            dragOffset.translate( -ds.x, -ds.y - tabAndWindowHeight );
                            draggingFrame = TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                            if (draggingFrame == null) {
                                return;
                            }
                            setCursor(new Cursor(Cursor.MOVE_CURSOR));
                            if ( draggingFrame.getWidth()< -1*dragOffset.x ) {
                                int borderWidth=5;
                                dragOffset.x= -1*(draggingFrame.getWidth()-borderWidth);
                            }
                        }
                        Point p = e.getPoint();
                        SwingUtilities.convertPointToScreen(p, (Component) e.getSource());
                        p.translate(dragOffset.x, dragOffset.y);
                        draggingFrame.setLocation(p);

                        TearoffTabbedPane drop= getHoverTP( e.getComponent(), e.getPoint() );

                        if ( dropDirty!=null ) { // give some hint that this is a drop target.
                            dropDirty.setDropDecorate(false);
                            dropDirty.repaint();
                        }

                        if ( drop!=null ) {
                            drop.setDropDecorate(true);
                            drop.repaint();
                            dropDirty= drop;
                        } else {
                            dropDirty= null;
                        }

                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }


        };
    }

    private void showPopupMenu(MouseEvent event) {
        Component selectedComponent;
        selectedTab = TearoffTabbedPane.this.indexAtLocation(event.getX(), event.getY());
        if (selectedTab != -1) {
            selectedComponent = TearoffTabbedPane.this.getComponentAt(selectedTab);
            if (parentPane == null && tabs.get(selectedComponent) != null) {
                tearOffMenu.show(TearoffTabbedPane.this, event.getX(), event.getY());
            } else {
                dockMenu.show(TearoffTabbedPane.this, event.getX(), event.getY());
            }
        }
    }

    private MouseAdapter getChildMouseAdapter() {
        return new MouseAdapter() {

            Component selectedComponent;

            {
                dockMenu.add(new JMenuItem(new AbstractAction("return undocked tab") {
                    @Override
                    public void actionPerformed(ActionEvent event) {

                        if (parentPane != null) {
                            selectedComponent = getComponent(selectedTab);
                            remove(selectedComponent);
                            parentPane.dock(selectedComponent);
                            if ( getTabCount()==0 ) {
                                SwingUtilities.getWindowAncestor(TearoffTabbedPane.this).dispose();
                            } else {
                                TearoffTabbedPane.this.resetTearOffBabysitterName();
                            }
                        } else {
                            throw new IllegalArgumentException("parentPane must not be null"); //findbugs pointed this out
                        }
                        
                    }
                }));
            }

            @Override
            public void mousePressed(MouseEvent event) {
                selectedTab = TearoffTabbedPane.this.indexAtLocation(event.getX(), event.getY());
                if (event.isPopupTrigger()) {
                    showPopupMenu(event);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                TearoffTabbedPane draggingTearOff;
                if ( draggingFrame!=null ) {
                    draggingTearOff= getTabbedPane(draggingFrame);
                    if ( draggingTearOff!=null && TearoffTabbedPane.this.parentPane.contains( SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), TearoffTabbedPane.this.parentPane ) ) ) {
                        logger.fine( "docking into ...");
                        TearoffTabbedPane.this.parentPane.dock(draggingTearOff.getComponentAt(0));
                        TearoffTabbedPane.this.parentPane.setDropDecorate(false);
                        draggingFrame.dispose();
                    } else {
                        if ( draggingTearOff!=null ) {
                            draggingTearOff.resetTearOffBabysitterName();
                        }
                    }
                    TearoffTabbedPane oldChildParent= getTabbedPane(e.getComponent());
                    if ( oldChildParent.getTabCount()==1 ) { // there's still a bug here if two are undocked.
                        SwingUtilities.getWindowAncestor(oldChildParent).dispose();
                    } 

                }
                if (dragStart != null && selectedTab != -1) {
                    //JFrame f= TearoffTabbedPane.this.tearOffIntoFrame( selectedTab );
                    //Point p= e.getPoint();
                    //SwingUtilities.convertPointToScreen( p ,(Component) e.getSource() );
                    //f.setLocation( p );
                    setCursor(null);
                    draggingFrame = null;
                }
                dragStart = null;
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        };

    }

    private MouseMotionListener getChildMouseMotionListener() {
        return new MouseMotionListener() {            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedTab == -1) {
                    return;
                }
                if (dragStart == null) {
                    dragStart = e.getPoint();
                } else {
                    if (dragStart.distance(e.getPoint()) > 10) {
                        if (draggingFrame == null) {
                            setSelectedIndex(selectedTab);
                            getComponentAt(selectedTab).setVisible(true);
                            dragOffset= getComponentAt(selectedTab).getLocationOnScreen();
                            Point ds= new Point(dragStart);
                            SwingUtilities.convertPointToScreen(ds, e.getComponent() );
                            int tabAndWindowHeight=40; // ubuntu, TODO: calculate
                            dragOffset.translate( -ds.x, -ds.y - tabAndWindowHeight );
                            final Component c = getComponentAt(selectedTab);
                            draggingFrame = TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                            TearoffTabbedPane carry= getTabbedPane(draggingFrame);
                            carry.resetTearOffBabysitterName();
                            carry.parentPane= TearoffTabbedPane.this.parentPane;
                            
                            TabDesc tabDesc= carry.parentPane.getTabDescByComponent(c);
                            tabDesc.babysitter= carry;
                            //TearoffTabbedPane.super.removeTabAt(selectedTab);
                            
                            //removeTabAt(selectedTab,false); 
                            
                            if (draggingFrame == null) {
                                return;
                            }
                            setCursor(new Cursor(Cursor.MOVE_CURSOR));
                            if ( draggingFrame.getWidth()< -1*dragOffset.x ) {
                                int borderWidth=5;
                                dragOffset.x= -1*(draggingFrame.getWidth()-borderWidth);
                            }
                        }
                        Point p = e.getPoint();
                        SwingUtilities.convertPointToScreen(p, (Component) e.getSource());
                        p.translate(dragOffset.x, dragOffset.y);
                        draggingFrame.setLocation(p);

                        TearoffTabbedPane drop=null;
                        Point o1= SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), TearoffTabbedPane.this.parentPane );
                        if ( TearoffTabbedPane.this.parentPane.contains( o1 )
                                && Math.abs( o1.getY() - TearoffTabbedPane.this.parentPane.getY() )< TOP_DROP_MARGIN )  {
                            drop= TearoffTabbedPane.this.parentPane;
                        }

                        if ( dropDirty!=null ) { // give some hint that this is a drop target.
                            dropDirty.setDropDecorate(false);
                            dropDirty.repaint();
                        }

                        if ( drop!=null ) {
                            drop.setDropDecorate(true);
                            drop.repaint();
                            dropDirty= drop;
                        } else {
                            dropDirty= null;
                        }

                    }
                }
                resetTearOffBabysitterName();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }


        };
    }

    /**
     * show all the tabs descriptions
     */
    public void peek() {
        System.err.println("--");
        for ( Entry<Component,TabDesc> entry : tabs.entrySet() ) {
            TabDesc d = (TabDesc) entry.getValue();
            System.err.println(d);
        }
    }

    private void showIt() {

        TabDesc desc = null;
        Component babyComponent = null;
        for (  Entry<Component,TabDesc> entry : tabs.entrySet() ) {
            TabDesc d = (TabDesc) entry.getValue();
            if (d.index == selectedTab) {
                desc = d;
                babyComponent = entry.getKey();
                break;
            }
        }
        if (desc==null) return;
        if (desc.babysitter instanceof Window) {
            Window babySitter = (Window) desc.babysitter;
            raiseApplicationWindow(babySitter);
            getTabbedPane(babyComponent).setSelectedComponent(babyComponent);
        } else if ( desc.babysitter instanceof TearoffTabbedPane ) {
            Window parent= SwingUtilities.getWindowAncestor(babyComponent);
            ((TearoffTabbedPane)desc.babysitter).setSelectedComponent(babyComponent);
            raiseApplicationWindow(parent);
        }
    }

    /**
     * raise the application window
     * http://stackoverflow.com/questions/309023/howto-bring-a-java-window-to-the-front
     */
    private static void raiseApplicationWindow( java.awt.Window window ) {
        window.setVisible(true);
        if ( window instanceof Frame ) {
            Frame frame= (Frame)window;
            int state = frame.getExtendedState();
            state &= ~JFrame.ICONIFIED;
            frame.setExtendedState(state);
        }
        window.setAlwaysOnTop(true); // security exception
        window.toFront();
        window.requestFocus();
        window.setAlwaysOnTop(false); // security exception  //ubuntu 10.04 this returns to the bottom
        //window.setVisible(false);
        //window.setVisible(true);
    }

    /**
     * tearoff into another tabbed pane, or create one if target is null.
     * @param target
     * @param selectedTab
     */
    private void tearoffIntoTearoffTabbedPane( TearoffTabbedPane target, int selectedTab ) {
        if (target != null) {
            TabDesc desc = getTabDesc(selectedTab);
            Component selectedComponent = getComponentAt(desc.index);
            //System.err.println("name="+selectedComponent.getName()+" hash="+selectedComponent.hashCode());  // see TearoffTabbedPaneDemo which sets name and indicates hashcode.
            TearoffTabbedPane.this.tearOff(selectedTab, target);
            target.add(desc.title, selectedComponent);
            target.setSelectedIndex(target.getTabCount() - 1);
            target.resetTearOffBabysitterName();
            Window w = SwingUtilities.getWindowAncestor(target);
            if (!target.isShowing()) {
                //w.setVisible(false);
                w.setVisible(true);
            }
            raiseApplicationWindow(w);            
        } else {
            TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
        }
    }

    private TabDesc getTabDesc( int tabNumber ) {
        for ( TabDesc td: tabs.values() ) {
            if ( td.index==tabNumber ) {
                return td;
            }
        }
        throw new IllegalArgumentException("no tab at index: "+tabNumber);
    }

    private TearoffTabbedPane getHoverTP( Component myFrame, Point myPosition ) {
        TearoffTabbedPane last=null;
        TearoffTabbedPane me= getTabbedPane( draggingFrame );

        for (  Entry<Component,TabDesc> entry : tabs.entrySet() ) {
            TabDesc d = (TabDesc) entry.getValue();
            if ( d.babysitter!=null ) {
                Component maybe= getTabbedPane(d.babysitter);
                if ( maybe!=null && maybe!=me ) {
                    Point p= SwingUtilities.convertPoint( myFrame, myPosition, maybe );
                    if ( maybe.getBounds().contains(p) && ( p.getY() - maybe.getY() ) < TOP_DROP_MARGIN ) {
                        last= (TearoffTabbedPane)maybe;
                    }
                }
            }
        }
        return last;
    }

    private class ParentMouseAdapter extends MouseAdapter {

        private ParentMouseAdapter() {
                tearOffMenu.add(new JMenuItem(new AbstractAction("undock") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                    }
                }));

                tearOffMenu.add(new JMenuItem(new AbstractAction("slide right") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        TearoffTabbedPane.this.slideRight(selectedTab);
                    }
                }));

                dockMenu.add(new JMenuItem(new AbstractAction("show") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        showIt();

                    //babySitter.toFront();  // no effect on Linux/Gnome
                    }
                }));
                dockMenu.add(new JMenuItem(new AbstractAction("dock") {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        TabDesc desc = null;
                        Component babyComponent = null;
                        for ( Component key: tabs.keySet() ) {
                            TabDesc d = (TabDesc) tabs.get(key);
                            if (d.index == selectedTab) {
                                desc = d;
                                babyComponent = key;
                                break;

                            }
                        }
                        
                        TearoffTabbedPane babySitterToUpdate= null;
                        if (desc==null) return;
                        if (desc.babysitter instanceof Window) {
                            ((Window) desc.babysitter).dispose();
                        } else if ( desc.babysitter instanceof TearoffTabbedPane ) {
                            TearoffTabbedPane bb= (TearoffTabbedPane) desc.babysitter;
                            if ( bb.getTabCount()==1 ) {
                                SwingUtilities.getWindowAncestor(bb).dispose();
                            } else {
                                babySitterToUpdate= bb;
                            }
                            // do nothing
                        }

                        
                        TearoffTabbedPane.this.dock(babyComponent);
                        
                        if ( babySitterToUpdate!=null ) {
                            babySitterToUpdate.resetTearOffBabysitterName();
                        }
                        
                    }
                }));
            }

            @Override
            public void mousePressed(MouseEvent event) {
                selectedTab = TearoffTabbedPane.this.indexAtLocation(event.getX(), event.getY());
                if (event.isPopupTrigger()) {
                    showPopupMenu(event);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if ( e.getClickCount()==2 ) {
                    showIt();
                    e.consume();
                }
            }


            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart != null && selectedTab != -1) {
                    //JFrame f= TearoffTabbedPane.this.tearOffIntoFrame( selectedTab );
                    //Point p= e.getPoint();
                    //SwingUtilities.convertPointToScreen( p ,(Component) e.getSource() );
                    //f.setLocation( p );
                    setCursor(null);

                    // See if there is another TearoffTabbedPane we can dock into.
                    TearoffTabbedPane last=null;
                    if ( draggingFrame!=null ) last= getHoverTP( e.getComponent(), e.getPoint() );

                    if ( last!=null ) {
                        TearoffTabbedPane babyComponent= getTabbedPane( draggingFrame );
                        if ( last!=babyComponent && babyComponent.getTabCount()==1 ) { // assert tabCount=1.
                            int i= TearoffTabbedPane.this.getSelectedIndex();
                            if (i>-1) TearoffTabbedPane.this.lastSelected= i;
                            TearoffTabbedPane.this.dock(babyComponent.getComponentAt(0)); // we need to dock it first, then tear it off into the other tab.
                            if (i>-1) TearoffTabbedPane.this.setSelectedIndex(i);
                            tearoffIntoTearoffTabbedPane( last, selectedTab );
                            draggingFrame.dispose();
                        }
                    }


                    if ( dropDirty!=null ) {
                        dropDirty.setDropDecorate(false);
                        dropDirty.repaint();
                    }

                    draggingFrame = null;
//
//                    if ( e.isShiftDown() ) {
//                        System.err.println("shift is down");
//                        //check for another Babysitter, dock into it...  Provide feedback...
//                    }
                }
                dragStart = null;
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

    }

    /**
     * isolate the logic of finding the TearoffTabbedPane.  This looks for compoents:
     * * that have child TearoffTabbedPane
     * * are child of a TearoffTabbedPane
     * @param comp
     * @return
     */
    private TearoffTabbedPane getTabbedPane( Component comp ) {
        if ( comp instanceof JFrame && ((JFrame)comp).getContentPane().getComponent(0) instanceof TearoffTabbedPane ) {
            return (TearoffTabbedPane)(((JFrame)comp).getContentPane().getComponent(0));
        } else if ( comp instanceof TearoffTabbedPane ) {
            return (TearoffTabbedPane) comp;
        } else if ( comp.getParent()!=null && ( comp.getParent() instanceof TearoffTabbedPane ) ) {
            return (TearoffTabbedPane)(comp.getParent());
        } else {
            return null;
        }
    }

    /**
     * get a component to occupy the space when a tab is undocked.
     * @return
     */
    static Component getTornOffComponent() {
        JPanel tornOffComponent = new JPanel();
        tornOffComponent.setLayout(new BorderLayout());
        tornOffComponent.add(new JLabel("<html><i>This tab is undocked.  Right-click on the tab name and select dock.</i></html>"), BorderLayout.NORTH);
        return tornOffComponent;
    }

    public void tearOff(int tabIndex, Container newContainer) {
        logger.log( Level.FINE, "tearOff({0},{1})", new Object[]{tabIndex, newContainer});
        int lastSelected1 = this.lastSelected;
        Component c = getComponentAt(tabIndex);
        String title = super.getTitleAt(tabIndex);
        super.removeTabAt(tabIndex);
        super.insertTab("(" + title + ")", null, getTornOffComponent(), null, tabIndex); // we don't really need to do this for child tabs.
        super.setEnabledAt(tabIndex, false);
        TabDesc td = ((TabDesc) tabs.get(c));
        if ( td!=null ) td.babysitter = newContainer; // drop into another frame
        if ( newContainer instanceof TearoffTabbedPane ) { // slide right
            TearoffTabbedPane tt= (TearoffTabbedPane)newContainer;
            Window ttp= SwingUtilities.getWindowAncestor(tt);
            int dx= ttp.getWidth() - ( tt.getWidth() - 20 );
            int dy= ttp.getHeight() - ( tt.getHeight() - 40 ); // kludge for size of labels
            if ( tt.getTabCount()==0 ) {
                ttp.setSize( c.getPreferredSize().width + dx, c.getPreferredSize().height + dy);
            }
            
        }
        if ( this.parentPane==null ) {
            setSelectedIndex(lastSelected1);
        }
    }

    /**
     * move the frame to follow the master frame.
     */
    private final static Object STICK_RIGHT= "right";

    /**
     * get the listener that will keep the two JFrames close together
     * @param panel1  component within the master frame.
     * @param frame1  master frame that controls.
     * @param panel2  component within the compliant frame
     * @param frame2  compliant frame that follows.
     * @param direction the direction, which is STICK_RIGHT (private) or null
     * @return a listener
     */
    public ComponentListener getFrameComponentListener(
            final Component panel1, final Component frame1,
            final Component panel2, final Component frame2, final Object direction ) {

        return new ComponentListener() {
            Component activeComponent;
            long activeComponentTime=0;

            @Override
            public void componentResized(ComponentEvent e) {
                long t= System.currentTimeMillis();
                if ( ( t-activeComponentTime ) > 100 ) {
                    activeComponent= e.getComponent();
                }
                if ( e.getComponent()==activeComponent ) {
                    activeComponentTime= t;
                    updateAttached( activeComponent, panel1, frame1, panel2, frame2, direction, true );
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                long t= System.currentTimeMillis();
                if ( ( t-activeComponentTime ) > 100 ) {
                    activeComponent= e.getComponent();
                }
                if ( e.getComponent()==activeComponent ) {
                    activeComponentTime= t;
                    updateAttached( activeComponent, panel1, frame1, panel2, frame2, direction, false );
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        };
    }

    private void updateAttached(
            final Component active,
            final Component panel1, final Component frame1,
            final Component panel2, final Component frame2, Object direction, boolean updateSize ) {
        Point p = SwingUtilities.convertPoint(panel1, 0, 0, frame1);
        Point p2 = SwingUtilities.convertPoint(panel2, 0, 0, frame2);
        Dimension s1= panel1.getSize();
        Dimension frameSize1= frame1.getSize();
        Dimension s2= panel2.getSize();

        if ( direction==STICK_RIGHT ) {
            if ( active==frame1 ) {
                int delta= frame2.getWidth() - (int)s2.getWidth();
                // wdelta shrinks right frame
                //GraphicsDevice gd= java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
                //TODO: bad assumption what if left is not index 0?
                if ( updateSize ) {
                    //int leftWidth= gd.getDisplayMode().getWidth();
                    //int wdelta= leftWidth - ( frame1.getX() + frame1.getWidth() + rightOffset + frame2.getWidth() );
                    //if ( frame1.getX() + frame1.getWidth() > leftWidth ) {
                    //    wdelta= 0;
                    //} // if we're not on the left side
                    //frame2.setSize( new Dimension( s2.width  + delta + wdelta, s1.height + p2.y ) );
                    frame2.setSize( new Dimension( s2.width  + delta, s1.height + p2.y ) );
                    //frame2.setSize( new Dimension( s1.width, s1.height + p2.y ) ); // old code
                }
                frame2.setLocation( frame1.getX() + frame1.getWidth() - p2.x + rightOffset, frame1.getY() + p.y - p2.y );
            } else {
                int x= Math.max( frame1.getX(), frame2.getX()-frameSize1.width + p2.x );
                rightOffset= frame2.getX()-s1.width - frame1.getX();
                if ( rightOffset>0 ) rightOffset=0;
                if ( rightOffset< -1*s1.width ) {
                    x+= s1.width + rightOffset;
                    rightOffset= -1 * s1.width;
                }
                frame1.setLocation( x, frame2.getY() - p.y + p2.y );
            }
        }
    }

    /**
     * provide a reference to the right tabbed pane, possibly creating it.
     * @return
     */
    private synchronized TearoffTabbedPane getRightTabbedPane( ) {
        if (rightPane == null) {

            final JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            rightPane = new TearoffTabbedPane(this);
            rightPane.setName("rightTearoffTabbedPane");
            rightFrame = new JFrame();

            rightFrame.add(rightPane);
            rightFrame.setIconImage( parent.getIconImage() );
            rightFrame.setTitle( parent.getTitle().toLowerCase() );

            final WindowStateListener listener = new WindowStateListener() {
                @Override
                public void windowStateChanged(WindowEvent e) {
                    rightFrame.setExtendedState(parent.getExtendedState());
                }
            };
            parent.addWindowStateListener(listener);

            rightFrame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    parent.removeWindowStateListener(listener);
                    parent.removeComponentListener(rightFrameListener);

                    if ( rightPane!=null ) {
                        for (Component c : new ArrayList<>(rightPane.tabs.keySet())) {
                            TearoffTabbedPane.this.dock(c);
                        }
                    }
                    rightFrame = null;
                    rightPane = null;
                }
            });

            copyInputMap(parent, rightFrame);
            rightFrameListener = getFrameComponentListener(this, parent, rightPane, rightFrame, STICK_RIGHT );
            
            parent.addComponentListener(rightFrameListener);
            rightFrame.addComponentListener(rightFrameListener);

            rightPane.setPreferredSize(this.getSize());

            rightFrame.pack();
            updateAttached( parent, this, parent, rightPane, rightFrame, STICK_RIGHT, true );

            rightFrame.setVisible(true);
            parent.toFront();

        }
        return rightPane;
    }

    /**
     * instead of undocking, "slide" the component into a second JFrame that follows the first.
     * This may create the JFrame that accepts tabs.
     * @param tabIndex the tab to slide (0 is the left or first tab)
     */
    public void slideRight(int tabIndex) {

        final Component c = getComponentAt(tabIndex);
        logger.log(Level.FINEST, "slideRight {0}", c);

        setSelectedIndex(tabIndex);
        c.setVisible(true);  // darwin bug297

        TabDesc td = (TabDesc) tabs.get(c);
        if (td == null) {
            return;
        }

        TearoffTabbedPane right = getRightTabbedPane();

        tearOff(tabIndex, right);

        right.add(td.title, c);
        right.setSelectedIndex(right.getTabCount()-1);
        if ( !right.isShowing() ) {
            Window w= SwingUtilities.getWindowAncestor(right);
            w.setVisible(false);
            w.setVisible(true);
        }
    }

    /**
     * create a new Frame to contain the tab that was torn off.  This may happen
     * with the menu item "undock" or when a drag is begun within the tab.
     * @param tabIndex the tab to slide (0 is the left or first tab)
     * @return the new frame
     */
    protected JFrame tearOffIntoFrame(int tabIndex) {
        final Component c = getComponentAt(tabIndex);
        logger.log(Level.FINEST, "tearOffInfoFrame {0}", c);
        setSelectedIndex(tabIndex);
        c.setVisible(true);  // darwin bug297
        Point p = c.getLocationOnScreen();
        TabDesc td = (TabDesc) tabs.get(c);
        if (td == null) {
            return null;
        }
        final JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        final JFrame newParent = new JFrame(td.title);
        newParent.setIconImage( parent.getIconImage() );
        final WindowStateListener listener = new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                newParent.setExtendedState(parent.getExtendedState());
            }
        };
        parent.addWindowStateListener(listener);

        final TearoffTabbedPane pane = new TearoffTabbedPane(this);
        
        final TearoffTabbedPane dockParent= this.parentPane!=null ? this.parentPane : this ;
        
        p.translate(20, 20);
        newParent.setLocation(p);
        newParent.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                Component[] cc= pane.getComponents();
                parent.removeWindowStateListener(listener);
                for ( Component c: cc ) {
                    dockParent.dock(c);
                }
            }
        });
        // possibly add listener for closing here for https://sourceforge.net/p/autoplot/bugs/1620/

        copyInputMap(parent, newParent);

        newParent.getContentPane().add(pane);

        tearOff(tabIndex, pane);
        td.babysitter= pane;
        pane.add(td.title, c);
        pane.setName(td.title);
        
        newParent.pack();
        newParent.setVisible(true);

        return newParent;
    }

    private void resetTearOffBabysitterName(  ) {
        Window wparent= SwingUtilities.getWindowAncestor(this);
        if ( wparent==null ) {
            return;
        }
        if ( !( wparent instanceof JFrame ) ) {
            throw new RuntimeException( "internal error, parent was not instance of JFrame" );
        }
        JFrame parent= (JFrame)wparent;
        
        if ( this.parentPane==null ) {
            throw new IllegalStateException("name should not be set for parent, only babysitters");
        }
        Container p= parent.getContentPane();
        Component tp= p.getComponent(0);
        if ( tp instanceof TearoffTabbedPane ) {
            TearoffTabbedPane tt= (TearoffTabbedPane)tp;
            
            StringBuilder b= new StringBuilder();
            for ( int i=0; i<tt.getTabCount(); i++ ) {
                try {
                TabDesc td= tt.getTabDesc(i);
                b.append(",").append( td.title);
                } catch ( IllegalArgumentException ex ) {
                    System.err.println("invalid");
                }
            }
            if ( b.length()>0 ) {
                parent.setTitle( b.toString().substring(1) );
                parent.setName( b.toString().substring(1).replaceAll(",","_") );
                tp.setName( b.toString().substring(1).replaceAll(",","_") );
            }
        }
    }
    
    /**
     * return the component into this TearoffTabbedPane.
     * @param c the component.
     */
    public void dock(Component c) {
        logger.log(Level.FINEST, "dock {0}", c);
        int selectedIndex = getSelectedIndex();
        TabDesc td = (TabDesc) this.tabs.get(c);
        if ( td==null ) {
            logger.log( Level.WARNING, "I thought this might happen.  td==null in dock...");
            return;
        }
        int index = td.index;
        if ( index>=super.getTabCount() ) {
            System.err.println("something has gone wrong.  We haven't accounted for a tab which was removed.");
        } else {
            super.removeTabAt(index);
        }
        super.insertTab(td.title, td.icon, c, td.tip, index);
        super.setEnabledAt(index, true);
        Container babysitter= td.babysitter;
        td.babysitter= null; // get rid of reference so it will be garbage collected.
        if ( babysitter!=null ) {
            if ( babysitter instanceof TearoffTabbedPane ) {
                TearoffTabbedPane tbabysitter= (TearoffTabbedPane)babysitter;
                if ( tbabysitter.getTabCount()==0 ) {
                    Window w= SwingUtilities.getWindowAncestor(tbabysitter);
                    if ( w.getComponentCount()==1 ) { // the tearoff tabbed pane
                        w.dispose();
                    } else {
                        tbabysitter.resetTearOffBabysitterName();
                    }
                }
            } else {
                babysitter.setVisible(false);
            }
        }
        
        if ( parentPane!=null ) {
            resetTearOffBabysitterName();
        }
        raiseApplicationWindow( SwingUtilities.getWindowAncestor(this) );
        setSelectedIndex(selectedIndex);
    }

    @Override
    public void addTab(String title, Icon icon, Component component) {
        super.addTab(title, icon, component);
        TabDesc td = new TabDesc(title, icon, null, indexOfComponent(component));
        tabs.put(component, td);
    }

    @Override
    public void addTab(String title, Component component) {
        super.addTab(title, component);
        TabDesc td = new TabDesc(title, null, null, indexOfComponent(component));
        tabs.put(component, td);
    }

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        TabDesc td = new TabDesc(title, icon, tip, index);
        tabs.put(component, td);
    }

    @Override
    public void addTab(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
        TabDesc td = new TabDesc(title, icon, tip, indexOfComponent(component));
        tabs.put(component, td);
    }

    @Override
    public void remove( Component c ) {
        logger.log(Level.FINE, "remove({0})", c);
        TabDesc desc= tabs.get(c);
        if ( desc==null ) {
            //System.err.println("here c has no desc");
            logger.fine("Component does not appear to be associated with this TearoffTabbedPane");
            return;
        }
        if ( desc.babysitter!=null ) {
            this.dock(c);
        }
        super.remove(c);
    }

    /**
     * return the component with the tab description containing this index.
     * @param index
     * @return 
     */
    private Component getTabComponentByIndex(int index) {
        for (  Entry<Component,TabDesc> entry : tabs.entrySet() ) {
            TabDesc td = entry.getValue();
            if (td.index == index) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * return the tab contents, the first tab with this name.
     * @param title
     * @return the component in this tab.
     */
    public Component getTabByTitle( String title ) {
        for (  Entry<Component,TabDesc> entry : tabs.entrySet() ) {
            TabDesc td = entry.getValue();
            if (td.title.equals(title)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private TabDesc getTabDescByComponent( Component c ) {
        return tabs.get(c);
    }

    @Override
    public void removeTabAt(int index) {
        removeTabAt( index, true );
    }
    
    private void removeTabAt(int index,boolean dock) {
        logger.log(Level.FINE, "removeTabAt({0})", index);
        Component c = getTabComponentByIndex(index);
        if ( c==null ) {
            System.err.println("no tab at index: "+index);
        }
        TabDesc tab = tabs.get(c);
        if ( tab!=null ) {
            if ( dock && tab.babysitter != null ) { //perhaps better to dock it first
                dock(c);
            }
            tabs.remove(c);
        } else {
            logger.fine("tabs didn't contain c, someone else removed it.");
            //TODO: clean this up.
        }
        for ( TabDesc t: tabs.values() ) {
            if ( t.index>=index ) {
                t.index--;
            }
        }
        super.removeTabAt(index);
    }

    @Override
    public void setSelectedIndex(int index) {
        logger.log( Level.FINER, "setSelectedIndex({0})", index );
        
        if (index != getSelectedIndex()) {
            lastSelected = getSelectedIndex();
        }
        super.setSelectedIndex(index);
    }
    
    /**
     * this will set the selected tab, or raise the babysitter
     * @param title 
     */
    public void setSelectedTab( String title ) {
        int sel= -1;
        for ( int i=0; i<getTabCount(); i++ ) {
            if ( this.getTitleAt(i).equals(title) ) {
                sel= i;
            }
        }
        if ( sel>-1 ) {
            this.setSelectedIndex(sel);
        }
    }
    
    public static void main( String[] args ) {
        TearoffTabbedPaneDemo.main(args);
    }
}

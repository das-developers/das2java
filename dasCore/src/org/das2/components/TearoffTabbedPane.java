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
import java.awt.Point;
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
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

/**
 *
 * @author Jeremy
 */
public class TearoffTabbedPane extends JTabbedPane {

    int selectedTab;
    Point dragStart;
    Point dragOffset;
    JFrame draggingFrame;
    JPopupMenu tearOffMenu = new JPopupMenu();
    JPopupMenu dockMenu = new JPopupMenu();

    private TearoffTabbedPane parentPane;

    private TearoffTabbedPane rightPane = null;
    private TearoffTabbedPane dropDirty = null;

    private JFrame rightFrame = null;
    private ComponentListener rightFrameListener;
    private int rightOffset= 0;

    private final static Logger logger= Logger.getLogger( TearoffTabbedPane.class.getCanonicalName() );

    HashMap<Component, TabDesc> tabs = new HashMap<Component, TabDesc>();
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

    private static class TabDesc {

        Icon icon;
        String title;
        String tip;
        int index;
        Container babysitter;

        TabDesc(String title, Icon icon, String tip, int index) {
            this.title = title;
            this.icon = icon;
            this.tip = tip;
            this.index = index;
            this.babysitter = null;
        }
    }

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
        }
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
        if ( getMouseMotionListeners().length>0 ) {
            MouseMotionListener ml= getMouseMotionListeners()[0];
            removeMouseMotionListener( ml );
        }

    }

    private MouseMotionListener getMouseMotionListener() {
        return new MouseMotionListener() {

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
                            dropDirty.setLocation( 0,0 );
                            dropDirty.repaint();
                        }
                        
                        if ( drop!=null ) {
                            drop.setLocation( 4,4 );
                            drop.repaint();
                            dropDirty= drop;
                        } else {
                            dropDirty= null;
                        }

                    }
                }
            }

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
                dockMenu.add(new JMenuItem(new AbstractAction("dock") {

                    public void actionPerformed(ActionEvent event) {

                        if (parentPane != null) {
                            selectedComponent = getComponent(selectedTab);
                            remove(selectedComponent);
                            parentPane.dock(selectedComponent);
                            if ( getTabCount()==0 ) {
                                SwingUtilities.getWindowAncestor(TearoffTabbedPane.this).dispose();
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
                TearoffTabbedPane draggingTearOff=null;
                if ( draggingFrame!=null ) {
                    draggingTearOff= getTabbedPane(draggingFrame);
                    if ( draggingTearOff!=null && TearoffTabbedPane.this.parentPane.contains( SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), TearoffTabbedPane.this.parentPane ) ) ) {
                        TearoffTabbedPane.this.parentPane.dock(draggingTearOff.getComponentAt(0));
                        draggingFrame.dispose();
                    }
                    TearoffTabbedPane oldChildParent= getTabbedPane(e.getComponent());
                    if ( oldChildParent.getTabCount()==0 ) {
                        SwingUtilities.getWindowAncestor(e.getComponent()).dispose();
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
                            draggingFrame = TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                            TearoffTabbedPane carry= getTabbedPane(draggingFrame);
                            carry.parentPane= TearoffTabbedPane.this.parentPane;
                            TearoffTabbedPane.super.removeTabAt(selectedTab);
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
                        if ( TearoffTabbedPane.this.parentPane.contains( SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), TearoffTabbedPane.this.parentPane ) ) ) {
                            drop= TearoffTabbedPane.this.parentPane;
                        }

                        if ( dropDirty!=null ) { // give some hint that this is a drop target.
                            dropDirty.setLocation( 0,0 );
                            dropDirty.repaint();
                        }

                        if ( drop!=null ) {
                            drop.setLocation( 4,4 );
                            drop.repaint();
                            dropDirty= drop;
                        } else {
                            dropDirty= null;
                        }

                    }
                }
            }

            public void mouseMoved(MouseEvent e) {
            }


        };
    }

    private void showIt() {

        TabDesc desc = null;
        Component babyComponent = null;
        for (Iterator i = tabs.keySet().iterator(); i.hasNext();) {
            Component key = (Component) i.next();
            TabDesc d = (TabDesc) tabs.get(key);
            if (d.index == selectedTab) {
                desc = d;
                babyComponent = key;
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
        window.setVisible(false);
        window.setVisible(true);
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
            if (!target.isShowing()) {
                Window w = SwingUtilities.getWindowAncestor(target);
                w.setVisible(false);
                w.setVisible(true);
            }
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

        for (Iterator i = tabs.keySet().iterator(); i.hasNext();) {
            Component key = (Component) i.next();
            TabDesc d = (TabDesc) tabs.get(key);
            if ( d.babysitter!=null ) {
                Component maybe= getTabbedPane(d.babysitter);
                if ( maybe!=null && maybe!=me ) {
                    Point p= SwingUtilities.convertPoint( myFrame, myPosition, maybe );
                    if ( maybe.getBounds().contains(p) ) {
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
                    public void actionPerformed(ActionEvent event) {
                        TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                    }
                }));

                tearOffMenu.add(new JMenuItem(new AbstractAction("slide right") {
                    public void actionPerformed(ActionEvent event) {
                        TearoffTabbedPane.this.slideRight(selectedTab);
                    }
                }));

                dockMenu.add(new JMenuItem(new AbstractAction("show") {
                    public void actionPerformed(ActionEvent event) {
                        showIt();

                    //babySitter.toFront();  // no effect on Linux/Gnome
                    }
                }));
                dockMenu.add(new JMenuItem(new AbstractAction("dock") {

                    public void actionPerformed(ActionEvent event) {
                        TabDesc desc = null;
                        Component babyComponent = null;
                        for (Iterator i = tabs.keySet().iterator(); i.hasNext();) {
                            Component key = (Component) i.next();
                            TabDesc d = (TabDesc) tabs.get(key);
                            if (d.index == selectedTab) {
                                desc = d;
                                babyComponent = key;
                                break;

                            }
                        }
                        if (desc==null) return;
                        if (desc.babysitter instanceof Window) {
                            ((Window) desc.babysitter).dispose();
                        } else if ( desc.babysitter instanceof TearoffTabbedPane ) {
                            TearoffTabbedPane bb= (TearoffTabbedPane) desc.babysitter;
                            if ( bb.getTabCount()==1 ) {
                                SwingUtilities.getWindowAncestor(bb).dispose();
                            }
                            // do nothing
                        }

                        TearoffTabbedPane.this.dock(babyComponent);
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
                    TearoffTabbedPane last= getHoverTP( e.getComponent(), e.getPoint() );

                    if ( last!=null && draggingFrame!=null ) {
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
                        dropDirty.setLocation( 0,0 );
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
        } else if ( comp instanceof JPanel && comp.getParent() instanceof TearoffTabbedPane ) {
            return (TearoffTabbedPane)(comp.getParent());
        } else if ( comp instanceof TearoffTabbedPane ) {
            return (TearoffTabbedPane) comp;
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
        int lastSelected1 = this.lastSelected;
        Component c = getComponentAt(tabIndex);
        String title = super.getTitleAt(tabIndex);
        super.removeTabAt(tabIndex);
        super.insertTab("(" + title + ")", null, getTornOffComponent(), null, tabIndex);
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
        setSelectedIndex(lastSelected1);
    }

    private final static Object STICK_RIGHT= "right";

    /**
     * get the listener that will keep the two JFrames close together
     * @param panel1  component within the master frame.
     * @param frame1  master frame that controls.
     * @param panel2  component within the compliant frame
     * @param frame2  compliant frame that follows.
     * @param direction
     * @return
     */
    public ComponentListener getFrameComponentListener(
            final Component panel1, final Component frame1,
            final Component panel2, final Component frame2, final Object direction ) {

        return new ComponentListener() {
            Component activeComponent;
            long activeComponentTime=0;

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

            public void componentShown(ComponentEvent e) {
            }

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
                if ( false && updateSize ) {
                    int frame1NotTabs= frameSize1.height-s1.height;
                    System.err.println(frame1NotTabs);
                    frame1.setSize( new Dimension( frameSize1.width, ( frameSize1.height-s1.height ) + s2.height ) );
                }
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
                        for (Component c : new ArrayList<Component>(rightPane.tabs.keySet())) {
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
     * @param tabIndex
     */
    protected void slideRight(int tabIndex) {

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
     * @param tabIndex
     * @return
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
        final JFrame babySitter = new JFrame(td.title);
        babySitter.setIconImage( parent.getIconImage() );
        final WindowStateListener listener = new WindowStateListener() {

            public void windowStateChanged(WindowEvent e) {
                babySitter.setExtendedState(parent.getExtendedState());
            }
        };
        parent.addWindowStateListener(listener);

        final JTabbedPane pane = new TearoffTabbedPane(this);

        p.translate(20, 20);
        babySitter.setLocation(p);
        babySitter.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                Component[] cc= pane.getComponents();
                parent.removeWindowStateListener(listener);
                for ( Component c: cc ) {
                    dock(c);
                }
            }
        });

        copyInputMap(parent, babySitter);

        babySitter.getContentPane().add(pane);

        tearOff(tabIndex, babySitter);
        pane.add(td.title, c);
        pane.setName(td.title);

        babySitter.pack();
        babySitter.setVisible(true);

        return babySitter;
    }

    public void dock(Component c) {
        logger.log(Level.FINEST, "dock {0}", c);
        int selectedIndex = getSelectedIndex();
        TabDesc td = (TabDesc) tabs.get(c);
        int index = td.index;
        super.removeTabAt(index);
        super.insertTab(td.title, td.icon, c, td.tip, index);
        super.setEnabledAt(index, true);
        td.babysitter= null; // get rid of reference so it will be garbage collected.
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

    private Component getTabComponentByIndex(int index) {
        for (Component key : tabs.keySet()) {
            TabDesc td = tabs.get(key);
            if (td.index == index) {
                return key;
            }
        }
        return null;
    }

    @Override
    public void removeTabAt(int index) {
        Component c = getTabComponentByIndex(index);
        super.removeTabAt(index);
        TabDesc tab = tabs.get(c);
        if ( tab!=null ) {
            if ( tab.babysitter != null ) { //perhaps better to dock it first
                if (tab.babysitter instanceof Window) {
                    ((Window) tab.babysitter).dispose();
                }
            }
            tabs.remove(c);
        } else {
            logger.fine("tabs didn't contain c, someone else removed it.");
            //TODO: clean this up.
        }
        
    }

    @Override
    public void setSelectedIndex(int index) {
        if (index != getSelectedIndex()) {
            lastSelected = getSelectedIndex();
        }
        super.setSelectedIndex(index);
        logger.log(Level.FINEST, "setSelectedIndex {0}", getSelectedComponent());
    }
}

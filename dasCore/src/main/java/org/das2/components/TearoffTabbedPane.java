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
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private JFrame rightFrame = null;
    private ComponentListener rightFrameListener;
    private int rightOffset= 0;

    private final static Logger logger= Logger.getLogger( TearoffTabbedPane.class.getCanonicalName() );

    HashMap<Component, TabDesc> tabs = new HashMap<>();
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

    public void slideRight(Component tab) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class TabDesc {

        Icon icon;
        String title;
        String tip;
        int index;
        Container babysitter;
        Component component;

        TabDesc(String title, Icon icon, Component component, String tip, int index) {
            this.title = title;
            this.icon = icon;
            this.component = component;
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
            MouseAdapter ma = getParentMouseAdapter();
            addMouseListener(ma);
            addMouseMotionListener(getMouseMotionListener());
        } else {
            parentPane = parent;
            addMouseListener(getChildMouseAdapter());
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
                        if (draggingFrame == null) {
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
                dockMenu.add(new JMenuItem(new AbstractAction("dock") {

						  @Override
                    public void actionPerformed(ActionEvent event) {
                        TabDesc desc = null;

                        for (Iterator i = tabs.keySet().iterator(); i.hasNext();) {
                            Component key = (Component) i.next();
                            TabDesc d = (TabDesc) tabs.get(key);
                        }

                        if (parentPane != null) {
                            selectedComponent = getComponent(selectedTab);
                            remove(selectedComponent);
                            parentPane.dock(selectedComponent);
                            if ( getTabCount()==0 ) {
                                SwingUtilities.getWindowAncestor(TearoffTabbedPane.this).dispose();
                            } 
                        } else {
                            if (desc.babysitter instanceof Window) {
                                ((Window) desc.babysitter).dispose();
                            }
                            parentPane.dock(selectedComponent);
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

    private MouseAdapter getParentMouseAdapter() {
        return new MouseAdapter() {

            {
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
            }
            Component selectedComponent;


            {
                dockMenu.add(new JMenuItem(new AbstractAction("show") {

						  @Override
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
                        if (desc.babysitter instanceof Window) {
                            Window babySitter = (Window) desc.babysitter;
                            babySitter.setVisible(false);
                            babySitter.setVisible(true);
                        } else if ( desc.babysitter instanceof TearoffTabbedPane ) {
                            Window parent= SwingUtilities.getWindowAncestor(babyComponent);
                            parent.setVisible(false);
                            parent.setVisible(true);
                        }
                        
                    //babySitter.toFront();  // no effect on Linux/Gnome
                    }
                }));
                dockMenu.add(new JMenuItem(new AbstractAction("dock") {

						  @Override
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
            public void mouseReleased(MouseEvent e) {
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
        int lastSelected = this.lastSelected;
        Component c = getComponentAt(tabIndex);
        String title = super.getTitleAt(tabIndex);
        super.removeTabAt(tabIndex);
        super.insertTab("(" + title + ")", null, getTornOffComponent(), null, tabIndex);
        super.setEnabledAt(tabIndex, false);
        TabDesc td = ((TabDesc) tabs.get(c));
        td.babysitter = newContainer;
        setSelectedIndex(lastSelected);
    }

    private final static Object STICK_LEFT= "left";
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
        Dimension frameSize2= frame2.getSize();

        if ( direction==STICK_RIGHT ) {
            if ( active==frame1 ) {
                if ( updateSize ) frame2.setSize( new Dimension( s1.width, s1.height + p2.y ) );
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

    private synchronized TearoffTabbedPane getRightTabbedPane() {
        if (rightPane == null) {

            final JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            rightPane = new TearoffTabbedPane(this);
            rightFrame = new JFrame();

            rightFrame.add(rightPane);
            rightFrame.setIconImage( parent.getIconImage() );

            final WindowStateListener listener = new WindowStateListener() {

                public void windowStateChanged(WindowEvent e) {
                    rightFrame.setExtendedState(parent.getExtendedState());
                }
            };
            parent.addWindowStateListener(listener);

            rightFrame.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    parent.removeWindowStateListener(listener);
                    parent.removeComponentListener(rightFrameListener);

                    for (Component c : new ArrayList<Component>(rightPane.tabs.keySet())) {
                        TearoffTabbedPane.this.dock(c);
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

    public void slideRight(int tabIndex) {

        final Component c = getComponentAt(tabIndex);
        logger.finest("slideRight "+c);

        setSelectedIndex(tabIndex);
        c.setVisible(true);  // darwin bug297

        TabDesc td = (TabDesc) tabs.get(c);
        if (td == null) {
            return;
        }
        final JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);

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

    protected JFrame tearOffIntoFrame(int tabIndex) {
        final Component c = getComponentAt(tabIndex);
        logger.finest("tearOffInfoFrame "+c);
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

				@Override
            public void windowStateChanged(WindowEvent e) {
                babySitter.setExtendedState(parent.getExtendedState());
            }
        };
        parent.addWindowStateListener(listener);

        p.translate(20, 20);
        babySitter.setLocation(p);
        babySitter.addWindowListener(new WindowAdapter() {

				@Override
            public void windowClosing(WindowEvent e) {
                parent.removeWindowStateListener(listener);
                dock(c);
            }
        });

        copyInputMap(parent, babySitter);

        JTabbedPane pane = new TearoffTabbedPane(this);
        babySitter.getContentPane().add(pane);

        tearOff(tabIndex, babySitter);
        pane.add(td.title, c);

        babySitter.pack();
        babySitter.setVisible(true);

        return babySitter;
    }

    public void dock(Component c) {
        logger.finest("dock "+c);
        int selectedIndex = getSelectedIndex();
        TabDesc td = (TabDesc) tabs.get(c);
        int index = td.index;
        super.removeTabAt(index);
        super.insertTab(td.title, td.icon, c, td.tip, index);
        super.setEnabledAt(index, true);
        setSelectedIndex(selectedIndex);
    }

	 @Override
    public void addTab(String title, Icon icon, Component component) {
        super.addTab(title, icon, component);
        TabDesc td = new TabDesc(title, icon, component, null, indexOfComponent(component));
        tabs.put(component, td);
    }

	 @Override
    public void addTab(String title, Component component) {
        super.addTab(title, component);
        TabDesc td = new TabDesc(title, null, component, null, indexOfComponent(component));
        tabs.put(component, td);
    }

	 @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        TabDesc td = new TabDesc(title, icon, component, tip, index);
        tabs.put(component, td);
    }

	 @Override
    public void addTab(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
        TabDesc td = new TabDesc(title, icon, component, tip, indexOfComponent(component));
        tabs.put(component, td);
    }

	 // This implementation looks backwords.  It looks like an implementation of
	 // getComponentByIndex, not getTabComponentByIndex
	 
    private Component getTabComponentByIndex(int index) {
        for (Component key : tabs.keySet()) {
            TabDesc td = tabs.get(key);
            if (td.index == index) {
                return key;
            }
        }
        return null;
    }

    private Component getTabComponentByTitle(String title) {
        for (Component key : tabs.keySet()) {
            TabDesc td = tabs.get(key);
            if (td.title == title) {
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
        logger.finest("setSelectedIndex "+getSelectedComponent());
    }
}

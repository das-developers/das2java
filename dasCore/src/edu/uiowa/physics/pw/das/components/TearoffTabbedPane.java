/*
 * TearoffTabbedPane.java
 *
 * Created on January 26, 2006, 7:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package edu.uiowa.physics.pw.das.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Icon;
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

    class TabDesc {

        Icon icon;
        String title;
        String tip;
        int index;
        Container babysitter;

        TabDesc(String title, Icon icon, Component component, String tip, int index) {
            this.title = title;
            this.icon = icon;
            this.tip = tip;
            this.index = index;
            this.babysitter = null;
        }
    }
    HashMap tabs = new HashMap();
    int lastSelected; /* keep track of selected index before context menu */


    public TearoffTabbedPane() {
        super();
        MouseAdapter ma = getMouseAdapter();
        addMouseListener(ma);
        addMouseMotionListener(getMouseMotionListener());
    }
    
    int selectedTab;
    Point dragStart;
    JFrame draggingFrame;
    JPopupMenu tearOffMenu = new JPopupMenu();

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
                            draggingFrame = TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                            if (draggingFrame == null) {
                                return;
                            }
                            setCursor(new Cursor(Cursor.MOVE_CURSOR));
                        }
                        Point p = e.getPoint();
                        SwingUtilities.convertPointToScreen(p, (Component) e.getSource());
                        draggingFrame.setLocation(p);
                    }
                }
            }

            public void mouseMoved(MouseEvent e) {
            }
            
        };
    }
    
    private MouseAdapter getMouseAdapter() {
        return new MouseAdapter() {

            {
                tearOffMenu.add(new JMenuItem(new AbstractAction("undock") {

                    public void actionPerformed(ActionEvent event) {
                        TearoffTabbedPane.this.tearOffIntoFrame(selectedTab);
                    }
                }));
            }
            Component selectedComponent;
            JPopupMenu dockMenu = new JPopupMenu();
            

            {
                dockMenu.add(new JMenuItem(new AbstractAction("show") {

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
                        JFrame babySitter = (JFrame) desc.babysitter;
                        babySitter.setVisible(true);
                        babySitter.toFront();  // no effect on Linux/Gnome
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

                        if (desc.babysitter instanceof Window) {
                            ((Window) desc.babysitter).dispose();
                        }

                        TearoffTabbedPane.this.dock(babyComponent);
                    }
                }));
            }

            public void mousePressed(MouseEvent event) {
                selectedTab = TearoffTabbedPane.this.indexAtLocation(event.getX(), event.getY());
                if (event.getButton() == MouseEvent.BUTTON3) {
                    selectedTab = TearoffTabbedPane.this.indexAtLocation(event.getX(), event.getY());
                    if (selectedTab != -1) {
                        selectedComponent = TearoffTabbedPane.this.getComponentAt(selectedTab);
                        if (tabs.get(selectedComponent) != null) {
                            tearOffMenu.show(TearoffTabbedPane.this, event.getX(), event.getY());
                        } else {
                            dockMenu.show(TearoffTabbedPane.this, event.getX(), event.getY());
                        }
                    }
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
            }
        };
    }

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

    private class AbstractWindowListener implements WindowListener {

        public void windowOpened(WindowEvent e) {
        }

        public void windowClosing(WindowEvent e) {
        }

        public void windowClosed(WindowEvent e) {
        }

        public void windowIconified(WindowEvent e) {
        }

        public void windowDeiconified(WindowEvent e) {
        }

        public void windowActivated(WindowEvent e) {
        }

        public void windowDeactivated(WindowEvent e) {
        }
    }

    protected JFrame tearOffIntoFrame(int tabIndex) {
        final Component c = getComponentAt(tabIndex);
        // TODO: can get IllegalComponentStateException with random clicks. See bug297
        setSelectedIndex(tabIndex);
        c.setVisible(true);  // darwin bug297
        Point p = c.getLocationOnScreen();
        TabDesc td = (TabDesc) tabs.get(c);
        if (td == null) {
            return null;
        }
        final JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        final JFrame babySitter = new JFrame(td.title);
        final WindowStateListener listener = new WindowStateListener() {

            public void windowStateChanged(WindowEvent e) {
                babySitter.setExtendedState(parent.getExtendedState());
            }
        };
        parent.addWindowStateListener(listener);

        p.translate(20, 20);
        babySitter.setLocation(p);
        babySitter.addWindowListener(new AbstractWindowListener() {

            public void windowClosing(WindowEvent e) {
                parent.removeWindowStateListener(listener);
                dock(c);
            }
        });
        JTabbedPane pane = new JTabbedPane();
        babySitter.getContentPane().add(pane);

        tearOff(tabIndex, babySitter);
        pane.add(td.title, c);

        babySitter.pack();
        babySitter.setVisible(true);

        return babySitter;
    }

    public void dock(Component c) {
        int selectedIndex = getSelectedIndex();
        TabDesc td = (TabDesc) tabs.get(c);
        int index = td.index;
        super.removeTabAt(index);
        super.insertTab(td.title, td.icon, c, td.tip, index);
        super.setEnabledAt(index, true);
        setSelectedIndex(selectedIndex);
    }

    public void addTab(String title, Icon icon, Component component) {
        super.addTab(title, icon, component);
        tabs.put(component, new TabDesc(title, icon, component, null, indexOfComponent(component)));
    }

    public void addTab(String title, Component component) {
        super.addTab(title, component);
        tabs.put(component, new TabDesc(title, null, component, null, indexOfComponent(component)));
    }

    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        super.insertTab(title, icon, component, tip, index);
        tabs.put(component, new TabDesc(title, icon, component, tip, index));
    }

    public void addTab(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
        tabs.put(component, new TabDesc(title, icon, component, tip, indexOfComponent(component)));
    }

    public void removeTabAt(int index) {
        super.removeTabAt(index);
        tabs.remove(getComponentAt(index));
    }

    public void setSelectedIndex(int index) {
        if (index != getSelectedIndex()) {
            lastSelected = getSelectedIndex();
        }
        super.setSelectedIndex(index);
    }
}

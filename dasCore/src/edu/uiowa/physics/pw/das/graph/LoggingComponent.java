/*
 * LoggingComponent.java
 *
 * Created on January 27, 2006, 2:15 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.*;
import java.util.logging.Logger;
import javax.swing.*;

/**
 *
 * @author eew
 */
public class LoggingComponent extends JComponent {
    
    Logger logger = DasLogger.getLogger();
    
    /** Creates a new instance of LoggingComponent */
    public LoggingComponent() {
    }

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public Component add(Component comp, int index) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        Component retValue;
        
        retValue = super.add(comp, index);
        return retValue;
    }

    public void add(Component comp, Object constraints, int index) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        super.add(comp, constraints, index);
    }

    public void show(boolean b) {
        if (logger != null) logger.entering(getClass().getName(), "show");

        super.show(b);
    }

    public void setVisible(boolean aFlag) {
        if (logger != null) logger.entering(getClass().getName(), "setVisible");

        super.setVisible(aFlag);
    }

    public void setVerifyInputWhenFocusTarget(boolean verifyInputWhenFocusTarget) {
        if (logger != null) logger.entering(getClass().getName(), "setVerifyInputWhenFocusTarget");

        super.setVerifyInputWhenFocusTarget(verifyInputWhenFocusTarget);
    }

    public void setRequestFocusEnabled(boolean requestFocusEnabled) {
        if (logger != null) logger.entering(getClass().getName(), "setRequestFocusEnabled");

        super.setRequestFocusEnabled(requestFocusEnabled);
    }

    public void setOpaque(boolean isOpaque) {
        if (logger != null) logger.entering(getClass().getName(), "setOpaque");

        super.setOpaque(isOpaque);
    }

    public void setIgnoreRepaint(boolean ignoreRepaint) {
        if (logger != null) logger.entering(getClass().getName(), "setIgnoreRepaint");

        super.setIgnoreRepaint(ignoreRepaint);
    }

    public void setFocusable(boolean focusable) {
        if (logger != null) logger.entering(getClass().getName(), "setFocusable");

        super.setFocusable(focusable);
    }

    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
        if (logger != null) logger.entering(getClass().getName(), "setFocusTraversalKeysEnabled");

        super.setFocusTraversalKeysEnabled(focusTraversalKeysEnabled);
    }

    public void setFocusCycleRoot(boolean focusCycleRoot) {
        if (logger != null) logger.entering(getClass().getName(), "setFocusCycleRoot");

        super.setFocusCycleRoot(focusCycleRoot);
    }

    public void setEnabled(boolean enabled) {
        if (logger != null) logger.entering(getClass().getName(), "setEnabled");

        super.setEnabled(enabled);
    }

    public void setDoubleBuffered(boolean aFlag) {
        if (logger != null) logger.entering(getClass().getName(), "setDoubleBuffered");

        super.setDoubleBuffered(aFlag);
    }

    public void setAutoscrolls(boolean autoscrolls) {
        if (logger != null) logger.entering(getClass().getName(), "setAutoscrolls");

        super.setAutoscrolls(autoscrolls);
    }

    public boolean requestFocus(boolean temporary) {
        if (logger != null) logger.entering(getClass().getName(), "requestFocus");

        boolean retValue;
        
        retValue = super.requestFocus(temporary);
        return retValue;
    }

    public void enable(boolean b) {
        if (logger != null) logger.entering(getClass().getName(), "enable");

        super.enable(b);
    }

    public void enableInputMethods(boolean enable) {
        if (logger != null) logger.entering(getClass().getName(), "enableInputMethods");

        super.enableInputMethods(enable);
    }

    public void setBounds(Rectangle r) {
        if (logger != null) logger.entering(getClass().getName(), "setBounds");

        super.setBounds(r);
    }

    public void scrollRectToVisible(Rectangle aRect) {
        if (logger != null) logger.entering(getClass().getName(), "scrollRectToVisible");

        super.scrollRectToVisible(aRect);
    }

    public void repaint(Rectangle r) {
        if (logger != null) logger.entering(getClass().getName(), "repaint");

        super.repaint(r);
    }

    public void paintImmediately(Rectangle r) {
        if (logger != null) logger.entering(getClass().getName(), "paintImmediately");

        super.paintImmediately(r);
    }

    public Rectangle getBounds(Rectangle rv) {
        if (logger != null) logger.entering(getClass().getName(), "getBounds");

        Rectangle retValue;
        
        retValue = super.getBounds(rv);
        return retValue;
    }

    public void computeVisibleRect(Rectangle visibleRect) {
        if (logger != null) logger.entering(getClass().getName(), "computeVisibleRect");

        super.computeVisibleRect(visibleRect);
    }

    public java.awt.image.VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps) throws AWTException {

        java.awt.image.VolatileImage retValue;
        
        retValue = super.createVolatileImage(width, height, caps);
        return retValue;
    }

    public void list(java.io.PrintStream out) {
        if (logger != null) logger.entering(getClass().getName(), "list");

        super.list(out);
    }

    public void setLayout(LayoutManager mgr) {
        if (logger != null) logger.entering(getClass().getName(), "setLayout");

        super.setLayout(mgr);
    }

    public boolean action(Event evt, Object what) {
        if (logger != null) logger.entering(getClass().getName(), "action");

        boolean retValue;
        
        retValue = super.action(evt, what);
        return retValue;
    }

    public boolean gotFocus(Event evt, Object what) {
        if (logger != null) logger.entering(getClass().getName(), "gotFocus");

        boolean retValue;
        
        retValue = super.gotFocus(evt, what);
        return retValue;
    }

    public boolean lostFocus(Event evt, Object what) {
        if (logger != null) logger.entering(getClass().getName(), "lostFocus");

        boolean retValue;
        
        retValue = super.lostFocus(evt, what);
        return retValue;
    }

    public void repaint(long tm) {
        if (logger != null) logger.entering(getClass().getName(), "repaint");

        super.repaint(tm);
    }

    public boolean postEvent(Event e) {
        if (logger != null) logger.entering(getClass().getName(), "postEvent");

        boolean retValue;
        
        retValue = super.postEvent(e);
        return retValue;
    }

    public void deliverEvent(Event e) {
        if (logger != null) logger.entering(getClass().getName(), "deliverEvent");

        super.deliverEvent(e);
    }

    public boolean handleEvent(Event evt) {
        if (logger != null) logger.entering(getClass().getName(), "handleEvent");

        boolean retValue;
        
        retValue = super.handleEvent(evt);
        return retValue;
    }

    public void list(java.io.PrintStream out, int indent) {
        if (logger != null) logger.entering(getClass().getName(), "list");

        super.list(out, indent);
    }

    public void setLocation(Point p) {
        if (logger != null) logger.entering(getClass().getName(), "setLocation");

        super.setLocation(p);
    }

    public Component getComponentAt(Point p) {
        if (logger != null) logger.entering(getClass().getName(), "getComponentAt");

        Component retValue;
        
        retValue = super.getComponentAt(p);
        return retValue;
    }

    public boolean contains(Point p) {
        if (logger != null) logger.entering(getClass().getName(), "contains");

        boolean retValue;
        
        retValue = super.contains(p);
        return retValue;
    }

    public Component findComponentAt(Point p) {
        if (logger != null) logger.entering(getClass().getName(), "findComponentAt");

        Component retValue;
        
        retValue = super.findComponentAt(p);
        return retValue;
    }

    public Point getLocation(Point rv) {
        if (logger != null) logger.entering(getClass().getName(), "getLocation");

        Point retValue;
        
        retValue = super.getLocation(rv);
        return retValue;
    }

    public void removeMouseListener(java.awt.event.MouseListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeMouseListener");

        super.removeMouseListener(l);
    }

    public void addMouseListener(java.awt.event.MouseListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addMouseListener");

        super.addMouseListener(l);
    }

    public void removeMouseWheelListener(java.awt.event.MouseWheelListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeMouseWheelListener");

        super.removeMouseWheelListener(l);
    }

    public void addMouseWheelListener(java.awt.event.MouseWheelListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addMouseWheelListener");

        super.addMouseWheelListener(l);
    }

    public void setTransferHandler(TransferHandler newHandler) {
        if (logger != null) logger.entering(getClass().getName(), "setTransferHandler");

        super.setTransferHandler(newHandler);
    }

    public void add(PopupMenu popup) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        super.add(popup);
    }

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void registerKeyboardAction(java.awt.event.ActionListener anAction, String aCommand, KeyStroke aKeyStroke, int aCondition) {
        if (logger != null) logger.entering(getClass().getName(), "registerKeyboardAction");

        super.registerKeyboardAction(anAction, aCommand, aKeyStroke, aCondition);
    }

    public void registerKeyboardAction(java.awt.event.ActionListener anAction, KeyStroke aKeyStroke, int aCondition) {
        if (logger != null) logger.entering(getClass().getName(), "registerKeyboardAction");

        super.registerKeyboardAction(anAction, aKeyStroke, aCondition);
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void setLocale(java.util.Locale l) {
        if (logger != null) logger.entering(getClass().getName(), "setLocale");

        super.setLocale(l);
    }

    public void unregisterKeyboardAction(KeyStroke aKeyStroke) {
        if (logger != null) logger.entering(getClass().getName(), "unregisterKeyboardAction");

        super.unregisterKeyboardAction(aKeyStroke);
    }

    public int getConditionForKeyStroke(KeyStroke aKeyStroke) {
        if (logger != null) logger.entering(getClass().getName(), "getConditionForKeyStroke");

        int retValue;
        
        retValue = super.getConditionForKeyStroke(aKeyStroke);
        return retValue;
    }

    public java.awt.event.ActionListener getActionForKeyStroke(KeyStroke aKeyStroke) {
        if (logger != null) logger.entering(getClass().getName(), "getActionForKeyStroke");

        java.awt.event.ActionListener retValue;
        
        retValue = super.getActionForKeyStroke(aKeyStroke);
        return retValue;
    }

    public boolean equals(Object obj) {
        if (logger != null) logger.entering(getClass().getName(), "equals");

        boolean retValue;
        
        retValue = super.equals(obj);
        return retValue;
    }

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public boolean prepareImage(Image image, int width, int height, java.awt.image.ImageObserver observer) {
        if (logger != null) logger.entering(getClass().getName(), "prepareImage");

        boolean retValue;
        
        retValue = super.prepareImage(image, width, height, observer);
        return retValue;
    }

    public boolean prepareImage(Image image, java.awt.image.ImageObserver observer) {
        if (logger != null) logger.entering(getClass().getName(), "prepareImage");

        boolean retValue;
        
        retValue = super.prepareImage(image, observer);
        return retValue;
    }

    public int checkImage(Image image, java.awt.image.ImageObserver observer) {
        if (logger != null) logger.entering(getClass().getName(), "checkImage");

        int retValue;
        
        retValue = super.checkImage(image, observer);
        return retValue;
    }

    public int checkImage(Image image, int width, int height, java.awt.image.ImageObserver observer) {
        if (logger != null) logger.entering(getClass().getName(), "checkImage");

        int retValue;
        
        retValue = super.checkImage(image, width, height, observer);
        return retValue;
    }

    public void setAlignmentY(float alignmentY) {
        if (logger != null) logger.entering(getClass().getName(), "setAlignmentY");

        super.setAlignmentY(alignmentY);
    }

    public void setAlignmentX(float alignmentX) {
        if (logger != null) logger.entering(getClass().getName(), "setAlignmentX");

        super.setAlignmentX(alignmentX);
    }

    public void removeAncestorListener(javax.swing.event.AncestorListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "removeAncestorListener");

        super.removeAncestorListener(listener);
    }

    public void addAncestorListener(javax.swing.event.AncestorListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "addAncestorListener");

        super.addAncestorListener(listener);
    }

    public void remove(MenuComponent popup) {
        if (logger != null) logger.entering(getClass().getName(), "remove");

        super.remove(popup);
    }

    public void setDropTarget(java.awt.dnd.DropTarget dt) {
        if (logger != null) logger.entering(getClass().getName(), "setDropTarget");

        super.setDropTarget(dt);
    }

    public void repaint(long tm, int x, int y, int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "repaint");

        super.repaint(tm, x, y, width, height);
    }

    public void removeHierarchyListener(java.awt.event.HierarchyListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeHierarchyListener");

        super.removeHierarchyListener(l);
    }

    public void addHierarchyListener(java.awt.event.HierarchyListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addHierarchyListener");

        super.addHierarchyListener(l);
    }

    public void removePropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "removePropertyChangeListener");

        super.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(String propertyName, java.beans.PropertyChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "addPropertyChangeListener");

        super.addPropertyChangeListener(propertyName, listener);
    }

    public void removeContainerListener(java.awt.event.ContainerListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeContainerListener");

        super.removeContainerListener(l);
    }

    public void addContainerListener(java.awt.event.ContainerListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addContainerListener");

        super.addContainerListener(l);
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        if (logger != null) logger.entering(getClass().getName(), "imageUpdate");

        boolean retValue;
        
        retValue = super.imageUpdate(img, infoflags, x, y, w, h);
        return retValue;
    }

    public java.util.EventListener[] getListeners(Class listenerType) {
        if (logger != null) logger.entering(getClass().getName(), "getListeners");

        java.util.EventListener[] retValue;
        
        retValue = super.getListeners(listenerType);
        return retValue;
    }

    public void setInputVerifier(InputVerifier inputVerifier) {
        if (logger != null) logger.entering(getClass().getName(), "setInputVerifier");

        super.setInputVerifier(inputVerifier);
    }

    public void list(java.io.PrintWriter out, int indent) {
        if (logger != null) logger.entering(getClass().getName(), "list");

        super.list(out, indent);
    }

    public void validate() {
        if (logger != null) logger.entering(getClass().getName(), "validate");

        super.validate();
    }

    public void updateUI() {
        if (logger != null) logger.entering(getClass().getName(), "updateUI");

        super.updateUI();
    }

    public void transferFocusUpCycle() {
        if (logger != null) logger.entering(getClass().getName(), "transferFocusUpCycle");

        super.transferFocusUpCycle();
    }

    public void transferFocusDownCycle() {
        if (logger != null) logger.entering(getClass().getName(), "transferFocusDownCycle");

        super.transferFocusDownCycle();
    }

    public void transferFocusBackward() {
        if (logger != null) logger.entering(getClass().getName(), "transferFocusBackward");

        super.transferFocusBackward();
    }

    public void transferFocus() {
        if (logger != null) logger.entering(getClass().getName(), "transferFocus");

        super.transferFocus();
    }

    public String toString() {
        if (logger != null) logger.entering(getClass().getName(), "toString");

        String retValue;
        
        retValue = super.toString();
        return retValue;
    }

    public Dimension size() {
        if (logger != null) logger.entering(getClass().getName(), "size");

        Dimension retValue;
        
        retValue = super.size();
        return retValue;
    }

    public void show() {
        if (logger != null) logger.entering(getClass().getName(), "show");

        super.show();
    }

    public void setSize(int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "setSize");

        super.setSize(width, height);
    }

    public void setLocation(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "setLocation");

        super.setLocation(x, y);
    }

    public void setBounds(int x, int y, int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "setBounds");

        super.setBounds(x, y, width, height);
    }

    public void revalidate() {
        if (logger != null) logger.entering(getClass().getName(), "revalidate");

        super.revalidate();
    }

    public void resize(int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "resize");

        super.resize(width, height);
    }

    public void reshape(int x, int y, int w, int h) {
        if (logger != null) logger.entering(getClass().getName(), "reshape");

        super.reshape(x, y, w, h);
    }

    public void resetKeyboardActions() {
        if (logger != null) logger.entering(getClass().getName(), "resetKeyboardActions");

        super.resetKeyboardActions();
    }

    public boolean requestFocusInWindow() {
        if (logger != null) logger.entering(getClass().getName(), "requestFocusInWindow");

        boolean retValue;
        
        retValue = super.requestFocusInWindow();
        return retValue;
    }

    public void requestFocus() {
        if (logger != null) logger.entering(getClass().getName(), "requestFocus");

        super.requestFocus();
    }

    public boolean requestDefaultFocus() {
        if (logger != null) logger.entering(getClass().getName(), "requestDefaultFocus");

        boolean retValue;
        
        retValue = super.requestDefaultFocus();
        return retValue;
    }

    public void repaint(int x, int y, int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "repaint");

        super.repaint(x, y, width, height);
    }

    public void repaint() {
        if (logger != null) logger.entering(getClass().getName(), "repaint");

        super.repaint();
    }

    public void removeNotify() {
        if (logger != null) logger.entering(getClass().getName(), "removeNotify");

        super.removeNotify();
    }

    public void removeAll() {
        if (logger != null) logger.entering(getClass().getName(), "removeAll");

        super.removeAll();
    }

    public Dimension preferredSize() {
        if (logger != null) logger.entering(getClass().getName(), "preferredSize");

        Dimension retValue;
        
        retValue = super.preferredSize();
        return retValue;
    }

    public void paintImmediately(int x, int y, int w, int h) {
        if (logger != null) logger.entering(getClass().getName(), "paintImmediately");

        super.paintImmediately(x, y, w, h);
    }

    public void nextFocus() {
        if (logger != null) logger.entering(getClass().getName(), "nextFocus");

        super.nextFocus();
    }

    public void move(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "move");

        super.move(x, y);
    }

    public boolean getIgnoreRepaint() {
        if (logger != null) logger.entering(getClass().getName(), "getIgnoreRepaint");

        boolean retValue;
        
        retValue = super.getIgnoreRepaint();
        return retValue;
    }

    public java.awt.event.HierarchyListener[] getHierarchyListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getHierarchyListeners");

        java.awt.event.HierarchyListener[] retValue;
        
        retValue = super.getHierarchyListeners();
        return retValue;
    }

    public java.awt.event.HierarchyBoundsListener[] getHierarchyBoundsListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getHierarchyBoundsListeners");

        java.awt.event.HierarchyBoundsListener[] retValue;
        
        retValue = super.getHierarchyBoundsListeners();
        return retValue;
    }

    public int getHeight() {
        if (logger != null) logger.entering(getClass().getName(), "getHeight");

        int retValue;
        
        retValue = super.getHeight();
        return retValue;
    }

    public GraphicsConfiguration getGraphicsConfiguration() {
        if (logger != null) logger.entering(getClass().getName(), "getGraphicsConfiguration");

        GraphicsConfiguration retValue;
        
        retValue = super.getGraphicsConfiguration();
        return retValue;
    }

    public Graphics getGraphics() {
        if (logger != null) logger.entering(getClass().getName(), "getGraphics");

        Graphics retValue;
        
        retValue = super.getGraphics();
        return retValue;
    }

    public Color getForeground() {
        if (logger != null) logger.entering(getClass().getName(), "getForeground");

        Color retValue;
        
        retValue = super.getForeground();
        return retValue;
    }

    public Font getFont() {
        if (logger != null) logger.entering(getClass().getName(), "getFont");

        Font retValue;
        
        retValue = super.getFont();
        return retValue;
    }

    public FocusTraversalPolicy getFocusTraversalPolicy() {
        if (logger != null) logger.entering(getClass().getName(), "getFocusTraversalPolicy");

        FocusTraversalPolicy retValue;
        
        retValue = super.getFocusTraversalPolicy();
        return retValue;
    }

    public boolean getFocusTraversalKeysEnabled() {
        if (logger != null) logger.entering(getClass().getName(), "getFocusTraversalKeysEnabled");

        boolean retValue;
        
        retValue = super.getFocusTraversalKeysEnabled();
        return retValue;
    }

    public java.awt.event.FocusListener[] getFocusListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getFocusListeners");

        java.awt.event.FocusListener[] retValue;
        
        retValue = super.getFocusListeners();
        return retValue;
    }

    public Container getFocusCycleRootAncestor() {
        if (logger != null) logger.entering(getClass().getName(), "getFocusCycleRootAncestor");

        Container retValue;
        
        retValue = super.getFocusCycleRootAncestor();
        return retValue;
    }

    public java.awt.dnd.DropTarget getDropTarget() {
        if (logger != null) logger.entering(getClass().getName(), "getDropTarget");

        java.awt.dnd.DropTarget retValue;
        
        retValue = super.getDropTarget();
        return retValue;
    }

    public int getDebugGraphicsOptions() {
        if (logger != null) logger.entering(getClass().getName(), "getDebugGraphicsOptions");

        int retValue;
        
        retValue = super.getDebugGraphicsOptions();
        return retValue;
    }

    public Cursor getCursor() {
        if (logger != null) logger.entering(getClass().getName(), "getCursor");

        Cursor retValue;
        
        retValue = super.getCursor();
        return retValue;
    }

    public java.awt.event.ContainerListener[] getContainerListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getContainerListeners");

        java.awt.event.ContainerListener[] retValue;
        
        retValue = super.getContainerListeners();
        return retValue;
    }

    public Component[] getComponents() {
        if (logger != null) logger.entering(getClass().getName(), "getComponents");

        Component[] retValue;
        
        retValue = super.getComponents();
        return retValue;
    }

    public ComponentOrientation getComponentOrientation() {
        if (logger != null) logger.entering(getClass().getName(), "getComponentOrientation");

        ComponentOrientation retValue;
        
        retValue = super.getComponentOrientation();
        return retValue;
    }

    public java.awt.event.ComponentListener[] getComponentListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getComponentListeners");

        java.awt.event.ComponentListener[] retValue;
        
        retValue = super.getComponentListeners();
        return retValue;
    }

    public int getComponentCount() {
        if (logger != null) logger.entering(getClass().getName(), "getComponentCount");

        int retValue;
        
        retValue = super.getComponentCount();
        return retValue;
    }

    public Component getComponentAt(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "getComponentAt");

        Component retValue;
        
        retValue = super.getComponentAt(x, y);
        return retValue;
    }

    public java.awt.image.ColorModel getColorModel() {
        if (logger != null) logger.entering(getClass().getName(), "getColorModel");

        java.awt.image.ColorModel retValue;
        
        retValue = super.getColorModel();
        return retValue;
    }

    public Rectangle getBounds() {
        if (logger != null) logger.entering(getClass().getName(), "getBounds");

        Rectangle retValue;
        
        retValue = super.getBounds();
        return retValue;
    }

    public javax.swing.border.Border getBorder() {
        if (logger != null) logger.entering(getClass().getName(), "getBorder");

        javax.swing.border.Border retValue;
        
        retValue = super.getBorder();
        return retValue;
    }

    public Color getBackground() {
        if (logger != null) logger.entering(getClass().getName(), "getBackground");

        Color retValue;
        
        retValue = super.getBackground();
        return retValue;
    }

    public boolean getAutoscrolls() {
        if (logger != null) logger.entering(getClass().getName(), "getAutoscrolls");

        boolean retValue;
        
        retValue = super.getAutoscrolls();
        return retValue;
    }

    public javax.swing.event.AncestorListener[] getAncestorListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getAncestorListeners");

        javax.swing.event.AncestorListener[] retValue;
        
        retValue = super.getAncestorListeners();
        return retValue;
    }

    public float getAlignmentY() {
        if (logger != null) logger.entering(getClass().getName(), "getAlignmentY");

        float retValue;
        
        retValue = super.getAlignmentY();
        return retValue;
    }

    public float getAlignmentX() {
        if (logger != null) logger.entering(getClass().getName(), "getAlignmentX");

        float retValue;
        
        retValue = super.getAlignmentX();
        return retValue;
    }

    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (logger != null) logger.entering(getClass().getName(), "getAccessibleContext");

        javax.accessibility.AccessibleContext retValue;
        
        retValue = super.getAccessibleContext();
        return retValue;
    }

    public void addNotify() {
        if (logger != null) logger.entering(getClass().getName(), "addNotify");

        super.addNotify();
    }

    public Rectangle bounds() {
        if (logger != null) logger.entering(getClass().getName(), "bounds");

        Rectangle retValue;
        
        retValue = super.bounds();
        return retValue;
    }

    public boolean contains(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "contains");

        boolean retValue;
        
        retValue = super.contains(x, y);
        return retValue;
    }

    public int countComponents() {
        if (logger != null) logger.entering(getClass().getName(), "countComponents");

        int retValue;
        
        retValue = super.countComponents();
        return retValue;
    }

    public Image createImage(int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "createImage");

        Image retValue;
        
        retValue = super.createImage(width, height);
        return retValue;
    }

    public JToolTip createToolTip() {
        if (logger != null) logger.entering(getClass().getName(), "createToolTip");

        JToolTip retValue;
        
        retValue = super.createToolTip();
        return retValue;
    }

    public java.awt.image.VolatileImage createVolatileImage(int width, int height) {
        if (logger != null) logger.entering(getClass().getName(), "createVolatileImage");

        java.awt.image.VolatileImage retValue;
        
        retValue = super.createVolatileImage(width, height);
        return retValue;
    }

    public void disable() {
        if (logger != null) logger.entering(getClass().getName(), "disable");

        super.disable();
    }

    public void doLayout() {
        if (logger != null) logger.entering(getClass().getName(), "doLayout");

        super.doLayout();
    }

    public void enable() {
        if (logger != null) logger.entering(getClass().getName(), "enable");

        super.enable();
    }

    public Component findComponentAt(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "findComponentAt");

        Component retValue;
        
        retValue = super.findComponentAt(x, y);
        return retValue;
    }

    public java.awt.im.InputContext getInputContext() {
        if (logger != null) logger.entering(getClass().getName(), "getInputContext");

        java.awt.im.InputContext retValue;
        
        retValue = super.getInputContext();
        return retValue;
    }

    public java.awt.event.InputMethodListener[] getInputMethodListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getInputMethodListeners");

        java.awt.event.InputMethodListener[] retValue;
        
        retValue = super.getInputMethodListeners();
        return retValue;
    }

    public java.awt.im.InputMethodRequests getInputMethodRequests() {
        if (logger != null) logger.entering(getClass().getName(), "getInputMethodRequests");

        java.awt.im.InputMethodRequests retValue;
        
        retValue = super.getInputMethodRequests();
        return retValue;
    }

    public InputVerifier getInputVerifier() {
        if (logger != null) logger.entering(getClass().getName(), "getInputVerifier");

        InputVerifier retValue;
        
        retValue = super.getInputVerifier();
        return retValue;
    }

    public Insets getInsets() {
        if (logger != null) logger.entering(getClass().getName(), "getInsets");

        Insets retValue;
        
        retValue = super.getInsets();
        return retValue;
    }

    public java.awt.event.KeyListener[] getKeyListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getKeyListeners");

        java.awt.event.KeyListener[] retValue;
        
        retValue = super.getKeyListeners();
        return retValue;
    }

    public LayoutManager getLayout() {
        if (logger != null) logger.entering(getClass().getName(), "getLayout");

        LayoutManager retValue;
        
        retValue = super.getLayout();
        return retValue;
    }

    public java.util.Locale getLocale() {
        if (logger != null) logger.entering(getClass().getName(), "getLocale");

        java.util.Locale retValue;
        
        retValue = super.getLocale();
        return retValue;
    }

    public Point getLocation() {
        if (logger != null) logger.entering(getClass().getName(), "getLocation");

        Point retValue;
        
        retValue = super.getLocation();
        return retValue;
    }

    public Point getLocationOnScreen() {
        if (logger != null) logger.entering(getClass().getName(), "getLocationOnScreen");

        Point retValue;
        
        retValue = super.getLocationOnScreen();
        return retValue;
    }

    public Dimension getMaximumSize() {
        if (logger != null) logger.entering(getClass().getName(), "getMaximumSize");

        Dimension retValue;
        
        retValue = super.getMaximumSize();
        return retValue;
    }

    public Dimension getMinimumSize() {
        if (logger != null) logger.entering(getClass().getName(), "getMinimumSize");

        Dimension retValue;
        
        retValue = super.getMinimumSize();
        return retValue;
    }

    public java.awt.event.MouseListener[] getMouseListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getMouseListeners");

        java.awt.event.MouseListener[] retValue;
        
        retValue = super.getMouseListeners();
        return retValue;
    }

    public java.awt.event.MouseMotionListener[] getMouseMotionListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getMouseMotionListeners");

        java.awt.event.MouseMotionListener[] retValue;
        
        retValue = super.getMouseMotionListeners();
        return retValue;
    }

    public java.awt.event.MouseWheelListener[] getMouseWheelListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getMouseWheelListeners");

        java.awt.event.MouseWheelListener[] retValue;
        
        retValue = super.getMouseWheelListeners();
        return retValue;
    }

    public String getName() {
        if (logger != null) logger.entering(getClass().getName(), "getName");

        String retValue;
        
        retValue = super.getName();
        return retValue;
    }

    public Component getNextFocusableComponent() {
        if (logger != null) logger.entering(getClass().getName(), "getNextFocusableComponent");

        Component retValue;
        
        retValue = super.getNextFocusableComponent();
        return retValue;
    }

    public Container getParent() {
        if (logger != null) logger.entering(getClass().getName(), "getParent");

        Container retValue;
        
        retValue = super.getParent();
        return retValue;
    }

    public java.awt.peer.ComponentPeer getPeer() {
        if (logger != null) logger.entering(getClass().getName(), "getPeer");

        java.awt.peer.ComponentPeer retValue;
        
        retValue = super.getPeer();
        return retValue;
    }

    public Dimension getPreferredSize() {
        if (logger != null) logger.entering(getClass().getName(), "getPreferredSize");

        Dimension retValue;
        
        retValue = super.getPreferredSize();
        return retValue;
    }

    public java.beans.PropertyChangeListener[] getPropertyChangeListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getPropertyChangeListeners");

        java.beans.PropertyChangeListener[] retValue;
        
        retValue = super.getPropertyChangeListeners();
        return retValue;
    }

    public KeyStroke[] getRegisteredKeyStrokes() {
        if (logger != null) logger.entering(getClass().getName(), "getRegisteredKeyStrokes");

        KeyStroke[] retValue;
        
        retValue = super.getRegisteredKeyStrokes();
        return retValue;
    }

    public JRootPane getRootPane() {
        if (logger != null) logger.entering(getClass().getName(), "getRootPane");

        JRootPane retValue;
        
        retValue = super.getRootPane();
        return retValue;
    }

    public Dimension getSize() {
        if (logger != null) logger.entering(getClass().getName(), "getSize");

        Dimension retValue;
        
        retValue = super.getSize();
        return retValue;
    }

    public String getToolTipText() {
        if (logger != null) logger.entering(getClass().getName(), "getToolTipText");

        String retValue;
        
        retValue = super.getToolTipText();
        return retValue;
    }

    public Toolkit getToolkit() {
        if (logger != null) logger.entering(getClass().getName(), "getToolkit");

        Toolkit retValue;
        
        retValue = super.getToolkit();
        return retValue;
    }

    public Container getTopLevelAncestor() {
        if (logger != null) logger.entering(getClass().getName(), "getTopLevelAncestor");

        Container retValue;
        
        retValue = super.getTopLevelAncestor();
        return retValue;
    }

    public TransferHandler getTransferHandler() {
        if (logger != null) logger.entering(getClass().getName(), "getTransferHandler");

        TransferHandler retValue;
        
        retValue = super.getTransferHandler();
        return retValue;
    }

    public String getUIClassID() {
        if (logger != null) logger.entering(getClass().getName(), "getUIClassID");

        String retValue;
        
        retValue = super.getUIClassID();
        return retValue;
    }

    public boolean getVerifyInputWhenFocusTarget() {
        if (logger != null) logger.entering(getClass().getName(), "getVerifyInputWhenFocusTarget");

        boolean retValue;
        
        retValue = super.getVerifyInputWhenFocusTarget();
        return retValue;
    }

    public java.beans.VetoableChangeListener[] getVetoableChangeListeners() {
        if (logger != null) logger.entering(getClass().getName(), "getVetoableChangeListeners");

        java.beans.VetoableChangeListener[] retValue;
        
        retValue = super.getVetoableChangeListeners();
        return retValue;
    }

    public Rectangle getVisibleRect() {
        if (logger != null) logger.entering(getClass().getName(), "getVisibleRect");

        Rectangle retValue;
        
        retValue = super.getVisibleRect();
        return retValue;
    }

    public int getWidth() {
        if (logger != null) logger.entering(getClass().getName(), "getWidth");

        int retValue;
        
        retValue = super.getWidth();
        return retValue;
    }

    public int getX() {
        if (logger != null) logger.entering(getClass().getName(), "getX");

        int retValue;
        
        retValue = super.getX();
        return retValue;
    }

    public int getY() {
        if (logger != null) logger.entering(getClass().getName(), "getY");

        int retValue;
        
        retValue = super.getY();
        return retValue;
    }

    public void grabFocus() {
        if (logger != null) logger.entering(getClass().getName(), "grabFocus");

        super.grabFocus();
    }

    public boolean hasFocus() {
        if (logger != null) logger.entering(getClass().getName(), "hasFocus");

        boolean retValue;
        
        retValue = super.hasFocus();
        return retValue;
    }

    public int hashCode() {
        if (logger != null) logger.entering(getClass().getName(), "hashCode");

        int retValue;
        
        retValue = super.hashCode();
        return retValue;
    }

    public void hide() {
        if (logger != null) logger.entering(getClass().getName(), "hide");

        super.hide();
    }

    public Insets insets() {
        if (logger != null) logger.entering(getClass().getName(), "insets");

        Insets retValue;
        
        retValue = super.insets();
        return retValue;
    }

    public boolean inside(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "inside");

        boolean retValue;
        
        retValue = super.inside(x, y);
        return retValue;
    }

    public void invalidate() {
        if (logger != null) logger.entering(getClass().getName(), "invalidate");

        super.invalidate();
    }

    public boolean isBackgroundSet() {
        if (logger != null) logger.entering(getClass().getName(), "isBackgroundSet");

        boolean retValue;
        
        retValue = super.isBackgroundSet();
        return retValue;
    }

    public boolean isCursorSet() {
        if (logger != null) logger.entering(getClass().getName(), "isCursorSet");

        boolean retValue;
        
        retValue = super.isCursorSet();
        return retValue;
    }

    public boolean isDisplayable() {
        if (logger != null) logger.entering(getClass().getName(), "isDisplayable");

        boolean retValue;
        
        retValue = super.isDisplayable();
        return retValue;
    }

    public boolean isDoubleBuffered() {
        if (logger != null) logger.entering(getClass().getName(), "isDoubleBuffered");

        boolean retValue;
        
        retValue = super.isDoubleBuffered();
        return retValue;
    }

    public boolean isEnabled() {
        if (logger != null) logger.entering(getClass().getName(), "isEnabled");

        boolean retValue;
        
        retValue = super.isEnabled();
        return retValue;
    }

    public boolean isFocusCycleRoot() {
        if (logger != null) logger.entering(getClass().getName(), "isFocusCycleRoot");

        boolean retValue;
        
        retValue = super.isFocusCycleRoot();
        return retValue;
    }

    public boolean isFocusOwner() {
        if (logger != null) logger.entering(getClass().getName(), "isFocusOwner");

        boolean retValue;
        
        retValue = super.isFocusOwner();
        return retValue;
    }

    public boolean isFocusTraversable() {
        if (logger != null) logger.entering(getClass().getName(), "isFocusTraversable");

        boolean retValue;
        
        retValue = super.isFocusTraversable();
        return retValue;
    }

    public boolean isFocusTraversalPolicySet() {
        if (logger != null) logger.entering(getClass().getName(), "isFocusTraversalPolicySet");

        boolean retValue;
        
        retValue = super.isFocusTraversalPolicySet();
        return retValue;
    }

    public boolean isFocusable() {
        if (logger != null) logger.entering(getClass().getName(), "isFocusable");

        boolean retValue;
        
        retValue = super.isFocusable();
        return retValue;
    }

    public boolean isFontSet() {
        if (logger != null) logger.entering(getClass().getName(), "isFontSet");

        boolean retValue;
        
        retValue = super.isFontSet();
        return retValue;
    }

    public boolean isForegroundSet() {
        if (logger != null) logger.entering(getClass().getName(), "isForegroundSet");

        boolean retValue;
        
        retValue = super.isForegroundSet();
        return retValue;
    }

    public boolean isLightweight() {
        if (logger != null) logger.entering(getClass().getName(), "isLightweight");

        boolean retValue;
        
        retValue = super.isLightweight();
        return retValue;
    }

    public boolean isManagingFocus() {
        if (logger != null) logger.entering(getClass().getName(), "isManagingFocus");

        boolean retValue;
        
        retValue = super.isManagingFocus();
        return retValue;
    }

    public boolean isMaximumSizeSet() {
        if (logger != null) logger.entering(getClass().getName(), "isMaximumSizeSet");

        boolean retValue;
        
        retValue = super.isMaximumSizeSet();
        return retValue;
    }

    public boolean isMinimumSizeSet() {
        if (logger != null) logger.entering(getClass().getName(), "isMinimumSizeSet");

        boolean retValue;
        
        retValue = super.isMinimumSizeSet();
        return retValue;
    }

    public boolean isOpaque() {
        if (logger != null) logger.entering(getClass().getName(), "isOpaque");

        boolean retValue;
        
        retValue = super.isOpaque();
        return retValue;
    }

    public boolean isOptimizedDrawingEnabled() {
        if (logger != null) logger.entering(getClass().getName(), "isOptimizedDrawingEnabled");

        boolean retValue;
        
        retValue = super.isOptimizedDrawingEnabled();
        return retValue;
    }

    public boolean isPaintingTile() {
        if (logger != null) logger.entering(getClass().getName(), "isPaintingTile");

        boolean retValue;
        
        retValue = super.isPaintingTile();
        return retValue;
    }

    public boolean isPreferredSizeSet() {
        if (logger != null) logger.entering(getClass().getName(), "isPreferredSizeSet");

        boolean retValue;
        
        retValue = super.isPreferredSizeSet();
        return retValue;
    }

    public boolean isRequestFocusEnabled() {
        if (logger != null) logger.entering(getClass().getName(), "isRequestFocusEnabled");

        boolean retValue;
        
        retValue = super.isRequestFocusEnabled();
        return retValue;
    }

    public boolean isShowing() {
        if (logger != null) logger.entering(getClass().getName(), "isShowing");

        boolean retValue;
        
        retValue = super.isShowing();
        return retValue;
    }

    public boolean isValid() {
        if (logger != null) logger.entering(getClass().getName(), "isValid");

        boolean retValue;
        
        retValue = super.isValid();
        return retValue;
    }

    public boolean isValidateRoot() {
        if (logger != null) logger.entering(getClass().getName(), "isValidateRoot");

        boolean retValue;
        
        retValue = super.isValidateRoot();
        return retValue;
    }

    public boolean isVisible() {
        if (logger != null) logger.entering(getClass().getName(), "isVisible");

        boolean retValue;
        
        retValue = super.isVisible();
        return retValue;
    }

    public void layout() {
        if (logger != null) logger.entering(getClass().getName(), "layout");

        super.layout();
    }

    public void list() {
        if (logger != null) logger.entering(getClass().getName(), "list");

        super.list();
    }

    public Component locate(int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "locate");

        Component retValue;
        
        retValue = super.locate(x, y);
        return retValue;
    }

    public Point location() {
        if (logger != null) logger.entering(getClass().getName(), "location");

        Point retValue;
        
        retValue = super.location();
        return retValue;
    }

    public Dimension minimumSize() {
        if (logger != null) logger.entering(getClass().getName(), "minimumSize");

        Dimension retValue;
        
        retValue = super.minimumSize();
        return retValue;
    }

    public void list(java.io.PrintWriter out) {
        if (logger != null) logger.entering(getClass().getName(), "list");

        super.list(out);
    }

    public void setNextFocusableComponent(Component aComponent) {
        if (logger != null) logger.entering(getClass().getName(), "setNextFocusableComponent");

        super.setNextFocusableComponent(aComponent);
    }

    public void setCursor(Cursor cursor) {
        if (logger != null) logger.entering(getClass().getName(), "setCursor");

        super.setCursor(cursor);
    }

    public void remove(Component comp) {
        if (logger != null) logger.entering(getClass().getName(), "remove");

        super.remove(comp);
    }

    public Component add(Component comp) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        Component retValue;
        
        retValue = super.add(comp);
        return retValue;
    }

    public boolean isAncestorOf(Component c) {
        if (logger != null) logger.entering(getClass().getName(), "isAncestorOf");

        boolean retValue;
        
        retValue = super.isAncestorOf(c);
        return retValue;
    }

    public Component add(String name, Component comp) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        Component retValue;
        
        retValue = super.add(name, comp);
        return retValue;
    }

    public void setFont(Font font) {
        if (logger != null) logger.entering(getClass().getName(), "setFont");

        super.setFont(font);
    }

    public FontMetrics getFontMetrics(Font font) {
        if (logger != null) logger.entering(getClass().getName(), "getFontMetrics");

        FontMetrics retValue;
        
        retValue = super.getFontMetrics(font);
        return retValue;
    }

    public boolean isFocusCycleRoot(Container container) {
        if (logger != null) logger.entering(getClass().getName(), "isFocusCycleRoot");

        boolean retValue;
        
        retValue = super.isFocusCycleRoot(container);
        return retValue;
    }

    public void setFocusTraversalKeys(int id, java.util.Set keystrokes) {
        if (logger != null) logger.entering(getClass().getName(), "setFocusTraversalKeys");

        super.setFocusTraversalKeys(id, keystrokes);
    }

    public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
        if (logger != null) logger.entering(getClass().getName(), "setFocusTraversalPolicy");

        super.setFocusTraversalPolicy(policy);
    }

    public void update(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "update");

        super.update(g);
    }

    public void removeFocusListener(java.awt.event.FocusListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeFocusListener");

        super.removeFocusListener(l);
    }

    public void printComponents(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "printComponents");

        super.printComponents(g);
    }

    public void printAll(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "printAll");

        super.printAll(g);
    }

    public void print(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "print");

        super.print(g);
    }

    public void paintComponents(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "paintComponents");

        super.paintComponents(g);
    }

    public void paintAll(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "paintAll");

        super.paintAll(g);
    }

    public void paint(Graphics g) {
        if (logger != null) logger.entering(getClass().getName(), "paint");

        super.paint(g);
    }

    public void addFocusListener(java.awt.event.FocusListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addFocusListener");

        super.addFocusListener(l);
    }

    public void setToolTipText(String text) {
        if (logger != null) logger.entering(getClass().getName(), "setToolTipText");

        super.setToolTipText(text);
    }

    public void setName(String name) {
        if (logger != null) logger.entering(getClass().getName(), "setName");

        super.setName(name);
    }

    public java.beans.PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (logger != null) logger.entering(getClass().getName(), "getPropertyChangeListeners");

        java.beans.PropertyChangeListener[] retValue;
        
        retValue = super.getPropertyChangeListeners(propertyName);
        return retValue;
    }

    public void setDebugGraphicsOptions(int debugOptions) {
        if (logger != null) logger.entering(getClass().getName(), "setDebugGraphicsOptions");

        super.setDebugGraphicsOptions(debugOptions);
    }

    public void remove(int index) {
        if (logger != null) logger.entering(getClass().getName(), "remove");

        super.remove(index);
    }

    public java.util.Set getFocusTraversalKeys(int id) {
        if (logger != null) logger.entering(getClass().getName(), "getFocusTraversalKeys");

        java.util.Set retValue;
        
        retValue = super.getFocusTraversalKeys(id);
        return retValue;
    }

    public Component getComponent(int n) {
        if (logger != null) logger.entering(getClass().getName(), "getComponent");

        Component retValue;
        
        retValue = super.getComponent(n);
        return retValue;
    }

    public boolean areFocusTraversalKeysSet(int id) {
        if (logger != null) logger.entering(getClass().getName(), "areFocusTraversalKeysSet");

        boolean retValue;
        
        retValue = super.areFocusTraversalKeysSet(id);
        return retValue;
    }

    public Insets getInsets(Insets insets) {
        if (logger != null) logger.entering(getClass().getName(), "getInsets");

        Insets retValue;
        
        retValue = super.getInsets(insets);
        return retValue;
    }

    public void removeInputMethodListener(java.awt.event.InputMethodListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeInputMethodListener");

        super.removeInputMethodListener(l);
    }

    public void addInputMethodListener(java.awt.event.InputMethodListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addInputMethodListener");

        super.addInputMethodListener(l);
    }

    public void setBorder(javax.swing.border.Border border) {
        if (logger != null) logger.entering(getClass().getName(), "setBorder");

        super.setBorder(border);
    }

    public void add(Component comp, Object constraints) {
        if (logger != null) logger.entering(getClass().getName(), "add");

        super.add(comp, constraints);
    }

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void removeVetoableChangeListener(java.beans.VetoableChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "removeVetoableChangeListener");

        super.removeVetoableChangeListener(listener);
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "removePropertyChangeListener");

        super.removePropertyChangeListener(listener);
    }

    public void addVetoableChangeListener(java.beans.VetoableChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "addVetoableChangeListener");

        super.addVetoableChangeListener(listener);
    }

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        if (logger != null) logger.entering(getClass().getName(), "addPropertyChangeListener");

        super.addPropertyChangeListener(listener);
    }

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    public Image createImage(java.awt.image.ImageProducer producer) {
        if (logger != null) logger.entering(getClass().getName(), "createImage");

        Image retValue;
        
        retValue = super.createImage(producer);
        return retValue;
    }

    public void removeHierarchyBoundsListener(java.awt.event.HierarchyBoundsListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeHierarchyBoundsListener");

        super.removeHierarchyBoundsListener(l);
    }

    public void addHierarchyBoundsListener(java.awt.event.HierarchyBoundsListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addHierarchyBoundsListener");

        super.addHierarchyBoundsListener(l);
    }

    public void removeComponentListener(java.awt.event.ComponentListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeComponentListener");

        super.removeComponentListener(l);
    }

    public void addComponentListener(java.awt.event.ComponentListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addComponentListener");

        super.addComponentListener(l);
    }

    public void removeKeyListener(java.awt.event.KeyListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeKeyListener");

        super.removeKeyListener(l);
    }

    public void addKeyListener(java.awt.event.KeyListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addKeyListener");

        super.addKeyListener(l);
    }

    public void removeMouseMotionListener(java.awt.event.MouseMotionListener l) {
        if (logger != null) logger.entering(getClass().getName(), "removeMouseMotionListener");

        super.removeMouseMotionListener(l);
    }

    public void addMouseMotionListener(java.awt.event.MouseMotionListener l) {
        if (logger != null) logger.entering(getClass().getName(), "addMouseMotionListener");

        super.addMouseMotionListener(l);
    }

    public void setForeground(Color fg) {
        if (logger != null) logger.entering(getClass().getName(), "setForeground");

        super.setForeground(fg);
    }

    public void setBackground(Color bg) {
        if (logger != null) logger.entering(getClass().getName(), "setBackground");

        super.setBackground(bg);
    }

    public Point getToolTipLocation(java.awt.event.MouseEvent event) {
        if (logger != null) logger.entering(getClass().getName(), "getToolTipLocation");

        Point retValue;
        
        retValue = super.getToolTipLocation(event);
        return retValue;
    }

    public String getToolTipText(java.awt.event.MouseEvent event) {
        if (logger != null) logger.entering(getClass().getName(), "getToolTipText");

        String retValue;
        
        retValue = super.getToolTipText(event);
        return retValue;
    }

    public boolean mouseUp(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseUp");

        boolean retValue;
        
        retValue = super.mouseUp(evt, x, y);
        return retValue;
    }

    public boolean keyDown(Event evt, int key) {
        if (logger != null) logger.entering(getClass().getName(), "keyDown");

        boolean retValue;
        
        retValue = super.keyDown(evt, key);
        return retValue;
    }

    public boolean keyUp(Event evt, int key) {
        if (logger != null) logger.entering(getClass().getName(), "keyUp");

        boolean retValue;
        
        retValue = super.keyUp(evt, key);
        return retValue;
    }

    public boolean mouseDown(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseDown");

        boolean retValue;
        
        retValue = super.mouseDown(evt, x, y);
        return retValue;
    }

    public boolean mouseDrag(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseDrag");

        boolean retValue;
        
        retValue = super.mouseDrag(evt, x, y);
        return retValue;
    }

    public boolean mouseEnter(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseEnter");

        boolean retValue;
        
        retValue = super.mouseEnter(evt, x, y);
        return retValue;
    }

    public boolean mouseExit(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseExit");

        boolean retValue;
        
        retValue = super.mouseExit(evt, x, y);
        return retValue;
    }

    public boolean mouseMove(Event evt, int x, int y) {
        if (logger != null) logger.entering(getClass().getName(), "mouseMove");

        boolean retValue;
        
        retValue = super.mouseMove(evt, x, y);
        return retValue;
    }

    public void setComponentOrientation(ComponentOrientation o) {
        if (logger != null) logger.entering(getClass().getName(), "setComponentOrientation");

        super.setComponentOrientation(o);
    }

    public void applyComponentOrientation(ComponentOrientation o) {
        if (logger != null) logger.entering(getClass().getName(), "applyComponentOrientation");

        super.applyComponentOrientation(o);
    }

    public void setSize(Dimension d) {
        if (logger != null) logger.entering(getClass().getName(), "setSize");

        super.setSize(d);
    }

    public void setPreferredSize(Dimension preferredSize) {
        if (logger != null) logger.entering(getClass().getName(), "setPreferredSize");

        super.setPreferredSize(preferredSize);
    }

    public void setMinimumSize(Dimension minimumSize) {
        if (logger != null) logger.entering(getClass().getName(), "setMinimumSize");

        super.setMinimumSize(minimumSize);
    }

    public void setMaximumSize(Dimension maximumSize) {
        if (logger != null) logger.entering(getClass().getName(), "setMaximumSize");

        super.setMaximumSize(maximumSize);
    }

    public void resize(Dimension d) {
        if (logger != null) logger.entering(getClass().getName(), "resize");

        super.resize(d);
    }

    public Dimension getSize(Dimension rv) {
        if (logger != null) logger.entering(getClass().getName(), "getSize");

        Dimension retValue;
        
        retValue = super.getSize(rv);
        return retValue;
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (logger != null) logger.entering(getClass().getName(), "firePropertyChange");

        super.firePropertyChange(propertyName, oldValue, newValue);
    }
    
}

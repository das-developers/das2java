/*
 * FavoritesSelector.java
 *
 * Created on March 18, 2004, 5:33 PM
 */

package edu.uiowa.physics.pw.das.components;

import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import java.awt.event.*;
import java.text.*;

/**
 *
 * @author  Jeremy
 */
public class FavoritesSelector {
    
    java.util.List favoritesList;
    String favoritesType;
    FavoritesListener listener=null;
    ActionListener actionListener;
    JPopupMenu popupMenu;
    boolean nextSelectionDeletes= false;
    
    /** Creates a new instance of FavoritesSelector */
    private FavoritesSelector( String _favoritesType) {
        favoritesList= new ArrayList();
        this.favoritesType= _favoritesType;
        this.readFromPersistentPrefs();
    }
    
    public interface FavoritesListener extends EventListener {
        void itemSelected( Object o );
        Object addFavoriteSelected();
    }
    
    public void addFavoritesListener( FavoritesListener _listener ) {
        if ( this.listener!=null ) {
            throw new IllegalArgumentException( "only one listener supported" );
        }
        this.listener= _listener;
    }
    
    public ActionListener getActionListener() {
        if ( this.actionListener==null ) {
            this.actionListener= new ActionListener() {
                public void actionPerformed( ActionEvent ev ) {
                    try {
                        String cmd= ev.getActionCommand();
                        System.out.println(cmd);
                        if ( cmd.equals("delete") ) {
                            FavoritesSelector.this.popupMenu.setVisible(true);
                            FavoritesSelector.this.nextSelectionDeletes= true;
                        } else if ( cmd.equals("add") ) {                            
                            if ( listener!=null ) {
                                Object o= FavoritesSelector.this.listener.addFavoriteSelected();
                                if ( o!=null ) {
                                    addFavorite(o);
                                }
                            }
                        } else {
                            if ( FavoritesSelector.this.nextSelectionDeletes ) {
                                removeFavorite( favoritesList.get( Integer.parseInt(cmd) ) );
                                FavoritesSelector.this.nextSelectionDeletes= false;
                            } else {
                                FavoritesSelector.this.listener.itemSelected( favoritesList.get( Integer.parseInt(cmd) ) );
                            }
                        }
                    } catch ( NumberFormatException e ) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        return this.actionListener;
    }
    
    private void resetPopupMenu() {
        JPopupMenu pm= getMenu();
        
        JMenuItem item;
        item= popupMenu.add("Add to favorites");
        item.setActionCommand("add");
        item.addActionListener(getActionListener());
        for ( int i= 0; i<favoritesList.size(); i++ ) {
            item= popupMenu.add( favoritesList.get(i).toString() );
            item.setActionCommand(""+i);
            item.addActionListener(getActionListener());
        }
        item= popupMenu.add("Add to favorites");
        item.setActionCommand("delete");
        item.addActionListener(getActionListener());
    }
    
    public JPopupMenu getMenu() {
        if ( popupMenu==null ) {
            this.popupMenu= new JPopupMenu();
            resetPopupMenu();
        }
        return popupMenu;
    }
    
    private void resetMenuActionCommands() {
        MenuElement[] items= popupMenu.getSubElements();
        for ( int i=1; i<items.length-2; i++ ) {
            ((JMenuItem)items[i]).setActionCommand(""+(i-1));
        }
    }
    
    public void addFavorite( Object o ) {
        String s= o.toString();
        if ( s.indexOf(";;")!=-1 ) {
            throw new IllegalArgumentException("o.toString() contains ;;");
        }
        favoritesList.add(o);
        JMenuItem item= new JMenuItem( o.toString() );
        item.addActionListener(getActionListener());
        if ( popupMenu!=null ) {
            popupMenu.add( item, favoritesList.size()+1 );
            resetMenuActionCommands();
        }
        writeToPersistentPrefs();
        
    }
    
    public void removeFavorite( Object o ) {
        int index= favoritesList.indexOf(o);
        favoritesList.remove(index);
        if ( popupMenu!=null ) {
            popupMenu.remove(index+1);
            resetMenuActionCommands();
        }
        writeToPersistentPrefs();
    }
    
    public void removeAll() {
        Object[] objs = new Object[ favoritesList.size() ];
        for ( int i=0; i<favoritesList.size(); i++ ) {
            objs[i]= favoritesList.get(i);
        }
        for ( int i=0; i<objs.length; i++ ) {
            removeFavorite(objs[i]);
        }
    }
    
    private void readFromPersistentPrefs() {
        Preferences prefs= Preferences.userNodeForPackage( FavoritesSelector.class );
        String listString= prefs.get( this.favoritesType,"" );
        String[] itemsString= listString.split(";;");
        favoritesList.clear();
        for ( int i=0; i<itemsString.length; i++ ) {
            favoritesList.add(itemsString[i]);
        }
    }
    
    private void writeToPersistentPrefs() {
        Preferences prefs= Preferences.userNodeForPackage( FavoritesSelector.class );
        StringBuffer listString= new StringBuffer();
        if ( favoritesList.size()>0 ) {
            listString.append(favoritesList.get(0).toString());
        }
        for ( int i=1; i<favoritesList.size(); i++ ) {
            listString.append( ";;"+favoritesList.get(i) );
        }
        prefs.put( this.favoritesType, listString.toString() );
        try {
            prefs.flush();
        } catch ( BackingStoreException e ) {
            e.printStackTrace();
        }
    }
    
    public int size() {
        return favoritesList.size();
    }
    
    public static FavoritesSelector getInstance( String _favoritesType ) {
        FavoritesSelector fs= new FavoritesSelector( _favoritesType );
        fs.removeAll();
        fs.addFavorite( "Strawberry" );
        fs.addFavorite( "Bananna" );
        fs.addFavorite( "Kiwi" );
        return fs;
    }
}

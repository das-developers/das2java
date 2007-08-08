/*
 * PersistentStateSupport.java
 *
 * Created on April 20, 2006, 1:23 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.dasml.SerializeUtil;
import edu.uiowa.physics.pw.das.dasml.DOMBuilder;
import edu.uiowa.physics.pw.das.graph.DasCanvas;
import edu.uiowa.physics.pw.das.util.fileSystem.Glob;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Jeremy
 */
public class PersistentStateSupport {
    
    String ext;
    File currentFile;
    JMenu openRecentMenu;
    SerializationStrategy strategy;
    Component component;
    
    private JMenuItem saveMenuItem;
    private JLabel currentFileLabel;
    private List recentFiles;
    
    public interface SerializationStrategy {
        // give me a document to serialize
        public Element serialize( Document document, DasProgressMonitor monitor ) throws IOException;
        
        // here's a document you gave me
        public void deserialize( Document doc, DasProgressMonitor monitor );
    }
    
    private static SerializationStrategy getCanvasStrategy( final DasCanvas canvas ) {
        return new SerializationStrategy() {
            public Element serialize(Document document, DasProgressMonitor monitor) {
                DOMBuilder builder= new DOMBuilder( canvas );
                Element element= builder.serialize( document, DasProgressPanel.createFramed("Serializing Canvas") );
                return element;
            }
            
            public void deserialize(Document document, DasProgressMonitor monitor) {
                Element element= document.getDocumentElement();
                SerializeUtil.processElement(element,canvas );
            }
        };
    }
    
    
    /**
     *  Provides a means for saving the application persistently, undo/redo support (TODO).
     *  canvas is the canvas to be serialized, extension identifies the application.  Note that
     *  internal changes to das may break saved files.
     */
    public PersistentStateSupport( DasCanvas canvas, String extension ) {
        this( canvas, getCanvasStrategy( canvas ), extension );
        
    }
    
    private void refreshRecentFilesMenu() {
        if ( openRecentMenu!=null ) {
            openRecentMenu.removeAll();
            for ( int i=0; i<recentFiles.size(); i++ ) {
                final File f= (File) recentFiles.get(i);
                Action a= new AbstractAction( String.valueOf(f) ) {
                    public void actionPerformed( ActionEvent e ) {
                        open(f);
                    }
                };
                openRecentMenu.add( a );
            }
        }
    }
    
    private void setRecentFiles( String code ) {
        recentFiles= new ArrayList();
        if ( code.equals("") ) return;
        String[] ss= code.split("::");
        for ( int i=0; i<ss.length; i++ ) {
            File f= new File( ss[i] );
            if ( !recentFiles.contains(f) ) {
                recentFiles.add( f );
            }
        }
        refreshRecentFilesMenu();
    }
    
    private String getRencentFilesString() {
        if (recentFiles.size()==0 ) {
            return "";
        } else {
            String result= String.valueOf( recentFiles.get(0) );
            for ( int i=1; i<recentFiles.size(); i++ ) {
                result+= "::"+String.valueOf(recentFiles.get(i));
            }
            return result;
        }
    }
    
    public PersistentStateSupport( Component parent, SerializationStrategy strategy, String extension ) {
        this.strategy= strategy;
        this.ext= "."+extension;
        Preferences prefs= Preferences.userNodeForPackage(PersistentStateSupport.class);
        String currentFileString= prefs.get( "PersistentStateSupport"+ext, "" );
        if ( !currentFileString.equals("") ) currentFile= new File( currentFileString );
        String recentFileString= prefs.get( "PersistentStateSupport"+ext+"_recent", "" );
        setRecentFiles( recentFileString );
    }
    
    private FileFilter simpleFilter( final String glob ) {
        final Pattern pattern= Glob.getPattern(glob);
        return new FileFilter() {
            public boolean accept( File pathname ) {
                return pattern.matcher(pathname.getName()).matches();
            }
            public String getDescription() { return glob; }
        };
    }
    
    public Action createSaveAsAction() {
        return new AbstractAction("Save As...") {
            public void actionPerformed( ActionEvent e ) {
                saveAs();
            }
        };
    }
    
    private void saveAs() {
        JFileChooser chooser= new JFileChooser();
        if ( currentFile!=null ) chooser.setCurrentDirectory(currentFile.getParentFile());
        if ( currentFile!=null ) chooser.setSelectedFile( currentFile );
        chooser.setFileFilter( simpleFilter("*"+ext ) );
        int result= chooser.showSaveDialog(this.component);
        if ( result==JFileChooser.APPROVE_OPTION ) {
            File f= chooser.getSelectedFile();
            if ( !f.getName().endsWith(ext) ) f= new File( f.getPath()+ext );
            currentFile= f;
            if ( saveMenuItem!=null ) saveMenuItem.setText("Save");
            if ( currentFileLabel!=null ) currentFileLabel.setText( String.valueOf( currentFile ) );
            addToRecent(currentFile);
            save(currentFile);
        }
        
    }
    
    /**
     * override me
     */
    protected void saveImpl( File f ) throws Exception {
        OutputStream out= new FileOutputStream( f );
        
        Document document= DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        
        Element element= strategy.serialize( document, DasProgressPanel.createFramed("Serializing") );
        
        document.appendChild( element );
        
        StringWriter writer = new StringWriter();
        OutputFormat format = new OutputFormat(org.apache.xml.serialize.Method.XML, "UTF-8", true);
        XMLSerializer serializer = new XMLSerializer( new OutputStreamWriter(out), format);
        
        serializer.serialize(document);
        out.close();
        
    }
    
    private void save( final File file ) {
        Runnable run= new Runnable() {
            public void run() {
                try {
                    File f= file;
                    if ( !f.getName().endsWith(ext) ) f= new File( f.getPath()+ext );
                    
                    saveImpl(f);
                    
                    Preferences prefs= Preferences.userNodeForPackage(PersistentStateSupport.class);
                    prefs.put( "PersistentStateSupport"+ext, currentFile.getAbsolutePath() );
                } catch ( IOException ex ) {
                    throw new RuntimeException(ex);
                } catch ( ParserConfigurationException ex ) {
                    throw new RuntimeException(ex);
                } catch ( Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        new Thread( run, "PersistentStateSupport.save" ).start();
    }
    
    public Action createSaveAction() {
        return new AbstractAction("Save") {
            public void actionPerformed( ActionEvent e ) {
                if ( currentFile==null ) {
                    saveAs();
                } else {
                    save(currentFile);
                }
            }
        };
    }
    
    /**
     * In the future, this should prompt for save if the app is dirty.
     */
    public Action createQuitAction() {
        return new AbstractAction("Quit") {
            public void actionPerformed( ActionEvent e ) {
                System.exit(0);
            }
        };
    }
    
    
    public JMenuItem createSaveMenuItem() {
        saveMenuItem= new JMenuItem(createSaveAction());
        if (currentFile!=null ) {
            saveMenuItem.setText("Save");
        }
        return saveMenuItem;
    }
    
    public JMenu createOpenRecentMenu() {
        JMenu menu= new JMenu("Open recent");
        menu.add( String.valueOf(currentFile) );
        openRecentMenu= menu;
        refreshRecentFilesMenu();
        return menu;
    }
    
    public JLabel createCurrentFileLabel() {
        currentFileLabel= new JLabel( String.valueOf(currentFile) );
        return currentFileLabel;
    }
    
    private static Document readDocument( File file ) throws IOException, ParserConfigurationException, SAXException {
        InputStream in= new FileInputStream(file);
        InputSource source = new InputSource();
        source.setCharacterStream(new InputStreamReader(in));
        DocumentBuilder builder;
        ErrorHandler eh= null;
        DocumentBuilderFactory domFactory= DocumentBuilderFactory.newInstance();
        builder = domFactory.newDocumentBuilder();
        builder.setErrorHandler(eh);
        Document document= builder.parse(source);
        return document;
    }
    
    private void addToRecent( File file ) {
        if ( recentFiles.contains(file) ) return;
        recentFiles.add(0,file);
        while (recentFiles.size()>7) {
            recentFiles.remove(7);
        }
        Preferences prefs= Preferences.userNodeForPackage(PersistentStateSupport.class);
        prefs.put( "PersistentStateSupport"+ext+"_recent", getRencentFilesString() );
        refreshRecentFilesMenu();
    }
    
    public Action createOpenAction() {
        return new AbstractAction("Open...") {
            public void actionPerformed( ActionEvent ev ) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    if ( currentFile!=null ) chooser.setCurrentDirectory(currentFile.getParentFile());
                    chooser.setFileFilter( simpleFilter( "*"+ext ) );
                    int result = chooser.showOpenDialog(component);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        open( chooser.getSelectedFile() );
                        currentFile= chooser.getSelectedFile();
                        addToRecent(currentFile);
                        if ( saveMenuItem!=null ) saveMenuItem.setText("Save");
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    /**
     * override me
     */
    protected void openImpl( File file ) throws Exception {
        Document document= readDocument( file );
        strategy.deserialize( document, DasProgressPanel.createFramed("deserializing") );
    }
    
    private void open( final File file ) {
        Runnable run = new Runnable() {
            public void run() {
                try {
                    openImpl(file);
                } catch ( IOException e ) {
                    throw new RuntimeException(e);
                } catch ( ParserConfigurationException e ) {
                    throw new RuntimeException(e);
                } catch ( SAXException e ) {
                    throw new RuntimeException(e);
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread( run, "PersistentStateSupport.open" ).start();
    }
    
    public void close() {
        currentFile= null;
    }
    
    /** Creates a new instance of PersistentStateSupport */
    public PersistentStateSupport() {
    }
    
}

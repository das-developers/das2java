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
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
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
    
    DasCanvas canvas;
    String ext;
    File currentFile;

    private JMenuItem saveMenuItem;
    
    /**
     *  Provides a means for saving the application persistently, undo/redo support (TODO).
     *  canvas is the canvas to be serialized, extension identifies the application.  Note that
     *  internal changes to das may break saved files.
     */
    public PersistentStateSupport( DasCanvas canvas, String extension ) {
        this.canvas= canvas;
        this.ext= "."+extension;
        Preferences prefs= Preferences.userNodeForPackage(PersistentStateSupport.class);
        String currentFileString= prefs.get( "PersistentStateSupport"+ext, "" );
        if ( !currentFileString.equals("") ) currentFile= new File( currentFileString );
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
        int result= chooser.showSaveDialog(canvas);
        if ( result==JFileChooser.APPROVE_OPTION ) {
            File f= chooser.getSelectedFile();
            if ( !f.getName().endsWith(ext) ) f= new File( f.getPath()+ext );
            currentFile= f;
            if ( saveMenuItem!=null ) saveMenuItem.setText("Save "+currentFile);
            save();
        }
        
    }
    
    private void save( ) {
        try {
            File f= currentFile;
            if ( !f.getName().endsWith(ext) ) f= new File( f.getPath()+ext );
            
            OutputStream out= new FileOutputStream( f );
            
            Document document= DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            
            DOMBuilder builder= new DOMBuilder( canvas );
            
            Element element= builder.serialize( document, DasProgressPanel.createFramed("Serializing Canvas") );
            
            document.appendChild( element );
            
            StringWriter writer = new StringWriter();
            OutputFormat format = new OutputFormat(org.apache.xml.serialize.Method.XML, "UTF-8", true);
            XMLSerializer serializer = new XMLSerializer( new OutputStreamWriter(out), format);
            
            serializer.serialize(document);
            out.close();
            
            Preferences prefs= Preferences.userNodeForPackage(PersistentStateSupport.class);
            prefs.put( "PersistentStateSupport"+ext, currentFile.getAbsolutePath() );
        } catch ( IOException ex ) {
            throw new RuntimeException(ex);
        } catch ( ParserConfigurationException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    public Action createSaveAction() {
        return new AbstractAction("Save") {
            public void actionPerformed( ActionEvent e ) {
                if ( currentFile==null ) {
                    saveAs();
                } else {
                    save();
                }
            }
        };
    }
    
    public JMenuItem createSaveMenuItem() {
        saveMenuItem= new JMenuItem(createSaveAction());
        if (currentFile!=null ) {
            saveMenuItem.setText("Save "+currentFile);
        }
        return saveMenuItem;
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
    
    public Action createOpenAction() {
        return new AbstractAction("Open...") {
            public void actionPerformed( ActionEvent ev ) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    if ( currentFile!=null ) chooser.setCurrentDirectory(currentFile.getParentFile());
                    chooser.setFileFilter( simpleFilter( "*"+ext ) );
                    int result = chooser.showOpenDialog(canvas);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        try {
                            Document document= readDocument( chooser.getSelectedFile() );
                            Element element= document.getDocumentElement();
                            SerializeUtil.processElement(element,canvas );
                            currentFile= chooser.getSelectedFile();
                             if ( saveMenuItem!=null ) saveMenuItem.setText("Save "+currentFile);
                        } catch ( IOException e ) {
                            throw new RuntimeException(e);
                        } catch ( ParserConfigurationException e ) {
                            throw new RuntimeException(e);
                        } catch ( SAXException e ) {
                            throw new RuntimeException(e);
                        }
                        
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
    public void close() {
        currentFile= null;
    }
    
    /** Creates a new instance of PersistentStateSupport */
    public PersistentStateSupport() {
    }
    
}

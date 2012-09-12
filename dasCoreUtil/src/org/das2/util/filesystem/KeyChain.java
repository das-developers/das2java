/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.awt.Component;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.das2.util.monitor.CancelledOperationException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * class that contains the credentials for websites.  This is first
 * introduced so that ftp://papco:@mrfrench.lanl.gov/ and subdirectories
 * would just ask for credentials once.  Also, this allows all the sensitive
 * information to be stored in one class.
 *
 * @author jbf
 */
public class KeyChain {

    /**
     * return the user info (user:password) for the given URI.
     * @param uri
     * @return
     */

    private static KeyChain instance;

    public static synchronized KeyChain getDefault() {
        if ( instance==null ) {
            instance= new KeyChain();
            instance.loadInitial();
        }
        return instance;
    }

    /**
     * If the keys file is found, then pre-load these credentials.
     * The keys file is in new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
     */
    private void loadInitial() {
        File keysFile= new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
        if ( keysFile.exists() ) {
            System.err.println("loading keys from "+ keysFile );
            BufferedReader r=null;
            try {
                r= new BufferedReader( new FileReader(keysFile) );
                String line= r.readLine();
                while ( line!=null ) {
                    int i= line.indexOf("#");
                    if ( i>-1 ) line= line.substring(0,i);
                    line= line.trim();
                    if ( line.length()>0 ) {
                        String[] ss= line.split("\\s+");
                        if ( ss.length!=2 ) {
                            System.err.println("skipping line because wrong number of fields: "+line );
                        } else {
                            String hash= ss[0].trim();
                            if ( hash.endsWith("/") ) {
                                hash= hash.substring(0,hash.length()-1);
                            }
                            String storedUserInfo= ss[1].trim();
                            keys.put( hash, storedUserInfo );
                        }
                    }
                    line= r.readLine();
                }
            } catch ( IOException ex ) {
                ex.printStackTrace();
            } finally {
                if ( r!=null ) {
                    try {
                        r.close();
                    } catch (IOException ex) {
                        Logger.getLogger(KeyChain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }
    }

    /**
     * dump the loaded keys into the file new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" )
     * TODO: note Java1.6 has File.setReadable( true, ownerOnly ).
     */
    public void writeKeysFile() {
        File keysFile= new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
        if ( keysFile.exists() ) throw new IllegalArgumentException("keys file "+keysFile + " already exists, delete it first");
        PrintWriter w=null;
        try {
            w= new PrintWriter( new FileWriter(keysFile) );
            w.println("# keys file produced on "+ new java.util.Date() );
            for ( Entry<String,String> key : keys.entrySet() ) {
                w.println( key.getKey() + "\t" + key.getValue() );
            }
        } catch ( IOException ex ) {
            ex.printStackTrace();
        } finally {
            if ( w!=null ) {
                w.close();
            }
        }

        System.err.println("******************************");
        System.err.println("created world-readable file ");
        System.err.println( keysFile);
        System.err.println("that contains all passwords!!!");
        System.err.println("******************************");

        JOptionPane.showMessageDialog( null,
                "<html>******************************<br>"
                +"created world-readable file <br>"
                +keysFile +"<br>"
                +"that contains all passwords!!!<br>"
                +"******************************" );
        
    }


    private Map<String,String> keys= new HashMap<String,String>();

    /**
     * parent component for password dialog.
     */
    private Component parent=null;

    /**
     * get the user credentials, maybe throwing CancelledOperationException if the
     * user hits cancel.
     * @param url
     * @return
     * @throws CancelledOperationException
     */
    public String getUserInfo( URI uri ) throws CancelledOperationException {
        try {
            return getUserInfo(uri.toURL());
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getUserInfo( URL url ) throws CancelledOperationException {
        String userInfo= url.getUserInfo();
        if ( userInfo==null ) return null;
        return getUserInfo( url, userInfo );
    }

    public void setParentGUI( Component c ) {
        this.parent= c;
    }

    /**
     * get the user credentials, maybe throwing CancelledOperationException if the
     * user hits cancel.  If the password is "pass" or "password" then don't use
     * it, prompt for it instead.
     * Note a %40 in the username is converted to @.
     * @param url
     * @param userInfo that is available separately.  (Java doesn't like user@usersHost:password@server)
     * @return
     * @throws CancelledOperationException
     */
    public String getUserInfo( URL url, String userInfo  ) throws CancelledOperationException {

        if ( userInfo==null ) return null;

        String userName=null;
        String[] ss= userInfo.split(":",-2);
        if ( !ss[0].equals("user") ) {
            userName= ss[0];
            if ( userName.contains("%40") ) {
                userName= userName.replaceAll( "%40", "@" );
                StringBuilder userInfob= new StringBuilder(userName);
                for ( int i=1; i<ss.length; i++ ) {
                    userInfob.append(":").append(ss[i]);
                }
                userInfo= userInfob.toString();
            }
        }
        
        String hash= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost();

        String storedUserInfo= keys.get(hash);
        if ( storedUserInfo!=null ) return storedUserInfo;

        String proto= url.getProtocol();
        
        if ( ss.length<2 || ss[1].length()==0 || userInfo.endsWith(":pass") || userInfo.endsWith(":password" ) ) {
            if ( !FileSystemSettings.hasAllPermission() || !"true".equals( System.getProperty("java.awt.headless") ) ) {
                JPanel panel= new JPanel();
                panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
                panel.add( new JLabel( url.getHost() ) );
                JSeparator sep= new JSeparator( SwingConstants.HORIZONTAL );
                sep.setPreferredSize( new Dimension(0,16) );
                panel.add( sep );
                panel.add( new JLabel("Username:") );
                JTextField userTf= new JTextField();
                if ( !ss[0].equals("user") ) userTf.setText(userName);
                panel.add( userTf );
                panel.add( new JLabel("Password:") );
                JPasswordField passTf= new JPasswordField();
                if ( ss.length>1 && !( ss[1].equals("pass")||ss[1].equals("password")) ) passTf.setText(ss[1]);
                panel.add( passTf );
                //int r= JOptionPane.showConfirmDialog( null, panel, "Authentication Required", JOptionPane.OK_CANCEL_OPTION );
                int r= JOptionPane.showConfirmDialog( parent, panel, proto + " Authentication Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null);
                if ( JOptionPane.OK_OPTION==r ) {
                    char[] pass= passTf.getPassword();
                    storedUserInfo= userTf.getText() + ":" + new String(pass);
                    keys.put( hash, storedUserInfo );
                    return storedUserInfo;
                } else if ( JOptionPane.CANCEL_OPTION==r ) {
                    throw new CancelledOperationException();
                }
            } else {
                if ( "true".equals( System.getProperty("java.awt.headless") ) ) { 
                    System.err.println("** java.awt.headless=true: HEADLESS MODE means needed credentials cannot be queried");
                }
                return userInfo;
            }
        }

        return userInfo;
    }

    public void clearUserPassword(URI uri) {
        try {
            clearUserPassword(uri.toURL());
        } catch (MalformedURLException ex) {
            Logger.getLogger(KeyChain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * remove the password from the list of known passwords.  This was introduced
     * because we needed to clear a bad password in FTP.
     * @param uri
     */
    public void clearUserPassword(URL url) {

        String userInfo= url.getUserInfo();
        if ( userInfo==null ) return;

        String userName=null;
        String[] ss= userInfo.split(":",-2);
        if ( !ss[0].equals("user") ) {
            userName= ss[0];
            if ( userName.contains("%40") ) {
                userName= userName.replaceAll( "%40", "@" );
            }
        }
        
        String hash= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost();

        String storedUserInfo= keys.get(hash);
        if ( storedUserInfo!=null ) {
            keys.remove(hash);
        }
    }

    /**
     * plug the username and password into the URI.
     * @param root
     */
    public URI resolveUserInfo(URI root) throws CancelledOperationException {
        try {
            String userInfo = getUserInfo(root);
            URI newuri = new URI(root.getScheme(), userInfo, root.getHost(), root.getPort(), root.getPath(), root.getQuery(), root.getFragment());
            return newuri;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public String hideUserInfo( URI root ) {
        String userInfo= root.getUserInfo();
        int i= userInfo.indexOf(":");
        if ( i>-1 ) {
            userInfo= userInfo.substring(0,i) + ":*****";
        }
        URI uri;
        try {
            uri = new URI(root.getScheme(), userInfo, root.getHost(), root.getPort(), root.getPath(), root.getQuery(), root.getFragment());
            return uri.toString(); // suspect https://sourceforge.net/tracker/?func=detail&aid=3055130&group_id=199733&atid=970682
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main( String[]args ) throws MalformedURLException, CancelledOperationException {
        KeyChain.getDefault().getUserInfo( new URL( "ftp://jbf@localhost/" ) );
    }

}


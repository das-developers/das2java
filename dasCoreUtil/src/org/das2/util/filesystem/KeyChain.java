/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.das2.util.monitor.CancelledOperationException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.das2.util.Base64;

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

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem");

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
            logger.log( Level.FINE, "loading keys from {0}", keysFile);
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
                            logger.log( Level.WARNING, "skipping line because wrong number of fields: {0}", line);
                        } else {
                            String hash= ss[0].trim();
                            if ( hash.endsWith("/") ) {
                                hash= hash.substring(0,hash.length()-1);
                            }
                            String storedUserInfo= ss[1].trim();
                            //TODO: shouldn't "http://ectsoc@www.rbsp-ect.lanl.gov" match "http://www.rbsp-ect.lanl.gov    ectsoc:..."
                            keys.put( hash, storedUserInfo );
                        }
                    }
                    line= r.readLine();
                }
            } catch ( IOException ex ) {
                logger.log( Level.SEVERE, "while loading keychain.txt file "+keysFile, ex );
            } finally {
                if ( r!=null ) {
                    try {
                        r.close();
                        logger.log(Level.FINE, "loaded keys from keychain file {0}", keysFile);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
            }

        }
    }

    /**
     * dump the loaded keys into the file new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" )
     */
    public void writeKeysFile() {
        try {
            writeKeysFile(false);
        } catch ( IOException ex ) {
            throw new IllegalArgumentException(ex);
        }
    }
    
    public void writeKeysFile( boolean toFile ) throws IOException {
        File keysFile= new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
        
        PrintWriter w=null;
        final ByteArrayOutputStream out= new ByteArrayOutputStream();
        
        w= new PrintWriter( out );
        w.println("# keys file produced on "+ new java.util.Date() );
        w.println("# "+keysFile );
        for ( Entry<String,String> key : keys.entrySet() ) {
            w.println( key.getKey() + "\t" + key.getValue() );
        }
        w.close();

        if ( toFile ) {
            FileOutputStream fout=null;
            try {
                fout= new FileOutputStream(keysFile);
                fout.write( out.toByteArray() );
            } finally {
                if ( fout!=null ) fout.close();
            }
            if ( !keysFile.setReadable(false) ) logger.warning("setReadable failure");
            if ( !keysFile.setReadable(false,false) ) logger.warning("setReadable failure");
            if ( !keysFile.setReadable(true,true) ) logger.warning("setReadable failure");
            if ( !keysFile.setWritable(false) ) logger.warning("setWritable failure");
            if ( !keysFile.setWritable(true,true) ) logger.warning("setWritable failure");
        }
        
        JButton button= new JButton( new AbstractAction( "Show Passwords") {
            public void actionPerformed(ActionEvent e) {
                JTextArea ta= new JTextArea();
                ta.setText( new String( out.toByteArray() ) );
                JOptionPane.showMessageDialog(parent, ta );
            }
        });
        
        String s= toFile ? "The keychain file has been created:" : "You must create a protected file";
        JPanel p= new JPanel(new BorderLayout());
        p.add( new JLabel("<html>******************************<br>"
                + s + "<br>"
                +keysFile +"<br>"
                +"that contains all passwords.<br>"
                +"Click the button below to show content, <b>which contains passwords.</b><br>"
                +"******************************" ) );
        p.add( button, BorderLayout.SOUTH );
        JOptionPane.showMessageDialog( parent, p );
        
    }


    private Map<String,String> keys= new HashMap<String,String>();

    /**
     * parent component for password dialog.
     */
    private Component parent=null;

    /**
     * get the user credentials, maybe throwing CancelledOperationException if the
     * user hits cancel.
     * @param uri
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
    
    /**
     * return the user info but base-64 encoded.  This is put in so that
     * a future version of the software can cache these as well.  This is
     * intended to be inserted like so:
     * <code>
     * connection= theUrl.getConnection();
     * String encode= KeyChain.getDefault().getUserInfoBase64Encoded( theUrl );
     * if ( encode!=null ) connection.setRequestProperty("Authorization", "Basic " + encode);
     * </code>
     * @param url the URL which may contain user info.
     * @return the base-64 encoded credentials.
     * @throws CancelledOperationException 
     */
    public String getUserInfoBase64Encoded( URL url ) throws CancelledOperationException {
        String userInfo= getUserInfo(url);
        if ( userInfo!=null ) {
            return Base64.encodeBytes( userInfo.getBytes());
        } else {
            return null;
        }
    }

    public void setParentGUI( Component c ) {
        this.parent= c;
    }

    /**
     * return null or the WWW-Authenticate string.
     * @param url
     * @return 
     */
    public String getWWWAuthenticate( URL url ) {
        try {
            URLConnection c= url.openConnection();
            c.connect();
            String s= c.getHeaderField("WWW-Authenticate");
            if ( s==null ) {
                return null;
            } else {
                int i= s.indexOf("\"");
                if ( i>-1 ) {
                    s= s.substring(i);
                }
                return s;
            }
            //BufferedReader in= new BufferedReader( new InputStreamReader( c.getInputStream() ) );
            //String line= "";
            //while ( line!=null ) line= in.readLine(); // eat the rest of the stream, because this is important.
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    /**
     * get the user credentials, maybe throwing CancelledOperationException if the
     * user hits cancel.  If the password is "pass" or "password" then don't use
     * it, prompt for it instead.
     * 
     * The userInfo passed in can contain just "user" or the user account to log in with, then
     * maybe a colon and "pass" or the password.  So examples include:<ul>
     * <li> null, where null is returned and credentials are presumed to 
     * <li> user, where both the username and password are needed.
     * <li> user:pass where both are needed
     * <li> joe:pass there the user joe is presumed and pass is needed
     * <li> joe:JoPass1 where both the user and password are already specified, and this is returned.
     * </ul>
     * 
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
        //TODO: shouldn't "http://ectsoc@www.rbsp-ect.lanl.gov" match "http://www.rbsp-ect.lanl.gov    ectsoc:"
        if ( storedUserInfo!=null ) return storedUserInfo;
        
        String n= "";
        if ( url.getProtocol().startsWith("http")  ) {
            n= getWWWAuthenticate(url); // extra call to server to get prompt
            if ( n==null ) n="";
        }

        String proto= url.getProtocol();
        
        if ( ss.length<2 || ss[1].length()==0 || userInfo.endsWith(":pass") || userInfo.endsWith(":password" ) ) {
            if ( !FileSystemSettings.hasAllPermission() || !"true".equals( System.getProperty("java.awt.headless") ) ) {
                JPanel panel= new JPanel();
                panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
                if ( n!=null && n.length()>0 ) { 
                    String s= ( userName!=null ? userName+"@" : "" ) + url.getHost() + url.getFile();
                    panel.add( new JLabel( "<html>Enter Login details to access<br>"+n+" on<br>"+s ));
                } else {
                    panel.add( new JLabel( url.getHost() ) );
                }
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
                    logger.log( Level.WARNING, "** java.awt.headless=true: HEADLESS MODE means needed credentials cannot be queried" );
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
            logger.log(Level.SEVERE, ex.getMessage(), ex);
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
        KeyChain.getDefault().getUserInfo( new URL( "http://junomwg@www-pw.physics.uiowa.edu/juno/mwg/" ) );
        KeyChain.getDefault().getUserInfo( new URL( "ftp://jbf@localhost/" ) );
    }

}


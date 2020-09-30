
package org.das2.util.filesystem;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import org.das2.util.monitor.CancelledOperationException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import org.das2.util.Base64;
import org.das2.util.CredentialsManager;

/**
 * class that contains the credentials for websites.  This is first
 * introduced so that ftp://papco:@mrfrench.lanl.gov/ and subdirectories
 * would just ask for credentials once.  Also, this allows all the sensitive
 * information to be stored in one class.
 *
 * @author jbf
 */
public class KeyChain {

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("das2.filesystem.keychain");

    private static KeyChain instance;

    /**
     * get the single instance of the class.
     * @return the single instance of the class.
     */
    public static synchronized KeyChain getDefault() {
        if ( instance==null ) {
            instance= new KeyChain();
            instance.loadInitial();
        }
        return instance;
    }
    
    private static Map<String,KeyChain> instances= new HashMap<>();
    
    /**
     * get the instance for this name.  This is added to support future applications, such as servlets, where
     * multiple users are using the same process.
     * 
     * @param name
     * @return 
     */
    public static synchronized KeyChain getInstance( String name ) {
        if ( name==null || name.length()==0 ) {
            return getDefault();
        } 
        KeyChain t= instances.get(name);
        if ( t!=null ) {
            return t;
        } else {
            t= new KeyChain();
            instances.put( name, t );
            return t;
		}
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
                            String storedUserInfo= ss[1].trim();
                            //TODO: shouldn't "http://ectsoc@www.rbsp-ect.lanl.gov" match "http://www.rbsp-ect.lanl.gov    ectsoc:..."
                            if ( !hash.endsWith("/") ) {
                                int k=hash.lastIndexOf("/");
                                hash= hash.substring(0,k+1);
                            }
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

        } else {
            logger.log( Level.FINE, "keychain file not found: {0}", keysFile);
        }
    }

    /**
     * append to the keys file.
     * @param url web file or folder 
     * @param key the key
     * @throws IOException 
     * @see #writeKeysFile(java.lang.String, java.lang.String) 
     */
    private void appendKeysFile( String url, String key ) throws IOException {
        
        int i= url.lastIndexOf('/');
        url= url.substring(0,i+1);  // remove file to attach to folder.
        
        File keysFile= new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
        PrintWriter w= null;
        try {
            if ( keysFile.exists() ) {
                if (  keysFile.canWrite() ) {
                    w= new PrintWriter( new FileWriter(keysFile,true) );
                } else {
                    throw new IOException( "Unable to append to file: "+ keysFile );
                }
            } else {
                try {
                    w= new PrintWriter( new FileWriter(keysFile) );
                    if ( !keysFile.setReadable(false) ) logger.warning("setReadable failure");
                    if ( !keysFile.setReadable(false,false) ) logger.warning("setReadable failure");
                    if ( !keysFile.setReadable(true,true) ) logger.warning("setReadable failure");
                    if ( !keysFile.setWritable(false) ) logger.warning("setWritable failure");
                    if ( !keysFile.setWritable(false,false) ) logger.warning("setWritable failure");
                    if ( !keysFile.setWritable(true,true) ) logger.warning("setWritable failure");                    
                    
                } catch ( IOException ex ) {
                    throw new IOException( "Unable to create file: "+ keysFile );
                }
            }
            w.append( url ).append("\t").append(key).append("\n");

        } finally {
            if ( w!=null ) w.close();
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
    
    /**
     * format the keys file.  Since Java 5 didn't have a way to restrict 
     * access to the file, this would simply display the keychain file contents
     * and have the operator write the keychain file to disk.  The required
     * Java7 is able to restict access to the file properly.
     * @param toFile the file should be created.
     * @throws IOException 
     * @see #appendKeysFile(java.lang.String, java.lang.String) 
     */
    public void writeKeysFile( boolean toFile ) throws IOException {
        File keysFile= new File( FileSystem.settings().getLocalCacheDir(), "keychain.txt" );
        
        PrintWriter w=null;
        final ByteArrayOutputStream out= new ByteArrayOutputStream();
        
        try {
            w= new PrintWriter( out );
            w.println("# keys file produced on "+ new java.util.Date() );
            w.println("# "+keysFile );
            for ( Entry<String,String> key : keys.entrySet() ) {
                w.println( key.getKey() + "\t" + key.getValue() );
            }
        } finally {
            if ( w!=null ) w.close();
        }
        
        if ( toFile ) {
            try (FileOutputStream fout = new FileOutputStream(keysFile)) {
                fout.write( out.toByteArray() );
            }
            if ( !keysFile.setReadable(false) ) logger.warning("setReadable failure");
            if ( !keysFile.setReadable(false,false) ) logger.warning("setReadable failure");
            if ( !keysFile.setReadable(true,true) ) logger.warning("setReadable failure");
            if ( !keysFile.setWritable(false) ) logger.warning("setWritable failure");
            if ( !keysFile.setWritable(false,false) ) logger.warning("setWritable failure");            
            if ( !keysFile.setWritable(true,true) ) logger.warning("setWritable failure");
        }
        
        JButton button= new JButton( new AbstractAction( "Show Passwords") {
            @Override
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

    /**
     * map from URL, without trailing slash, to key.
     */
    private final Map<String,String> keys= new LinkedHashMap<>();
    
    /**
     * map from URL, without trailing slash, to cookie.
     */
    private final Map<String,String> cookies= new HashMap<>();

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
     * @see #getUserInfo(java.net.URL) 
     */
    public String getUserInfo( URI uri ) throws CancelledOperationException {
        try {
            return getUserInfo(uri.toURL());
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * return the user info (username:password) associated with this URL.
     * Note if there is no user info in the URI, this will return null!  
     * 
     * @param url the URL
     * @return null or the user info.
     * @throws CancelledOperationException 
     * @see #getUserInfo(java.net.URL, java.lang.String) which will always check and prompt.
     * 
     */
    public String getUserInfo( URL url ) throws CancelledOperationException {
        String userInfo= url.getUserInfo();
        if ( userInfo==null ) return null;
        return getUserInfo( url, userInfo );
    }
    
    /**
     * return null or the stored user info.  This allows clients to attempt to
     * use stored user info without bothering the scientist if the info isn't 
     * needed.
     * @param url the URL
     * @return the user info (user:password) associated, or null if the user info isn't found.
     */
    public String checkUserInfo( URL url ) {
        String userInfo= url.getUserInfo();
        String userName= null;
        if ( userInfo!=null ) {
            String[] ss= userInfo.split(":",2);
            userName= ss[0];
        }
        String path= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost() + url.getPath();
        String storedUserInfo= lookupStoredUserInfo(path);
        return storedUserInfo;
    }
    
    /**
     * insert the userInfo into the table of stored passwords.
     * TODO: note the path is not used in the hash, and it should be.
     * @param url
     * @param userInfo 
     */
    public void setUserInfo( URL url, String userInfo ) {
        String hash= url.getProtocol() + "://" + url.getHost() + "/"; //TODO: whah?  This still doesn't use the path!
        keys.put( hash, userInfo );
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
            return Base64.getEncoder().encodeToString( userInfo.getBytes());
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
                logger.fine("WWW-Authenticate is not provided.");
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
     * look up the stored user info, looking at http://autoplot.org/data/path/a/, 
     * then http://autoplot.org/data/path/, then http://autoplot.org/data/, etc.
     * @param path the path to lookup, with folders ending in /.
     * @return null or the user info.
     */
    private String lookupStoredUserInfo( String path ) {
        int stop= path.indexOf("://");
        int i=path.lastIndexOf('/');
        while( i>stop ) {
            String hash= path.substring(0,i+1);
            String storedUserInfo= keys.get(hash);
            if ( storedUserInfo!=null ) {
                return storedUserInfo;
            } else {
                i= path.lastIndexOf('/',i-1);
            }
        }
        return null;
    }

    /**
     * store the user info, presuming that the site uses the same keys for each
     * level, until another key is found.
     * @param path
     * @param storedUserInfo 
     */
    private void storeUserInfo(String path, String storedUserInfo) {
        int stop= path.indexOf("://")+3;
        int i=path.lastIndexOf('/');
        while( i>stop ) {
            String hash= path.substring(0,i+1);
            if ( keys.get(hash)==null ) {
                keys.put( hash, storedUserInfo );
            }
            i= path.lastIndexOf('/',i-1);
        }
    }
    
    /**
     * remove all keys starting within path.  path should not contain a username.
     * @param path 
     */
    private void clearUserInfo( String path ) {
        int stop= path.indexOf("://")+3;
        int i=path.lastIndexOf('/');
        while( i>stop ) {
            String hash= path.substring(0,i+1);
            if ( keys.get(hash)!=null ) {
                keys.remove(hash);
            }
            i= path.lastIndexOf('/',i-1);
        }        
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
     * @return the userinfo, like "us3r:passw0rd"
     * @throws CancelledOperationException
     * @see https://sourceforge.net/p/autoplot/bugs/1652/
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
        
        String path= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost() + url.getPath();

        String storedUserInfo= lookupStoredUserInfo(path);
        
        //TODO: shouldn't "http://ectsoc@www.rbsp-ect.lanl.gov" match "http://www.rbsp-ect.lanl.gov    ectsoc:"
        if ( storedUserInfo!=null ) return storedUserInfo;
        
        String n= "";
        if ( url.getProtocol().startsWith("http")  ) {
            n= getWWWAuthenticate(url); // extra call to server to get prompt
            if ( n==null ) n="";
        }

        String proto= url.getProtocol();
        String s= proto + "://" +( userName!=null ? userName+"@" : "" ) + url.getHost() + url.getFile();
        
        if ( ss.length<2 || ss[1].length()==0 || userInfo.endsWith(":pass") || userInfo.endsWith(":password" ) ) {
            if ( !FileSystemSettings.hasAllPermission() || !"true".equals( System.getProperty("java.awt.headless") ) ) {
                JPanel panel= new JPanel();
                panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
                if ( n.length()>0 ) { 
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
                
                JCheckBox storeKeychain= new JCheckBox("store password in keychain.txt file");
                storeKeychain.setToolTipText("<html>passwords can be stored in keychain.txt files in your cache, but beware of security implications and confusion this can cause.");

                panel.add( storeKeychain );
                
                //int r= JOptionPane.showConfirmDialog( null, panel, "Authentication Required", JOptionPane.OK_CANCEL_OPTION );
                int r= JOptionPane.showConfirmDialog( parent, panel, proto + " Authentication Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null);
                if ( JOptionPane.OK_OPTION==r ) {
                    char[] pass= passTf.getPassword();
                    storedUserInfo= userTf.getText() + ":" + new String(pass);
                    storeUserInfo( path, storedUserInfo );
                    if ( storeKeychain.isSelected() ) {
                        try {
                            appendKeysFile( path, storedUserInfo );
                        } catch (IOException ex) {
                            logger.log( Level.WARNING, null, ex );
                        }
                    }
                    return storedUserInfo;
                } else if ( JOptionPane.CANCEL_OPTION==r ) {
                    throw new CancelledOperationException();
                }
            } else {
                if ( "true".equals( System.getProperty("java.awt.headless") ) ) { 
                    Console c= System.console();
                    if ( c==null ) {
                        logger.log( Level.WARNING, "** java.awt.headless=true: HEADLESS MODE means needed credentials cannot be queried" );
                        throw new CancelledOperationException("HEADLESS MODE means needed credentials cannot be queried");
                    } else {
                        c.printf( "Enter Login details to access \n%s on\n%s\n", n, s );
                        String user;
                        if ( !ss[0].equals("user") ) {
                            user= c.readLine( "Username (leave empty for %s): ", ss[0] );
                            if ( user.trim().length()==0 ) {
                                user= ss[0];
                            }
                        } else {
                            user= c.readLine( "Username: " );
                        }
                        char[] pass= c.readPassword( "Password: " );
                        storedUserInfo= user + ":" + new String(pass);
                        return storedUserInfo;
                    }
                } else {
                    return userInfo;
                }
            }
        }

        return userInfo;
    }

    /**
     * clear all passwords.
     */
    public void clearAll() {
        logger.fine("clear all cached passwords in the keychain, and reload all keychain.txt files.");
        keys.clear();
        loadInitial();
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
     * @param url
     */
    public void clearUserPassword(URL url) {

        String userName=null;
        
        String userInfo= url.getUserInfo();
        if ( userInfo==null ) {
            userInfo= lookupStoredUserInfo(url.toString());
        } else {
            String[] ss= userInfo.split(":",-2);
            if ( !ss[0].equals("user") ) {
                userName= ss[0];
                if ( userName.contains("%40") ) {
                    userName= userName.replaceAll( "%40", "@" );
                }
            }
        }
        if ( userInfo==null ) return;
        
        String hash= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost() + url.getPath();
        if ( hash.endsWith("/") ) {
            
        }

        clearUserInfo( hash );

    }

    /**
     * plug the username and password into the URI.
     * @param root the URI, possibly needing a username and password.
     * @return the URI with the username and password.
     * @throws org.das2.util.monitor.CancelledOperationException
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

    /**
     * Add a cookie for the URL.  This was added as a work-around to provide
     * access to the MMS data server at LASP.
     * @param url
     * @param cookie 
     */
    public void addCookie( String url, String cookie ) {
        if ( url.endsWith("/") ) {
            url= url.substring(0,url.length()-1);
        }
        if ( !url.equals("https://lasp.colorado.edu/mms/sdc/about/browse") ) {
            System.err.println("Warning: This is only works for https://lasp.colorado.edu/mms/sdc/about/browse");
        }
        cookies.put( url, cookie );
    }
    
    /**
     * Some servers want cookies to handle the authentication.  This checks for
     * "https://lasp.colorado.edu/mms/sdc/about/browse/" and handles logins for
     * this server.
     * @param url
     * @return null or the cookie to include in the request header.
     */
    protected String getCookie(URL url) {
        String cookie= null;
        if ( url.toString().contains("https://lasp.colorado.edu/mms/sdc/about/browse/") ) {
            String hash=             "https://lasp.colorado.edu/mms/sdc/about/browse";
            cookie= cookies.get(hash);
            if ( cookie==null ) {
                try {
                    
                    System.err.println( "See http://stackoverflow.com/questions/9619030/resolving-javax-net-ssl-sslhandshakeexception-sun-security-validator-validatore");
                    System.err.println( "jsse.enableSNIExtension="+ System.getProperty("jsse.enableSNIExtension") );
                    
                    URL urlr= new URL( "https://lasp-login.colorado.edu/idp/Authn/UserPassword" );
                    HttpURLConnection conn= (HttpURLConnection) urlr.openConnection();
                    
                    conn.setDoOutput(true);
                    conn.connect();
                    
                    HttpUtil.consumeStream(conn.getErrorStream());
                    HttpUtil.consumeStream(conn.getInputStream());
                    
                    String cookie0= conn.getHeaderField("Set-Cookie");
                    conn.disconnect();

                    String user= getUserInfo( new URL("https://lasp.colorado.edu/mms/sdc/about/browse"), "user:" );
                     
                    conn= (HttpURLConnection) urlr.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
                    int i= user.indexOf(":");
                    String username= user.substring(0,i);
                    String password= user.substring(i+1);
                    String encodedData =  "j_username="+URLEncoder.encode(username,"US-ASCII")+"&j_password="+URLEncoder.encode(password,"US-ASCII"); 
                    conn.setRequestProperty( "Referer", "https://lasp-login.colorado.edu/Authn/UserPassword" );
                    conn.setRequestProperty( "Content-Length", String.valueOf(encodedData.length()));
                    conn.setRequestProperty( "Cookie", cookie0 );
                    conn.connect();
                    OutputStream os = conn.getOutputStream();
                    os.write(encodedData.getBytes("US-ASCII"));
                    os.close();
                    String cookie1= conn.getHeaderField("Set-Cookie");
                    os.close();
                    cookie= cookie1;
                } catch (MalformedURLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                } catch (CancelledOperationException | IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            //cookie= ""; // see email to jeremy-faden@uiowa.edu at 2015-09-01T12:50 CDT for cookie
            //cookie= "_shibsession_64656661756c7468747470733a2f2f646d7a2d73686962322e6c6173702e636f6c6f7261646f2e6564752f73686962626f6c657468=_76cd22bfba4f8da6a96910259901710b"; 
        }
        return cookie;
    }

}


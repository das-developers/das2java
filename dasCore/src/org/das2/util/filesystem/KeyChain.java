/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.das2.CancelledOperationException;

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
        }
        return instance;
    }

    private Map<String,String> keys= new HashMap<String,String>();

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

    /**
     * get the user credentials, maybe throwing CancelledOperationException if the
     * user hits cancel.
     * @param url
     * @return
     * @throws CancelledOperationException
     */
    public String getUserInfo( URL url ) throws CancelledOperationException {

        String userInfo= url.getUserInfo();
        if ( userInfo==null ) return null;

        String userName=null;
        String[] ss= userInfo.split(":",-2);
        if ( !ss[0].equals("user") ) {
            userName= ss[0];
        }
        String hash= url.getProtocol() + "://" + ( userName!=null ? userName+"@" : "" ) + url.getHost();

        String storedUserInfo= keys.get(hash);
        if ( storedUserInfo!=null ) return storedUserInfo;

        if ( ss.length<2 || ss[1].length()==0 || userInfo.equals("user:pass") ) {
            if ( !FileSystemSettings.hasAllPermission() || !"true".equals( System.getProperty("java.awt.headless") ) ) {
                JPanel panel= new JPanel();
                panel.setLayout( new BoxLayout(panel, BoxLayout.Y_AXIS ) );
                panel.add( new JLabel("Username:") );
                JTextField userTf= new JTextField();
                if ( !ss[0].equals("user") ) userTf.setText(ss[0]);
                panel.add( userTf );
                panel.add( new JLabel("Password:") );
                JPasswordField passTf= new JPasswordField();
                if ( ss.length>1 && !ss[1].equals("pass") ) passTf.setText(ss[1]);
                panel.add( passTf );
                int r= JOptionPane.showConfirmDialog( null, panel, "Authentication Required", JOptionPane.OK_CANCEL_OPTION );
                if ( JOptionPane.OK_OPTION==r ) {
                    char[] pass= passTf.getPassword();
                    storedUserInfo= userTf.getText() + ":" + new String(pass);
                    keys.put( hash, storedUserInfo );
                    return storedUserInfo;
                } else if ( JOptionPane.CANCEL_OPTION==r ) {
                    throw new CancelledOperationException();
                }
            } else {
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
}

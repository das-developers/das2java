/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.das2.DasApplication;

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

    public String getUserInfo( URL url ) {

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
            if ( !DasApplication.hasAllPermission() || !"true".equals( System.getProperty("java.awt.headless") ) ) {
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
                if ( JOptionPane.OK_OPTION==JOptionPane.showConfirmDialog( null, panel, "Authentication Required", JOptionPane.OK_OPTION ) ) {
                    char[] pass= passTf.getPassword();
                    storedUserInfo= userTf.getText() + ":" + new String(pass);
                    keys.put( hash, storedUserInfo );
                    return storedUserInfo;
                }
            } else {
                return userInfo;
            }
        }

        return userInfo;
    }
}

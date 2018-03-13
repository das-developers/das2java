/* Part of the Das2 libraries, which the LGPL with class-path exception license */
package org.das2.util;

import java.awt.Frame;
import java.awt.Window;
import java.io.Console;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/** Provides per-resource login credentials 
 * 
 * This class maintains a map of login credentials by resource ID.  The resource ID's 
 * themselves are just strings.  The only expectation on resource ID strings is that they
 * should be suitable for use as the Keys to a hash map.  Otherwise no formation rules are
 * assumed nor expected.  User names and passwords for multiple web-sites, ftp sites, etc.
 * are maintained by a single instance of this class.  Call:
 * 
 *    CredentialsManager.getManager()
 * 
 * to get a reference to that single instance.
 * 
 * In a graphical environment this class handles presenting dialogs to the user to gather
 * logon credentials.  In a shell environment it will interact with the TTY to get user
 * information.  
 * 
 * An example of using this class to handle Das 2.1 server authentication which html
 * location information formatting follows:
 * <code>
 * 
 * CredentialsManage cm = CrentialsManager.getMannager();
 * String sLocId = "planet.physics.uiowa.edu/das/das2Server|voyager1/pwi/SpecAnalyzer-4s-Efield";
 * 
 * if(!cm.hasCredentials(sLocId)){
 *    DasServer svr = DasServer.create(sDsdf);
 *    String sDesc = String.Format("<html><h1>%s</h1><h3>Server:  %s</h3><h3>DataSource: %s</h3>",
 *                                 DasServer.getName(), "planet.physics.uiowa.edu",
 *                                 "voyager1 > pwi > SpecAnalyzer-4s-Efield");
 *    cm.setDescription(sLocId, sDesc, DasServer.getLogo());
 * }
 * 
 * String sHash = getHttpBasicHash(sLocId)
 * 
 * </code>
 * 
 * Two previous classes, org.das2.util.filesystem.KeyChain (autoplot) and
 * org.das2.client.Authenticator have approached this problem as well.  However both of
 * those classes make assumptions that are not valid in general.  The first assumes that
 * the caller somehow knows the username.  The second assumes that you are talking to
 * a first generation Das 2.1 server.  Details of server communication are beyond the 
 * scope of this class.
 * 
 * @author cwp
 */
public class CredentialsManager{
	
	///////////////////////////////////////////////////////////////////////////////////
	// Static Section
	
	// A map of credentials managers versus lookup key
	static final HashMap<String, CredentialsManager> g_dManagers = new HashMap();
	static{
		g_dManagers.put(null, new CredentialsManager(null));
	}
	
	/** Get a reference to the authentication manager.  
	 * 
	 * Typically this is the function you want to use to get started.
	 */
	public static CredentialsManager getMannager(){
		return g_dManagers.get(null);
	}
	
	/** Get an authentication manager associated with an associated tracking string.
	 * This is probably <b>not</b> the function you are looking for.  It only exists for
	 * odd cases where two different credential managers are active in a single
	 * application at the same time. 
	 * 
	 * @param sWhich - A string used to differentiate this credentials manager from the
	 * default instance.
	 */ 
	public static CredentialsManager getMannager(String sWhich){
		if(!g_dManagers.containsKey(sWhich)){
			synchronized(g_dManagers) {
				g_dManagers.put(sWhich, new CredentialsManager(sWhich));
			}
		}
		return g_dManagers.get(sWhich);
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	// Instance Section
	
	protected static class Location{
		String sLocId;
		String sDesc;
		ImageIcon iconLogo;
		String sUser;
		String sPasswd;

		protected Location(String sLocationId, String sDescription, ImageIcon icon){
			sLocId = sLocationId;
			sDesc = sDescription;
			iconLogo = icon;
			sUser = null;
			sPasswd = null;
		}
		
		protected boolean hasCredentials(){
			return (sUser != null)||(sPasswd != null);
		}
	}
	
	String m_sName;
	HashMap<String, Location> m_dLocs;
	CredentialsDialog m_dlg;
	
	protected CredentialsManager(String sName){
		m_sName = sName;
		m_dLocs = new HashMap();
		m_dlg = null;
	}
	
	/** Provide a description of a location for use in authentication dialogs.
	 * 
	 * Use this function to tie a string description to a location id.  This description
	 * will be used when interacting with the user.  If no description is present, then just
	 * the location ID itself will be used to identify the site to the end user.
	 * Usually location strings aren't that easy to read so use of this function or the
	 * version with an icon argument is recommended, though not required.
	 * 
	 * @param sLocationId The location to describe, can not be null.
	 * @param sDescription A string to present to a user when prompting for a credentials
	 * for this location, may be a basic HTML string.
	 */
	public void setDescription(String sLocationId, String sDescription){
		setDescription(sLocationId, sDescription, null);
	}
	
	/** Provide a description of a location with an Image Icon
	 * 
	 * Use this function to tie a string description and an Icon to a location.
	 * 
	 * @param sLocationId The location to describe, can not be null.
	 * @param sDescription The description, may be a simply formatted HTML string.
	 * @param icon An icon to display for the server.
	 */
	public synchronized void setDescription(String sLocationId, String sDescription, 
		                                     ImageIcon icon)
	{
		if(!m_dLocs.containsKey(sLocationId)){
			m_dLocs.put(sLocationId, new Location(sLocationId, sDescription, icon));
		}
		else{
			Location loc = m_dLocs.get(sLocationId);
			loc.sDesc = sDescription;
			loc.iconLogo = icon;
		}
	}
	
	/** Determine if there are any stored credentials for this location 
	 * 
	 * If either a username or a password have been provided for the location
	 * then it is considered to have credentials
	 * 
	 * @param sLocationId The location to describe, can not be null.
	 * @return true if there are stored credentials, false otherwise
	 */
	public boolean hasCredentials(String sLocationId){
		if(!m_dLocs.containsKey(sLocationId)) return false;
		Location loc = m_dLocs.get(sLocationId);
		return loc.hasCredentials();
	}
	
   public void setHttpBasicHashRaw(String sLocationId, String userInfo){
		if(!hasCredentials(sLocationId)){
			m_dLocs.put(sLocationId, new Location(sLocationId, null, null));
		}
		Location loc = m_dLocs.get(sLocationId);

		String[] ss = userInfo.split(":", -2); //TODO: allow colons in passwords

		loc.sUser = ss[0];
		loc.sPasswd = ss[1];

	}
        
	/** Determine if a given location has been described
	 * 
	 * Gathering descriptive information about a remote location may trigger communication
	 * with a remote site.  Use this function to see if such communication is needed.
	 * 
	 * @param sLocationId The location in question
	 * @return true if the location has been described, false otherwise
	 */
	public boolean hasDescription(String sLocationId){
		if(!m_dLocs.containsKey(sLocationId)) return false;
		Location loc = m_dLocs.get(sLocationId);
		return (loc.sDesc != null)&&(!loc.sDesc.isEmpty());
	}
	
	/** Determine is a site image has been set for a location ID.  
	 * 
	 * This function is provided because retrieving the logo for a site may trigger remote
	 * host communication.  Use this function to see if such communication is needed.
	 * @param sLocationId the location in question
	 * @return true if the location has as attached icon logo
	 */
	public boolean hasIcon(String sLocationId){
		if(!m_dLocs.containsKey(sLocationId)) return false;
		Location loc = m_dLocs.get(sLocationId);
		return loc.iconLogo != null;
	}
	
	/** Get credentials in the form of a hashed HTTP Basic authentication string
	 * 
	 * If there are no credentials stored for the given location id, this function may
	 * trigger interaction with the user, such as presenting modal dialogs, or changing the
	 * TTY to non-echo.
	 * 
	 * @param sLocationId A unique string identifying a location.  There are no formation
	 * rules on the string, but convenience functions are provided if a uniform naming 
	 * convention is desired.
	 * 
	 * @return The string USERNAME + ":" + PASSWORD that is then run through a base64 
	 * encoding.  If no credentials are available for the given location ID and none can be 
	 * gathered from the user (possibly due to the java.awt.headless being set or the
	 * user pressing cancel), null is returned.
	 * @see #getHttpBasicHashRaw(java.lang.String) 
	 */
	public String getHttpBasicHash(String sLocationId){
		String sTmp= getHttpBasicHashRaw( sLocationId );
		String sHash = Base64.encodeBytes( sTmp.getBytes());
		return sHash;
	}
        
   /** Get credentials in the form of a hashed HTTP Basic authentication string.
	 *
	 * If there are no credentials stored for the given location id, this function
	 * may trigger interaction with the user, such as presenting modal dialogs, or
	 * changing the TTY to non-echo.
	 *
	 * @param sLocationId A unique string identifying a location. There are no formation
	 * rules on the string, but convenience functions are provided if a uniform naming
	 * convention is desired.
	 *
	 * @return The string USERNAME + ":" + PASSWORD. If no credentials are available for
	 * the given location ID and none can be gathered from the user (possibly due to the
	 * java.awt.headless being set or the user pressing cancel), null is returned.
	 * @see #getHttpBasicHash(java.lang.String)
	 */
	public String getHttpBasicHashRaw(String sLocationId){

		if(!m_dLocs.containsKey(sLocationId)){
			synchronized(this){
				//Check again.  Though unlikely, the key could have been added between
				//the call above and the start of the sychronized section
				if(!m_dLocs.containsKey(sLocationId)){
					m_dLocs.put(sLocationId, new Location(sLocationId, null, null));
				}
			}
		}
		
		Location loc = m_dLocs.get(sLocationId);
		if(!hasCredentials(sLocationId)){
			if("true".equals( System.getProperty("java.awt.headless"))){
				if(!getCredentialsCmdLine(loc)) return null;
			}
			else{
				if(!getCredentialsGUI(loc)) return null;
			}
		}
		
		String sTmp = loc.sUser + ":" + loc.sPasswd;
                return sTmp;
        }
	
	/** Let the credentials manager know that stored credentials for a location are invalid
	 * 
	 * @param sLocationId
	 * @return 
	 */
	public synchronized void invalidate(String sLocationId){
		if(!m_dLocs.containsKey(sLocationId)) return;
		Location loc = m_dLocs.get(sLocationId);
		loc.sUser = null;
		loc.sPasswd = null;
	}
	
	////////////////////////// User Interaction ////////////////////////////////////
	
	/** Gather User Credentials
	 * 
	 * @param loc The Location in question
	 * @return True if user hit OK, False if user canceled the operation
	 */
	protected synchronized boolean getCredentialsGUI(final Location loc) {
		
		// Check again to see if another thread managed to set the credentials before
		// this method started.  Need to avoid the double-authenticate dialogs problem
		// I'm not sure how to prevent the double cancel problem at this time. --cwp
		if( loc.hasCredentials()) return true;
		
		try{
			SwingUtilities.invokeAndWait(
				new Runnable(){
					@Override
					public void run(){
						// make the dialog if it doesn't exist
						if(m_dlg == null){
							Frame wParent = null;
							Window[] lTopWnds = Window.getOwnerlessWindows();
							for(Window wnd: lTopWnds){
								if(wnd.isVisible() && wnd instanceof Frame){
									wParent = (Frame)wnd;
									break;
								}
							}
							m_dlg = new CredentialsDialog(wParent);
						}
						String sTmp = loc.sDesc;
						if((sTmp == null)||(sTmp.isEmpty())) sTmp = loc.sLocId;
						m_dlg.runDialog(sTmp, loc.iconLogo, loc.sUser, loc.sPasswd);
					}
				}
			);
		} catch(InterruptedException ex) {
			LoggerManager.getLogger("das2.util").severe(ex.toString());
			return false;
                } catch (  InvocationTargetException ex){ 
			LoggerManager.getLogger("das2.util").severe(ex.toString());
			return false;
		}
		
		if(m_dlg.getReturn() == JOptionPane.CANCEL_OPTION) return false;
		loc.sUser = m_dlg.getUser();
		loc.sPasswd = m_dlg.getPasswd();
		
		return true;
	}
	
	/**
	 * get the credentials from the command line.
	 *
	 * @param loc
	 * @return
	 */
	protected synchronized boolean getCredentialsCmdLine(Location loc){

		Console c = System.console();

		if(c == null){
			throw new IllegalArgumentException("Console is not available to query username and password for " + loc.sDesc);
		}
		else{
			c.printf("%s\n", loc.sDesc);
			loc.sUser = c.readLine("Username: ");
			loc.sPasswd = new String(c.readPassword("Password: "));
			return true;
		}
	}
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * method for getting useful build and version information.
 * TODO: Splash should call this to get version, not the other way around.
 * @author jbf
 */
public class AboutUtil {

    private static final Logger logger= LoggerManager.getLogger("das2");
    
    /**
     * return HTML code describing the release version, Java version, build time, etc.
     * @return 
     */
    public static String getAboutHtml() {
        String dasVersion = Splash.getVersion();
        String javaVersion = System.getProperty("java.version"); // applet okay
        String buildTime = "???";
        java.net.URL buildURL = AboutUtil.class.getResource("/buildTime.txt");
        if (buildURL != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(buildURL.openStream()))) {
                buildTime = reader.readLine();
            } catch (IOException ex) {
                logger.log( Level.WARNING, ex.getMessage(), ex );
            }
        }
        String arch = System.getProperty("os.arch"); // applet okay
        DecimalFormat nf = new DecimalFormat("0.0");
        String mem = nf.format(Runtime.getRuntime().maxMemory() / (1024 * 1024));
        StringBuilder aboutContent = new StringBuilder("<html>" +
                //"<img src='images/dasSplash.gif'><br>" +
                "release version: " + dasVersion +
                "\n<br>build time: " + buildTime +
                "\n<br>java version: " + javaVersion +
                "\n<br>max memory (Mb): " + mem +
                "\n<br>arch: " + arch +
                "\n<br>\n");

        try {
            List<String> bis = getBuildInfos();
            for (String bi : bis) {
                aboutContent.append( "<br> " ).append(bi).append("\n");
            }
        } catch (IOException ex) {
            logger.log( Level.WARNING, ex.getMessage(), ex );
        }

        aboutContent.append( "</html>" );

        return aboutContent.toString();
    }

    /**
     * searches class path for META-INF/build.txt, returns nice strings
     * @return one line per jar
     * @throws IOException when META-INF/build.txt cannot be loaded.
     */
    public static List<String> getBuildInfos() throws IOException {
        ClassLoader loader= AboutUtil.class.getClassLoader();
        if ( loader==null ) loader= ClassLoader.getSystemClassLoader();
        Enumeration<URL> urls = loader.getResources("META-INF/build.txt");

        List<String> result = new ArrayList<>();

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();

            String jar = url.toString();

            int i = jar.indexOf(".jar");
            int i0 = jar.lastIndexOf("/", i - 1);

            String name;
            if (i != -1) {
                name = jar.substring(i0 + 1, i + 4);
            } else {
                name = jar.substring(6);
            }

            Properties props = new Properties();
            props.load(url.openStream());

            String cvsTagName = props.getProperty("build.tag");
            String version;
            if (cvsTagName == null || cvsTagName.length() <= 9) {
                version = "untagged_version";
            } else {
                version = cvsTagName.substring(6, cvsTagName.length() - 2);
            }

            String userName= " " + props.getProperty("build.user.name");
            if ( userName.trim().length()==0 ) userName= "";
            String ch= name + ": " + version + "(" + props.getProperty("build.timestamp") + userName + ")";
            if ( result.contains(ch) ) {
                
            } else {
                result.add(ch);
            }

        }
        return result;

    }

    /**
     * return the tag assigned to the class, but looking for "META-INF/build.txt" in the jar file.
     * @param clas
     * @return release tag or "(dev)" if the tag is not found.
     * @throws IOException 
     */
    public static String getReleaseTag( Class clas ) throws IOException {
        Properties props = new Properties();
        String clasFile= clas.getName().replaceAll("\\.","/")+".class";
        URL url = clas.getClassLoader().getResource( clasFile );
        if ( url!=null )  {
            String surl= url.toString();
            url= new URL( new URL( surl.substring(0,surl.length()-clasFile.length() ) ), "META-INF/build.txt" );
            props.load(url.openStream());
            String tagName = props.getProperty("build.tag");
            if ( tagName!=null && tagName.trim().length()>0 ) {
                return tagName;
            }
        }
        return "(dev)";
    }

    /**
     * evaluate if the current JRE version is at least a given level.  This
     * was introduced that 
     * @param neededVersion the Java version, such as "1.8.0_102"
     * @return true if the JRE is at least the version, or if the JRE cannot be parsed.
     * @throws java.text.ParseException if the JRE version reported doesn't match "(\\d+)\\.(\\d+)\\.\\d+\\_(\\d+)"
     */
    public static boolean isJreVersionAtLeast( String neededVersion ) throws ParseException {
        String javaVersion=  System.getProperty("java.version"); // applet okay
        Pattern p= Pattern.compile("(\\d+)\\.(\\d+)\\.\\d+\\_(\\d+)");
        Matcher mneeded= p.matcher(neededVersion);
        if ( !mneeded.matches() ) {
            throw new IllegalArgumentException("requested jre version must be of the form like 1.8.0_102 to match "+p.pattern());
        }
        Matcher mhave= p.matcher(javaVersion);
        if ( mhave.matches() ) {
            for ( int i=1; i<4; i++ ) {
                if ( mneeded.group(i).compareTo(mhave.group(i) ) > 0 ) {
                    return false;
                } else if ( mneeded.group(i).compareTo(mhave.group(i) ) < 0 )  {
                    return true;
                }
            }
        } else {
            throw new ParseException("JRE version is not identified properly: "+javaVersion,0 );
        }
        return neededVersion.equals(javaVersion);
    }
    
    /**
     * Identify the release version by looking a non-null build.tag.  It's expected
     * that the build script will insert build.tag into META-INF/build.txt
     * @return build tag, which should not contain spaces, or (dev) if no tag is found.
     * @throws java.io.IOException
     */
    public static String getReleaseTag() throws IOException {
        Enumeration<URL> urls = AboutUtil.class.getClassLoader().getResources("META-INF/build.txt");
        Properties props = new Properties();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            props.load(url.openStream());
            String tagName = props.getProperty("build.tag");
            if ( tagName!=null && tagName.trim().length()>0 ) {
                return tagName;
            }
        }
        return "(dev)";
    }
    
    /**
     * Identify the release by looking for build.jenkinsURL .  It's expected
     * that the build script will insert build.tag into META-INF/build.txt
     * @return build URL, or "" if one is not found.
     * @throws java.io.IOException
     */
    public static String getJenkinsURL() throws IOException {
        Enumeration<URL> urls = AboutUtil.class.getClassLoader().getResources("META-INF/build.txt");
        Properties props = new Properties();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            props.load(url.openStream());
            String tagName = props.getProperty("build.jenkinsURL");
            if ( tagName!=null && tagName.trim().length()>0 ) {
                return tagName;
            }
        }
        return "";
    }    
    
}

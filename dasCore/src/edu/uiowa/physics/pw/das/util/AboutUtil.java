/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.physics.pw.das.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * method for getting useful build and version information.
 * TODO: Splash should call this to get version, not the other way around.
 * @author jbf
 */
public class AboutUtil {

    public static String getAboutHtml() {
        String dasVersion = Splash.getVersion();
        String javaVersion = System.getProperty("java.version"); // applet okay
        String buildTime = "???";
        java.net.URL buildURL = AboutUtil.class.getResource("/buildTime.txt");
        if (buildURL != null) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(buildURL.openStream()));
                buildTime = reader.readLine();
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        String arch = System.getProperty("os.arch"); // applet okay
        DecimalFormat nf = new DecimalFormat("0.0");
        String mem = nf.format(Runtime.getRuntime().maxMemory() / (1024 * 1024));
        String aboutContent = "<html>" +
                //"<img src='images/dasSplash.gif'><br>" +
                "release version: " + dasVersion +
                "<br>build time: " + buildTime +
                "<br>java version: " + javaVersion +
                "<br>max memory (Mb): " + mem +
                "<br>arch: " + arch +
                "<br><br>";

        try {
            List<String> bis = getBuildInfos();
            for (int i = 0; i < bis.size(); i++) {
                aboutContent += "<br> " + bis.get(i) + "";
            }
        } catch (IOException ex) {

        }

        aboutContent += "</html>";

        return aboutContent;
    }

    /**
     * searches class path for META-INF/build.txt, returns nice strings
     * @return one line per jar
     */
    public static List<String> getBuildInfos() throws IOException {
        Enumeration<URL> urls = AboutUtil.class.getClassLoader().getResources("META-INF/build.txt");

        List<String> result = new ArrayList<String>();

        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();

            String jar = url.toString();

            System.err.println(jar);

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

            result.add(name + ": " + version + "(" + props.getProperty("build.timestamp") + " " + props.getProperty("build.user.name") + ")");

        }
        return result;

    }
}

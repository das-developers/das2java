/*
 * ActionTek.java
 *
 * Created on April 22, 2005, 1:45 PM
 */

package edu.uiowa.physics.pw.das.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author Jeremy
 */
public class ActionTek {
    
    URL root;
    String username;
    String password;
    HashMap stats;
    
    int wanPacketsSent;
    int wanPacketsReceived;
    
    /** Creates a new instance of ActionTek */
    public ActionTek( URL modemLocation, String username, String password ) {
        this.username= username;
        this.password= password;
        this.root= modemLocation;
        this.stats= new HashMap();
    }
    
    private URLConnection connect( URL url ) throws IOException {
        URLConnection uc= url.openConnection();
        
        String userPassword = username + ":" + password;
        String encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
        uc.setRequestProperty("Authorization", "Basic " + encoding );
        uc.connect();
        return uc;
    }
    
    public void readWanStatus() throws IOException {
        URL url;
        
        Pattern nameValPattern= Pattern.compile( "\\s*form\\.(.*)\\.value\\s*=\\s*\"(.*)\";\\s*" );
        
        try {
            url= new URL( root, "cgi-bin/webcm?getpage=..%2Fhtml%2Fstatus_wan_ppp.html&var%3Aconname=connection0&var%3Acontype=pppoa&var%3Asub=wan" );
            
            URLConnection uc= connect( url );
            InputStream in= uc.getInputStream();
            StringBuffer sb= new StringBuffer(3000);
            
            BufferedReader reader= new BufferedReader( new InputStreamReader( in ) );
            
            String line= reader.readLine();
            Matcher m;
            while ( line!=null ) {
                if ( ( m=nameValPattern.matcher(line) ).matches() ) {
                    String name= m.group(1);
                    String value= m.group(2);
                    stats.put( name, value );
                }
                line= reader.readLine();
            }
            reader.close();
        } catch ( MalformedURLException e ) {
            throw new RuntimeException(e);
        } catch ( UnsupportedEncodingException e ) {            
            throw new RuntimeException(e);
        } 
    }
    
    public long wanPacketsSent() {
        return Long.parseLong( (String)stats.get("sts2_pppsentpkt") );
    }
    
    public long wanPacketsReceived() {
        return Long.parseLong( (String)stats.get("sts2_ppprecvpkt") );
    }
    
    public static void main( String[] args ) throws Exception {
        try {
            args= new String[] { "jbf", "op)nsN0w" };
            ActionTek t= new ActionTek( new URL( "http://10.0.0.240/" ), args[0], args[1] );
            
            Probe p= Probe.newProbe("wan activity");
            
            t.readWanStatus();
            long r0= t.wanPacketsReceived();
            long s0= t.wanPacketsSent();
            long t0= System.currentTimeMillis();
            long sent, recv;
            
            while (true) {
                try {
                    long telapse= System.currentTimeMillis()-t0;
                    t.readWanStatus();
                    recv=  t.wanPacketsReceived();
                    p.add( "received", (double)( recv-r0 ) / (telapse+1) );
                    sent= t.wanPacketsSent();
                    p.add( "sent", (double)(sent-s0)/(telapse+1) );
                    t0+= telapse;
                    r0= recv;
                    s0= sent;
                    Thread.sleep(1000-telapse);
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
            
        } catch ( Exception e ) {
            e.printStackTrace();
            
        }
    }
    
}

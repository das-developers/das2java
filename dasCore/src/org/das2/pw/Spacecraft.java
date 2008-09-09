/*
 * Spacecraft.java
 *
 * Created on June 18, 2004, 9:59 AM
 */

package org.das2.pw;

import java.util.*;

/**
 *
 * @author  Jeremy
 */
public class Spacecraft {
    
    String id;
    
    /* useful for a set of identical spacecraft.  e.g. voyager, cluster */
    protected abstract static class GroupSpacecraft extends Spacecraft {
        int number;
        String group;
        static HashMap groups;
        protected GroupSpacecraft( String group, String id, int number ) {
            super(id);
            this.group= group;
            this.number= number;
            if ( groups==null ) {
                groups= new HashMap();
            }
            if ( groups.containsKey(group) ) {
                HashMap x= (HashMap)groups.get(group);
                x.put( new Integer(number),this );
            } else {
                HashMap x= new HashMap();
                x.put(new Integer(number),this);
                groups.put( group, x );
            }
        }
        protected static Spacecraft getByNumber( String group, int number ) {
            if ( groups.containsKey(group) ) {
                HashMap x= (HashMap)(groups.get(group));
                return (Spacecraft)x.get(new Integer(number));
            } else {
                throw new IllegalArgumentException( "No such group: "+group );
            }
        }
        protected int getGroupNumber() {
            return this.number;
        }
        
    }
    
    private Spacecraft( String id ) {
        this.id= id;
    }
    
    public static Spacecraft voyager1= new Spacecraft( "Voyager 1" );
    public static Spacecraft voyager2= new Spacecraft( "Voyager 2" );
    public static ClusterSpacecraft clusterRumba= new ClusterSpacecraft( "Rumba", 1 );
    public static ClusterSpacecraft clusterSalsa= new ClusterSpacecraft( "Salsa", 2 );
    public static ClusterSpacecraft clusterSamba= new ClusterSpacecraft( "Samba", 3 );
    public static ClusterSpacecraft clusterTango= new ClusterSpacecraft( "Tango", 4 );

    public static void main( String[]args ) {        
        System.out.println( ClusterSpacecraft.getByEsaNumber(2) );
        System.out.println( clusterSalsa );
    }
    
}

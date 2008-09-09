package org.das2.pw;

import java.awt.*;

public class ClusterSpacecraft extends Spacecraft.GroupSpacecraft {        
    
    protected ClusterSpacecraft(String id, int number) {
        super( "Cluster2", id, number );
    }
    
    /**
     * returns the Esa Number 1,2,3 or 4 for the Cluster Spacecraft 
     */
    public int getEsaNumber() {
        return number;
    }
    
    
    /**
     * returns the Cluster Spacecraft for the Esa Number 1,2,3 or 4 
     */
    public static ClusterSpacecraft getByEsaNumber(int number) {
        return (ClusterSpacecraft)getByNumber("Cluster2",number);
    }
    
    /**
     * returns the Cluster Spacecraft for the wideband status byteEsa 4,5,6,7     
     */
    public static ClusterSpacecraft getByWbdStatusByte( int number ) {      
        if (true) throw new IllegalArgumentException("looks like there's a bug here");
        final int[] esaFromStatus= new int[] { 0, 0, 0, 0, 2, 3, 4, 1 };        
        return getByEsaNumber( esaFromStatus[number] );
    }
    
    public int getInstrumentNumber() {
        final int instFromEsa[] = new int[] { 0, 9, 6, 7, 8 };
        return instFromEsa[ getEsaNumber() ];
    }
        
    public int getDsnNumber() {
        final int dsnFromEsa[] = new int[] { 0, 183, 185, 194, 196 };
        return dsnFromEsa[ getEsaNumber() ];
    }
    
    public String getName() {
        final String[] nameFromEsa= new String[] { "", "Rumba", "Salsa", "Samba", "Tango" };
        return nameFromEsa[ getEsaNumber() ];
    }
    
    public Color getColor() {
        final Color[] colorFromEsa= new Color[] { null, Color.BLACK, Color.RED, Color.GREEN, Color.MAGENTA };
        return colorFromEsa[ getEsaNumber() ];
    }
        
    public String toString() {
        return "Cluster " + getEsaNumber();
    }
}


package org.das2.dataset;

import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import org.das2.datum.UnitsUtil;
import java.util.*;

public class NearestNeighborTableDataSet implements TableDataSet {
    
    TableDataSet source;
	 
	 Map m_override;  /* Have to keep track of your overrides for getProperty */
    
    int[] imap;
    
    int[][] jmap;
    
    int[] itableMap;
    
    RebinDescriptor ddX;
    
    RebinDescriptor ddY;
	     
    NearestNeighborTableDataSet( 
		 TableDataSet source, RebinDescriptor ddX, RebinDescriptor ddY, Map override
	 ) {
        imap= new int[ddX.numberOfBins()];
        if ( ddY==null ) {
            if ( source.tableCount()>1 ) {
                throw new IllegalArgumentException();
            }
            jmap= new int[source.tableCount()][source.getYLength(0)];
        } else {
            jmap= new int[source.tableCount()][ddY.numberOfBins()];
        }
        itableMap= new int[ddX.numberOfBins()];
        
        this.ddX= ddX;
        this.ddY= ddY;
        this.source= source;
		  
		  if(override == null)
				m_override = new HashMap<>();
		  else
			  m_override = override;
        
        if ( source.getXLength()==0 ) {
            for ( int i=0; i<imap.length; i++ ) imap[i]= -1;
            
        } else {
			  
			  // Cascade getting the interpolation widths as follows:
			  //  
			  //  xOverRide = null :        Use xTagWidth
			  //  xOverRide > xTagWidth:    Use xOverRide else use xTagWidth
			  
			  Datum xTagWidth= (Datum)source.getProperty(DataSet.PROPERTY_X_TAG_WIDTH);
			  if ( xTagWidth==null ) xTagWidth= DataSetUtil.guessXTagWidth(source);
			  
			  Datum xOverRideWidth = (Datum)m_override.get(DataSet.PROPERTY_X_TAG_WIDTH);
			  if(xOverRideWidth != null){
				   xTagWidth = xTagWidth.compareTo(xOverRideWidth) < 0 ? 
						         xOverRideWidth : xTagWidth ;
			  }
			  
			  Datum yTagWidth = (Datum)m_override.get(DataSet.PROPERTY_Y_TAG_WIDTH);
			  if(yTagWidth == null)
				  yTagWidth = (Datum)source.getProperty(DataSet.PROPERTY_Y_TAG_WIDTH);
			  
            if ( yTagWidth==null ) yTagWidth= TableUtil.guessYTagWidth(source);
            
            DatumVector xx= ddX.binCentersDV();
            double[] yy;
            if ( ddY==null ) {
                yy= TableUtil.getYTagArrayDouble( source, 0, source.getYUnits() );
            } else {
                yy= ddY.binCenters();
            }
            
            int itable0=-1;
            int guess= 0;
            for ( int i=0; i<imap.length; i++ ) {
                imap[i]= DataSetUtil.closestColumn( source, xx.get(i), guess );
                guess= imap[i];
                Datum xclose= source.getXTagDatum(imap[i]);
                Units xunits= xTagWidth.getUnits();
                
                if ( Math.abs(xclose.subtract( xx.get(i) ).doubleValue(xunits) ) > xTagWidth.doubleValue(xunits)/1.90 ) {
                    imap[i]=-1;
                } else {
                    int itable= source.tableOfIndex(imap[i]);
                    itableMap[i]= itable;
                    if ( itable0!=itable ) {
                        if ( ddY==null ) {
                            for ( int j=0; j<jmap[itable].length; j++ ) {
                                jmap[itable][j]= j;
                            }
                        } else {
                            for ( int j=0; j<jmap[itable].length; j++ ) {
                                jmap[itable][j]= TableUtil.closestRow(source,itable,yy[j], ddY.getUnits());
                                Units yunits= yTagWidth.getUnits();
                                if ( UnitsUtil.isRatiometric(yunits) ) {
                                    double yclose= source.getYTagDouble(itable, jmap[itable][j], ddY.getUnits() );
                                    if ( Math.abs( Math.log( yy[j] / yclose ) ) > yTagWidth.doubleValue(Units.logERatio)/1.90  ) jmap[itable][j]=-1;
                                } else {
                                    Datum yclose= source.getYTagDatum( itable, jmap[itable][j] );
                                    if ( Math.abs( yclose.subtract(yy[j],ddY.getUnits()).doubleValue(yunits)) > yTagWidth.doubleValue(yunits)/1.90 ) {
                                        jmap[itable][j]= -1;
                                    }
                                }
                            }
                        }
                        itable0= itable;
                    }
                }
            }
        }
    }
    
	 @Override
    public Datum getDatum(int i, int j) {
        if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
            return source.getDatum(imap[i], jmap[itableMap[i]][j]);
        } else {
            return source.getZUnits().createDatum(source.getZUnits().getFillDouble());
        }
    }
    
	 @Override
    public double getDouble(int i, int j, Units units) {
        try {
            if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
                return source.getDouble(imap[i], jmap[itableMap[i]][j], units);
            } else {
                return source.getZUnits().getFillDouble();
            }
        } catch ( ArrayIndexOutOfBoundsException e ) {
            System.err.println("here: "+e);
            throw new RuntimeException(e);
        }
    }
    
	 @Override
    public int getInt(int i, int j, Units units) {
        if ( imap[i]!=-1 && jmap[itableMap[i]][j]!=-1 ) {
            return source.getInt(imap[i], jmap[itableMap[i]][j],units);
        } else {
            return source.getZUnits().getFillDatum().intValue(source.getZUnits());
        }
    }
    
	 @Override
    public DataSet getPlanarView(String planeID) {
        TableDataSet ds = (TableDataSet)source.getPlanarView(planeID);
        if (ds != null) {
            return new NearestNeighborTableDataSet(ds,ddX,ddY, null);
        } else {
            return null;
        }
    }
    
	 @Override
    public String[] getPlaneIds() {
        return source.getPlaneIds();
    }
    
	 @Override
    public Object getProperty(String name) {
		Object ret = m_override.get(name);
		if(ret != null) return source.getProperty(name);
		return ret;
    }
    
	 @Override
    public Map getProperties() {
		 if(m_override.isEmpty()) return source.getProperties();
		 
		 // Fun, now we get to merge
		 HashMap<Object, Object> mRet = new HashMap<>(m_override);
		 Map srcProps = source.getProperties();
		 for(Object key: srcProps.keySet()){
			 mRet.put(key, srcProps.get(key));
		 }
		 return mRet;
    }
    
	 @Override
    public int getXLength() {
        return imap.length;
    }
    
	 @Override
    public VectorDataSet getXSlice(int i) {
        return new XSliceDataSet(this,i);
    }
    
	 @Override
    public VectorDataSet getYSlice(int j, int table) {
        return new YSliceDataSet(this, j, table);
    }
    
	 @Override
    public Datum getXTagDatum(int i) {
        return ddX.getUnits().createDatum(getXTagDouble(i,ddX.getUnits()));
    }
    
	 @Override
    public double getXTagDouble(int i, Units units) {
        return ddX.binCenter(i,units);
    }
    
	 @Override
    public int getXTagInt(int i, Units units) {
        return (int)getXTagDouble(i,units);
    }
    
	 @Override
    public Units getXUnits() {
        return ddX.getUnits();
    }
    
	 @Override
    public int getYLength(int table) {
        if ( ddY==null ) {
            return source.getYLength(table);
        } else {
            return ddY.numberOfBins();
        }
    }
    
	 @Override
    public Datum getYTagDatum(int table, int j) {
        if ( ddY==null ) {
            return source.getYTagDatum( table, j );
        } else {
            return ddY.getUnits().createDatum(getYTagDouble(table,j,ddY.getUnits()));
        }
    }
    
	 @Override
    public double getYTagDouble(int table, int j, Units units) {
        if ( ddY==null ) {
            return source.getYTagDouble( table, j, units );
        } else {
            return ddY.binCenter(j,units);
        }
    }
    
	 @Override
    public int getYTagInt(int table, int j, Units units) {
        return (int)getYTagDouble( table, j, units);
    }
    
	 @Override
    public Units getYUnits() {
        if ( ddY==null ) {
            return source.getYUnits();
        } else {
            return ddY.getUnits();
        }
    }
    
	 @Override
    public Units getZUnits() {
        return source.getZUnits();
    }
    
	 @Override
    public int tableCount() {
        return 1;
    }
    
	 @Override
    public int tableEnd(int table) {
        return ddX.numberOfBins();
    }
    
	 @Override
    public int tableOfIndex(int i) {
        return 0;
    }
    
	 @Override
    public int tableStart(int table) {
        return 0;
    }
    
	 @Override
    public String toString() {
        return "NearestNeighborTableDataSet " + TableUtil.toString(this);
    }
    
	 @Override
    public double[] getDoubleScan(int i, Units units) {
        int yLength = getYLength(tableOfIndex(i));
        double[] array = new double[yLength];
        for (int j = 0; j < yLength; j++) {
            array[j] = getDouble(i, j, units);
        }
        return array;
    }
    
	 @Override
    public DatumVector getScan(int i) {
        Units zUnits = getZUnits();
        return DatumVector.newDatumVector(getDoubleScan(i, zUnits), zUnits);
    }
    
	 @Override
    public DatumVector getYTags(int table) {
        double[] tags = new double[getYLength(table)];
        Units yUnits = getYUnits();
        for (int j = 0; j < tags.length; j++) {
            tags[j] = getYTagDouble(table, j, yUnits);
        }
        return DatumVector.newDatumVector(tags, yUnits);
    }

	 @Override
    public Object getProperty(int table, String name) {
        return getProperty(name);
    }
    
}


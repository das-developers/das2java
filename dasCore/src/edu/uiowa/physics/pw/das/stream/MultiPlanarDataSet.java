/* File: MultiPlanarDataSet.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.client.NoSuchDataSetException;
import edu.uiowa.physics.pw.das.client.StandardDataStreamSource;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.util.StreamTool.DelimeterNotFoundException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;


import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author  jbf
 */
public class MultiPlanarDataSet {
    
    private ArrayList dataSets;
    StreamDescriptor streamDescriptor=null;
    
    public MultiPlanarDataSet() {
        dataSets= new ArrayList();
    }
    
    public String[] getDataSetNames() {
        String[] result= new String[dataSets.size()];
        for (int i=0; i<dataSets.size(); i++) {
            result[i]= ((DataSet)dataSets.get(i)).getName();
        }
        return result;
    }
    
    public DataSet getDataSet(String name) throws NoSuchDataSetException {
        DataSet result=null;
        String[] dataSetNames= getDataSetNames();
        for (int i=0; i<dataSets.size(); i++) {
            if (name.equals(dataSetNames[i])) {
                result= (DataSet)dataSets.get(i);
            }
        }
        if ( result==null ) {
            throw new NoSuchDataSetException("MultiPlanarDataSet does not contain '"+name+"'");
        }
        
        return result;
        
    }
    
    
    public void addDataSet(DataSet dataSet) {
        dataSets.add(dataSet);
    }
    
    
    private ArrayList createSkeleton( String header )
    //  create the mpds object and objects for datasets within.
    throws DasException {
        
        ArrayList skeletonArrayList= new ArrayList();
        
        DocumentBuilder builder;
        try {
            builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch ( ParserConfigurationException ex ) {
            throw new IllegalStateException(ex.getMessage());
        }
        Document document=null;
        try {
            document= builder.parse(new StringBufferInputStream(header));
        } catch ( SAXException ex ) {
            ex.printStackTrace();
            DasIOException e= new DasIOException(ex.getMessage());
            e.initCause(ex);
            throw e;
        } catch ( IOException ex) {
            throw new DasIOException(ex);
        }
        
        Node docNode= document.getDocumentElement();
        streamDescriptor= new StreamDescriptor(docNode);
        
        if (!docNode.getNodeName().equals("dataSet")) {
            docNode.toString();
        } else {
            NamedNodeMap attrNode= docNode.getAttributes();
            
            NodeList children= docNode.getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                Node node= children.item(i);
                
                if ( node.getNodeType()==Node.ELEMENT_NODE ) {
                    String name= node.getNodeName();
                    if ( name.equals("X")) {
                        skeletonArrayList.add(new StreamXDescriptor(node,streamDescriptor));
                    } else if ( name.equals("YScan")) {
                        StreamYScanDescriptor d= new StreamYScanDescriptor(node,streamDescriptor);
                        skeletonArrayList.add(d);
                    } else if ( name.equals("MultiY")) {
                        StreamMultiYDescriptor d= new StreamMultiYDescriptor(node,streamDescriptor);
                        skeletonArrayList.add(d);
                    } else {
                        node.toString();
                    }
                } else {
                    // Text nodes, etc.
                }
            }
        }
        
        return skeletonArrayList;
    }
    
    
    public void read( InputStream in ) throws DasException {
        
        MultiPlanarDataSet result= this;
        
        PushbackInputStream pin= new PushbackInputStream(in);
        ArrayList list= new ArrayList();
        
        String header=null;
        
        int totalBytesRead=0;
        
        try {
            int bytesRead=0;
            int offset=0;
            
            
            String streamPrefix= "das2\177\177";
            
            try {
                byte[] x= StreamTool.advanceTo(pin,streamPrefix.getBytes());
            } catch ( DelimeterNotFoundException v ) {
                throw new DasStreamFormatException("Stream does not appear to be a das2 stream");
            }
            
            
            byte[] iheader;
            try {
                iheader= StreamTool.advanceTo(pin,"\177\177".getBytes());
            } catch ( DelimeterNotFoundException v ) {
                throw new DasStreamFormatException("Stream does not appear to be a das2 stream");
            }
            
            header= new String(iheader);
            
        } catch ( IOException e ) {
            throw new DasIOException(e.getMessage());
            
        }
        
        ArrayList skeleton= createSkeleton( header );
        
        if ( streamDescriptor.isCompressed() ) {
            try {
                in= new GZIPInputStream(in);
            } catch ( IOException ex) {
                throw new DasIOException("Error in gzip input stream");
            }
        }
        
        boolean notDone= true;
        int bytesPerRecord= 0;
        for ( int i=0; i<skeleton.size(); i++ ) {
            bytesPerRecord+= ((SkeletonDescriptor)skeleton.get(i)).getSizeBytes();
        }
        
        byte[] buf= new byte[bytesPerRecord];
        
        long timer0= System.currentTimeMillis();
        
        int irec=0;
        while (notDone) {
            int off=0;
            int bytesRead=0;
            while ( bytesRead!=-1 && off<bytesPerRecord) {
                try {
                    bytesRead= in.read(buf,off,bytesPerRecord-off);
                    if (bytesRead!=-1) {
                        off+= bytesRead;
                        totalBytesRead+= bytesRead;
                    }
                } catch ( IOException e ) {
                    throw new DasIOException(e.getMessage());
                }
            }
            if (bytesRead==-1) {
                notDone= false;
                try { in.close(); } catch ( IOException e ) { throw new DasIOException(e);}
            } else {
                irec++;
                int off1=0;
                for ( int i=0; i<skeleton.size(); i++) {
                    SkeletonDescriptor d= (SkeletonDescriptor)skeleton.get(i);
                    int len= d.getSizeBytes();
                    d.read(buf,off1,len);
                    off1+= len;
                }
            }
        }
        createDataSets(skeleton);
    }
    
    
    public DataSet getPrimaryDataSet() {
        String[] dataSetNames= getDataSetNames();
        String primaryDataSetName="";
        try {
            return getDataSet(primaryDataSetName);
        } catch ( NoSuchDataSetException ex ) {
            try {
                return getDataSet(dataSetNames[0]);
            } catch ( NoSuchDataSetException ex2 ) {
                throw new IllegalStateException("MultiPlanarDataSet fails to find primaryDataSet");
            }
        }
    }
    
    public DataSet getWeightsDataSet(String name) throws NoSuchDataSetException {
        if ( name.equals("") ) {
            return getDataSet("weights");
        } else {
            return getDataSet(name+".weights");
        }
    }
    
    public DataSet getPeaksDataSet(String name) throws NoSuchDataSetException {
        if ( name.equals("") ) {
            return getDataSet("peaks");
        } else {
            return getDataSet(name+".peaks");
        }
    }
    
    public DataSet getErrorsDataSet(String name) throws NoSuchDataSetException {
        if ( name.equals("") ) {
            return getDataSet("errors");
        } else {
            return getDataSet(name+".errors");
        }
    }
    
    public DataSet getVarianceDataSet(String name) throws NoSuchDataSetException {
        if ( name.equals("") ) {
            return getDataSet("variance");
        } else {
            return getDataSet(name+".variance");
        }
    }
    
    private void createDataSets(ArrayList skeleton) {
        int nrec= ((SkeletonDescriptor)skeleton.get(0)).getNumRecords();
        Datum xValues[]=null;
        Units xUnits=null;
        for (int i=0; i<skeleton.size(); i++) {
            SkeletonDescriptor d= (SkeletonDescriptor)skeleton.get(i);
            if ( d instanceof StreamXDescriptor ) {
                xValues= ((StreamXDescriptor)d).getValues();
            } else {
                addDataSet( d.asDataSet(xValues) );
            }
        }
        DataSet ds= getPrimaryDataSet();
        String name= ds.getName();
        if ( ds instanceof XTaggedYScanDataSet ) {
            XTaggedYScanDataSet xtysds= (XTaggedYScanDataSet) ds;
            XTaggedYScanDataSet wds= null;
            try {
                wds= (XTaggedYScanDataSet) this.getWeightsDataSet(name);
                xtysds.setWeights(wds.data);
            } catch ( NoSuchDataSetException ex ) { 
            }
            try {
                XTaggedYScanDataSet pds= (XTaggedYScanDataSet) this.getPeaksDataSet(name);
                xtysds.setPeaks(pds.data);
            } catch ( NoSuchDataSetException ex ) {
            }
        }                        
    }
    
    private static boolean test1() {
        try {
            //URL url= new URL("http://www-pw.physics.uiowa.edu/~jbf/validDas2Stream");
            URL url= new URL("http://www-pw.physics.uiowa.edu/~jbf/compressedDas2Stream");
            InputStream in= url.openStream();
            PushbackInputStream pin= new PushbackInputStream(in,100);
            System.out.println(isMultiPlanarDataSetStream(pin));
            MultiPlanarDataSet mpds= new MultiPlanarDataSet();
            mpds.read(pin);
            String[] names= mpds.getDataSetNames();
            for (int i=0; i<names.length; i++) {
                System.out.println(names[i]);
            }
            XTaggedYScanDataSet ds= (XTaggedYScanDataSet)mpds.getPrimaryDataSet();
            //try {
            //    ds.dumpToStream(new FileOutputStream("/home/jbf/x.dat"));
            //} catch ( Exception ex ) {
            //    System.out.println(ex);
            //}
            ds.visualize();
            return true;
        } catch ( MalformedURLException ex ) {
            System.out.println(ex);
            return false;
        } catch ( IOException ex ) {
            System.out.println(ex);
            return false;
        } catch ( DasException ex ) {
            System.out.println(ex);
            return false;
        }
    }
    
    private static boolean test2() {
        try {
            XTaggedYScanDataSetDescriptor dsd= (XTaggedYScanDataSetDescriptor)DataSetDescriptor.create("http://www-pw.physics.uiowa.edu/das/dasServerTest1?das2/demo/sa-4s-sd_jbf");
            StandardDataStreamSource sds= dsd.getStandardDataStreamSource();
            InputStream in;
            in= sds.getReducedInputStream(dsd,TimeUtil.createValid("1979-3-1"), TimeUtil.createValid("1979-3-2"), Datum.create(120.,Units.seconds));
            MultiPlanarDataSet mpds= new MultiPlanarDataSet();
            mpds.read(in);                       
            XTaggedYScanDataSet ds= (XTaggedYScanDataSet)mpds.getPrimaryDataSet();
            ds.visualize();

            RebinDescriptor ddX= new RebinDescriptor( ds.getStartTime(),
              ds.getEndTime(),100, false );
            XTaggedYScanDataSet ds2= ds.binAverageX(ddX);
            ds2.visualize();
            try {
                ds.dumpToStream(new FileOutputStream("/home/jbf/xxx.dat"));
                ds2.dumpToStream(new FileOutputStream("/home/jbf/xxx2.dat"));
            } catch ( Exception ex) {
                ex.printStackTrace();
            }
            
            System.out.println(""+ds);            
            return true;
        } catch ( DasException ex ) {
            System.out.println(""+ex);
            return false;
        }
    }
    
    public static void main(String[] args) {
        System.out.println(test1());
        //System.out.println(test2());
    }
    
    public static boolean isMultiPlanarDataSetStream(PushbackInputStream in) throws DasIOException {
        byte[] streamPrefix= "das2\177\177".getBytes();
        int len= streamPrefix.length;
        
        byte[] buffer= new byte[len];
        int offset=0;
        int bytesRead=0;
        
        try {
            while (offset<len && bytesRead!=-1) {
                bytesRead= in.read(buffer,offset,len-offset);
                offset+=bytesRead;
            }
            boolean match=true;
            if (offset==len) {
                for (int ii=0; ii<len; ii++) if ( streamPrefix[ii]!=buffer[ii] ) match=false;
            } else {
                match=false;
            }
            in.unread(buffer);
            return match;
        } catch ( IOException ex ) {
            throw new DasIOException( ex.getMessage() );
        }
        
    }
    
}

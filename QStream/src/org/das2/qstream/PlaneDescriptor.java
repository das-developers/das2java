/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

import java.util.Arrays;
import org.das2.datum.Units;
import org.das2.qds.QDataSet;
import org.das2.qds.util.DataSetBuilder;
import org.w3c.dom.Element;

/**
 * Describes one of the bundled elements, for example one QDataSet,
 * within a packet.
 * @author jbf
 */
public class PlaneDescriptor implements Cloneable {

    private TransferType type;
    private Element domElement;
    private int rank;
    private int[] qube;
    private int elements;// for each record
    private QDataSet ds;// builder when we are reading in.
    private Units units; // store the units here, to support NominalUnits and to avoid name clashes when "default" is used for the name.
    private DataSetBuilder builder;// dataset that we are writing out.
    private String name;// builder when we are reading in.

    void setDomElement(Element packetElement) {
        domElement = packetElement;
    }

    Element getDomElement() {
        return domElement;
    }

    int sizeBytes() {
        return elements * type.sizeBytes();
    }

    @Override
    public String toString() {
        return "plane " + getName();
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int[] getQube() {
        return Arrays.copyOf(qube,qube.length);
    }

    public void setQube(int[] qube) {
        this.qube = Arrays.copyOf(qube,qube.length);
        int ele1 = 1;

        for (int i = 0; i < getQube().length; i++) {
            ele1 *= getQube()[i];
        }
        this.elements = ele1;
    }

    public int getElements() {
        return elements;
    }

    /**
     * return the dataset encoded in this plane.  Note this is just a convenient place
     * to store a dataset, and someone else must calculate it and store it with setDs.
     * @return
     */
    public QDataSet getDs( ) {
        return ds;
    }

    public void setDs(QDataSet ds) {
        this.ds = ds;
    }

    public Units getUnits() {
        return units;
    }

    public void setUnits(Units units) {
        this.units = units;
    }

    public DataSetBuilder getBuilder() {
        return builder;
    }

    public void setBuilder(DataSetBuilder builder) {
        this.builder = builder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransferType getType() {
        return type;
    }

    public void setType(TransferType type) {
        this.type = type;
    }

    /**
     * support for bundle datasets.
     */
    private String[] bundles= null;
    void setBundles(String[] sbundles) {
        this.bundles= Arrays.copyOf(sbundles,sbundles.length);
    }

    /**
     * get a copy of a list of all the names of the bundled datasets.
     * @return 
     */
    public String[] getBundles() {
        return Arrays.copyOf(this.bundles,this.bundles.length);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone(); 
        //TODO: verify this is a deep enough copy.
    }
    
    
}

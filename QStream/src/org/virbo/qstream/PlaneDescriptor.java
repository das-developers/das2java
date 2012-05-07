/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.qstream;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsutil.DataSetBuilder;
import org.w3c.dom.Element;

/**
 *
 * @author jbf
 */
public class PlaneDescriptor {

    private TransferType type;
    private Element domElement;
    private int rank;
    private int[] qube;
    private int elements;// for each record
    private QDataSet ds;// builder when we are reading in.
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
        return qube;
    }

    public void setQube(int[] qube) {
        this.qube = qube;
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
     * return the dataset encoded in this plane
     * @return
     */
    public QDataSet getDs( ) {
        return ds;
    }

    public void setDs(QDataSet ds) {
        this.ds = ds;
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
    String[] bundles= null;
    void setBundles(String[] sbundles) {
        this.bundles= sbundles;
    }

    public String[] getBundles() {
        return this.bundles;
    }
}

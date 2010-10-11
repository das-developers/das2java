/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.stream;

/**
 * these objects are written as beans with read/write properties,
 * so that they can be used for both formatting and parsing 
 * streams.
 * @author jbf
 */
public class StreamDescriptor {
    private String compression="none";
    private String syntax="xml";
    private String schema="";
    private boolean ignoreTrailingWhitespace= false;
    private PacketDescriptorFactory factory;

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public boolean isIgnoreTrailingWhitespace() {
        return ignoreTrailingWhitespace;
    }

    public void setIgnoreTrailingWhitespace(boolean ignoreTrailingWhitespace) {
        this.ignoreTrailingWhitespace = ignoreTrailingWhitespace;
    }

    public PacketDescriptorFactory getFactory() {
        return factory;
    }

    public void setFactory(PacketDescriptorFactory factory) {
        this.factory = factory;
    }
    
}

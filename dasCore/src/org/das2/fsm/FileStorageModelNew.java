/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.fsm;

import java.io.File;
import org.das2.datum.CacheTag;
import org.das2.datum.DatumRange;
import org.das2.datum.TimeParser;
import org.das2.util.filesystem.FileSystem;

/**
 * Preserve the old name for Jython applications.
 * @author jbf
 * @deprecated use FileStorageModelNew
 */
public final class FileStorageModelNew {
    
    private FileStorageModelNew() {
        throw new UnsupportedOperationException("use static create methods");
    }
    
    public static FileStorageModel create( FileSystem root, String template, String fieldName, TimeParser.FieldHandler fieldHandler ) {
        return FileStorageModel.create( root, template, fieldName, fieldHandler );
    }
    public static FileStorageModel create( FileSystem root, String template ) {
        return FileStorageModel.create( root, template );
    }
    
    public static CacheTag getCacheTagFor( FileStorageModel fsm, DatumRange range, String[] names ) {
        return FileStorageModel.getCacheTagFor(fsm, range, names);
    }
    
    public static CacheTag getCacheTagFor( FileStorageModel fsm, DatumRange range, File[] files ) {
        return FileStorageModel.getCacheTagFor( fsm, range, files );
    }
       
    public static int splitIndex(String surl) { 
        return FileStorageModel.splitIndex(surl);
    }
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util.filesystem;

import java.io.File;
import org.das2.fsm.FileStorageModel;
import org.das2.util.filesystem.FileSystem;

/**
 *
 * @author jbf
 */
public class TestVersioning {
    public static void main( String[] args ) throws Exception {
        System.err.println("---");
        FileSystem fs= FileSystem.create( new File("/tmp/").toURI() );
        FileStorageModel fsm= FileStorageModel.create( fs, "data_$Y_$m_$d_v$(v,sep).qds" );
        File[] ff= fsm.getBestFilesFor(null);
        for ( File f: ff ) {
            System.err.println(f);
        }
        System.err.println("---");
        fsm= FileStorageModel.create( fs, "data_$Y_$m_$d_v$v.qds" );
        ff= fsm.getBestFilesFor(null);
        for ( File f: ff ) {
            System.err.println(f);
        }
    }
}
